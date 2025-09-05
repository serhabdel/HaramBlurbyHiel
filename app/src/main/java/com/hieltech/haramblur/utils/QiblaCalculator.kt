package com.hieltech.haramblur.utils

import android.hardware.GeomagneticField
import android.location.Location
import android.content.Context
import kotlin.math.*

object QiblaCalculator {
    // Kaaba coordinates
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    /**
     * Calculates the true bearing (relative to true north) from a user location to the Kaaba.
     */
    fun calculateQiblaBearing(userLat: Double, userLon: Double): Double {
        require(userLat in -90.0..90.0 && userLon in -180.0..180.0) { "Invalid coordinates" }
        val lat1 = Math.toRadians(userLat)
        val lon1 = Math.toRadians(userLon)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = atan2(y, x)
        var deg = Math.toDegrees(brng)
        if (deg < 0) deg += 360.0
        return deg
    }

    fun distanceToMecca(userLat: Double, userLon: Double): Double {
        val result = FloatArray(1)
        Location.distanceBetween(userLat, userLon, KAABA_LAT, KAABA_LON, result)
        return result[0].toDouble() // meters
    }

    /**
     * Returns local magnetic declination in degrees for the given coordinates and time.
     * Positive declination means magnetic north is east of true north.
     * @param context Android context
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param altMeters Altitude in meters (optional, defaults to 0)
     * @param timeMillis Time in milliseconds (defaults to current time)
     */
    fun getMagneticDeclination(
        context: Context,
        lat: Double,
        lon: Double,
        altMeters: Float? = null,
        timeMillis: Long = System.currentTimeMillis()
    ): Float {
        return try {
            require(lat in -90.0..90.0 && lon in -180.0..180.0) { "Invalid coordinates" }
            val altitude = altMeters ?: 0f
            val geo = GeomagneticField(lat.toFloat(), lon.toFloat(), altitude, timeMillis)
            geo.declination
        } catch (_: Throwable) {
            0f
        }
    }

    /**
     * Applies magnetic declination to a magnetic bearing to obtain true bearing.
     * Formula: True = Magnetic + Declination
     * Both inputs are in degrees. Result is normalized to [0, 360).
     */
    fun correctForMagneticDeclination(bearing: Double, declination: Float): Double {
        val safeDecl = declination.coerceIn(-180f, 180f)
        val corrected = bearing + safeDecl
        return normalize(corrected)
    }

    /**
     * Converts a device magnetic azimuth (0-360 relative to magnetic north) to true azimuth
     * by applying the local magnetic declination.
     */
    fun calculateTrueAzimuth(magneticAzimuth: Double, declination: Float): Double {
        require(magneticAzimuth.isFinite()) { "magneticAzimuth must be finite" }
        return correctForMagneticDeclination(normalize(magneticAzimuth), declination)
    }

    fun normalize(deg: Double): Double {
        return normalizeDegrees(deg)
    }

    fun shortestDiff(a: Float, b: Float): Float {
        return shortestAngleDiff(a, b)
    }
}
