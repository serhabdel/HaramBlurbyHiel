package com.hieltech.haramblur.utils

/**
 * Utility functions for angle and bearing calculations.
 * Single source of truth for angle normalization and difference calculations.
 */

/**
 * Normalizes an angle in degrees to the range [0, 360).
 * @param angle Angle in degrees
 * @return Normalized angle in degrees
 */
fun normalizeDegrees(angle: Float): Float {
    var a = angle % 360f
    if (a < 0) a += 360f
    return a
}

/**
 * Normalizes an angle in degrees to the range [0, 360).
 * @param angle Angle in degrees
 * @return Normalized angle in degrees
 */
fun normalizeDegrees(angle: Double): Double {
    var a = angle % 360.0
    if (a < 0) a += 360.0
    return a
}

/**
 * Calculates the shortest signed angle difference between two angles.
 * @param from Starting angle in degrees
 * @param to Target angle in degrees
 * @return Signed shortest angle difference in degrees (-180 to +180)
 */
fun shortestAngleDiff(from: Float, to: Float): Float {
    var diff = (to - from + 540f) % 360f - 180f
    if (diff < -180f) diff += 360f
    return diff
}

/**
 * Calculates the shortest signed angle difference between two angles.
 * @param from Starting angle in degrees
 * @param to Target angle in degrees
 * @return Signed shortest angle difference in degrees (-180 to +180)
 */
fun shortestAngleDiff(from: Double, to: Double): Double {
    var diff = (to - from + 540.0) % 360.0 - 180.0
    if (diff < -180.0) diff += 360.0
    return diff
}
