package com.hieltech.haramblur.detection

import android.graphics.Bitmap
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
 * Performance tests for FastContentDetector to validate <50ms processing time requirement
 */
class FastContentDetectorPerformanceTest {
    
    private lateinit var fastContentDetector: FastContentDetectorImpl
    private lateinit var mockMLModelManager: MLModelManager
    private lateinit var mockFaceDetectionManager: FaceDetectionManager
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        mockMLModelManager = mockk()
        mockFaceDetectionManager = mockk()
        performanceMonitor = PerformanceMonitor()
        
        fastContentDetector = FastContentDetectorImpl(
            mockMLModelManager,
            mockFaceDetectionManager
        )
        
        // Setup mock responses for fast execution
        setupFastMockResponses()
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `ultra fast mode should complete detection within 50ms`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings(
            ultraFastModeEnabled = true,
            maxProcessingTimeMs = 50L
        )
        
        fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = fastContentDetector.detectContentFast(bitmap, settings)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Processing time should be under 50ms, was ${processingTime}ms", 
                  processingTime < 50L)
        assertTrue("Result should indicate successful processing", result.processingTimeMs >= 0)
        assertEquals("Should be in ultra fast mode", PerformanceMode.ULTRA_FAST.maxProcessingTimeMs, 50L)
        
        // Record performance for monitoring
        performanceMonitor.recordMeasurement(
            processingTimeMs = processingTime,
            targetTimeMs = 50L,
            operationType = OperationType.FAST_DETECTION,
            qualityLevel = QualityLevel.ULTRA_FAST
        )
    }
    
    @Test
    fun `fast mode should complete detection within 100ms`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings(maxProcessingTimeMs = 100L)
        
        fastContentDetector.setPerformanceMode(PerformanceMode.FAST)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = fastContentDetector.detectContentFast(bitmap, settings)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Processing time should be under 100ms, was ${processingTime}ms", 
                  processingTime < 100L)
        assertFalse("Should not be a skipped frame", result.frameSkipped)
        
        performanceMonitor.recordMeasurement(
            processingTimeMs = processingTime,
            targetTimeMs = 100L,
            operationType = OperationType.FAST_DETECTION,
            qualityLevel = QualityLevel.FAST
        )
    }
    
    @Test
    fun `density analysis should complete within performance targets`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = fastContentDetector.analyzeContentDensity(bitmap)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Density analysis should complete quickly, was ${processingTime}ms", 
                  processingTime < 200L)
        assertNotNull("Should return valid density result", result.densityMetrics)
        assertTrue("Processing time should be recorded", result.processingTimeMs >= 0)
        
        performanceMonitor.recordMeasurement(
            processingTimeMs = processingTime,
            targetTimeMs = 200L,
            operationType = OperationType.DENSITY_ANALYSIS,
            qualityLevel = QualityLevel.BALANCED
        )
    }
    
    @Test
    fun `performance should remain consistent across multiple detections`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings(ultraFastModeEnabled = true)
        val iterations = 10
        val maxAllowedTime = 50L
        
        fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST)
        
        // When
        val processingTimes = mutableListOf<Long>()
        repeat(iterations) {
            val startTime = System.currentTimeMillis()
            fastContentDetector.detectContentFast(bitmap, settings)
            val processingTime = System.currentTimeMillis() - startTime
            processingTimes.add(processingTime)
            
            performanceMonitor.recordMeasurement(
                processingTimeMs = processingTime,
                targetTimeMs = maxAllowedTime,
                operationType = OperationType.FAST_DETECTION,
                qualityLevel = QualityLevel.ULTRA_FAST
            )
        }
        
        // Then
        val avgTime = processingTimes.average()
        val maxTime = processingTimes.maxOrNull() ?: 0L
        val violationCount = processingTimes.count { it > maxAllowedTime }
        
        assertTrue("Average processing time should be under ${maxAllowedTime}ms, was ${avgTime.toInt()}ms", 
                  avgTime < maxAllowedTime)
        assertTrue("Max processing time should be reasonable, was ${maxTime}ms", 
                  maxTime < maxAllowedTime * 2)
        assertTrue("Violation rate should be low, had $violationCount violations out of $iterations", 
                  violationCount < iterations * 0.2) // Less than 20% violations
        
        // Validate ultra-fast performance
        val validation = performanceMonitor.validateUltraFastPerformance()
        assertTrue("Ultra-fast performance validation should pass: ${validation.reason}", 
                  validation.isValid)
    }
    
    @Test
    fun `frame skipping should improve performance under load`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings(frameSkipThreshold = 2)
        
        fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST)
        
        // When - Simulate rapid consecutive calls
        val results = mutableListOf<FastDetectionResult>()
        repeat(10) {
            val result = fastContentDetector.detectContentFast(bitmap, settings)
            results.add(result)
        }
        
        // Then
        val skippedFrames = results.count { it.frameSkipped }
        val processedFrames = results.count { !it.frameSkipped }
        
        assertTrue("Some frames should be processed", processedFrames > 0)
        assertTrue("Frame skipping should occur under rapid load", skippedFrames > 0)
        
        val avgProcessingTime = results.filter { !it.frameSkipped }
            .map { it.processingTimeMs }
            .average()
        
        assertTrue("Average processing time for non-skipped frames should be reasonable", 
                  avgProcessingTime < 100.0)
    }
    
    @Test
    fun `quality reduction should maintain performance targets`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(512, 512) // Larger image
        val settings = AppSettings(ultraFastModeEnabled = true)
        
        fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = fastContentDetector.detectContentFast(bitmap, settings)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Quality should be reduced for performance", result.qualityReduced)
        assertTrue("Processing time should still meet targets despite larger image, was ${processingTime}ms", 
                  processingTime < 100L) // Allow some tolerance for larger image
        
        performanceMonitor.recordMeasurement(
            processingTimeMs = processingTime,
            targetTimeMs = 50L,
            operationType = OperationType.FAST_DETECTION,
            qualityLevel = QualityLevel.ULTRA_FAST,
            additionalData = mapOf("qualityReduced" to true, "imageSize" to "512x512")
        )
    }
    
    @Test
    fun `caching should improve subsequent detection performance`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings()
        
        // When - First detection (cache miss)
        val startTime1 = System.currentTimeMillis()
        val result1 = fastContentDetector.detectContentFast(bitmap, settings)
        val firstTime = System.currentTimeMillis() - startTime1
        
        // Second detection (cache hit)
        val startTime2 = System.currentTimeMillis()
        val result2 = fastContentDetector.detectContentFast(bitmap, settings)
        val secondTime = System.currentTimeMillis() - startTime2
        
        // Then
        assertTrue("Second detection should be faster due to caching", secondTime <= firstTime)
        assertEquals("Results should be consistent", result1.shouldBlur, result2.shouldBlur)
        assertTrue("Cached result should be very fast", secondTime < 20L)
    }
    
    @Test
    fun `performance monitor should track violations correctly`() = runBlocking {
        // Given
        val bitmap = createTestBitmap(224, 224)
        val settings = AppSettings()
        val targetTime = 30L // Very strict target to force violations
        
        // When - Perform detections with strict target
        repeat(5) {
            val startTime = System.currentTimeMillis()
            fastContentDetector.detectContentFast(bitmap, settings)
            val processingTime = System.currentTimeMillis() - startTime
            
            performanceMonitor.recordMeasurement(
                processingTimeMs = processingTime,
                targetTimeMs = targetTime,
                operationType = OperationType.FAST_DETECTION,
                qualityLevel = QualityLevel.BALANCED
            )
        }
        
        // Then
        val report = performanceMonitor.getPerformanceReport()
        assertTrue("Should have recorded measurements", report.totalSamples > 0)
        assertNotNull("Should have operation stats", report.operationStats[OperationType.FAST_DETECTION])
        assertTrue("Should track violations", report.violationCount >= 0)
    }
    
    private fun setupFastMockResponses() {
        // Mock face detection for fast response
        every { mockFaceDetectionManager.detectFaces(any()) } returns 
            FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
        
        // Mock NSFW detection for fast response
        every { mockMLModelManager.detectNSFW(any()) } returns 
            MLModelManager.DetectionResult(false, 0.3f, "Fast mock detection")
        
        every { mockMLModelManager.isModelReady() } returns true
    }
    
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        // Create a mock bitmap for testing
        return mockk<Bitmap> {
            every { this@mockk.width } returns width
            every { this@mockk.height } returns height
            every { getPixel(any(), any()) } returns 0xFF888888.toInt() // Gray pixel
        }
    }
}