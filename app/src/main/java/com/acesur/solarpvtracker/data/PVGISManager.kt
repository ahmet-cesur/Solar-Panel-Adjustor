package com.acesur.solarpvtracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class PVGISManager(private val context: Context, private val preferencesManager: PreferencesManager) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getOptimalTilt(latitude: Double, longitude: Double): Float? {
        val result = getFullEstimation(latitude, longitude)
        return try {
            result?.getJSONObject("inputs")
                ?.getJSONObject("mounting_system")
                ?.getJSONObject("fixed")
                ?.getJSONObject("slope")
                ?.getDouble("value")
                ?.toFloat()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Haversine formula to calculate distance between two points in km
     */
    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    suspend fun getFullEstimation(latitude: Double, longitude: Double): JSONObject? {
        val lastFetchTime = preferencesManager.pvgisLastFetchTime.first()
        val lastLocation = preferencesManager.pvgisLastLocation.first()
        val cachedJson = preferencesManager.pvgisMonthlyData.first()
        
        val currentTime = System.currentTimeMillis()
        val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)
        
        var isWithinRadius = false
        if (lastLocation != null) {
            val parts = lastLocation.split(",")
            if (parts.size == 2) {
                val lastLat = parts[0].toDoubleOrNull() ?: 0.0
                val lastLon = parts[1].toDoubleOrNull() ?: 0.0
                val distance = calculateDistanceInKm(latitude, longitude, lastLat, lastLon)
                isWithinRadius = distance <= 5.0
            }
        }
        
        if (cachedJson != null && (isWithinRadius || (currentTime - lastFetchTime < ONE_DAY_MS))) {
            return try { JSONObject(cachedJson) } catch (e: Exception) { null }
        }

        return try {
            val result = fetchFullFromApi(latitude, longitude)
            val jsonStr = result.toString()
            val angle = result.getJSONObject("inputs")
                .getJSONObject("mounting_system")
                .getJSONObject("fixed")
                .getJSONObject("slope")
                .getDouble("value")
                .toFloat()
            
            preferencesManager.savePVGISFullData(angle, latitude, longitude, jsonStr)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            cachedJson?.let { try { JSONObject(it) } catch (ex: Exception) { null } }
        }
    }

    private suspend fun fetchFullFromApi(latitude: Double, longitude: Double): JSONObject = withContext(Dispatchers.IO) {
        val url = "https://re.jrc.ec.europa.eu/api/v5_2/PVcalc?" +
                "lat=${latitude}&lon=${longitude}" +
                "&peakpower=1&loss=14&optimalinclination=1&outputformat=json"

        val request = Request.Builder().url(url).build()
        
        suspendCancellableCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            continuation.resumeWithException(IOException("Unexpected code $response"))
                            return
                        }

                        val jsonStr = response.body?.string() ?: ""
                        try {
                            val json = JSONObject(jsonStr)
                            continuation.resume(json) {}
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }
    }
}
