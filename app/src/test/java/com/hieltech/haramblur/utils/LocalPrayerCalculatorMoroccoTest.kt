package com.hieltech.haramblur.utils

import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import com.hieltech.haramblur.data.prayer.PrayerTimings
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Test class for Morocco-specific prayer time calculations
 * Validates that the Morocco Ministry method produces expected results
 */
class LocalPrayerCalculatorMoroccoTest {

    @Test
    fun `test Morocco Ministry method uses correct angles`() {
        val calculator = LocalPrayerCalculator
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 15) // A known date for testing
        }
        
        // Casablanca coordinates
        val latitude = 33.5731
        val longitude = -7.5898
        
        val result = calculator.computeForMorocco(calendar, latitude, longitude)
        
        // Verify that all prayer times are calculated
        assertNotNull(result.Fajr)
        assertNotNull(result.Dhuhr)
        assertNotNull(result.Asr)
        assertNotNull(result.Maghrib)
        assertNotNull(result.Isha)
        assertNotNull(result.Sunrise)
        assertNotNull(result.Sunset)
        
        // Basic sanity checks for prayer times
        assertTrue(result.Fajr < result.Sunrise) // Fajr before sunrise
        assertTrue(result.Sunrise < result.Dhuhr) // Sunrise before Dhuhr
        assertTrue(result.Dhuhr < result.Asr) // Dhuhr before Asr
        assertTrue(result.Asr < result.Maghrib) // Asr before Maghrib
        assertTrue(result.Maghrib < result.Isha) // Maghrib before Isha
    }

    @Test
    fun `test Morocco Ministry method ID is correct`() {
        assertEquals(15, PrayerCalculationMethod.MOROCCO_MINISTRY.id)
        assertEquals("Morocco Ministry of Islamic Affairs", PrayerCalculationMethod.MOROCCO_MINISTRY.displayName)
    }

    @Test
    fun `test Moroccan city adjustments are applied correctly`() {
        val calculator = LocalPrayerCalculator
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 15)
        }
        
        // Casablanca coordinates
        val latitude = 33.5731
        val longitude = -7.5898
        
        // Test without adjustments
        val resultWithoutAdjustments = calculator.computeForMorocco(calendar, latitude, longitude)
        
        // Test with Casablanca adjustments
        val adjustments = calculator.getMoroccanCityAdjustments("casablanca")
        val resultWithAdjustments = calculator.computeForMorocco(
            calendar, 
            latitude, 
            longitude, 
            adjustmentsMinutes = adjustments
        )
        
        // Verify that adjustments are applied
        assertNotEquals(resultWithoutAdjustments.Fajr, resultWithAdjustments.Fajr)
        assertNotEquals(resultWithoutAdjustments.Isha, resultWithAdjustments.Isha)
    }

    @Test
    fun `test Moroccan location helper recommends correct method`() {
        val recommendedMethod = MoroccanLocationHelper.getRecommendedCalculationMethod()
        assertEquals(PrayerCalculationMethod.MOROCCO_MINISTRY, recommendedMethod)
    }

    @Test
    fun `test AutoCalculationMethodDetector detects Morocco correctly`() {
        val detector = AutoCalculationMethodDetector()
        
        // Casablanca coordinates
        val latitude = 33.5731
        val longitude = -7.5898
        
        val detectedMethod = detector.detectCalculationMethod(latitude, longitude)
        assertEquals(PrayerCalculationMethod.MOROCCO_MINISTRY, detectedMethod)
        
        val recommendation = detector.getRecommendation(latitude, longitude)
        assertEquals(PrayerCalculationMethod.MOROCCO_MINISTRY, recommendation.method)
        assertEquals(AutoCalculationMethodDetector.ConfidenceLevel.HIGH, recommendation.confidence)
        assertTrue(recommendation.reason.contains("Morocco"))
    }

    @Test
    fun `test different Moroccan cities have different adjustments`() {
        val calculator = LocalPrayerCalculator
        
        val casablancaAdjustments = calculator.getMoroccanCityAdjustments("casablanca")
        val marrakechAdjustments = calculator.getMoroccanCityAdjustments("marrakech")
        val agadirAdjustments = calculator.getMoroccanCityAdjustments("agadir")
        
        // Verify that different cities have different adjustments
        assertNotEquals(casablancaAdjustments, marrakechAdjustments)
        assertNotEquals(marrakechAdjustments, agadirAdjustments)
        assertNotEquals(casablancaAdjustments, agadirAdjustments)
        
        // Verify specific adjustments
        assertEquals(-2, casablancaAdjustments["Fajr"])
        assertEquals(2, casablancaAdjustments["Isha"])
        assertEquals(-1, marrakechAdjustments["Fajr"])
        assertEquals(1, marrakechAdjustments["Isha"])
    }

    @Test
    fun `test non-Moroccan cities return empty adjustments`() {
        val calculator = LocalPrayerCalculator
        
        val parisAdjustments = calculator.getMoroccanCityAdjustments("paris")
        val londonAdjustments = calculator.getMoroccanCityAdjustments("london")
        
        assertTrue(parisAdjustments.isEmpty())
        assertTrue(londonAdjustments.isEmpty())
    }
}