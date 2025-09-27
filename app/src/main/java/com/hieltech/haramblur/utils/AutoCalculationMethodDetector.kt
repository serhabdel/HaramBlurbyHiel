package com.hieltech.haramblur.utils

import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.LocationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatically detects and suggests the best prayer calculation method based on user's location
 */
@Singleton
class AutoCalculationMethodDetector @Inject constructor(
    private val moroccanLocationHelper: MoroccanLocationHelper
) {

    /**
     * Detect the best calculation method based on location
     */
    fun detectCalculationMethod(latitude: Double, longitude: Double): PrayerCalculationMethod {
        return when {
            // Morocco - Use Morocco Ministry method (18° Fajr, 17° Isha - official method)
            moroccanLocationHelper.isInMorocco(latitude, longitude) -> {
                PrayerCalculationMethod.MOROCCO_MINISTRY
            }
            
            // Saudi Arabia and surrounding Gulf region
            isInSaudiArabia(latitude, longitude) -> {
                PrayerCalculationMethod.UMM_AL_QURA_UNIVERSITY
            }
            
            // Gulf countries (UAE, Kuwait, Qatar, Bahrain, Oman)
            isInGulfRegion(latitude, longitude) -> {
                PrayerCalculationMethod.GULF_REGION
            }
            
            // Kuwait specifically
            isInKuwait(latitude, longitude) -> {
                PrayerCalculationMethod.KUWAIT
            }
            
            // Qatar specifically
            isInQatar(latitude, longitude) -> {
                PrayerCalculationMethod.QATAR
            }
            
            // Egypt
            isInEgypt(latitude, longitude) -> {
                PrayerCalculationMethod.EGYPTIAN_GENERAL_AUTHORITY
            }
            
            // Iran
            isInIran(latitude, longitude) -> {
                PrayerCalculationMethod.INSTITUTE_OF_GEOPHYSICS
            }
            
            // Pakistan and surrounding region
            isInPakistan(latitude, longitude) -> {
                PrayerCalculationMethod.UNIVERSITY_OF_KARACHI
            }
            
            // North America
            isInNorthAmerica(latitude, longitude) -> {
                PrayerCalculationMethod.ISLAMIC_SOCIETY_OF_NORTH_AMERICA
            }
            
            // Singapore and Malaysia
            isInSingaporeRegion(latitude, longitude) -> {
                PrayerCalculationMethod.MAJLIS_UGAMA_ISLAM_SINGAPURA
            }
            
            // Default to Muslim World League for other regions
            else -> PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE
        }
    }

    /**
     * Get calculation method recommendation with confidence level
     */
    fun getRecommendation(latitude: Double, longitude: Double): CalculationMethodRecommendation {
        val method = detectCalculationMethod(latitude, longitude)
        val confidence = getConfidenceLevel(latitude, longitude, method)
        val reason = getRecommendationReason(latitude, longitude, method)
        
        return CalculationMethodRecommendation(
            method = method,
            confidence = confidence,
            reason = reason
        )
    }

    /**
     * Check if current settings are optimal for location
     */
    fun isOptimalForLocation(settings: AppSettings): Boolean {
        val lat = settings.locationLatitude
        val lon = settings.locationLongitude
        
        if (lat == null || lon == null) return true // Can't determine without location
        
        val recommendedMethod = detectCalculationMethod(lat, lon)
        return settings.prayerCalculationMethod == recommendedMethod.id
    }

    /**
     * Get suggestion message for user
     */
    fun getSuggestionMessage(settings: AppSettings): String? {
        val lat = settings.locationLatitude
        val lon = settings.locationLongitude
        
        if (lat == null || lon == null) return null
        
        val recommendation = getRecommendation(lat, lon)
        val currentMethod = PrayerCalculationMethod.values()
            .find { it.id == settings.prayerCalculationMethod }
        
        return if (currentMethod != recommendation.method) {
            "For your location, we recommend using ${recommendation.method.displayName}. ${recommendation.reason}"
        } else null
    }

    // Region detection functions
    private fun isInSaudiArabia(lat: Double, lon: Double): Boolean {
        return lat in 16.0..32.5 && lon in 34.5..55.7
    }

    private fun isInGulfRegion(lat: Double, lon: Double): Boolean {
        // UAE, Bahrain, Oman (excluding Saudi, Kuwait, Qatar which have specific methods)
        return (lat in 22.5..26.5 && lon in 51.0..56.5) || // UAE
               (lat in 25.5..26.5 && lon in 50.0..51.0) || // Bahrain
               (lat in 16.0..26.5 && lon in 51.5..60.0)    // Oman
    }

    private fun isInKuwait(lat: Double, lon: Double): Boolean {
        return lat in 28.5..30.1 && lon in 46.5..48.5
    }

    private fun isInQatar(lat: Double, lon: Double): Boolean {
        return lat in 24.4..26.2 && lon in 50.7..51.7
    }

    private fun isInEgypt(lat: Double, lon: Double): Boolean {
        return lat in 22.0..31.7 && lon in 25.0..35.0
    }

    private fun isInIran(lat: Double, lon: Double): Boolean {
        return lat in 25.0..40.0 && lon in 44.0..63.5
    }

    private fun isInPakistan(lat: Double, lon: Double): Boolean {
        return lat in 23.5..37.1 && lon in 60.9..77.8
    }

    private fun isInNorthAmerica(lat: Double, lon: Double): Boolean {
        return lat in 25.0..72.0 && lon in -168.0..-52.0
    }

    private fun isInSingaporeRegion(lat: Double, lon: Double): Boolean {
        // Singapore and Malaysia
        return lat in 1.0..7.5 && lon in 99.5..105.0
    }

    private fun getConfidenceLevel(lat: Double, lon: Double, method: PrayerCalculationMethod): ConfidenceLevel {
        return when {
            moroccanLocationHelper.isInMorocco(lat, lon) && method == PrayerCalculationMethod.MOROCCO_MINISTRY ->
                ConfidenceLevel.HIGH
            method == PrayerCalculationMethod.UMM_AL_QURA_UNIVERSITY && isInSaudiArabia(lat, lon) ->
                ConfidenceLevel.HIGH
            method == PrayerCalculationMethod.ISLAMIC_SOCIETY_OF_NORTH_AMERICA && isInNorthAmerica(lat, lon) ->
                ConfidenceLevel.HIGH
            method == PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE ->
                ConfidenceLevel.HIGH // MWL is widely accepted
            else -> ConfidenceLevel.MEDIUM
        }
    }

    private fun getRecommendationReason(lat: Double, lon: Double, method: PrayerCalculationMethod): String {
        return when {
            moroccanLocationHelper.isInMorocco(lat, lon) && method == PrayerCalculationMethod.MOROCCO_MINISTRY ->
                "This is Morocco's official prayer time calculation method used by the Ministry of Islamic Affairs, using 18° for Fajr and 17° for Isha."
            method == PrayerCalculationMethod.UMM_AL_QURA_UNIVERSITY ->
                "This method is widely used in Saudi Arabia and the surrounding region."
            method == PrayerCalculationMethod.ISLAMIC_SOCIETY_OF_NORTH_AMERICA ->
                "This method is commonly used in North America."
            method == PrayerCalculationMethod.EGYPTIAN_GENERAL_AUTHORITY ->
                "This method is officially used in Egypt."
            method == PrayerCalculationMethod.GULF_REGION ->
                "This method is optimized for Gulf countries."
            else ->
                "This method is widely accepted internationally."
        }
    }

    data class CalculationMethodRecommendation(
        val method: PrayerCalculationMethod,
        val confidence: ConfidenceLevel,
        val reason: String
    )

    enum class ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }
}
