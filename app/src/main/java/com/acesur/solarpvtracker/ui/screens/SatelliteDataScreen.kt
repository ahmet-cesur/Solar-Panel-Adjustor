package com.acesur.solarpvtracker.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.ui.theme.*
import com.acesur.solarpvtracker.data.UserLocation
import com.acesur.solarpvtracker.data.WeatherManager
import com.acesur.solarpvtracker.data.WeatherData
import androidx.compose.ui.platform.LocalContext

import com.acesur.solarpvtracker.data.PreferencesManager
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteDataScreen(
    onNavigateBack: () -> Unit,
    userLocation: UserLocation?,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val weatherManager = remember { WeatherManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(userLocation, refreshTrigger) {
        userLocation?.let { loc ->
            // 1. Try to load from cache if state is empty
            if (weatherData == null) {
                val cachedJson = preferencesManager.weatherCache.first()
                if (cachedJson != null) {
                    weatherData = weatherManager.parseWeatherData(cachedJson)
                }
            }

            // 2. Check if we need a refresh (older than 3h or forced)
            val lastFetch = preferencesManager.weatherLastFetchTime.first()
            val threeHours = 3 * 60 * 60 * 1000L
            val isOld = System.currentTimeMillis() - lastFetch > threeHours
            
            if (isOld || weatherData == null || refreshTrigger > 0) {
                isLoading = true
                val result = weatherManager.getWeatherData(loc.latitude, loc.longitude)
                if (result != null) {
                    weatherData = result.first
                    preferencesManager.saveWeatherCache(result.second)
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.satellite_data)) },
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
            // Header Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.realtime_forecast),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (weatherData != null) stringResource(R.string.satellite_feeds_desc) else stringResource(R.string.getting_location),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        if (weatherData != null) {
                            Text(
                                text = stringResource(R.string.last_updated, weatherData!!.lastUpdateTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    FilledIconButton(
                        onClick = { refreshTrigger++ },
                        enabled = !isLoading,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatCard(
                    title = stringResource(R.string.cloud_cover),
                    value = if (isLoading) "…" else if (weatherData != null) stringResource(R.string.percent_fmt, weatherData!!.cloudCover) else stringResource(R.string.placeholder_value),
                    icon = Icons.Default.Cloud,
                    color = SkyBlue,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    title = stringResource(R.string.visibility),
                    value = if (isLoading) "…" else if (weatherData != null) "${String.format("%.0f", weatherData!!.visibility)}${stringResource(R.string.unit_km)}" else stringResource(R.string.placeholder_value),
                    icon = Icons.Default.Visibility,
                    color = SolarGreen,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    title = stringResource(R.string.uv_index),
                    value = if (isLoading) "…" else if (weatherData != null) String.format("%.1f", weatherData!!.uvIndex) else stringResource(R.string.placeholder_value),
                    icon = Icons.Default.WbSunny,
                    color = SunYellow,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    title = stringResource(R.string.humidity),
                    value = if (isLoading) "…" else if (weatherData != null) stringResource(R.string.percent_fmt, weatherData!!.humidity) else stringResource(R.string.placeholder_value),
                    icon = Icons.Default.WaterDrop,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Forecast Section
            Text(
                text = stringResource(R.string.next_5_days_yield),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Forecast Items
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (weatherData != null) {
                weatherData!!.dailyForecast.forEach { forecast ->
                    ForecastRow(
                        day = stringResource(forecast.dayResId),
                        date = forecast.dateDisplay,
                        status = stringResource(forecast.statusResId),
                        yieldValue = forecast.yield,
                        weatherCode = forecast.weatherCode
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = stringResource(R.string.location_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = onRefreshLocation) {
                    Text(stringResource(R.string.refresh))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Disclaimer removed as requested
        }
    }
}

@Composable
fun CompactStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                color = Color.Gray,
                maxLines = 1
            )
            Text(
                text = value, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ForecastRow(day: String, date: String, status: String, yieldValue: Double, weatherCode: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$day $date", fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.yield_fmt_unit, yieldValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Fixed color logic based on weatherCode instead of localized string contains
            val statusColor = when (weatherCode) {
                0, 1 -> SolarGreen // Clear or Mainly clear
                2, 3 -> SolarOrange // Partly cloudy, Overcast
                45, 48 -> Color.Gray // Fog
                51, 53, 55, 61, 63, 65, 80, 81, 82 -> SkyBlue // Drizzle, Rain, Showers
                71, 73, 75, 77, 85, 86 -> Color.Gray // Snow
                95, 96, 99 -> SolarOrange // Thunderstorm
                else -> Color.Gray
            }
            
            Text(
                text = status,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
