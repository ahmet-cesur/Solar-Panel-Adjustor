package com.acesur.solarpvtracker.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import kotlinx.coroutines.flow.first

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val isFromGps: Boolean = true
)

class LocationHelper(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): UserLocation? {
        // Check if GPS usage is enabled
        val useGps = preferencesManager.useGps.first()
        
        if (!useGps) {
            val lat = preferencesManager.manualLatitude.first()
            val lng = preferencesManager.manualLongitude.first()
            return UserLocation(
                latitude = lat,
                longitude = lng,
                isFromGps = false
            )
        }
    
        if (!hasLocationPermission()) {
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(
                            UserLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                isFromGps = true
                            )
                        )
                    } else {
                        // Try to get last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                continuation.resume(
                                    UserLocation(
                                        latitude = lastLocation.latitude,
                                        longitude = lastLocation.longitude,
                                        isFromGps = true
                                    )
                                )
                            } else {
                                continuation.resume(null)
                            }
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    continuation.resume(null)
                }
                
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }
}
