package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.data.LocationHelper
import com.acesur.solarpvtracker.data.UserLocation
import com.acesur.solarpvtracker.data.PVGISManager
import com.acesur.solarpvtracker.sensor.TiltSensorManager
import com.acesur.solarpvtracker.solar.SolarCalculator
import com.acesur.solarpvtracker.ui.components.BubbleLevel
import com.acesur.solarpvtracker.ui.components.CompassIndicator
import com.acesur.solarpvtracker.ui.components.PendulumWidget
import com.acesur.solarpvtracker.ui.components.TiltGauge
import com.acesur.solarpvtracker.ui.theme.SkyBlue
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import com.acesur.solarpvtracker.ui.theme.FestivalRed
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

enum class AngleMode {
    FIXED,      // Year-round fixed angle (latitude-based)
    DAILY,      // Today's optimal angle
    NEXT_30_DAYS // Average of next 30 days
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiltmeterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { com.acesur.solarpvtracker.data.PreferencesManager(context) }
    val tiltSensorManager = remember { TiltSensorManager(context) }
    val locationHelper = remember { LocationHelper(context, preferencesManager) }
    val solarCalculator = remember { SolarCalculator() }
    val pvgisManager = remember { PVGISManager(context, preferencesManager) }
    
    val tiltData by tiltSensorManager.tiltDataFlow.collectAsState(
        initial = com.acesur.solarpvtracker.sensor.TiltData(0f, 0f, 0f)
    )
    
    var location by remember { mutableStateOf<UserLocation?>(null) }
    var angleMode by remember { mutableStateOf(AngleMode.FIXED) }
    var pvgisOptimalAngle by remember { mutableStateOf<Float?>(null) }
    
    // Fetch PVGIS angle when location is available
    LaunchedEffect(location) {
        location?.let { loc ->
            pvgisOptimalAngle = pvgisManager.getOptimalTilt(loc.latitude, loc.longitude)
        }
    }
    
    // Calculate optimal tilt angle based on selected mode
    val optimalTiltAngle = remember(location, angleMode, pvgisOptimalAngle) {
        location?.let { loc ->
            when (angleMode) {
                AngleMode.FIXED -> {
                    // Try to use PVGIS angle first, fallback to latitude
                    pvgisOptimalAngle ?: abs(loc.latitude).toFloat()
                }
                AngleMode.DAILY -> {
                    // Today's optimal angle
                    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    solarCalculator.calculateOptimalTiltAngle(loc.latitude, dayOfYear).toFloat()
                }
                AngleMode.NEXT_30_DAYS -> {
                    // Average of next 30 days
                    val calendar = Calendar.getInstance()
                    var sum = 0.0
                    for (i in 0 until 30) {
                        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                        sum += solarCalculator.calculateOptimalTiltAngle(loc.latitude, dayOfYear)
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    (sum / 30.0).toFloat()
                }
            }
        } ?: 30f // Default fallback
    }
    
    // Calculate panel tilt angle (from horizontal)
    // Use signed pitch to distinguish between forward and backward tilt
    val panelAngle = (-tiltData.pitch).coerceIn(-90f, 90f)
    
    // Calculate azimuth and required direction
    val currentAzimuth = tiltData.azimuth
    val isNorthernHemisphere = (location?.latitude ?: 0.0) >= 0
    // User requested: Seek 0° in Northern Hemisphere, 180° in Southern Hemisphere
    val requiredAzimuth = if (isNorthernHemisphere) 0f else 180f
    val azimuthDiscrepancy = calculateAzimuthDiscrepancy(currentAzimuth, requiredAzimuth)
    
    // Calculate tilt discrepancy from optimal
    val tiltDiscrepancy = panelAngle - optimalTiltAngle
    
    // Check if within 5 degrees for both tilt and azimuth, AND phone is held vertically (roll <= 5 deg)
    val isVertical = abs(tiltData.roll) <= 5f
    val isOnTarget = abs(tiltDiscrepancy) <= 5f && abs(azimuthDiscrepancy) <= 5f && isVertical
    
    // For bubble level: normalize errors to -1 to 1 range
    val bubbleXError = (azimuthDiscrepancy / 45f).coerceIn(-1f, 1f)
    val bubbleYError = (tiltDiscrepancy / 45f).coerceIn(-1f, 1f)
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch {
                location = locationHelper.getCurrentLocation()
            }
        }
    }
    
    val useGps by preferencesManager.useGps.collectAsState(initial = true)
    
    LaunchedEffect(useGps) {
        if (!useGps) {
             location = locationHelper.getCurrentLocation()
        } else {
            if (locationHelper.hasLocationPermission()) {
                location = locationHelper.getCurrentLocation()
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
                title = { Text(stringResource(R.string.tiltmeter)) },
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
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Angle Calculation Mode Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnTarget) 
                        SolarGreen.copy(alpha = 0.2f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fixed Angle
                        Button(
                            onClick = { angleMode = AngleMode.FIXED },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (angleMode == AngleMode.FIXED) SolarOrange else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (angleMode == AngleMode.FIXED) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.fixed_angle),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                maxLines = 1
                            )
                        }
                        
                        // Daily
                        Button(
                            onClick = { angleMode = AngleMode.DAILY },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (angleMode == AngleMode.DAILY) SolarOrange else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (angleMode == AngleMode.DAILY) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.daily_angle),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                maxLines = 1
                            )
                        }
                        
                        // Next 30 Days
                        Button(
                            onClick = { angleMode = AngleMode.NEXT_30_DAYS },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (angleMode == AngleMode.NEXT_30_DAYS) SolarOrange else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (angleMode == AngleMode.NEXT_30_DAYS) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.next_30_days),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // Target and Readings Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left: Mode Description & Target
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (angleMode) {
                                    AngleMode.FIXED -> stringResource(R.string.fixed_angle_desc)
                                    AngleMode.DAILY -> stringResource(R.string.daily_angle_desc)
                                    AngleMode.NEXT_30_DAYS -> stringResource(R.string.next_30_days_desc)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.target_label, optimalTiltAngle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SkyBlue
                            )
                        }

                        // Right: Live Readings
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current Tilt
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.current_tilt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.1f°", panelAngle),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Tilt Diff
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.tilt_diff),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%+.1f°", tiltDiscrepancy),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(tiltDiscrepancy) <= 5f) SolarGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (isOnTarget) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.optimal_position_status),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SolarGreen,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calculate combined error for feedback
            val combinedError = maxOf(abs(tiltDiscrepancy), abs(azimuthDiscrepancy))
            
            // Layout: Bubble Level (Left) and Compass (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bubble Level
                BubbleLevel(
                    xTilt = bubbleXError,
                    yTilt = bubbleYError,
                    targetXOffset = 0f,
                    targetYOffset = 0f,
                    isOnTarget = isOnTarget,
                    showTarget = true,
                    currentAzimuth = currentAzimuth,
                    targetAzimuth = requiredAzimuth,
                    combinedError = combinedError,
                    isNorthernHemisphere = isNorthernHemisphere,
                    modifier = Modifier.size(200.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Compass Indicator (Smaller, on the right)
                Box(
                    modifier = Modifier.width(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompassIndicator(
                        currentAzimuth = currentAzimuth,
                        targetAzimuth = requiredAzimuth,
                        isNorthernHemisphere = isNorthernHemisphere,
                        azimuthError = abs(azimuthDiscrepancy)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Angle Graph (Tilt Gauge)
            // Add Pendulum Widget to left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                PendulumWidget(
                    roll = tiltData.roll,
                    modifier = Modifier
                        .width(60.dp)
                        .height(200.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.height(220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Guidance Text
                    if (abs(tiltDiscrepancy) > 2f) {
                        val text = if (tiltDiscrepancy < 0) stringResource(R.string.lift_up) else stringResource(R.string.tilt_down)
                        val color = if (tiltDiscrepancy < 0) SolarGreen else FestivalRed // Or use consistent Logic
                        // Usually Lift Up means we are below target (need to go up). Color orange/red?
                        // Let's use customized colors or just Primary/Secondary
                        
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(32.dp)) // Keep space to prevent jumping
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        TiltGauge(
                            angle = panelAngle,
                            targetAngle = optimalTiltAngle,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Spacer(modifier = Modifier.height(16.dp))

            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.instructions_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tiltmeter_usage_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // PVGIS Attribution
                    if (angleMode == AngleMode.FIXED && pvgisOptimalAngle != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = SolarGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.pvgis_active),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Coordinate Display (Moved to bottom)
            location?.let { loc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SolarOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%s: %.4f°, %s: %.4f°", 
                                stringResource(R.string.latitude), loc.latitude,
                                stringResource(R.string.longitude), loc.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun calculateAzimuthDiscrepancy(currentAzimuth: Float, idealAzimuth: Float): Float {
    var diff = idealAzimuth - currentAzimuth
    while (diff > 180) diff -= 360
    while (diff < -180) diff += 360
    return diff
}
