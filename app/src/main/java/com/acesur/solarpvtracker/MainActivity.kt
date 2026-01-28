package com.acesur.solarpvtracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.acesur.solarpvtracker.ads.AdManager
import com.acesur.solarpvtracker.data.PreferencesManager
import com.acesur.solarpvtracker.navigation.AppNavigation
import com.acesur.solarpvtracker.navigation.Screen
import com.acesur.solarpvtracker.ui.theme.SolarPVTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

import com.acesur.solarpvtracker.billing.BillingManager
import com.acesur.solarpvtracker.ui.screens.supportedLanguages

import androidx.lifecycle.lifecycleScope

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {

    private var sessionStartTime: Long = 0L
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adManager: AdManager
    private lateinit var billingManager: BillingManager
    
    companion object {
        // Store the current locale for attachBaseContext
        private var currentLocale: Locale? = null
        
        private fun getLocaleFromCode(languageCode: String): Locale {
            return when (languageCode) {
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "ar" -> Locale("ar")
                else -> Locale(languageCode)
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Try to get locale from companion object first (for recreate scenario)
        var locale = currentLocale
        
        // If not set, try to read from SharedPreferences (DataStore's underlying storage)
        if (locale == null) {
            try {
                val prefs = newBase.getSharedPreferences("solar_pv_settings.preferences_pb", Context.MODE_PRIVATE)
                // DataStore preferences are stored differently, so we need to check the actual file
                // Let's use a simpler approach - check regular shared preferences for our language key
            } catch (e: Exception) {
                // Ignore - will use default
            }
            
            // Read from DataStore's SharedPreferences file
            // DataStore uses a different format, so let's store the language in a simple SharedPreferences for this purpose
            try {
                val simplePrefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
                val savedLanguage = simplePrefs.getString("language", null)
                if (savedLanguage != null) {
                    locale = getLocaleFromCode(savedLanguage)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        if (locale != null) {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }
    
    override fun onResume() {
        super.onResume()
        sessionStartTime = System.currentTimeMillis()
    }
    
    override fun onPause() {
        super.onPause()
        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            // Reset to 0 to avoid double counting if onPause calls multiple times
            sessionStartTime = 0 
            lifecycleScope.launch {
                preferencesManager.incrementUsageTime(sessionDuration)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        adManager = AdManager(this)
        
        // Initialize Billing
        billingManager = BillingManager(this, lifecycleScope) { purchase ->
            // Handle successful purchase
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                purchase.products.forEach { productId ->
                    when (productId) {
                        BillingManager.PRODUCT_ID_30_DAYS -> {
                            preferencesManager.setAdFreeFor30Days()
                        }
                        BillingManager.PRODUCT_ID_FOREVER -> {
                            preferencesManager.setAdFreeForever()
                        }
                    }
                }
            }
        }
        billingManager.startConnection()
        
        // Initialize AdMob
        adManager.initialize {
            // Preload interstitial ad
            adManager.loadInterstitialAd()
        }
        
        // Get initial settings
        var isFirstLaunch = runBlocking { preferencesManager.isFirstLaunch.first() }
        val savedLanguage = runBlocking { preferencesManager.language.first() }
        
        // Logic for Auto-Language Detection on First Launch
        if (isFirstLaunch) {
            val deviceLanguageCode = Locale.getDefault().language
            val isSupported = supportedLanguages.any { it.code == deviceLanguageCode }
            
            if (isSupported) {
                // Automatically select the language and skip the selection screen
                runBlocking {
                    preferencesManager.setLanguage(deviceLanguageCode)
                    preferencesManager.setFirstLaunchComplete()
                }
                applyLanguage(deviceLanguageCode)
                // Need to recreate so attachBaseContext runs with the new locale
                recreate()
                return // Don't continue executing onCreate after recreate
            }
        } else {
            // Apply saved language
            applyLanguage(savedLanguage)
        }
        
        enableEdgeToEdge()
        
        setContent {
            // Observe simple preference flows
            val darkMode by preferencesManager.darkMode.collectAsStateWithLifecycle(initialValue = false)
            val isAdFree by preferencesManager.isAdFree.collectAsStateWithLifecycle(initialValue = false)
            
            // Rate App Logic
            val appUsageTime by preferencesManager.appUsageTime.collectAsStateWithLifecycle(initialValue = 0L)
            val rateAppShownCount by preferencesManager.rateAppShownCount.collectAsStateWithLifecycle(initialValue = 0)
            val rateAppLastShownTime by preferencesManager.rateAppLastShownTime.collectAsStateWithLifecycle(initialValue = 0L)
            // Start as TRUE (hidden) to avoid "flicker out". If false, it will appear momentarily.
            val hasRatedApp by preferencesManager.hasRatedApp.collectAsStateWithLifecycle(initialValue = true)
            val neverShowRateApp by preferencesManager.neverShowRateApp.collectAsStateWithLifecycle(initialValue = false)
            


            // Observe product details for dynamic pricing
            val productDetailsList by billingManager.productDetails.collectAsStateWithLifecycle()
            val foreverPrice = remember(productDetailsList) {
                productDetailsList
                    .find { it.productId == BillingManager.PRODUCT_ID_FOREVER }
                    ?.oneTimePurchaseOfferDetails
                    ?.formattedPrice
            }
            
            SolarPVTrackerTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    var showRateDialog by remember { mutableStateOf(false) }

                    // Logic to trigger Rate App Dialog
                    LaunchedEffect(appUsageTime, rateAppShownCount, rateAppLastShownTime, hasRatedApp, neverShowRateApp) {
                        if (!hasRatedApp && !neverShowRateApp && rateAppShownCount < 5) {
                            if (rateAppShownCount == 0) {
                                // First time: Wait for 10 minutes total usage
                                val threshold = 10 * 60 * 1000L
                                if (appUsageTime < threshold) {
                                    delay(threshold - appUsageTime)
                                }
                                showRateDialog = true
                            } else {
                                // Reminder: Every 10 days
                                val interval = 10 * 24 * 60 * 60 * 1000L
                                if (System.currentTimeMillis() - rateAppLastShownTime >= interval) {
                                    showRateDialog = true
                                }
                            }
                        }
                    }

                    if (showRateDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showRateDialog = false
                                // Treat dismissal as "Remind Later" implicitly, count it as shown
                                lifecycleScope.launch { preferencesManager.setRateAppShown() }
                            },
                            title = { Text(stringResource(R.string.rate_app_title)) },
                            text = { Text(stringResource(R.string.rate_app_message)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showRateDialog = false
                                    lifecycleScope.launch {
                                        preferencesManager.setHasRatedApp()
                                        // Open Play Store
                                        val appPackageName = "com.acesur.solarpvtracker"
                                        try {
                                            // User requested specific HTTPS link
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                                        } catch (e: ActivityNotFoundException) {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                                        }
                                    }
                                }) {
                                    Text(stringResource(R.string.rate_now))
                                }
                            },
                            dismissButton = {
                                Row {
                                    TextButton(onClick = {
                                        showRateDialog = false
                                        lifecycleScope.launch { preferencesManager.setNeverShowRateApp() }
                                    }) { Text(stringResource(R.string.no_thanks)) }
                                    
                                    TextButton(onClick = {
                                        showRateDialog = false
                                        lifecycleScope.launch { preferencesManager.setRateAppShown() }
                                    }) { Text(stringResource(R.string.remind_later)) }
                                }
                            }
                        )
                    }

                    // Auto-show Remove Ads screen every 30 minutes if not ad-free
                    LaunchedEffect(isAdFree) {
                        if (!isAdFree) {
                            while (true) {
                                delay(30 * 60 * 1000L) // 30 minutes
                                // Check if we are not already on the screen
                                if (navController.currentDestination?.route != Screen.RemoveAds.route) {
                                    navController.navigate(Screen.RemoveAds.route)
                                }
                            }
                        }
                    }
                    
                    AppNavigation(
                        navController = navController,
                        isFirstLaunch = isFirstLaunch,
                        onLanguageSelected = { languageCode ->
                            // Save language preference
                            runBlocking {
                                preferencesManager.setLanguage(languageCode)
                                preferencesManager.setFirstLaunchComplete()
                            }
                            // Apply language and recreate
                            applyLanguage(languageCode)
                            recreate()
                        },
                        onNavigateWithAd = { destination, navigate ->
                            // Show interstitial with 30% probability
                            adManager.maybeShowInterstitialAd(
                                activity = this@MainActivity,
                                onAdDismissed = { navigate() },
                                onNoAdShown = { navigate() }
                            )
                        },
                        onPurchaseProduct = { productId ->
                            billingManager.launchBillingFlow(this@MainActivity, productId)
                        },
                        foreverPrice = foreverPrice,
                        hasRatedApp = hasRatedApp,
                        onRateApp = {
                            lifecycleScope.launch {
                                preferencesManager.setHasRatedApp()
                                // Open Play Store
                                val appPackageName = "com.acesur.solarpvtracker"
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                                } catch (e: ActivityNotFoundException) {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::billingManager.isInitialized) {
            billingManager.endConnection()
        }
    }
    
    private fun applyLanguage(languageCode: String) {
        val locale = getLocaleFromCode(languageCode)
        
        // Store locale in companion object for attachBaseContext (used when recreate() is called)
        currentLocale = locale
        Locale.setDefault(locale)
        
        // Also save to SharedPreferences for next app launch (attachBaseContext reads this)
        getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language", languageCode)
            .apply()
    }
}