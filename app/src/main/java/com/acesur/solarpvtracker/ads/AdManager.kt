package com.acesur.solarpvtracker.ads

import android.app.Activity
import android.content.Context
import com.acesur.solarpvtracker.data.PreferencesManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/**
 * AdManager handles AdMob initialization and ad loading.
 * 
 * IMPORTANT: Replace test ad unit IDs with your real AdMob IDs before publishing:
 * - Banner: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
 * - Interstitial: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
 */
class AdManager(private val context: Context) {
    
    companion object {
        // Real Ad Unit IDs provided by user
        const val BANNER_AD_UNIT_ID = "ca-app-pub-6223654168327818/8114482012"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6223654168327818/3481920672"
        
        // Interstitial ad probability (30%)
        const val INTERSTITIAL_PROBABILITY = 0.30f
        
        private var isInitialized = false
    }
    
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private val preferencesManager = PreferencesManager(context)
    
    /**
     * Check if ads should be shown (user hasn't purchased ad removal)
     */
    fun shouldShowAds(): Boolean {
        return runBlocking {
            !preferencesManager.isAdFree.first()
        }
    }
    
    /**
     * Initialize Mobile Ads SDK (call once at app startup)
     */
    fun initialize(onInitComplete: () -> Unit = {}) {
        if (isInitialized) {
            onInitComplete()
            return
        }
        
        MobileAds.initialize(context) {
            isInitialized = true
            onInitComplete()
        }
    }
    
    /**
     * Load an interstitial ad
     */
    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}, onAdFailed: (String) -> Unit = {}) {
        if (!shouldShowAds()) return
        
        if (isLoadingInterstitial || interstitialAd != null) {
            return
        }
        
        isLoadingInterstitial = true
        
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    onAdLoaded()
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            // Preload next ad
                            loadInterstitialAd()
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                        }
                    }
                }
                
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingInterstitial = false
                    interstitialAd = null
                    onAdFailed(adError.message)
                }
            }
        )
    }
    
    /**
     * Show interstitial ad with 30% probability
     * Returns true if ad was shown, false otherwise
     */
    fun maybeShowInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit = {},
        onNoAdShown: () -> Unit = {}
    ) {
        if (!shouldShowAds()) {
            onNoAdShown()
            return
        }
        
        // 30% probability
        if (Random.nextFloat() > INTERSTITIAL_PROBABILITY) {
            onNoAdShown()
            return
        }
        
        showInterstitialAd(activity, onAdDismissed)
    }
    
    /**
     * Show interstitial ad if available (always show, ignore probability)
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (!shouldShowAds()) {
            onAdDismissed()
            return
        }
        
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    // Preload next ad
                    loadInterstitialAd()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
    
    /**
     * Check if interstitial ad is ready
     */
    fun isInterstitialReady(): Boolean = interstitialAd != null && shouldShowAds()
    
    /**
     * Create an AdRequest for banner ads
     */
    fun createAdRequest(): AdRequest = AdRequest.Builder().build()
}
