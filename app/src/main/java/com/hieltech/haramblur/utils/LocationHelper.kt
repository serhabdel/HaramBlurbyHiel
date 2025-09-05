package com.hieltech.haramblur.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.hieltech.haramblur.data.prayer.LocationData
import com.hieltech.haramblur.data.LocationAccuracy
import com.hieltech.haramblur.data.LocationPermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * Helper class for location services and permissions
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // In-memory cache for last known good location
    private var cachedLocation: LocationData? = null
    private var cachedTimestamp: Long = 0L

    /**
     * Result type describing current permission granularity.
     */
    sealed class PermissionResult {
        object FineGranted : PermissionResult()
        object CoarseGranted : PermissionResult()
        object Denied : PermissionResult()
    }

    /**
     * Returns a richer location permission status based on current grants.
     */
    fun getLocationPermissionStatus(): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return if (fineGranted || coarseGranted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.DENIED
    }

    /**
     * Returns a sealed permission result to differentiate fine vs coarse grant.
     */
    fun getPermissionResult(): PermissionResult {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) return PermissionResult.FineGranted

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return if (coarseGranted) PermissionResult.CoarseGranted else PermissionResult.Denied
    }

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

        val priority = when (getPermissionResult()) {
            PermissionResult.FineGranted -> Priority.PRIORITY_HIGH_ACCURACY
            PermissionResult.CoarseGranted -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            PermissionResult.Denied -> return null
        }

        return withTimeoutOrNull(10000L) { // 10 second timeout
            suspendCancellableCoroutine { continuation ->
                try {
                    val locationRequest = LocationRequest.Builder(
                        priority,
                        5000L
                    ).setMinUpdateIntervalMillis(2000L)
                        .setWaitForAccurateLocation(priority == Priority.PRIORITY_HIGH_ACCURACY)
                        .build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val location = locationResult.lastLocation
                            if (location != null) {
                                val locationData = com.hieltech.haramblur.data.prayer.LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy
                                )
                                // cache
                                cacheLocation(locationData)
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
            }?.also { cacheLocation(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a recently cached location if fresh enough.
     */
    fun getCachedLocation(maxAgeMs: Long = 2 * 60 * 1000L): LocationData? { // 2 minutes default freshness
        val age = System.currentTimeMillis() - cachedTimestamp
        return if (cachedLocation != null && age in 0..maxAgeMs) cachedLocation else null
    }

    private fun cacheLocation(loc: LocationData) {
        cachedLocation = loc
        cachedTimestamp = System.currentTimeMillis()
    }

    /**
     * Clear in-memory cache (useful on logout or manual refresh resets).
     */
    fun clearCache() {
        cachedLocation = null
        cachedTimestamp = 0L
    }

    /**
     * Classify accuracy in meters to a user-friendly tier.
     */
    fun classifyAccuracy(accuracyMeters: Float?): LocationAccuracy {
        if (accuracyMeters == null || accuracyMeters.isNaN()) return LocationAccuracy.UNKNOWN
        return when {
            accuracyMeters <= 20f -> LocationAccuracy.HIGH
            accuracyMeters <= 100f -> LocationAccuracy.MEDIUM
            else -> LocationAccuracy.LOW
        }
    }

    /**
     * Summarize current signal: accuracy tier and freshness.
     */
    data class SignalSummary(
        val accuracyTier: LocationAccuracy,
        val ageMs: Long,
        val isFresh: Boolean
    )

    fun summarizeSignal(accuracyMeters: Float?, timestampMs: Long?, freshThresholdMs: Long = 2 * 60 * 1000L): SignalSummary {
        val tier = classifyAccuracy(accuracyMeters)
        val age = if (timestampMs == null || timestampMs <= 0L) Long.MAX_VALUE else System.currentTimeMillis() - timestampMs
        val fresh = age in 0..freshThresholdMs
        return SignalSummary(tier, age, fresh)
    }

    /**
     * Try to get the best available location with timeout, optional retry and fallbacks (current -> last known -> cache).
     */
    suspend fun getBestLocation(
        timeoutMs: Long = 10_000L,
        retries: Int = 1,
        useLastKnownFallback: Boolean = true,
        useCacheFallback: Boolean = true
    ): LocationData? {
        if (!hasLocationPermission()) return null

        // Try current location with optional retries
        var attempt = 0
        while (attempt <= max(0, retries)) {
            val fresh = withTimeoutOrNull(timeoutMs) { getCurrentLocation() }
            if (fresh != null) return fresh
            attempt++
        }

        if (useLastKnownFallback) {
            val lastKnown = withTimeoutOrNull(3000L) { getLastKnownLocation() }
            if (lastKnown != null) return lastKnown
        }

        if (useCacheFallback) {
            val cached = getCachedLocation()
            if (cached != null) return cached
        }

        return null
    }

    /**
     * Get location permission request code
     */
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}