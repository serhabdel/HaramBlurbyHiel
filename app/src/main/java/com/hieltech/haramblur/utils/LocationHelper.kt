package com.hieltech.haramblur.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.hieltech.haramblur.data.prayer.LocationData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Helper class for location services and permissions
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted
     */
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

    /**
     * Get current location with timeout
     */
    suspend fun getCurrentLocation(): com.hieltech.haramblur.data.prayer.LocationData? {
        if (!hasLocationPermission()) {
            return null
        }

        return withTimeoutOrNull(10000L) { // 10 second timeout
            suspendCancellableCoroutine { continuation ->
                try {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        5000L
                    ).build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val location = locationResult.lastLocation
                            if (location != null) {
                                val locationData = com.hieltech.haramblur.data.prayer.LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy
                                )
                                continuation.resume(locationData)
                                fusedLocationClient.removeLocationUpdates(this)
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                            if (!locationAvailability.isLocationAvailable) {
                                continuation.resume(null)
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }
                    }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        null
                    )

                    continuation.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Get last known location (faster but less accurate)
     */
    suspend fun getLastKnownLocation(): com.hieltech.haramblur.data.prayer.LocationData? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val task = fusedLocationClient.lastLocation
            val location = kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { continuation ->
                task.addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            }

            location?.let {
                com.hieltech.haramblur.data.prayer.LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get location permission request code
     */
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}