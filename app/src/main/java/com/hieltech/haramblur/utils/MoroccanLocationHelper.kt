package com.hieltech.haramblur.utils

import com.hieltech.haramblur.data.cities.CitySelection
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import kotlin.math.*

/**
 * Helper for detecting Moroccan locations and providing appropriate prayer calculation methods
 */
class MoroccanLocationHelper {

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
        MoroccanCity("Errachidia", 31.9314, -4.4244, "Drâa-Tafilalet"),
        // Additional major cities
        MoroccanCity("Salé", 34.0531, -6.7986, "Rabat-Salé-Kénitra"),
        MoroccanCity("Temara", 33.9255, -6.9146, "Rabat-Salé-Kénitra"),
        MoroccanCity("Skhirate", 33.8542, -7.0336, "Rabat-Salé-Kénitra"),
        MoroccanCity("Ain Harrouda", 33.7389, -6.8967, "Rabat-Salé-Kénitra"),
        MoroccanCity("Tiflet", 33.8947, -6.3067, "Rabat-Salé-Kénitra"),
        MoroccanCity("Sidi Slimane", 34.2625, -5.9292, "Rabat-Salé-Kénitra"),
        MoroccanCity("Sidi Kacem", 34.2236, -5.7061, "Rabat-Salé-Kénitra"),
        MoroccanCity("Sidi Yahya El Gharb", 34.3079, -6.3119, "Rabat-Salé-Kénitra"),
        MoroccanCity("Mechraa Bel Ksiri", 34.1686, -5.9605, "Rabat-Salé-Kénitra"),
        MoroccanCity("Souk El Arbaa", 34.3113, -6.0292, "Rabat-Salé-Kénitra"),
        MoroccanCity("Aïn Aouda", 33.8767, -6.6112, "Rabat-Salé-Kénitra"),
        MoroccanCity("Oued Laou", 35.4647, -5.3142, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Fnideq", 35.8436, -5.3672, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("M'diq", 35.6958, -5.3167, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Martil", 35.6167, -5.2789, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Chefchaouen", 35.1689, -5.2675, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Ouezzane", 34.7947, -5.8708, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Ksar Sghir", 35.7953, -5.9158, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Asilah", 35.4653, -6.0342, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Assilah", 35.4653, -6.0342, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Tanger", 35.7595, -5.8340, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Al Hoceïma", 35.2487, -3.9367, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Imzouren", 35.1489, -3.8236, "Tanger-Tétouan-Al Hoceïma"),
        MoroccanCity("Nador", 35.1681, -2.9287, "Oriental"),
        MoroccanCity("Berkane", 34.9205, -2.3194, "Oriental"),
        MoroccanCity("Taourirt", 34.4078, -2.8972, "Oriental"),
        MoroccanCity("Jerada", 34.3167, -2.1589, "Oriental"),
        MoroccanCity("Oujda", 34.6814, -1.9086, "Oriental"),
        MoroccanCity("Ahfir", 34.9528, -2.0958, "Oriental"),
        MoroccanCity("Beni Ansar", 35.2833, -2.7833, "Oriental"),
        MoroccanCity("Nador Port", 35.2261, -2.8333, "Oriental"),
        MoroccanCity("Fès", 34.0181, -5.0078, "Fès-Meknès"),
        MoroccanCity("Meknès", 33.8935, -5.5473, "Fès-Meknès"),
        MoroccanCity("Taza", 34.2133, -4.0103, "Fès-Meknès"),
        MoroccanCity("Sefrou", 33.8356, -5.0831, "Fès-Meknès"),
        MoroccanCity("Boulemane", 33.7275, -4.7708, "Fès-Meknès"),
        MoroccanCity("El Hajeb", 33.6839, -5.3656, "Fès-Meknès"),
        MoroccanCity("Ifrane", 33.5333, -5.1083, "Fès-Meknès"),
        MoroccanCity("Missour", 33.5456, -4.1589, "Fès-Meknès"),
        MoroccanCity("Midelt", 32.6833, -4.7333, "Drâa-Tafilalet"),
        MoroccanCity("Errachidia", 31.9314, -4.4244, "Drâa-Tafilalet"),
        MoroccanCity("Ouarzazate", 30.9335, -6.9370, "Drâa-Tafilalet"),
        MoroccanCity("Zagora", 30.3458, -5.8425, "Drâa-Tafilalet"),
        MoroccanCity("Tinghir", 31.5172, -5.5297, "Drâa-Tafilalet"),
        MoroccanCity("Marrakech", 31.6295, -7.9811, "Marrakech-Safi"),
        MoroccanCity("Safi", 32.2994, -9.2372, "Marrakech-Safi"),
        MoroccanCity("Essaouira", 31.5125, -9.7700, "Marrakech-Safi"),
        MoroccanCity("El Kelaa des Sraghna", 32.0614, -7.4614, "Marrakech-Safi"),
        MoroccanCity("Chichaoua", 31.5436, -8.7611, "Marrakech-Safi"),
        MoroccanCity("Youssoufia", 32.2489, -8.5333, "Marrakech-Safi"),
        MoroccanCity("Agadir", 30.4278, -9.5981, "Souss-Massa"),
        MoroccanCity("Inezgane", 30.3528, -9.5436, "Souss-Massa"),
        MoroccanCity("Ait Melloul", 30.3258, -9.5089, "Souss-Massa"),
        MoroccanCity("Tiznit", 29.6974, -9.7316, "Souss-Massa"),
        MoroccanCity("Taroudant", 30.4708, -8.8736, "Souss-Massa"),
        MoroccanCity("Oulad Teima", 30.3986, -9.2286, "Souss-Massa"),
        MoroccanCity("Bouizakarne", 30.2219, -9.2072, "Souss-Massa"),
        MoroccanCity("Ait Baha", 30.0403, -9.0908, "Souss-Massa"),
        MoroccanCity("Biougra", 30.0908, -9.2581, "Souss-Massa"),
        MoroccanCity("Guelmim", 28.9870, -10.0574, "Guelmim-Oued Noun"),
        MoroccanCity("Tan-Tan", 28.4380, -11.1033, "Guelmim-Oued Noun"),
        MoroccanCity("Laayoune", 27.1442, -13.2032, "Laâyoune-Sakia El Hamra"),
        MoroccanCity("Dakhla", 23.7136, -15.9333, "Dakhla-Oued Ed Dahab"),
        MoroccanCity("Smara", 26.7425, -11.6775, "Laâyoune-Sakia El Hamra"),
        MoroccanCity("Boujdour", 26.1300, -14.4725, "Laâyoune-Sakia El Hamra"),
        MoroccanCity("Aousserd", 25.8750, -13.0500, "Dakhla-Oued Ed Dahab"),
        MoroccanCity("Casablanca", 33.5731, -7.5898, "Casablanca-Settat"),
        MoroccanCity("Mohammedia", 33.6866, -7.3830, "Casablanca-Settat"),
        MoroccanCity("El Jadida", 33.2316, -8.5007, "Casablanca-Settat"),
        MoroccanCity("Settat", 33.0013, -7.6216, "Casablanca-Settat"),
        MoroccanCity("Berrechid", 33.2650, -7.5800, "Casablanca-Settat"),
        MoroccanCity("Bouskoura", 33.4708, -7.6583, "Casablanca-Settat"),
        MoroccanCity("Dar Bouazza", 33.5069, -7.9308, "Casablanca-Settat"),
        MoroccanCity("Nouaceur", 33.3689, -7.5869, "Casablanca-Settat"),
        MoroccanCity("Mediouna", 33.4286, -7.4725, "Casablanca-Settat"),
        MoroccanCity("Tit Mellil", 33.5036, -7.4167, "Casablanca-Settat"),
        MoroccanCity("Béni Mellal", 32.3373, -6.3498, "Béni Mellal-Khénifra"),
        MoroccanCity("Khouribga", 32.8811, -6.9063, "Béni Mellal-Khénifra"),
        MoroccanCity("Fquih Ben Salah", 32.5069, -6.0694, "Béni Mellal-Khénifra"),
        MoroccanCity("Souk Sebt Oulad Nemma", 32.6417, -6.2389, "Béni Mellal-Khénifra"),
        MoroccanCity("Oued Zem", 32.8667, -6.5667, "Béni Mellal-Khénifra"),
        MoroccanCity("Khenifra", 32.9425, -5.6642, "Béni Mellal-Khénifra"),
        MoroccanCity("Azrou", 33.4386, -5.2208, "Béni Mellal-Khénifra"),
        MoroccanCity("Mrirt", 33.4367, -5.6139, "Béni Mellal-Khénifra"),
        MoroccanCity("Ksar el-Kebir", 35.0017, -5.9008, "Tanger-Tétouan-Al Hoceïma")
    )

    /**
     * Morocco's approximate bounding box
     */
    private val MOROCCO_MIN_LAT = 27.662
    private val MOROCCO_MAX_LAT = 35.922
    private val MOROCCO_MIN_LON = -13.168
    private val MOROCCO_MAX_LON = -0.991

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
        return PrayerCalculationMethod.MOROCCO_MINISTRY
    }

    /**
     * Get regional prayer time adjustments for Morocco
     * Returns adjustments in minutes for each prayer time
     */
    fun getRegionalAdjustments(region: String): Map<String, Int> {
        return when (region) {
            "Tanger-Tétouan-Al Hoceïma" -> mapOf(
                "Fajr" to -2,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -2
            )
            "Oriental" -> mapOf(
                "Fajr" to -3,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -3
            )
            "Fès-Meknès" -> mapOf(
                "Fajr" to -2,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -2
            )
            "Rabat-Salé-Kénitra" -> mapOf(
                "Fajr" to -1,
                "Dhuhr" to 0,
                "Asr" to 0,
                "Maghrib" to 0,
                "Isha" to -1
            )
            "Béni Mellal-Khénifra" -> mapOf(
                "Fajr" to -2,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -2
            )
            "Casablanca-Settat" -> mapOf(
                "Fajr" to -1,
                "Dhuhr" to 0,
                "Asr" to 0,
                "Maghrib" to 0,
                "Isha" to -1
            )
            "Marrakech-Safi" -> mapOf(
                "Fajr" to -2,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -2
            )
            "Drâa-Tafilalet" -> mapOf(
                "Fajr" to -3,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -3
            )
            "Souss-Massa" -> mapOf(
                "Fajr" to -2,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -2
            )
            "Guelmim-Oued Noun" -> mapOf(
                "Fajr" to -3,
                "Dhuhr" to -1,
                "Asr" to -1,
                "Maghrib" to 0,
                "Isha" to -3
            )
            "Laâyoune-Sakia El Hamra" -> mapOf(
                "Fajr" to -4,
                "Dhuhr" to -2,
                "Asr" to -2,
                "Maghrib" to 0,
                "Isha" to -4
            )
            "Dakhla-Oued Ed Dahab" -> mapOf(
                "Fajr" to -5,
                "Dhuhr" to -2,
                "Asr" to -2,
                "Maghrib" to 0,
                "Isha" to -5
            )
            else -> mapOf(
                "Fajr" to -1,
                "Dhuhr" to 0,
                "Asr" to 0,
                "Maghrib" to 0,
                "Isha" to -1
            )
        }
    }

    /**
     * Get regional adjustments for a specific city
     */
    fun getCityAdjustments(cityName: String): Map<String, Int> {
        val city = moroccanCities.find { it.name.equals(cityName, ignoreCase = true) }
        return city?.let { getRegionalAdjustments(it.region) } ?: emptyMap()
    }

    /**
     * Get regional adjustments for coordinates
     */
    fun getAdjustmentsForCoordinates(latitude: Double, longitude: Double): Map<String, Int> {
        val city = findNearestMoroccanCity(latitude, longitude)
        return city?.let { getRegionalAdjustments(it.region) } ?: emptyMap()
    }

    /**
     * Check if a location is in a specific region of Morocco
     */
    fun isInMoroccanRegion(latitude: Double, longitude: Double, region: String): Boolean {
        val city = findNearestMoroccanCity(latitude, longitude)
        return city?.region == region
    }

    /**
     * Get all Moroccan regions
     */
    fun getMoroccanRegions(): List<String> {
        return moroccanCities.map { it.region }.distinct().sorted()
    }

    /**
     * Get cities in a specific region
     */
    fun getCitiesInRegion(region: String): List<MoroccanCity> {
        return moroccanCities.filter { it.region == region }
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
