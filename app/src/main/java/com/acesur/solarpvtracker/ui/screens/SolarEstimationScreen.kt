package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.data.LocationHelper
import com.acesur.solarpvtracker.data.UserLocation
import com.acesur.solarpvtracker.solar.PVOutput
import com.acesur.solarpvtracker.solar.SolarCalculator
import com.acesur.solarpvtracker.solar.SolarRadiation
import com.acesur.solarpvtracker.ui.components.StatCard
import com.acesur.solarpvtracker.ui.theme.SkyBlue
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import com.acesur.solarpvtracker.ui.theme.SunYellow
import com.acesur.solarpvtracker.data.PVGISManager
import com.acesur.solarpvtracker.ui.theme.TechBlue
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarEstimationScreen(
    onNavigateBack: () -> Unit,
    userLocation: UserLocation?,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { com.acesur.solarpvtracker.data.PreferencesManager(context) }
    // LocationHelper managed by MainActivity
    val solarCalculator = remember { SolarCalculator() }
    val pvgisManager = remember { PVGISManager(context, preferencesManager) }
    
    // Alias for compatibility
    val location = userLocation
    
    var isLoadingLocation by remember { mutableStateOf(false) } // Keep local loading state for UI feedback
    
    // Panel settings
    var panelWattage by remember { mutableStateOf("400") }
    var panelCount by remember { mutableStateOf("10") }
    var efficiency by remember { mutableStateOf("20") }
    
    // Calculation results
    var radiation by remember { mutableStateOf<SolarRadiation?>(null) }
    var pvOutput by remember { mutableStateOf<PVOutput?>(null) }
    var pvgisData by remember { mutableStateOf<JSONObject?>(null) }
    var isPvgisUsed by remember { mutableStateOf(false) }
    
    // Define calculate function
    fun doCalculation() {
        location?.let { loc ->
            val wattage = panelWattage.toIntOrNull() ?: 400
            val count = panelCount.toIntOrNull() ?: 1
            val eff = (efficiency.toFloatOrNull() ?: 20f) / 100f
            
            // Local estimation (immediate)
            radiation = solarCalculator.estimateSolarRadiation(loc.latitude)
            
            // Try to get PVGIS estimation (async)
            scope.launch {
                val pvgisResult = pvgisManager.getFullEstimation(loc.latitude, loc.longitude)
                if (pvgisResult != null) {
                    pvgisData = pvgisResult
                    isPvgisUsed = true
                    
                    // Parse PVGIS monthly data
                    try {
                        val monthlyArray = pvgisResult.getJSONObject("outputs")
                            .getJSONObject("monthly")
                            .getJSONArray("fixed")
                        
                        val monthlyBreakdown = mutableListOf<Double>()
                        var totalYearly = 0.0
                        
                        // PVGIS returns energy for 1kWp system. We scale it.
                        val scalingFactor = (wattage * count) / 1000.0
                        
                        for (i in 0 until monthlyArray.length()) {
                            val monthData = monthlyArray.getJSONObject(i)
                            val energyMonth = monthData.getDouble("E_m") * scalingFactor
                            monthlyBreakdown.add(energyMonth)
                            totalYearly += energyMonth
                        }
                        
                        pvOutput = PVOutput(
                            dailyOutput = totalYearly / 365.0,
                            monthlyOutput = totalYearly / 12.0,
                            yearlyOutput = totalYearly,
                            monthlyBreakdown = monthlyBreakdown
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback to local if parsing fails 
                        pvOutput = solarCalculator.calculatePVOutput(loc.latitude, wattage, count, eff)
                    }
                } else {
                    isPvgisUsed = false
                    pvOutput = solarCalculator.calculatePVOutput(loc.latitude, wattage, count, eff)
                }
            }
        }
    }
    
    // Trigger calculation when location changes
    LaunchedEffect(location) {
        if (location != null) {
            doCalculation()
        } else {
            onRefreshLocation()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            onRefreshLocation()
        }
    }
    
    val useGps by preferencesManager.useGps.collectAsState(initial = true)
    
    LaunchedEffect(useGps) {
        if (!useGps) {
            onRefreshLocation()
        } else {
             // Check permission (Context based)
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onRefreshLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
             // If no permission, MainActivity logic or onRefreshLocation handles it? 
             // Actually onRefreshLocation checks permission. If missing, UI needs to show button to request.
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.solar_estimation)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // Panel Settings
            Text(
                text = stringResource(R.string.panel_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = panelWattage,
                    onValueChange = { 
                        panelWattage = it
                        doCalculation()
                    },
                    label = { Text(stringResource(R.string.wattage), style = MaterialTheme.typography.bodySmall) },
                    suffix = { Text("W", style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = panelCount,
                    onValueChange = { 
                        panelCount = it
                        doCalculation()
                    },
                    label = { Text(stringResource(R.string.panel_count), style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )

                OutlinedTextField(
                    value = efficiency,
                    onValueChange = { 
                        efficiency = it
                        doCalculation()
                    },
                    label = { Text(stringResource(R.string.efficiency), style = MaterialTheme.typography.bodySmall) },
                    suffix = { Text("%", style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Monthly Output Chart
            if (pvOutput?.monthlyBreakdown?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.monthly_production_chart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                MonthlyEnergyChart(
                    monthlyProduction = pvOutput!!.monthlyBreakdown,
                    isPvgis = isPvgisUsed
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Solar Radiation Results
            if (radiation != null) {
                Text(
                    text = stringResource(R.string.solar_radiation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.daily),
                        value = String.format("%.1f", radiation?.dailyIrradiance),
                        unit = "kWh/m²",
                        icon = Icons.Default.Today,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SunYellow.copy(alpha = 0.2f)
                    )
                    StatCard(
                        title = stringResource(R.string.yearly),
                        value = String.format("%.0f", radiation?.yearlyIrradiance),
                        unit = "kWh/m²",
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SolarOrange.copy(alpha = 0.2f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // PV Output Results
            if (pvOutput != null) {
                Text(
                    text = stringResource(R.string.estimated_output),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.daily),
                        value = String.format("%.1f", pvOutput?.dailyOutput),
                        unit = "kWh",
                        icon = Icons.Default.ElectricBolt,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SkyBlue.copy(alpha = 0.2f)
                    )
                    StatCard(
                        title = stringResource(R.string.monthly),
                        value = String.format("%.0f", pvOutput?.monthlyOutput),
                        unit = "kWh",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SolarGreen.copy(alpha = 0.2f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SolarOrange.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.annual_production) + ": ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format("%.0f", pvOutput?.yearlyOutput),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kWh/year",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

            }
            
            // Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SolarOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.getting_location),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (location != null) {
                        Text(
                            text = String.format("%s: %.4f°, %.4f°", stringResource(R.string.location), location?.latitude, location?.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.location_unavailable),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = {
                            onRefreshLocation()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, 
                            contentDescription = stringResource(R.string.refresh),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Disclaimer
            Text(
                text = stringResource(R.string.estimation_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun MonthlyEnergyChart(
    monthlyProduction: List<Double>,
    isPvgis: Boolean
) {
    val maxProduction = (monthlyProduction.maxOrNull() ?: 1.0) * 1.7 // Reduced from 2.2 to 1.7 as requested
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyProduction.forEachIndexed { index, value ->
                    val barHeight = (value / maxProduction).toFloat()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Value on top - smaller font to fit
                        Text(
                            text = if (value >= 1000) String.format("%.0fK", value/1000) else String.format("%.0f", value),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight(barHeight.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isPvgis) 
                                            listOf(TechBlue, TechBlue.copy(alpha = 0.6f))
                                        else 
                                            listOf(SolarOrange, SolarOrange.copy(alpha = 0.6f))
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Month Label - 1 Letter to ensure fit, but centered
                        Text(
                            text = months[index].take(1),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
            
            if (isPvgis) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SolarGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Source: PVGIS API",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SolarGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
