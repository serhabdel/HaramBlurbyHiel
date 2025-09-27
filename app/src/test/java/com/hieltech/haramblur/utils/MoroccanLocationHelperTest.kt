package com.hieltech.haramblur.utils

import org.junit.Test
import org.junit.Assert.*
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod

/**
 * Test suite for MoroccanLocationHelper
 * Validates Moroccan location detection, regional adjustments, and city lookup
 */
class MoroccanLocationHelperTest {

    @Test
    fun `test isInMorocco with valid Moroccan coordinates`() {
        // Test major Moroccan cities
        assertTrue(MoroccanLocationHelper.isInMorocco(33.5731, -7.5898)) // Casablanca
        assertTrue(MoroccanLocationHelper.isInMorocco(34.0209, -6.8416)) // Rabat
        assertTrue(MoroccanLocationHelper.isInMorocco(31.6295, -7.9811)) // Marrakech
        assertTrue(MoroccanLocationHelper.isInMorocco(30.4278, -9.5981)) // Agadir
        assertTrue(MoroccanLocationHelper.isInMorocco(35.7595, -5.8340)) // Tangier
        assertTrue(MoroccanLocationHelper.isInMorocco(34.6814, -1.9086)) // Oujda
        assertTrue(MoroccanLocationHelper.isInMorocco(23.7136, -15.9333)) // Dakhla (Southern Morocco)
    }

    @Test
    fun `test isInMorocco with invalid coordinates`() {
        // Test non-Moroccan cities
        assertFalse(MoroccanLocationHelper.isInMorocco(36.8065, 10.1815)) // Tunis, Tunisia
        assertFalse(MoroccanLocationHelper.isInMorocco(33.8869, 10.0982)) // Sfax, Tunisia
        assertFalse(MoroccanLocationHelper.isInMorocco(36.7538, 3.0588)) // Algiers, Algeria
        assertFalse(MoroccanLocationHelper.isInMorocco(35.6896, 139.6917)) // Tokyo, Japan
        assertFalse(MoroccanLocationHelper.isInMorocco(40.7128, -74.0060)) // New York, USA
    }

    @Test
    fun `test findNearestMoroccanCity with major cities`() {
        // Test finding nearest major cities
        val casablanca = MoroccanLocationHelper.findNearestMoroccanCity(33.5731, -7.5898)
        assertNotNull(casablanca)
        assertEquals("Casablanca", casablanca?.name)

        val rabat = MoroccanLocationHelper.findNearestMoroccanCity(34.0209, -6.8416)
        assertNotNull(rabat)
        assertEquals("Rabat", rabat?.name)

        val marrakech = MoroccanLocationHelper.findNearestMoroccanCity(31.6295, -7.9811)
        assertNotNull(marrakech)
        assertEquals("Marrakech", marrakech?.name)
    }

    @Test
    fun `test findNearestMoroccanCity with coordinates near multiple cities`() {
        // Test coordinates between Casablanca and Rabat
        val betweenCasablancaRabat = MoroccanLocationHelper.findNearestMoroccanCity(33.8, -7.2)
        assertNotNull(betweenCasablancaRabat)
        assertTrue(betweenCasablancaRabat?.name == "Casablanca" || betweenCasablancaRabat?.name == "Rabat")

        // Test coordinates in a region with multiple cities
        val northernMorocco = MoroccanLocationHelper.findNearestMoroccanCity(35.5, -5.5)
        assertNotNull(northernMorocco)
        assertTrue(listOf("Tangier", "Tetouan", "Fnideq").contains(northernMorocco?.name))
    }

    @Test
    fun `test findNearestMoroccanCity with non-Moroccan coordinates`() {
        // Test with coordinates outside Morocco
        val outsideMorocco = MoroccanLocationHelper.findNearestMoroccanCity(36.8065, 10.1815) // Tunis
        assertNull(outsideMorocco)
    }

    @Test
    fun `test getSuggestedMoroccanCities returns top 5 nearest`() {
        // Test with coordinates in Casablanca area
        val suggestedCities = MoroccanLocationHelper.getSuggestedMoroccanCities(33.5731, -7.5898)
        assertNotNull(suggestedCities)
        assertTrue(suggestedCities.size <= 5)
        assertTrue(suggestedCities.any { it.name == "Casablanca" })

        // Test with coordinates in Tangier area
        val suggestedCitiesTangier = MoroccanLocationHelper.getSuggestedMoroccanCities(35.7595, -5.8340)
        assertNotNull(suggestedCitiesTangier)
        assertTrue(suggestedCitiesTangier.size <= 5)
        assertTrue(suggestedCitiesTangier.any { it.name == "Tangier" })
    }

    @Test
    fun `test getRegionalAdjustments returns correct adjustments for each region`() {
        // Test Casablanca-Settat region
        val casablancaAdjustments = MoroccanLocationHelper.getRegionalAdjustments("Casablanca-Settat")
        assertEquals(-1, casablancaAdjustments["Fajr"])
        assertEquals(0, casablancaAdjustments["Dhuhr"])
        assertEquals(0, casablancaAdjustments["Asr"])
        assertEquals(0, casablancaAdjustments["Maghrib"])
        assertEquals(-1, casablancaAdjustments["Isha"])

        // Test Oriental region
        val oujdaAdjustments = MoroccanLocationHelper.getRegionalAdjustments("Oriental")
        assertEquals(-3, oujdaAdjustments["Fajr"])
        assertEquals(-1, oujdaAdjustments["Dhuhr"])
        assertEquals(-1, oujdaAdjustments["Asr"])
        assertEquals(0, oujdaAdjustments["Maghrib"])
        assertEquals(-3, oujdaAdjustments["Isha"])

        // Test Laâyoune-Sakia El Hamra region (Southern Morocco)
        val laayouneAdjustments = MoroccanLocationHelper.getRegionalAdjustments("Laâyoune-Sakia El Hamra")
        assertEquals(-4, laayouneAdjustments["Fajr"])
        assertEquals(-2, laayouneAdjustments["Dhuhr"])
        assertEquals(-2, laayouneAdjustments["Asr"])
        assertEquals(0, laayouneAdjustments["Maghrib"])
        assertEquals(-4, laayouneAdjustments["Isha"])

        // Test Dakhla-Oued Ed Dahab region (Southernmost Morocco)
        val dakhlaAdjustments = MoroccanLocationHelper.getRegionalAdjustments("Dakhla-Oued Ed Dahab")
        assertEquals(-5, dakhlaAdjustments["Fajr"])
        assertEquals(-2, dakhlaAdjustments["Dhuhr"])
        assertEquals(-2, dakhlaAdjustments["Asr"])
        assertEquals(0, dakhlaAdjustments["Maghrib"])
        assertEquals(-5, dakhlaAdjustments["Isha"])
    }

    @Test
    fun `test getCityAdjustments returns correct adjustments for specific cities`() {
        // Test major cities
        val casablancaAdjustments = MoroccanLocationHelper.getCityAdjustments("Casablanca")
        assertEquals(-1, casablancaAdjustments["Fajr"])
        assertEquals(0, casablancaAdjustments["Dhuhr"])
        assertEquals(0, casablancaAdjustments["Asr"])
        assertEquals(0, casablancaAdjustments["Maghrib"])
        assertEquals(-1, casablancaAdjustments["Isha"])

        val oujdaAdjustments = MoroccanLocationHelper.getCityAdjustments("Oujda")
        assertEquals(-3, oujdaAdjustments["Fajr"])
        assertEquals(-1, oujdaAdjustments["Dhuhr"])
        assertEquals(-1, oujdaAdjustments["Asr"])
        assertEquals(0, oujdaAdjustments["Maghrib"])
        assertEquals(-3, oujdaAdjustments["Isha"])

        val dakhlaAdjustments = MoroccanLocationHelper.getCityAdjustments("Dakhla")
        assertEquals(-5, dakhlaAdjustments["Fajr"])
        assertEquals(-2, dakhlaAdjustments["Dhuhr"])
        assertEquals(-2, dakhlaAdjustments["Asr"])
        assertEquals(0, dakhlaAdjustments["Maghrib"])
        assertEquals(-5, dakhlaAdjustments["Isha"])

        // Test with unknown city
        val unknownCityAdjustments = MoroccanLocationHelper.getCityAdjustments("Unknown City")
        assertTrue(unknownCityAdjustments.isEmpty())
    }

    @Test
    fun `test getAdjustmentsForCoordinates returns correct adjustments`() {
        // Test with Casablanca coordinates
        val casablancaAdjustments = MoroccanLocationHelper.getAdjustmentsForCoordinates(33.5731, -7.5898)
        assertEquals(-1, casablancaAdjustments["Fajr"])
        assertEquals(0, casablancaAdjustments["Dhuhr"])
        assertEquals(0, casablancaAdjustments["Asr"])
        assertEquals(0, casablancaAdjustments["Maghrib"])
        assertEquals(-1, casablancaAdjustments["Isha"])

        // Test with Oujda coordinates
        val oujdaAdjustments = MoroccanLocationHelper.getAdjustmentsForCoordinates(34.6814, -1.9086)
        assertEquals(-3, oujdaAdjustments["Fajr"])
        assertEquals(-1, oujdaAdjustments["Dhuhr"])
        assertEquals(-1, oujdaAdjustments["Asr"])
        assertEquals(0, oujdaAdjustments["Maghrib"])
        assertEquals(-3, oujdaAdjustments["Isha"])

        // Test with non-Moroccan coordinates
        val nonMoroccoAdjustments = MoroccanLocationHelper.getAdjustmentsForCoordinates(36.8065, 10.1815) // Tunis
        assertTrue(nonMoroccoAdjustments.isEmpty())
    }

    @Test
    fun `test isInMoroccanRegion returns correct results`() {
        // Test coordinates in specific regions
        assertTrue(MoroccanLocationHelper.isInMoroccanRegion(33.5731, -7.5898, "Casablanca-Settat")) // Casablanca
        assertTrue(MoroccanLocationHelper.isInMoroccanRegion(34.0209, -6.8416, "Rabat-Salé-Kénitra")) // Rabat
        assertTrue(MoroccanLocationHelper.isInMoroccanRegion(34.6814, -1.9086, "Oriental")) // Oujda
        assertTrue(MoroccanLocationHelper.isInMoroccanRegion(23.7136, -15.9333, "Dakhla-Oued Ed Dahab")) // Dakhla

        // Test coordinates not in specified region
        assertFalse(MoroccanLocationHelper.isInMoroccanRegion(33.5731, -7.5898, "Oriental")) // Casablanca not in Oriental
        assertFalse(MoroccanLocationHelper.isInMoroccanRegion(34.6814, -1.9086, "Casablanca-Settat")) // Oujda not in Casablanca-Settat
    }

    @Test
    fun `test getMoroccanRegions returns all regions`() {
        val regions = MoroccanLocationHelper.getMoroccanRegions()
        assertNotNull(regions)
        assertTrue(regions.isNotEmpty())
        
        // Check for major regions
        assertTrue(regions.contains("Casablanca-Settat"))
        assertTrue(regions.contains("Rabat-Salé-Kénitra"))
        assertTrue(regions.contains("Fès-Meknès"))
        assertTrue(regions.contains("Marrakech-Safi"))
        assertTrue(regions.contains("Tanger-Tétouan-Al Hoceïma"))
        assertTrue(regions.contains("Oriental"))
        assertTrue(regions.contains("Drâa-Tafilalet"))
        assertTrue(regions.contains("Souss-Massa"))
        assertTrue(regions.contains("Guelmim-Oued Noun"))
        assertTrue(regions.contains("Laâyoune-Sakia El Hamra"))
        assertTrue(regions.contains("Dakhla-Oued Ed Dahab"))
    }

    @Test
    fun `test getCitiesInRegion returns correct cities`() {
        // Test Casablanca-Settat region
        val casablancaSettatCities = MoroccanLocationHelper.getCitiesInRegion("Casablanca-Settat")
        assertTrue(casablancaSettatCities.isNotEmpty())
        assertTrue(casablancaSettatCities.any { it.name == "Casablanca" })
        assertTrue(casablancaSettatCities.any { it.name == "Mohammedia" })
        assertTrue(casablancaSettatCities.any { it.name == "El Jadida" })
        assertTrue(casablancaSettatCities.any { it.name == "Settat" })

        // Test Oriental region
        val orientalCities = MoroccanLocationHelper.getCitiesInRegion("Oriental")
        assertTrue(orientalCities.isNotEmpty())
        assertTrue(orientalCities.any { it.name == "Oujda" })
        assertTrue(orientalCities.any { it.name == "Nador" })
        assertTrue(orientalCities.any { it.name == "Berkane" })

        // Test with unknown region
        val unknownRegionCities = MoroccanLocationHelper.getCitiesInRegion("Unknown Region")
        assertTrue(unknownRegionCities.isEmpty())
    }

    @Test
    fun `test getRecommendedCalculationMethod returns Morocco Ministry method`() {
        val method = MoroccanLocationHelper.getRecommendedCalculationMethod()
        assertEquals(PrayerCalculationMethod.MOROCCO_MINISTRY, method)
    }

    @Test
    fun `test MoroccanCity toCitySelection conversion`() {
        val moroccanCity = MoroccanLocationHelper.MoroccanCity(
            name = "Casablanca",
            latitude = 33.5731,
            longitude = -7.5898,
            region = "Casablanca-Settat"
        )

        val citySelection = moroccanCity.toCitySelection()
        assertEquals("Casablanca", citySelection.name)
        assertEquals("Morocco", citySelection.country)
        assertEquals("MA", citySelection.countryCode)
        assertEquals(33.5731, citySelection.latitude)
        assertEquals(-7.5898, citySelection.longitude)
    }
}