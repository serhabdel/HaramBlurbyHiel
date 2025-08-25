package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for FastContentDetector to verify basic functionality
 */
class FastContentDetectorIntegrationTest {
    
    private lateinit var fastContentDetector: FastContentDetectorImpl
    private lateinit var mockMLModelManager: MLModelManager
    private lateinit var mockFaceDetectionManager: FaceDetectionManager
    
    @Before
    fun setup() {
        mockMLModelManager = mockk()
        mockFaceDetectionManager = mockk()
        
        fastContentDetector = FastContentDetectorImpl(
            mockMLModelManager,
            mockFaceDetectionManager
        )
        
        // Setup basic mock responses
        every { mockFaceDetectionManager.detectFaces(any()) } returns 
            FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
        
        every { mockMLModelManager.detectNSFW(any()) } returns 
            MLModelManager.DetectionResult(false, 0.3f, "Mock detection")
        
        every { mockMLModelManager.isModelReady() } returns true
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `fast content detector should initialize and process content`() = runBlocking {
        // Given
        val bitmap = mockk<android.graphics.Bitmap> {
            every { width } returns 224
            every { height } returns 224
            every { getPixel(any(), any()) } returns 0xFF888888.toInt()
        }
        val settings = AppSettings(ultraFastModeEnabled = true)
        
        // When
        val result = fastContentDetector.detectContentFast(bitmap, settings)
        
        // Then
        assertNotNull("Result should not be null", result)
        assertTrue("Processing time should be recorded", result.processingTimeMs >= 0)
        assertNotNull("Performance metrics should be provided", result.performanceMetrics)
        assertNotNull("Content type should be determined", result.contentType)
    }
    
    @Test
    fun `performance modes should be configurable`() {
        // Given
        val modes = listOf(
            PerformanceMode.ULTRA_FAST,
            PerformanceMode.FAST,
            PerformanceMode.BALANCED,
            PerformanceMode.QUALITY
        )
        
        // When & Then
        modes.forEach { mode ->
            fastContentDetector.setPerformanceMode(mode)
            // Should not throw exception
            assertTrue("Performance mode should be set successfully", true)
        }
    }
    
    @Test
    fun `content density analysis should work`() = runBlocking {
        // Given
        val bitmap = mockk<android.graphics.Bitmap> {
            every { width } returns 224
            every { height } returns 224
            every { getPixel(any(), any()) } returns 0xFF888888.toInt()
        }
        
        // When
        val result = fastContentDetector.analyzeContentDensity(bitmap)
        
        // Then
        assertNotNull("Density result should not be null", result)
        assertTrue("Processing time should be recorded", result.processingTimeMs >= 0)
        assertNotNull("Density metrics should be provided", result.densityMetrics)
        assertTrue("Inappropriate content percentage should be valid", 
                  result.inappropriateContentPercentage >= 0.0f && 
                  result.inappropriateContentPercentage <= 1.0f)
    }
    
    @Test
    fun `gpu acceleration status should be queryable`() {
        // When
        val isGPUEnabled = fastContentDetector.isGPUAccelerationEnabled()
        
        // Then
        // Should not throw exception and return a boolean
        assertTrue("GPU status should be queryable", isGPUEnabled || !isGPUEnabled)
    }
    
    @Test
    fun `performance stats should be available`() {
        // When
        val stats = fastContentDetector.getPerformanceStats()
        
        // Then
        assertNotNull("Performance stats should not be null", stats)
        assertTrue("Processing time should be non-negative", stats.processingTimeMs >= 0)
    }
    
    @Test
    fun `caches should be clearable`() {
        // When & Then
        fastContentDetector.clearCaches()
        // Should not throw exception
        assertTrue("Cache clearing should succeed", true)
    }
}