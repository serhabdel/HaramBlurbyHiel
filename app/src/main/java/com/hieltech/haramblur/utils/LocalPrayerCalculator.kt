package com.hieltech.haramblur.utils

import com.hieltech.haramblur.data.prayer.PrayerTimings
import kotlin.math.*
import java.util.*
import javax.inject.Inject

/**
 * Lightweight local prayer time calculator (Adhan-like angles) for offline fallback.
 * Uses standard solar position formulas (NOAA) and common method angle presets.
 *
 * Features:
 * - Morocco-specific calculation method (18° Fajr, 17° Isha) matching Ministry of Islamic Affairs
 * - Automatic timezone detection and handling
 * - City-specific adjustments for major Moroccan cities
 * - Offline calculation without API dependencies
 *
 * NOTE: This is a simplified implementation intended as a reliable fallback.
 */
class LocalPrayerCalculator @Inject constructor(
    private val moroccanLocationHelper: MoroccanLocationHelper
) {

    data class Angles(val fajr: Double, val isha: Double, val ishaIntervalMinutes: Int? = null)

    private fun methodAngles(method: Int): Angles = when (method) {
        1 -> Angles(18.0, 18.0) // Karachi
        2 -> Angles(15.0, 15.0) // ISNA
        3 -> Angles(18.0, 17.0) // MWL
        4 -> Angles(18.5, 0.0, ishaIntervalMinutes = 90) // Umm Al‑Qura (approx; 90m)
        5 -> Angles(19.5, 17.5) // Egypt
        7 -> Angles(17.7, 14.0) // Tehran (approx)
        13 -> Angles(18.0, 17.0) // Diyanet (approx)
        15 -> Angles(18.0, 17.0) // Morocco Ministry of Islamic Affairs (18° Fajr, 17° Isha)
        else -> Angles(18.0, 17.0)
    }

    private fun dayOfYear(cal: Calendar): Int {
        return cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun solarDeclination(d: Double): Double {
        // approximate declination in radians
        val g = Math.toRadians(357.529 + 0.98560028 * d)
        val q = Math.toRadians(280.459 + 0.98564736 * d)
        val L = q + Math.toRadians(1.915) * sin(g) + Math.toRadians(0.020) * sin(2 * g)
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        return asin(sin(e) * sin(L))
    }

    private fun equationOfTime(d: Double): Double {
        // in minutes
        val g = Math.toRadians(357.529 + 0.98560028 * d)
        val q = Math.toRadians(280.459 + 0.98564736 * d)
        val L = q + Math.toRadians(1.915) * sin(g) + Math.toRadians(0.020) * sin(2 * g)
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val ra = atan2(cos(e) * sin(L), cos(L))
        var R = Math.toDegrees(q - ra)
        R = (R + 360.0) % 360.0
        if (R > 180) R -= 360
        return 4.0 * R
    }

    private fun hourAngle(latRad: Double, dec: Double, altitudeDeg: Double): Double {
        val h = Math.toRadians(altitudeDeg)
        val cosH = (sin(h) - sin(latRad) * sin(dec)) / (cos(latRad) * cos(dec))
        return when {
            cosH <= -1 -> Math.PI // never sets/rises – polar circle; clamp
            cosH >= 1 -> 0.0
            else -> acos(cosH)
        }
    }

    // Asr hour angle based on shadow factor (1 for Shafi, 2 for Hanafi)
    private fun asrHourAngle(latRad: Double, dec: Double, factor: Int): Double {
        val angle = -atan(1.0 / (factor + tan(abs(latRad - dec))))
        return hourAngle(latRad, dec, Math.toDegrees(angle))
    }

    private fun timeString(hours: Double): String {
        var h = hours
        while (h < 0) h += 24.0
        while (h >= 24) h -= 24.0
        val m = ((h - floor(h)) * 60.0).roundToInt()
        val hh = floor(h).toInt()
        val mm = if (m == 60) 0 else m
        val adjH = if (m == 60) (hh + 1) % 24 else hh
        return String.format(Locale.US, "%02d:%02d", adjH, mm)
    }

    fun compute(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double,
        method: Int,
        asrFactor: Int = 1,
        adjustmentsMinutes: Map<String, Int> = emptyMap()
    ): PrayerTimings {
        val d = dayOfYear(calendar).toDouble()
        val dec = solarDeclination(d)
        val eqtMin = equationOfTime(d) // minutes
        val latRad = Math.toRadians(latitude)

        // Solar noon
        val noon = 12 + tzOffsetHours - (longitude / 15.0) - (eqtMin / 60.0)

        // Sunrise/sunset
        val sunriseHa = hourAngle(latRad, dec, -0.833)
        val sunrise = noon - Math.toDegrees(sunriseHa) / 15.0
        val sunset = noon + Math.toDegrees(sunriseHa) / 15.0

        // Fajr / Isha
        val angles = methodAngles(method)
        val fajrHa = hourAngle(latRad, dec, -angles.fajr)
        val fajr = noon - Math.toDegrees(fajrHa) / 15.0

        val isha = if (angles.ishaIntervalMinutes != null) {
            sunset + angles.ishaIntervalMinutes / 60.0
        } else {
            val ishaHa = hourAngle(latRad, dec, -angles.isha)
            noon + Math.toDegrees(ishaHa) / 15.0
        }

        // Asr
        val asrHa = asrHourAngle(latRad, dec, asrFactor)
        val asr = noon + Math.toDegrees(asrHa) / 15.0

        // Dhuhr
        val dhuhr = noon

        // Format with adjustments
        fun adj(name: String, value: Double): String {
            val addMin = adjustmentsMinutes[name] ?: 0
            return timeString(value + addMin / 60.0)
        }

        return PrayerTimings(
            Fajr = adj("Fajr", fajr),
            Dhuhr = adj("Dhuhr", dhuhr),
            Asr = adj("Asr", asr),
            Maghrib = adj("Maghrib", sunset),
            Isha = adj("Isha", isha),
            Sunrise = adj("Sunrise", sunrise),
            Sunset = adj("Sunset", sunset),
            Imsak = null,
            Midnight = null,
            Firstthird = null,
            Lastthird = null
        )
    }

    /**
     * Convenience method for calculating prayer times in Morocco
     * Automatically uses Morocco Ministry method and handles timezone
     */
    fun computeForMorocco(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        asrFactor: Int = 1,
        adjustmentsMinutes: Map<String, Int> = emptyMap()
    ): PrayerTimings {
        // Morocco uses UTC+1 (or UTC+0 during Ramadan in some years)
        val tzOffsetHours = TimeZone.getDefault().rawOffset / (1000 * 60 * 60).toDouble()
        
        return compute(
            calendar = calendar,
            latitude = latitude,
            longitude = longitude,
            tzOffsetHours = tzOffsetHours,
            method = 15, // Morocco Ministry method ID
            asrFactor = asrFactor,
            adjustmentsMinutes = adjustmentsMinutes
        )
    }

    /**
     * Get recommended adjustments for Moroccan cities using MoroccanLocationHelper
     * These are regional adjustments based on local practices
     */
    fun getMoroccanCityAdjustments(cityName: String): Map<String, Int> {
        return moroccanLocationHelper.getCityAdjustments(cityName)
    }

    /**
     * Get Moroccan adjustments for coordinates using MoroccanLocationHelper
     */
    fun getMoroccanAdjustmentsForCoordinates(latitude: Double, longitude: Double): Map<String, Int> {
        return moroccanLocationHelper.getAdjustmentsForCoordinates(latitude, longitude)
    }
}

