package com.acesur.solarpvtracker.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.data.LocationHelper
import com.acesur.solarpvtracker.data.UserLocation
import com.acesur.solarpvtracker.export.PdfExportManager
import com.acesur.solarpvtracker.solar.DailyTiltAngle
import com.acesur.solarpvtracker.solar.MonthlyTiltAngle
import com.acesur.solarpvtracker.solar.SolarCalculator
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiltAngleReportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { com.acesur.solarpvtracker.data.PreferencesManager(context) }
    val locationHelper = remember { LocationHelper(context, preferencesManager) }
    val solarCalculator = remember { SolarCalculator() }
    val pdfExportManager = remember { PdfExportManager(context) }
    
    var location by remember { mutableStateOf<UserLocation?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    
    var showDailyView by remember { mutableStateOf(false) }
    var dailyAngles by remember { mutableStateOf<List<DailyTiltAngle>>(emptyList()) }
    var monthlyAngles by remember { mutableStateOf<List<MonthlyTiltAngle>>(emptyList()) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch {
                isLoadingLocation = true
                location = locationHelper.getCurrentLocation()
                location?.let { loc ->
                    dailyAngles = solarCalculator.generateDailyTiltAngles(loc.latitude)
                    monthlyAngles = solarCalculator.generateMonthlyTiltAngles(loc.latitude)
                }
                isLoadingLocation = false
            }
        }
    }
    
    val useGps by preferencesManager.useGps.collectAsState(initial = true)
    
    LaunchedEffect(useGps) {
        if (!useGps) {
            isLoadingLocation = true
            location = locationHelper.getCurrentLocation()
            location?.let { loc ->
                dailyAngles = solarCalculator.generateDailyTiltAngles(loc.latitude)
                monthlyAngles = solarCalculator.generateMonthlyTiltAngles(loc.latitude)
            }
            isLoadingLocation = false
        } else {
            if (locationHelper.hasLocationPermission()) {
                isLoadingLocation = true
                location = locationHelper.getCurrentLocation()
                location?.let { loc ->
                    dailyAngles = solarCalculator.generateDailyTiltAngles(loc.latitude)
                    monthlyAngles = solarCalculator.generateMonthlyTiltAngles(loc.latitude)
                }
                isLoadingLocation = false
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
    
    fun exportToPdf() {
        location?.let { loc ->
            scope.launch {
                isExporting = true
                withContext(Dispatchers.IO) {
                    val file = if (showDailyView) {
                        pdfExportManager.generateDailyReport(dailyAngles, loc.latitude, loc.longitude)
                    } else {
                        pdfExportManager.generateMonthlyReport(monthlyAngles, loc.latitude, loc.longitude)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (file != null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.pdf_saved, file.name),
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Share
                            val shareIntent = pdfExportManager.createShareIntent(file)
                            context.startActivity(shareIntent)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.pdf_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                isExporting = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tilt_report)) },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { exportToPdf() },
                containerColor = SolarGreen,
                contentColor = Color.White,
                icon = {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, null)
                    }
                },
                text = { Text(stringResource(R.string.export_pdf)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Location info
            if (location != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SolarOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%.4f°, %.4f°", location?.latitude, location?.longitude),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Toggle tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = !showDailyView,
                    onClick = { showDailyView = false },
                    label = { Text(stringResource(R.string.monthly_12)) },
                    leadingIcon = if (!showDailyView) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = showDailyView,
                    onClick = { showDailyView = true },
                    label = { Text(stringResource(R.string.daily_365)) },
                    leadingIcon = if (showDailyView) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subheader
            Text(
                text = stringResource(R.string.tilt_report_subheader),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoadingLocation) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (showDailyView) {
                // Daily list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(dailyAngles) { angle ->
                        DailyAngleRow(angle)
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // FAB space
                    }
                }
            } else {
                // Monthly list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(monthlyAngles) { angle ->
                        MonthlyAngleRow(angle)
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // FAB space
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyAngleRow(angle: DailyTiltAngle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.day_label, angle.dayOfYear),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = angle.date,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.1f°", angle.optimalAngle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SolarOrange
            )
        }
    }
}

@Composable
private fun MonthlyAngleRow(angle: MonthlyTiltAngle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val calendar = remember { java.util.Calendar.getInstance() }
                calendar.set(java.util.Calendar.MONTH, angle.month - 1)
                val monthName = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()) ?: ""
                
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = angle.notesResId?.let { stringResource(it) } ?: angle.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format("%.1f°", angle.optimalAngle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SolarOrange
            )
        }
    }
}
