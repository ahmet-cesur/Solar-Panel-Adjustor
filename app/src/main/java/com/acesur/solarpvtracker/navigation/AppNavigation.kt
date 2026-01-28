package com.acesur.solarpvtracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.acesur.solarpvtracker.ui.screens.*
import com.acesur.solarpvtracker.billing.BillingManager

sealed class Screen(val route: String) {
    object LanguageSelection : Screen("language_selection")
    object Home : Screen("home")
    object Tiltmeter : Screen("tiltmeter")
    object SolarEstimation : Screen("solar_estimation")
    object OptimalAngle : Screen("optimal_angle")
    object TiltAngleReport : Screen("tilt_angle_report")
    object Settings : Screen("settings")
    object RemoveAds : Screen("remove_ads")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    isFirstLaunch: Boolean,
    onLanguageSelected: (String) -> Unit,
    onNavigateWithAd: (destination: String, navigate: () -> Unit) -> Unit = { _, nav -> nav() },
    onPurchaseProduct: (String) -> Unit = {},
    foreverPrice: String? = null,
    hasRatedApp: Boolean = false,
    onRateApp: () -> Unit = {}
) {
    val startDestination = if (isFirstLaunch) Screen.LanguageSelection.route else Screen.Home.route
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onLanguageSelected = { languageCode ->
                    onLanguageSelected(languageCode)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTiltmeter = { 
                    onNavigateWithAd(Screen.Tiltmeter.route) {
                        navController.navigate(Screen.Tiltmeter.route)
                    }
                },
                onNavigateToSolarEstimation = { 
                    onNavigateWithAd(Screen.SolarEstimation.route) {
                        navController.navigate(Screen.SolarEstimation.route)
                    }
                },
                onNavigateToOptimalAngle = { 
                    onNavigateWithAd(Screen.OptimalAngle.route) {
                        navController.navigate(Screen.OptimalAngle.route)
                    }
                },
                onNavigateToTiltReport = { 
                    onNavigateWithAd(Screen.TiltAngleReport.route) {
                        navController.navigate(Screen.TiltAngleReport.route)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToRemoveAds = { navController.navigate(Screen.RemoveAds.route) },
                hasRatedApp = hasRatedApp,
                onRateApp = onRateApp
            )
        }
        
        composable(Screen.Tiltmeter.route) {
            TiltmeterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SolarEstimation.route) {
            SolarEstimationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.OptimalAngle.route) {
            OptimalAngleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.TiltAngleReport.route) {
            TiltAngleReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLanguageChange = { languageCode ->
                    onLanguageSelected(languageCode)
                }
            )
        }
        
        composable(Screen.RemoveAds.route) {
            RemoveAdsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPurchase30Days = {
                    onPurchaseProduct(BillingManager.PRODUCT_ID_30_DAYS)
                },
                onPurchaseForever = {
                    onPurchaseProduct(BillingManager.PRODUCT_ID_FOREVER)
                },
                foreverPrice = foreverPrice
            )
        }
    }
}
