package com.hieltech.haramblur.data.compass

import com.hieltech.haramblur.data.LocationAccuracy

/**
 * Sensor accuracy levels reported by the compass system.
 */
enum class SensorAccuracy {
    HIGH,
    MEDIUM,
    LOW,
    UNRELIABLE,
    UNAVAILABLE
}

/**
 * Calibration status for the compass.
 */
enum class CalibrationStatus {
    CALIBRATED,
    NEEDS_CALIBRATION,
    CALIBRATING,
    SENSOR_UNAVAILABLE
}

/**
 * Error types for compass-related failures.
 */
sealed class CompassError {
    object SENSOR_UNAVAILABLE : CompassError()
    object LOCATION_UNAVAILABLE : CompassError()
    object PERMISSION_DENIED : CompassError()
    data class CALCULATION_ERROR(val message: String? = null) : CompassError()
}

/**
 * Runtime compass state derived from sensors and calculations.
 */
data class CompassState(
    // Raw magnetic azimuth from sensors (relative to magnetic north), normalized to [0,360)
    val deviceAzimuth: Float = 0f,
    // True azimuth (relative to true north), normalized to [0,360)
    val trueAzimuth: Float = 0f,
    // True bearing to Qibla (relative to true north), normalized to [0,360)
    val qiblaBearing: Double = 0.0,
    val angleToQibla: Float = 0f, // signed shortest angle device->qibla
    val sensorAccuracy: SensorAccuracy = SensorAccuracy.UNAVAILABLE,
    val isCalibrationNeeded: Boolean = false,
    val magneticDeclination: Float = 0f
)

/**
 * Aggregate UI state for Qibla compass.
 */
data class QiblaCompassData(
    val compassState: CompassState = CompassState(),
    val locationAccuracy: LocationAccuracy = LocationAccuracy.UNKNOWN,
    val lastLocationUpdate: Long? = null,
    val isLoading: Boolean = true,
    val error: CompassError? = null
)

/**
 * User preferences for compass behavior and UI.
 */
data class CompassSettings(
    val enableVibration: Boolean = false,
    val showDegreeMarkings: Boolean = true,
    val compassSensitivity: Float = 1.0f,
    val accuracyThreshold: Float = 20.0f,
    val qiblaToleranceDegrees: Float = 5.0f,
    val animationSpeed: Float = 1.0f,
    val enableMagneticDeclination: Boolean = true,
    val updateRateHz: Int = 15
)

/**
 * Preferred sizes for compass UI rendering.
 */
enum class CompassSize { SMALL, MEDIUM, LARGE }

/**
 * Constants and helpers for angle/bearing operations.
 */
object CompassConstants {
    const val SENSOR_UPDATE_RATE_HZ_DEFAULT = 15
    const val LOW_PASS_ALPHA = 0.15f
}

// Angle utility functions moved to com.hieltech.haramblur.utils.Angles

/**
 * Calculates the angle from the device heading to the Qibla direction, properly handling
 * magnetic declination by converting magnetic azimuth to true azimuth first.
 *
 * @param magneticAzimuth Device heading relative to magnetic north [0,360)
 * @param qiblaBearing True bearing to Qibla [0,360)
 * @param declination Local magnetic declination in degrees (east positive)
 * @return Signed shortest angle (degrees) from device heading to Qibla
 */
fun calculateAngleToQiblaWithDeclination(
    magneticAzimuth: Float,
    qiblaBearing: Double,
    declination: Float
): Float {
    // Convert magnetic to true azimuth using QiblaCalculator for consistency and robustness
    val trueAz = com.hieltech.haramblur.utils.QiblaCalculator.calculateTrueAzimuth(magneticAzimuth.toDouble(), declination).toFloat()
    return com.hieltech.haramblur.utils.shortestAngleDiff(trueAz, qiblaBearing.toFloat())
}

fun bearingToCardinal(deg: Double): String {
    val dirs = arrayOf("N","NE","E","SE","S","SW","W","NW")
    val ix = ((deg / 45.0) + 0.5).toInt() and 7
    return dirs[ix]
}
