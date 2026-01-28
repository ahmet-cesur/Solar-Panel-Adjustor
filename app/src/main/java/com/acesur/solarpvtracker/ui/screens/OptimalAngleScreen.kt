package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.data.LocationHelper
import com.acesur.solarpvtracker.data.UserLocation
import com.acesur.solarpvtracker.solar.OptimalTiltAngle
import com.acesur.solarpvtracker.solar.SolarCalculator
import com.acesur.solarpvtracker.ui.theme.SkyBlue
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import com.acesur.solarpvtracker.ui.theme.SunYellow
import com.acesur.solarpvtracker.data.PVGISManager
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
private enum class OptimalAngleMode {
    DAILY,
    NEXT_30_DAYS,
    FIXED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimalAngleScreen(
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
    
    var isLoadingLocation by remember { mutableStateOf(false) }
    var seasonalAngles by remember { mutableStateOf<List<OptimalTiltAngle>>(emptyList()) }
    var pvgisOptimalAngle by remember { mutableStateOf<Float?>(null) }
    
    // Update seasonal angles when location changes
    LaunchedEffect(location) {
        location?.let { loc ->
            seasonalAngles = solarCalculator.getSeasonalTiltAngles(loc.latitude)
            // PVGIS fetch is handled in another LaunchedEffect below (or merged here)
        }
    }
    
    // Fetch PVGIS angle when location is available
    LaunchedEffect(location) {
        location?.let { loc ->
            pvgisOptimalAngle = pvgisManager.getOptimalTilt(loc.latitude, loc.longitude)
        }
    }
    
     // Trigger location refresh if missing
    LaunchedEffect(location) {
        if (location == null) {
            onRefreshLocation()
        }
    }
    
    // Calculate today's optimal angle
    val todayOptimalAngle = remember(location) {
        location?.let { loc ->
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            solarCalculator.calculateOptimalTiltAngle(loc.latitude, dayOfYear)
        } ?: 0.0
    }
    
    // Calculate next 30 days average
    val next30DaysAngle = remember(location) {
        location?.let { loc ->
            val calendar = Calendar.getInstance()
            var sum = 0.0
            for (i in 0 until 30) {
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                sum += solarCalculator.calculateOptimalTiltAngle(loc.latitude, dayOfYear)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            sum / 30.0
        } ?: 0.0
    }
    
    // Year-round fixed angle
    val fixedAngle = remember(location, pvgisOptimalAngle) {
        pvgisOptimalAngle?.toDouble() ?: location?.let { loc -> abs(loc.latitude) } ?: 0.0
    }
    
    // State for selected angle mode to display in graph
    var selectedMode by remember { mutableStateOf(OptimalAngleMode.DAILY) }
    
    // Determine angle to display based on selection
    val displayAngle = when (selectedMode) {
        OptimalAngleMode.DAILY -> todayOptimalAngle
        OptimalAngleMode.NEXT_30_DAYS -> next30DaysAngle
        OptimalAngleMode.FIXED -> fixedAngle
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
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.optimal_angle)) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    
    val coordinatePrecision by preferencesManager.coordinatePrecision.collectAsState(initial = 4)
    
                    if (isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.getting_location))
                    } else if (location != null) {
                        Column {
                            Text(
                                text = stringResource(R.string.your_latitude),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format("%.${coordinatePrecision}f°", location?.latitude),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.location_unavailable),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Panel Angle Diagram
            PanelDiagram(
                angle = displayAngle.toFloat()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Quick Reference Angles Section
            Text(
                text = stringResource(R.string.quick_reference),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Today's Optimal Angle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SolarGreen.copy(alpha = 0.15f)
                ),
                border = if (selectedMode == OptimalAngleMode.DAILY) BorderStroke(2.dp, SolarGreen) else null,
                onClick = { selectedMode = OptimalAngleMode.DAILY }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = null,
                        tint = SolarGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.daily_angle),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.daily_angle_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%.1f°", todayOptimalAngle),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SolarGreen
                    )
                }
            }
            
            // Next 30 Days Average
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SkyBlue.copy(alpha = 0.15f)
                ),
                border = if (selectedMode == OptimalAngleMode.NEXT_30_DAYS) BorderStroke(2.dp, SkyBlue) else null,
                onClick = { selectedMode = OptimalAngleMode.NEXT_30_DAYS }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.next_30_days),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.next_30_days_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%.1f°", next30DaysAngle),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SkyBlue
                    )
                }
            }
            

            
            // Year-round Fixed Angle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SolarOrange.copy(alpha = 0.15f)
                ),
                border = if (selectedMode == OptimalAngleMode.FIXED) BorderStroke(2.dp, SolarOrange) else null,
                onClick = { selectedMode = OptimalAngleMode.FIXED }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = SolarOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fixed_angle),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.fixed_angle_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%.1f°", fixedAngle),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SolarOrange
                    )
                }
                
                // PVGIS Source Badge
                if (pvgisOptimalAngle != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = SolarGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Source: PVGIS API",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolarGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Seasonal Recommendations
            Text(
                text = stringResource(R.string.seasonal_recommendations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            seasonalAngles.filter { it.descriptionResId != R.string.year_round_description }.forEachIndexed { index, angleData ->
                val colors = listOf(SunYellow, SolarOrange, SkyBlue, MaterialTheme.colorScheme.primary)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.getOrElse(index) { SolarOrange }.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = angleData.descriptionResId?.let { stringResource(it) } ?: angleData.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = String.format("%.1f°", angleData.angle),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.getOrElse(index) { SolarOrange }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tilt_angle_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tilt_angle_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelDiagram(
    angle: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SkyBlue.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp, 150.dp)) {
                val centerX = size.width / 2
                val groundY = size.height - 20f
                
                // Ground line - Green like grass
                drawLine(
                    color = SolarGreen,
                    start = Offset(20f, groundY),
                    end = Offset(size.width - 20f, groundY),
                    strokeWidth = 6f // Slightly thicker for grass effect
                )
                
                // Panel
                val panelLength = 120f
                val angleRad = Math.toRadians(angle.toDouble())
                val panelEndX = centerX + (panelLength * cos(angleRad)).toFloat()
                val panelEndY = groundY - (panelLength * sin(angleRad)).toFloat()
                
                // Panel shadow
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(centerX + 3f, groundY),
                    end = Offset(panelEndX + 3f, panelEndY + 3f),
                    strokeWidth = 12f
                )
                
                // Panel
                drawLine(
                    color = SolarOrange,
                    start = Offset(centerX, groundY),
                    end = Offset(panelEndX, panelEndY),
                    strokeWidth = 10f
                )
                
                // Sun (Moved to Left)
                val sunX = 60f
                val sunY = 50f
                drawCircle(
                    color = SunYellow,
                    radius = 25f,
                    center = Offset(sunX, sunY)
                )
                
                // Sun rays
                for (i in 0..7) {
                    val rayAngle = Math.toRadians(i * 45.0)
                    drawLine(
                        color = SunYellow,
                        start = Offset(
                            sunX + (30f * cos(rayAngle)).toFloat(),
                            sunY + (30f * sin(rayAngle)).toFloat()
                        ),
                        end = Offset(
                            sunX + (42f * cos(rayAngle)).toFloat(),
                            sunY + (42f * sin(rayAngle)).toFloat()
                        ),
                        strokeWidth = 3f
                    )
                }
                
                // Angle arc
                val arcPath = Path().apply {
                    moveTo(centerX + 40f, groundY)
                    for (a in 0..angle.toInt()) {
                        val arcRad = Math.toRadians(a.toDouble())
                        lineTo(
                            centerX + (40f * cos(arcRad)).toFloat(),
                            groundY - (40f * sin(arcRad)).toFloat()
                        )
                    }
                }
                drawPath(arcPath, SolarOrange.copy(alpha = 0.5f), style = Stroke(width = 2f))
                
                // Draw Text inside the angle (between green line and panel)
                drawIntoCanvas {
                    val paint = android.graphics.Paint().apply {
                        textSize = 48f
                        color = SolarOrange.toArgb()
                        textAlign = android.graphics.Paint.Align.LEFT
                        isFakeBoldText = true
                    }
                    
                    // Position at half angle, somewhat distant from center
                    val textDist = 90f
                    val halfAngleRad = angleRad / 2.5 // Slightly lower than half to avoid hitting the panel line if angle is small
                    val textX = centerX + (textDist * cos(halfAngleRad)).toFloat()
                    val textY = groundY - (textDist * sin(halfAngleRad)).toFloat()
                    
                    it.nativeCanvas.drawText(
                        String.format("%.1f°", angle),
                        textX,
                        textY,
                        paint
                    )
                }
            }
        }
    }
}
