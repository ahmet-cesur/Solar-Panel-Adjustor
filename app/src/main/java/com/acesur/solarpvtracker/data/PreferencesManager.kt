package com.acesur.solarpvtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "solar_pv_settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_USE_GPS = booleanPreferencesKey("use_gps")
        private val KEY_MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")
        private val KEY_MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")
        private val KEY_PANEL_WATTAGE = intPreferencesKey("panel_wattage")
        private val KEY_PANEL_COUNT = intPreferencesKey("panel_count")
        private val KEY_PANEL_EFFICIENCY = floatPreferencesKey("panel_efficiency")
        
        // Ad-free settings
        private val KEY_AD_FREE = booleanPreferencesKey("ad_free")
        private val KEY_AD_FREE_EXPIRY = longPreferencesKey("ad_free_expiry")
        private val KEY_EASTER_EGG_ACTIVATED = booleanPreferencesKey("easter_egg_activated")
        
        // Rate App Logic
        private val KEY_APP_USAGE_TIME = longPreferencesKey("app_usage_time")
        private val KEY_RATE_APP_SHOWN_COUNT = intPreferencesKey("rate_app_shown_count")
        private val KEY_RATE_APP_LAST_SHOWN_TIME = longPreferencesKey("rate_app_last_shown_time")
        private val KEY_HAS_RATED_APP = booleanPreferencesKey("has_rated_app")
        private val KEY_NEVER_SHOW_RATE_APP = booleanPreferencesKey("never_show_rate_app")
    }
    
    // Language
    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }
    
    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = languageCode
        }
    }
    
    // First Launch
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_FIRST_LAUNCH] ?: true
    }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] = false
        }
    }
    
    // Dark Mode
    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }
    
    // GPS Mode
    val useGps: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_USE_GPS] ?: true
    }
    
    suspend fun setUseGps(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_GPS] = enabled
        }
    }
    
    // Manual Location
    val manualLatitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_MANUAL_LATITUDE] ?: 0.0
    }
    
    val manualLongitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_MANUAL_LONGITUDE] ?: 0.0
    }
    
    suspend fun setManualLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MANUAL_LATITUDE] = latitude
            preferences[KEY_MANUAL_LONGITUDE] = longitude
        }
    }
    
    // Panel Settings
    val panelWattage: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PANEL_WATTAGE] ?: 400
    }
    
    val panelCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PANEL_COUNT] ?: 1
    }
    
    val panelEfficiency: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_PANEL_EFFICIENCY] ?: 0.20f
    }
    
    suspend fun setPanelSettings(wattage: Int, count: Int, efficiency: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PANEL_WATTAGE] = wattage
            preferences[KEY_PANEL_COUNT] = count
            preferences[KEY_PANEL_EFFICIENCY] = efficiency
        }
    }
    
    // Ad-Free Status
    val isAdFree: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val isPermanent = preferences[KEY_AD_FREE] ?: false
        val expiryTime = preferences[KEY_AD_FREE_EXPIRY] ?: 0L
        
        isPermanent || (expiryTime > System.currentTimeMillis())
    }
    
    val adFreeExpiry: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_AD_FREE_EXPIRY] ?: 0L
    }
    
    val isPermanentAdFree: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AD_FREE] ?: false
    }
    
    /**
     * Set ad-free for 30 days
     */
    suspend fun setAdFreeFor30Days() {
        val expiryTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        context.dataStore.edit { preferences ->
            preferences[KEY_AD_FREE_EXPIRY] = expiryTime
        }
    }
    
    /**
     * Set ad-free permanently
     */
    suspend fun setAdFreeForever() {
        context.dataStore.edit { preferences ->
            preferences[KEY_AD_FREE] = true
        }
    }
    
    /**
     * Reset ad-free status (for testing)
     */
    suspend fun resetAdFreeStatus() {
        context.dataStore.edit { preferences ->
            preferences[KEY_AD_FREE] = false
            preferences[KEY_AD_FREE_EXPIRY] = 0L
            preferences[KEY_EASTER_EGG_ACTIVATED] = false
        }
    }
    
    /**
     * Check if easter egg was activated
     */
    val isEasterEggActivated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_EASTER_EGG_ACTIVATED] ?: false
    }
    
    /**
     * Set easter egg activated (for ad-free via easter egg)
     */
    suspend fun setEasterEggActivated() {
        context.dataStore.edit { preferences ->
            preferences[KEY_AD_FREE] = true
            preferences[KEY_EASTER_EGG_ACTIVATED] = true
        }
    }

    // Rate App Logic
    val appUsageTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_USAGE_TIME] ?: 0L
    }

    suspend fun incrementUsageTime(timeMillis: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_APP_USAGE_TIME] ?: 0L
            preferences[KEY_APP_USAGE_TIME] = current + timeMillis
        }
    }

    val rateAppShownCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_RATE_APP_SHOWN_COUNT] ?: 0
    }

    val rateAppLastShownTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_RATE_APP_LAST_SHOWN_TIME] ?: 0L
    }

    val hasRatedApp: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAS_RATED_APP] ?: false
    }

    val neverShowRateApp: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NEVER_SHOW_RATE_APP] ?: false
    }

    suspend fun setRateAppShown() {
        val currentTime = System.currentTimeMillis()
        context.dataStore.edit { preferences ->
            val currentCount = preferences[KEY_RATE_APP_SHOWN_COUNT] ?: 0
            preferences[KEY_RATE_APP_SHOWN_COUNT] = currentCount + 1
            preferences[KEY_RATE_APP_LAST_SHOWN_TIME] = currentTime
        }
    }

    suspend fun setHasRatedApp() {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_RATED_APP] = true
        }
    }

    suspend fun setNeverShowRateApp() {
        context.dataStore.edit { preferences ->
            preferences[KEY_NEVER_SHOW_RATE_APP] = true
        }
    }
    
    suspend fun resetRateAppStats() {
        context.dataStore.edit { preferences ->
            preferences[KEY_RATE_APP_SHOWN_COUNT] = 0
            preferences[KEY_RATE_APP_LAST_SHOWN_TIME] = 0L
            preferences[KEY_HAS_RATED_APP] = false
            preferences[KEY_NEVER_SHOW_RATE_APP] = false
        }
    }
}
