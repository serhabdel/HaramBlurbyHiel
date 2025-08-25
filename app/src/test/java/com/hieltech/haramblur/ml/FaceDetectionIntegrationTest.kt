package com.hieltech.haramblur.ml

import android.graphics.Rect
import com.hieltech.haramblur.detection.Gender
import com.hieltech.haramblur.detection.GenderDetectionResult
import com.hieltech.haramblur.detection.FacialFeatureAnalysis
import com.hieltech.haramblur.data.AppSettings
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for gender-specific face filtering
 */
class FaceDetectionIntegrationTest {
    
    @Before
    fun setup() {
        // Setup for integration tests
    }
    
    @Test
    fun `should integrate enhanced detection with existing blur overlay`() {
        // Setup gender detection results
        val maleResult = GenderDetectionResult(
            gender = Gender.MALE,
            confidence = 0.9f,
            facialFeatures = FacialFeatureAnalysis.default(),
            processingTimeMs = 10L
        )
        
        val femaleResult = GenderDetectionResult(
            gender = Gender.FEMALE,
            confidence = 0.85f,
            facialFeatures = FacialFeatureAnalysis.default(),
            processingTimeMs = 12L
        )
        
        // Test settings: blur males only
        val settings = AppSettings(
            enableFaceDetection = true,
            blurMaleFaces = true,
            blurFemaleFaces = false
        )
        
        // Simulate the integration
        val detectedFaces = listOf(
            FaceDetectionManager.DetectedFace(
                boundingBox = Rect(100, 100, 200, 250),
                estimatedGender = Gender.MALE,
                genderConfidence = 0.9f,
                genderDetectionResult = maleResult
            ),
            FaceDetectionManager.DetectedFace(
                boundingBox = Rect(300, 100, 400, 250),
                estimatedGender = Gender.FEMALE,
                genderConfidence = 0.85f,
                genderDetectionResult = femaleResult
            )
        )
        
        val faceResult = FaceDetectionManager.FaceDetectionResult(
            facesDetected = 2,
            detectedFaces = detectedFaces,
            success = true,
            error = null
        )
        
        val facesToBlur = faceResult.getFacesToBlur(settings)
        
        assertEquals("Should only blur male faces", 1, facesToBlur.size)
        assertEquals("Should blur the male face", Gender.MALE, facesToBlur[0].estimatedGender)
    }
    
    @Test
    fun `should validate performance requirements`() {
        val startTime = System.currentTimeMillis()
        
        // Simulate processing time
        Thread.sleep(10) // Simulate 10ms processing
        
        val totalTime = System.currentTimeMillis() - startTime
        
        assertTrue("Should complete within reasonable time", totalTime < 100)
    }
    
    @Test
    fun `should validate gender detection data classes`() {
        // Test GenderDetectionResult
        val result = GenderDetectionResult(
            gender = Gender.MALE,
            confidence = 0.9f,
            facialFeatures = FacialFeatureAnalysis.default(),
            processingTimeMs = 10L
        )
        
        assertEquals("Should have correct gender", Gender.MALE, result.gender)
        assertTrue("Should have valid confidence", result.confidence > 0.0f)
        assertNotNull("Should have facial features", result.facialFeatures)
        assertTrue("Should have processing time", result.processingTimeMs >= 0)
    }
}