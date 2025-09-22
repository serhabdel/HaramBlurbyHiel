package com.hieltech.haramblur.utils

import com.hieltech.haramblur.data.cities.CitySelection
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import kotlin.math.*

/**
 * Helper for detecting Moroccan locations and providing appropriate prayer calculation methods
 */
object MoroccanLocationHelper {

    /**
     * Major Moroccan cities with their coordinates
     */
    private val moroccanCities = listOf(
        MoroccanCity("Casablanca", 33.5731, -7.5898, "Casablanca-Settat"),
        MoroccanCity("Rabat", 34.0209, -6.8416, "Rabat-Salé-Kénitra"),
        MoroccanCity("Fès", 34.0181, -5.0078, "Fès-Meknès"),
        MoroccanCity("Marrakech", 31.6295, -7.9811, "Marrakech-Safi"),
        MoroccanCity("Agadir", 30.4278, -9.5981, "Souss-Massa"),
        MoroccanCity("Tangier", 35.7595, -5.8340, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Meknès", 33.8935, -5.5473, "Fès-Meknès"),
        MoroccanCity("Oujda", 34.6814, -1.9086, "Oriental"),
        MoroccanCity("Kenitra", 34.2610, -6.5802, "Rabat-Salé-Kénitra"),
        MoroccanCity("Tetouan", 35.5889, -5.3626, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Safi", 32.2994, -9.2372, "Marrakech-Safi"),
        MoroccanCity("Mohammedia", 33.6866, -7.3830, "Casablanca-Settat"),
        MoroccanCity("Khouribga", 32.8811, -6.9063, "Béni Mellal-Khénifra"),
        MoroccanCity("El Jadida", 33.2316, -8.5007, "Casablanca-Settat"),
        MoroccanCity("Béni Mellal", 32.3373, -6.3498, "Béni Mellal-Khénifra"),
        MoroccanCity("Nador", 35.1681, -2.9287, "Oriental"),
        MoroccanCity("Taza", 34.2133, -4.0103, "Fès-Meknès"),
        MoroccanCity("Settat", 33.0013, -7.6216, "Casablanca-Settat"),
        MoroccanCity("Larache", 35.1932, -6.1563, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Ksar El Kebir", 35.0017, -5.9008, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Khemisset", 33.8244, -6.0661, "Rabat-Salé-Kénitra"),
        MoroccanCity("Guelmim", 28.9870, -10.0574, "Guelmim-Oued Noun"),
        MoroccanCity("Berrechid", 33.2650, -7.5800, "Casablanca-Settat"),
        MoroccanCity("Ouarzazate", 30.9335, -6.9370, "Drâa-Tafilalet"),
        MoroccanCity("Tiznit", 29.6974, -9.7316, "Souss-Massa"),
        MoroccanCity("Errachidia", 31.9314, -4.4244, "Drâa-Tafilalet")
    )

    /**
     * Morocco's approximate bounding box
     */
    private const val MOROCCO_MIN_LAT = 27.662
    private const val MOROCCO_MAX_LAT = 35.922
    private const val MOROCCO_MIN_LON = -13.168
    private const val MOROCCO_MAX_LON = -0.991

    /**
     * Check if coordinates are within Morocco
     */
    fun isInMorocco(latitude: Double, longitude: Double): Boolean {
        return latitude in MOROCCO_MIN_LAT..MOROCCO_MAX_LAT &&
               longitude in MOROCCO_MIN_LON..MOROCCO_MAX_LON
    }

    /**
     * Find the nearest Moroccan city to given coordinates
     */
    fun findNearestMoroccanCity(latitude: Double, longitude: Double): MoroccanCity? {
        if (!isInMorocco(latitude, longitude)) return null

        return moroccanCities.minByOrNull { city ->
            calculateDistance(latitude, longitude, city.latitude, city.longitude)
        }
    }

    /**
     * Get suggested cities based on location (returns top 5 nearest)
     */
    fun getSuggestedMoroccanCities(latitude: Double, longitude: Double): List<MoroccanCity> {
        if (!isInMorocco(latitude, longitude)) return emptyList()

        return moroccanCities
            .map { city ->
                city to calculateDistance(latitude, longitude, city.latitude, city.longitude)
            }
            .sortedBy { it.second }
            .take(5)
            .map { it.first }
    }

    /**
     * Convert MoroccanCity to CitySelection
     */
    fun MoroccanCity.toCitySelection(): CitySelection {
        return CitySelection(
            name = name,
            country = "Morocco",
            countryCode = "MA",
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Get recommended prayer calculation method for Morocco
     */
    fun getRecommendedCalculationMethod(): PrayerCalculationMethod {
        return PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Data class for Moroccan cities
     */
    data class MoroccanCity(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val region: String
    )
}
