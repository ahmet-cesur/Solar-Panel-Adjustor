package com.acesur.solarpvtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.ads.AdManager
import com.acesur.solarpvtracker.data.PreferencesManager
import com.acesur.solarpvtracker.ui.components.BannerAdView
import com.acesur.solarpvtracker.ui.components.FeatureCard
import com.acesur.solarpvtracker.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTiltmeter: () -> Unit,
    onNavigateToSolarEstimation: () -> Unit,
    onNavigateToOptimalAngle: () -> Unit,
    onNavigateToTiltReport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRemoveAds: () -> Unit,
    onNavigateToSatelliteData: () -> Unit,
    hasRatedApp: Boolean,
    onRateApp: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val adManager = remember { AdManager(context) }
    val scope = rememberCoroutineScope()
    
    // Observe ad-free status reactively
    val isAdFree by preferencesManager.isAdFree.collectAsStateWithLifecycle(initialValue = false)
    val isEasterEggActivated by preferencesManager.isEasterEggActivated.collectAsStateWithLifecycle(initialValue = false)
    
    // Counter-easter egg tap counter
    var reverseEasterEggTapCount by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header (Counter-easter egg: tap 15 times to remove easter egg activation)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkyBlue)
                            .clickable {
                                // Only allow easter egg removal if it was activated via easter egg
                                if (isEasterEggActivated) {
                                    reverseEasterEggTapCount++
                                    if (reverseEasterEggTapCount >= 15) {
                                        scope.launch {
                                            preferencesManager.resetAdFreeStatus()
                                        }
                                    }
                                }
                            }
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.welcome_message),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                        }
                    }
                }
                
                // Tiltmeter
                item {
                    FeatureCard(
                        title = stringResource(R.string.tiltmeter),
                        description = stringResource(R.string.tiltmeter_desc),
                        icon = Icons.Default.ScreenRotation,
                        onClick = onNavigateToTiltmeter,
                        gradientColors = listOf(SolarOrange, SolarOrangeLight)
                    )
                }
                
                // Solar Estimation
                item {
                    FeatureCard(
                        title = stringResource(R.string.solar_estimation),
                        description = stringResource(R.string.solar_estimation_desc),
                        icon = Icons.Default.WbSunny,
                        onClick = onNavigateToSolarEstimation,
                        gradientColors = listOf(SunYellow, SunYellowLight)
                    )
                }
                
                // Optimal Angle
                item {
                    FeatureCard(
                        title = stringResource(R.string.optimal_angle),
                        description = stringResource(R.string.optimal_angle_desc),
                        icon = Icons.Default.Architecture,
                        onClick = onNavigateToOptimalAngle,
                        gradientColors = listOf(SkyBlue, SkyBlueLight)
                    )
                }
                
                // Tilt Angle Report
                item {
                    FeatureCard(
                        title = stringResource(R.string.tilt_report),
                        description = stringResource(R.string.tilt_report_desc),
                        icon = Icons.Default.PictureAsPdf,
                        onClick = onNavigateToTiltReport,
                        gradientColors = listOf(SolarGreen, SolarGreenLight)
                    )
                }

                // Satellite Data
                item {
                    FeatureCard(
                        title = stringResource(R.string.satellite_data),
                        description = stringResource(R.string.satellite_data_desc),
                        icon = Icons.Default.SatelliteAlt,
                        onClick = onNavigateToSatelliteData,
                        gradientColors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6)) // Blue palette
                    )
                }

                // Rate App Button (only if not rated)
                if (!hasRatedApp) {
                    item {
                        FeatureCard(
                            title = stringResource(R.string.rate_app_title),
                            description = stringResource(R.string.rate_app_card_desc),
                            icon = Icons.Default.Star,
                            onClick = onRateApp,
                            gradientColors = listOf(Color(0xFF673AB7), Color(0xFF9575CD)) // DeepPurple palette
                        )
                    }
                }

                // Remove Ads
                if (!isAdFree) {
                    item {
                        FeatureCard(
                            title = stringResource(R.string.remove_ads),
                            description = stringResource(R.string.remove_ads_desc), 
                            icon = Icons.Default.Block,
                            onClick = onNavigateToRemoveAds,
                            gradientColors = listOf(FestivalRed, FestivalRedLight)
                        )
                    }
                }
            }
            
            // Banner Ad at bottom (only if NOT ad-free)
            if (!isAdFree) {
                BannerAdView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}
