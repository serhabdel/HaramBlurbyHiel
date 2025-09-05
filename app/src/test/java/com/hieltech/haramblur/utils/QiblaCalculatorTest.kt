package com.hieltech.haramblur.utils

import android.content.Context
import com.hieltech.haramblur.data.compass.calculateAngleToQiblaWithDeclination
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock

class QiblaCalculatorTest {

    private val mockContext = mock(Context::class.java)

    @Test
    fun `calculateQiblaBearing for London should return correct bearing`() {
        // London coordinates: 51.5074° N, 0.1278° W
        val londonLat = 51.5074
        val londonLon = -0.1278
        val expectedBearing = 118.0 // Approximate bearing from London to Mecca
        
        val actualBearing = QiblaCalculator.calculateQiblaBearing(londonLat, londonLon)
        
        // Allow for small margin of error due to calculation precision
        assertTrue("London Qibla bearing should be around 118°", 
            actualBearing in (expectedBearing - 2.0)..(expectedBearing + 2.0))
    }

    @Test
    fun `calculateQiblaBearing for New York should return correct bearing`() {
        // New York coordinates: 40.7128° N, 74.0060° W
        val nyLat = 40.7128
        val nyLon = -74.0060
        val expectedBearing = 58.0 // Approximate bearing from New York to Mecca
        
        val actualBearing = QiblaCalculator.calculateQiblaBearing(nyLat, nyLon)
        
        assertTrue("New York Qibla bearing should be around 58°", 
            actualBearing in (expectedBearing - 2.0)..(expectedBearing + 2.0))
    }

    @Test
    fun `calculateQiblaBearing for Jakarta should return correct bearing`() {
        // Jakarta coordinates: 6.2088° S, 106.8456° E
        val jakartaLat = -6.2088
        val jakartaLon = 106.8456
        val expectedBearing = 295.0 // Approximate bearing from Jakarta to Mecca
        
        val actualBearing = QiblaCalculator.calculateQiblaBearing(jakartaLat, jakartaLon)
        
        assertTrue("Jakarta Qibla bearing should be around 295°", 
            actualBearing in (expectedBearing - 2.0)..(expectedBearing + 2.0))
    }

    @Test
    fun `calculateQiblaBearing for Sydney should return correct bearing`() {
        // Sydney coordinates: 33.8688° S, 151.2093° E
        val sydneyLat = -33.8688
        val sydneyLon = 151.2093
        val expectedBearing = 277.0 // Approximate bearing from Sydney to Mecca
        
        val actualBearing = QiblaCalculator.calculateQiblaBearing(sydneyLat, sydneyLon)
        
        assertTrue("Sydney Qibla bearing should be around 277°", 
            actualBearing in (expectedBearing - 2.0)..(expectedBearing + 2.0))
    }

    @Test
    fun `calculateTrueAzimuth with positive declination should add declination`() {
        val magneticAzimuth = 45.0
        val declination = 8.0f // East declination
        val expectedTrueAzimuth = 53.0
        
        val actualTrueAzimuth = QiblaCalculator.calculateTrueAzimuth(magneticAzimuth, declination)
        
        assertEquals("True azimuth should be magnetic + declination", 
            expectedTrueAzimuth, actualTrueAzimuth, 0.1)
    }

    @Test
    fun `calculateTrueAzimuth with negative declination should subtract declination`() {
        val magneticAzimuth = 45.0
        val declination = -12.0f // West declination
        val expectedTrueAzimuth = 33.0
        
        val actualTrueAzimuth = QiblaCalculator.calculateTrueAzimuth(magneticAzimuth, declination)
        
        assertEquals("True azimuth should be magnetic + declination", 
            expectedTrueAzimuth, actualTrueAzimuth, 0.1)
    }

    @Test
    fun `calculateTrueAzimuth with zero declination should return same value`() {
        val magneticAzimuth = 45.0
        val declination = 0.0f
        
        val actualTrueAzimuth = QiblaCalculator.calculateTrueAzimuth(magneticAzimuth, declination)
        
        assertEquals("True azimuth should equal magnetic azimuth when declination is zero", 
            magneticAzimuth, actualTrueAzimuth, 0.1)
    }

    @Test
    fun `calculateTrueAzimuth normalizes result to 0-360 range`() {
        val magneticAzimuth = 350.0
        val declination = 20.0f
        val expectedTrueAzimuth = 10.0 // 350 + 20 = 370, normalized to 10
        
        val actualTrueAzimuth = QiblaCalculator.calculateTrueAzimuth(magneticAzimuth, declination)
        
        assertEquals("True azimuth should be normalized to 0-360 range", 
            expectedTrueAzimuth, actualTrueAzimuth, 0.1)
    }

    @Test
    fun `calculateAngleToQiblaWithDeclination handles 0-360 wrap correctly`() {
        val magneticAzimuth = 10.0f
        val qiblaBearing = 350.0
        val declination = 0.0f
        val expectedAngle = -20.0f // Shortest path from 10° to 350° is -20°
        
        val actualAngle = calculateAngleToQiblaWithDeclination(magneticAzimuth, qiblaBearing, declination)
        
        assertEquals("Angle should handle 0-360 wrap correctly", 
            expectedAngle, actualAngle, 0.1f)
    }

    @Test
    fun `calculateAngleToQiblaWithDeclination with positive declination`() {
        val magneticAzimuth = 45.0f
        val qiblaBearing = 60.0
        val declination = 5.0f // East declination
        val expectedAngle = 10.0f // True azimuth = 50°, angle to 60° = 10°
        
        val actualAngle = calculateAngleToQiblaWithDeclination(magneticAzimuth, qiblaBearing, declination)
        
        assertEquals("Angle should account for declination", 
            expectedAngle, actualAngle, 0.1f)
    }

    @Test
    fun `calculateAngleToQiblaWithDeclination with negative declination`() {
        val magneticAzimuth = 45.0f
        val qiblaBearing = 60.0
        val declination = -5.0f // West declination
        val expectedAngle = 20.0f // True azimuth = 40°, angle to 60° = 20°
        
        val actualAngle = calculateAngleToQiblaWithDeclination(magneticAzimuth, qiblaBearing, declination)
        
        assertEquals("Angle should account for negative declination", 
            expectedAngle, actualAngle, 0.1f)
    }

    @Test
    fun `calculateAngleToQiblaWithDeclination edge case around 180 degrees`() {
        val magneticAzimuth = 179.0f
        val qiblaBearing = 181.0
        val declination = 0.0f
        val expectedAngle = 2.0f // Shortest path from 179° to 181° is 2°
        
        val actualAngle = calculateAngleToQiblaWithDeclination(magneticAzimuth, qiblaBearing, declination)
        
        assertEquals("Angle should handle edge case around 180° correctly", 
            expectedAngle, actualAngle, 0.1f)
    }

    @Test
    fun `getMagneticDeclination returns valid range`() {
        val lat = 40.7128 // New York
        val lon = -74.0060
        
        val declination = QiblaCalculator.getMagneticDeclination(mockContext, lat, lon)
        
        // Magnetic declination should be within reasonable bounds
        assertTrue("Declination should be within reasonable bounds", 
            declination in -30.0f..30.0f)
    }

    @Test
    fun `getMagneticDeclination with altitude parameter`() {
        val lat = 40.7128 // New York
        val lon = -74.0060
        val altitude = 100.0f // 100 meters above sea level
        
        val declinationWithAltitude = QiblaCalculator.getMagneticDeclination(mockContext, lat, lon, altitude)
        val declinationWithoutAltitude = QiblaCalculator.getMagneticDeclination(mockContext, lat, lon)
        
        // Declination should be within reasonable bounds
        assertTrue("Declination with altitude should be within reasonable bounds", 
            declinationWithAltitude in -30.0f..30.0f)
        assertTrue("Declination without altitude should be within reasonable bounds", 
            declinationWithoutAltitude in -30.0f..30.0f)
    }

    @Test
    fun `distanceToMecca returns positive value`() {
        val lat = 40.7128 // New York
        val lon = -74.0060
        val expectedDistance = 10000.0 // Approximately 10,000 km from New York to Mecca
        
        val actualDistance = QiblaCalculator.distanceToMecca(lat, lon)
        
        assertTrue("Distance to Mecca should be positive", actualDistance > 0)
        assertTrue("Distance from New York to Mecca should be around 10,000 km", 
            actualDistance in (expectedDistance - 1000.0)..(expectedDistance + 1000.0))
    }

    @Test
    fun `calculateQiblaBearing throws exception for invalid coordinates`() {
        // Test invalid latitude
        try {
            QiblaCalculator.calculateQiblaBearing(91.0, 0.0)
            fail("Should throw exception for invalid latitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention invalid coordinates", 
                e.message?.contains("Invalid coordinates") == true)
        }
        
        // Test invalid longitude
        try {
            QiblaCalculator.calculateQiblaBearing(0.0, 181.0)
            fail("Should throw exception for invalid longitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention invalid coordinates", 
                e.message?.contains("Invalid coordinates") == true)
        }
    }
}
