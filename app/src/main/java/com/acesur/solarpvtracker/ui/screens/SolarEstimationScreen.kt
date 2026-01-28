package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarEstimationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { com.acesur.solarpvtracker.data.PreferencesManager(context) }
    val locationHelper = remember { LocationHelper(context, preferencesManager) }
    val solarCalculator = remember { SolarCalculator() }
    
    var location by remember { mutableStateOf<UserLocation?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    // Panel settings
    var panelWattage by remember { mutableStateOf("400") }
    var panelCount by remember { mutableStateOf("10") }
    var efficiency by remember { mutableStateOf("20") }
    
    // Calculation results
    var radiation by remember { mutableStateOf<SolarRadiation?>(null) }
    var pvOutput by remember { mutableStateOf<PVOutput?>(null) }
    
    // Define calculate function
    fun doCalculation() {
        location?.let { loc ->
            val wattage = panelWattage.toIntOrNull() ?: 400
            val count = panelCount.toIntOrNull() ?: 1
            val eff = (efficiency.toFloatOrNull() ?: 20f) / 100f
            
            radiation = solarCalculator.estimateSolarRadiation(loc.latitude)
            pvOutput = solarCalculator.calculatePVOutput(loc.latitude, wattage, count, eff)
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch {
                isLoadingLocation = true
                location = locationHelper.getCurrentLocation()
                isLoadingLocation = false
                doCalculation()
            }
        }
    }
    
    val useGps by preferencesManager.useGps.collectAsState(initial = true)
    
    LaunchedEffect(useGps) {
        if (!useGps) {
            isLoadingLocation = true
            location = locationHelper.getCurrentLocation()
            isLoadingLocation = false
            doCalculation()
        } else {
            if (locationHelper.hasLocationPermission()) {
                isLoadingLocation = true
                location = locationHelper.getCurrentLocation()
                isLoadingLocation = false
                doCalculation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
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
            // Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SolarOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.getting_location))
                    } else if (location != null) {
                        Column {
                            Text(
                                text = stringResource(R.string.location),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format("%.4f°, %.4f°", location?.latitude, location?.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.location_unavailable),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = {
                        scope.launch {
                            isLoadingLocation = true
                            location = locationHelper.getCurrentLocation()
                            isLoadingLocation = false
                            doCalculation()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Panel Settings
            Text(
                text = stringResource(R.string.panel_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = panelWattage,
                    onValueChange = { 
                        panelWattage = it
                        doCalculation()
                    },
                    label = { Text(stringResource(R.string.wattage)) },
                    suffix = { Text("W") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = panelCount,
                    onValueChange = { 
                        panelCount = it
                        doCalculation()
                    },
                    label = { Text(stringResource(R.string.panel_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = efficiency,
                onValueChange = { 
                    efficiency = it
                    doCalculation()
                },
                label = { Text(stringResource(R.string.efficiency)) },
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SolarOrange.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.annual_production),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format("%.0f", pvOutput?.yearlyOutput),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "kWh/year",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Disclaimer
            Text(
                text = stringResource(R.string.estimation_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
