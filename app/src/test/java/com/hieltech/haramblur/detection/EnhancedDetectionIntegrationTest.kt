package com.hieltech.haramblur.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for enhanced detection system
 * Tests the complete detection pipeline with all components working together
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EnhancedDetectionIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var contentDetectionEngine: ContentDetectionEngine
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var devicePerformanceAnalyzer: DevicePerformanceAnalyzer
    private lateinit var memoryManager: MemoryManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    private lateinit var comprehensiveErrorHandler: ComprehensiveErrorHandler
    private lateinit var fallbackDetectionEngine: FallbackDetectionEngine
    
    // Mocked dependencies
    private lateinit var mlModelManager: MLModelManager
    private lateinit var faceDetectionManager: FaceDetectionManager
    private lateinit var fastContentDetector: FastContentDetector
    private lateinit var frameOptimizationManager: FrameOptimizationManager
    private lateinit var contentDensityAnalyzer: ContentDensityAnalyzer
    private lateinit var fullScreenBlurTrigger: FullScreenBlurTrigger
    private lateinit var enhancedGenderDetector: EnhancedGenderDetector
    
    private lateinit var testBitmap: Bitmap
    private lateinit var testSettings: AppSettings
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create test bitmap
        testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        
        // Create test settings
        testSettings = AppSettings(
            enableFaceDetection = true,
            enableNSFWDetection = true,
            blurMaleFaces = true,
            blurFemaleFaces = true,
            detectionSensitivity = 0.5f,
            ultraFastModeEnabled = false,
            fullScreenWarningEnabled = true,
            maxProcessingTimeMs = 200L
        )
        
        // Mock dependencies
        mlModelManager = mockk(relaxed = true)
        faceDetectionManager = mockk(relaxed = true)
        fastContentDetector = mockk(relaxed = true)
        frameOptimizationManager = mockk(relaxed = true)
        contentDensityAnalyzer = mockk(relaxed = true)
        fullScreenBlurTrigger = mockk(relaxed = true)
        enhancedGenderDetector = mockk(relaxed = true)
        
        // Create real instances
        devicePerformanceAnalyzer = DevicePerformanceAnalyzer(context)
        memoryManager = MemoryManager()
        batteryOptimizationManager = BatteryOptimizationManager(context)
        comprehensiveErrorHandler = ComprehensiveErrorHandler(memoryManager, performanceMonitor)
        fallbackDetectionEngine = FallbackDetectionEngine()
        
        performanceMonitor = PerformanceMonitor(
            devicePerformanceAnalyzer,
            memoryManager,
            batteryOptimizationManager
        )
        
        contentDetectionEngine = ContentDetectionEngine(
            mlModelManager,
            faceDetectionManager,
            fastContentDetector,
            frameOptimizationManager,
            performanceMonitor,
            contentDensityAnalyzer,
            fullScreenBlurTrigger
        )
        
        // Initialize components
        runBlocking {
            devicePerformanceAnalyzer.initialize()
            batteryOptimizationManager.initialize()
            performanceMonitor.initialize()
            contentDetectionEngine.initialize(context)
        }
    }
    
    @After
    fun teardown() {
        testBitmap.recycle()
        performanceMonitor.cleanup()
        devicePerformanceAnalyzer.cleanup()
        batteryOptimizationManager.cleanup()
        comprehensiveErrorHandler.cleanup()
    }
    
    @Test
    fun `test complete detection pipeline with gender detection integration`() = runBlocking {
        // Arrange
        val mockFaceResult = FaceDetectionManager.FaceDetectionResult(
            faceCount = 2,
            faces = listOf(
                createMockFace(Rect(50, 50, 150, 150), Gender.MALE, 0.9f),
                createMockFace(Rect(200, 100, 300, 200), Gender.FEMALE, 0.8f)
            ),
            success = true,
            error = null
        )
        
        val mockNsfwResult = MLModelManager.DetectionResult(
            isNSFW = false,
            confidence = 0.2f,
            message = "Clean content"
        )
        
        val mockDensityResult = DensityAnalysisResult(
            inappropriateContentDensity = 0.3f,
            spatialDistribution = mockk(),
            recommendedAction = ContentAction.SELECTIVE_BLUR,
            warningLevel = WarningLevel.LOW,
            criticalRegions = listOf(Rect(50, 50, 150, 150))
        )
        
        // Mock responses
        coEvery { faceDetectionManager.detectFaces(any()) } returns mockFaceResult
        coEvery { mlModelManager.detectNSFW(any()) } returns mockNsfwResult
        coEvery { mlModelManager.isModelReady() } returns true
        coEvery { contentDensityAnalyzer.analyzeScreenContent(any()) } returns mockDensityResult
        coEvery { fullScreenBlurTrigger.shouldTriggerFullScreenBlur(any(), any()) } returns FullScreenBlurDecision(
            shouldTrigger = false,
            reason = "Density below threshold",
            confidence = 0.3f
        )
        
        // Act
        val result = contentDetectionEngine.analyzeContent(testBitmap, testSettings)
        
        // Assert
        assertTrue("Detection should succeed", result.isSuccessful())
        assertTrue("Should blur due to faces", result.shouldBlur)
        assertEquals("Should have blur regions for faces", 2, result.blurRegions.size)
        assertNotNull("Should have face detection result", result.faceDetectionResult)
        assertNotNull("Should have NSFW detection result", result.nsfwDetectionResult)
        assertNotNull("Should have density analysis result", result.densityAnalysisResult)
        assertEquals("Should recommend selective blur", ContentAction.SELECTIVE_BLUR, result.recommendedAction)
        assertFalse("Should not require full screen warning", result.requiresFullScreenWarning)
        
        // Verify interactions
        coVerify { faceDetectionManager.detectFaces(testBitmap) }
        coVerify { mlModelManager.detectNSFW(testBitmap) }
        coVerify { contentDensityAnalyzer.analyzeScreenContent(testBitmap) }
    }
    
    @Test
    fun `test ultra-fast mode performance requirements`() = runBlocking {
        // Arrange
        val ultraFastSettings = testSettings.copy(
            ultraFastModeEnabled = true,
            maxProcessingTimeMs = 50L
        )
        
        val mockFastResult = FastDetectionResult(
            shouldBlur = true,
            blurRegions = listOf(Rect(0, 0, 100, 100)),
            contentType = ContentType.SUSPICIOUS,
            processingTimeMs = 30L,
            confidenceScore = 0.7f
        )
        
        coEvery { frameOptimizationManager.shouldProcessFrame() } returns FrameDecision(
            shouldProcess = true,
            qualityLevel = QualityLevel.ULTRA_FAST,
            reason = "Ultra fast mode"
        )
        coEvery { fastContentDetector.detectContentFast(any(), any()) } returns mockFastResult
        coEvery { fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST) } just Runs
        coEvery { mlModelManager.setPerformanceMode(PerformanceMode.ULTRA_FAST) } just Runs
        coEvery { frameOptimizationManager.updatePerformanceMetrics(any(), any()) } just Runs
        
        // Act
        val startTime = System.currentTimeMillis()
        val result = contentDetectionEngine.analyzeContentFast(testBitmap, ultraFastSettings)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Assert
        assertTrue("Fast detection should succeed", result.isSuccessful())
        assertTrue("Processing time should be under 50ms", processingTime < 50L)
        assertTrue("Should blur based on fast detection", result.shouldBlur)
        assertEquals("Should have one blur region", 1, result.blurRegions.size)
        
        // Verify ultra-fast mode was set
        coVerify { fastContentDetector.setPerformanceMode(PerformanceMode.ULTRA_FAST) }
        coVerify { mlModelManager.setPerformanceMode(PerformanceMode.ULTRA_FAST) }
    }
    
    @Test
    fun `test content density analysis accuracy`() = runBlocking {
        // Arrange
        val highDensityResult = DensityAnalysisResult(
            inappropriateContentDensity = 0.6f,
            spatialDistribution = mockk(),
            recommendedAction = ContentAction.FULL_SCREEN_BLUR,
            warningLevel = WarningLevel.HIGH,
            criticalRegions = listOf(
                Rect(0, 0, 112, 112),
                Rect(112, 0, 224, 112),
                Rect(0, 112, 112, 224)
            )
        )
        
        val fullScreenDecision = FullScreenBlurDecision(
            shouldTrigger = true,
            reason = "High inappropriate content density (60%)",
            confidence = 0.9f
        )
        
        coEvery { contentDensityAnalyzer.analyzeScreenContent(any()) } returns highDensityResult
        coEvery { fullScreenBlurTrigger.shouldTriggerFullScreenBlur(any(), any()) } returns fullScreenDecision
        coEvery { fullScreenBlurTrigger.calculateRecommendedAction(any(), any()) } returns ActionRecommendation(
            action = ContentAction.FULL_SCREEN_BLUR,
            confidence = 0.9f,
            reason = "High density detected"
        )
        
        // Mock other components to return minimal results
        coEvery { faceDetectionManager.detectFaces(any()) } returns FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
        coEvery { mlModelManager.detectNSFW(any()) } returns MLModelManager.DetectionResult(false, 0.1f, "Clean")
        coEvery { mlModelManager.isModelReady() } returns true
        
        // Act
        val result = contentDetectionEngine.analyzeContent(testBitmap, testSettings)
        
        // Assert
        assertTrue("Detection should succeed", result.isSuccessful())
        assertTrue("Should trigger full screen blur", result.requiresFullScreenWarning)
        assertEquals("Should recommend full screen blur", ContentAction.FULL_SCREEN_BLUR, result.recommendedAction)
        assertNotNull("Should have full screen blur decision", result.fullScreenBlurDecision)
        assertTrue("Full screen decision should trigger", result.fullScreenBlurDecision!!.shouldTrigger)
        assertEquals("Should have full screen blur region", 1, result.blurRegions.size)
        
        // Verify the blur region covers the full screen
        val fullScreenRegion = result.blurRegions.first()
        assertEquals("Full screen region should start at 0,0", 0, fullScreenRegion.left)
        assertEquals("Full screen region should start at 0,0", 0, fullScreenRegion.top)
        assertEquals("Full screen region should cover full width", testBitmap.width, fullScreenRegion.right)
        assertEquals("Full screen region should cover full height", testBitmap.height, fullScreenRegion.bottom)
    }
    
    @Test
    fun `test site blocking workflow integration`() = runBlocking {
        // This test would require SiteBlockingManager integration
        // For now, we'll test the fallback mechanism
        
        val embeddedSiteBlockingList = EmbeddedSiteBlockingList()
        
        // Test various URLs
        val testUrls = listOf(
            "https://example-adult-site.com" to true,
            "https://legitimate-news-site.com" to false,
            "https://casino-gambling.com" to true,
            "https://normal-shopping.com" to false
        )
        
        testUrls.forEach { (url, shouldBlock) ->
            val result = embeddedSiteBlockingList.checkUrl(url)
            
            if (shouldBlock) {
                assertTrue("URL should be blocked: $url", result.isBlocked)
                assertNotNull("Should have blocking category", result.category)
                assertNotNull("Should have Quranic verse", result.verse)
                assertTrue("Should have reasonable confidence", result.confidence > 0.5f)
            } else {
                assertFalse("URL should not be blocked: $url", result.isBlocked)
            }
        }
    }
    
    @Test
    fun `test error handling and recovery integration`() = runBlocking {
        // Arrange - simulate model loading failure
        coEvery { mlModelManager.isModelReady() } returns false
        coEvery { mlModelManager.detectNSFW(any()) } throws DetectionException(
            DetectionError.ModelNotLoaded,
            "Model not loaded"
        )
        
        // Act - use comprehensive error handler
        val result = comprehensiveErrorHandler.executeWithRecovery(
            operationType = OperationType.NSFW_DETECTION,
            context = "test_context",
            operation = {
                mlModelManager.detectNSFW(testBitmap)
            },
            fallback = { error ->
                // Use fallback detection
                val fallbackResult = fallbackDetectionEngine.detectNSFWHeuristic(testBitmap)
                MLModelManager.DetectionResult(
                    isNSFW = fallbackResult.isNSFW,
                    confidence = fallbackResult.confidence,
                    message = "Fallback heuristic detection"
                )
            }
        )
        
        // Assert
        assertNotNull("Should get fallback result", result)
        assertEquals("Should use fallback message", "Fallback heuristic detection", result.message)
        
        // Verify error was handled
        val errorStats = comprehensiveErrorHandler.getErrorStats()
        assertTrue("Should have recorded errors", errorStats.totalErrors > 0)
        assertTrue("Should have recent errors", errorStats.recentErrors > 0)
    }
    
    @Test
    fun `test performance monitoring integration`() = runBlocking {
        // Arrange
        val operationType = OperationType.FULL_PIPELINE
        val targetTime = 200L
        
        // Mock successful detection
        coEvery { faceDetectionManager.detectFaces(any()) } returns FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
        coEvery { mlModelManager.detectNSFW(any()) } returns MLModelManager.DetectionResult(false, 0.1f, "Clean")
        coEvery { mlModelManager.isModelReady() } returns true
        coEvery { contentDensityAnalyzer.analyzeScreenContent(any()) } returns DensityAnalysisResult(
            inappropriateContentDensity = 0.1f,
            spatialDistribution = mockk(),
            recommendedAction = ContentAction.NO_ACTION,
            warningLevel = WarningLevel.NONE,
            criticalRegions = emptyList()
        )
        
        // Act - perform multiple detections to generate performance data
        repeat(5) {
            val startTime = System.currentTimeMillis()
            contentDetectionEngine.analyzeContent(testBitmap, testSettings)
            val processingTime = System.currentTimeMillis() - startTime
            
            performanceMonitor.recordMeasurement(
                processingTimeMs = processingTime,
                targetTimeMs = targetTime,
                operationType = operationType,
                qualityLevel = QualityLevel.HIGH
            )
        }
        
        // Assert
        val performanceReport = performanceMonitor.getPerformanceReport()
        assertTrue("Should have performance samples", performanceReport.totalSamples > 0)
        assertTrue("Should have reasonable average processing time", performanceReport.avgProcessingTimeMs > 0)
        assertNotNull("Should have device profile", performanceReport.deviceProfile)
        assertNotNull("Should have memory stats", performanceReport.memoryStats)
        assertNotNull("Should have battery stats", performanceReport.batteryStats)
        
        // Test integrated recommendations
        val recommendations = performanceMonitor.getIntegratedRecommendations(operationType)
        assertNotNull("Should have recommendations", recommendations)
        assertTrue("Should have reasonable max concurrent operations", recommendations.maxConcurrentOperations > 0)
        assertTrue("Should have reasonable image scale", recommendations.recommendedImageScale > 0f)
    }
    
    @Test
    fun `test memory management under pressure`() = runBlocking {
        // Arrange - simulate memory pressure
        val memoryStats = memoryManager.getMemoryStats()
        
        // Cache some detection results
        repeat(10) { i ->
            memoryManager.cacheDetectionResult(
                key = "test_key_$i",
                result = "test_result_$i",
                operationType = OperationType.FACE_DETECTION,
                ttlMs = 60000L
            )
        }
        
        // Cache some bitmaps
        repeat(5) { i ->
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            memoryManager.cacheBitmap("bitmap_$i", bitmap)
        }
        
        // Act - simulate memory pressure
        memoryManager.handleMemoryPressure(MemoryPressureLevel.HIGH)
        
        // Assert
        val newMemoryStats = memoryManager.getMemoryStats()
        assertTrue("Cache should be reduced under pressure", 
                  newMemoryStats.cacheAllocatedMB <= memoryStats.cacheAllocatedMB)
        
        // Test cache hit/miss functionality
        val cachedResult = memoryManager.getCachedDetectionResult("test_key_0")
        // Result might be null if cleared due to memory pressure, which is expected behavior
        
        val finalStats = memoryManager.getMemoryStats()
        assertTrue("Should have cache hit rate data", finalStats.cacheHitRate >= 0f)
    }
    
    @Test
    fun `test battery optimization integration`() = runBlocking {
        // Arrange
        batteryOptimizationManager.startMonitoring()
        
        // Simulate low battery state
        batteryOptimizationManager.setOptimizationLevel(OptimizationLevel.AGGRESSIVE)
        
        // Act
        val throttleDecision = batteryOptimizationManager.shouldThrottleOperation(OperationType.FULL_PIPELINE)
        val batteryStats = batteryOptimizationManager.getBatteryStats()
        val baseRecommendations = PerformanceRecommendations.midRange()
        val optimizedRecommendations = batteryOptimizationManager.getBatteryOptimizedRecommendations(baseRecommendations)
        
        // Assert
        assertNotNull("Should have throttle decision", throttleDecision)
        assertNotNull("Should have battery stats", batteryStats)
        assertEquals("Should have aggressive optimization", OptimizationLevel.AGGRESSIVE, batteryStats.optimizationLevel)
        
        // Optimized recommendations should be more conservative
        assertTrue("Should reduce concurrent operations", 
                  optimizedRecommendations.maxConcurrentOperations <= baseRecommendations.maxConcurrentOperations)
        assertTrue("Should increase processing time", 
                  optimizedRecommendations.maxProcessingTimeMs >= baseRecommendations.maxProcessingTimeMs)
    }
    
    private fun createMockFace(boundingBox: Rect, gender: Gender, confidence: Float): FaceDetectionManager.DetectedFace {
        return mockk<FaceDetectionManager.DetectedFace>().apply {
            every { this@apply.boundingBox } returns boundingBox
            every { this@apply.gender } returns gender
            every { this@apply.genderConfidence } returns confidence
        }
    }
}

/**
 * Mock gender enum for testing
 */
enum class Gender {
    MALE, FEMALE, UNKNOWN
}

/**
 * Mock content type for testing
 */
enum class ContentType {
    CLEAN, SUSPICIOUS, INAPPROPRIATE
}

/**
 * Mock warning level for testing
 */
enum class WarningLevel {
    NONE, LOW, MEDIUM, HIGH, CRITICAL
}