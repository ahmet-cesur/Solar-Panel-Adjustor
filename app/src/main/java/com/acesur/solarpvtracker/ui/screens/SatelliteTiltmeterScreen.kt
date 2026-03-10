package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteTiltmeterScreen(
    onNavigateBack: () -> Unit,
    userLocation: UserLocation?,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { com.acesur.solarpvtracker.data.PreferencesManager(context) }
    val tiltSensorManager = remember { TiltSensorManager(context) }
    val pvgisManager = remember { PVGISManager(context, preferencesManager) }
    
    val tiltData by tiltSensorManager.tiltDataFlow.collectAsState(
        initial = com.acesur.solarpvtracker.sensor.TiltData(0f, 0f, 0f)
    )
    
    val coordinatePrecision by preferencesManager.coordinatePrecision.collectAsState(initial = 4)
    
    var pvgisOptimalAngle by remember { mutableStateOf<Float?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Fetch PVGIS angle when location is available
    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            isLoading = true
            pvgisOptimalAngle = pvgisManager.getOptimalTilt(loc.latitude, loc.longitude)
            isLoading = false
        }
    }
    
    // Request location if missing
    LaunchedEffect(Unit) {
        if (userLocation == null) {
            onRefreshLocation()
        }
    }
    
    val optimalTiltAngle = pvgisOptimalAngle ?: 30f // Default fallback
    
    val panelAngle = (-tiltData.pitch).coerceIn(-90f, 90f)
    val currentAzimuth = tiltData.azimuth
    val isNorthernHemisphere = (userLocation?.latitude ?: 0.0) >= 0
    val requiredAzimuth = if (isNorthernHemisphere) 0f else 180f
    val azimuthDiscrepancy = calculateAzimuthDiscrepancy(currentAzimuth, requiredAzimuth)
    val tiltDiscrepancy = panelAngle - optimalTiltAngle
    
    val isVertical = abs(tiltData.roll) <= 5f
    val isOnTarget = abs(tiltDiscrepancy) <= 5f && abs(azimuthDiscrepancy) <= 5f && isVertical
    
    val bubbleXError = (azimuthDiscrepancy / 45f).coerceIn(-1f, 1f)
    val bubbleYError = (tiltDiscrepancy / 45f).coerceIn(-1f, 1f)
    
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
                title = { Text(stringResource(R.string.satellite_tiltmeter)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // Satellite Optimization Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnTarget) 
                        SolarGreen.copy(alpha = 0.2f) 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SatelliteAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pvgis_active),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )
                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = SolarGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.pvgis_link_text),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SkyBlue,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(start = 36.dp)
                            .clickable {
                                val url = context.getString(R.string.pvgis_url, userLocation?.latitude ?: 0.0, userLocation?.longitude ?: 0.0)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.target_label, optimalTiltAngle),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SkyBlue
                            )
                            Text(
                                text = stringResource(R.string.fixed_angle_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.current_tilt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.angle_degree_fmt, panelAngle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Re-use logic from TiltmeterScreen for visualization
            val combinedError = maxOf(abs(tiltDiscrepancy), abs(azimuthDiscrepancy))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    
                    if (isOnTarget) {
                        Text(text = stringResource(R.string.emoji_thumbs_up), fontSize = 32.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                PendulumWidget(
                    roll = tiltData.roll,
                    modifier = Modifier.width(60.dp).height(200.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (abs(tiltDiscrepancy) > 2f) {
                        Text(
                            text = if (tiltDiscrepancy < 0) stringResource(R.string.lift_up) else stringResource(R.string.tilt_down),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    TiltGauge(
                        angle = panelAngle,
                        targetAngle = optimalTiltAngle,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        text = stringResource(R.string.satellite_tiltmeter_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Location Card
            userLocation?.let { loc ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
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
                            text = String.format("%s: %.${coordinatePrecision}f°, %s: %.${coordinatePrecision}f°", 
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
