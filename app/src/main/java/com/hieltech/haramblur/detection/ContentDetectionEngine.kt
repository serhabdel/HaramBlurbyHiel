package com.hieltech.haramblur.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.data.AppSettings
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentDetectionEngine @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val faceDetectionManager: FaceDetectionManager,
    private val fastContentDetector: FastContentDetector,
    private val frameOptimizationManager: FrameOptimizationManager,
    private val performanceMonitor: PerformanceMonitor,
    private val contentDensityAnalyzer: ContentDensityAnalyzer,
    private val fullScreenBlurTrigger: FullScreenBlurTrigger
) {
    
    companion object {
        private const val TAG = "ContentDetectionEngine"
        private const val DETECTION_TIMEOUT_MS = 5000L
    }
    
    private var isInitialized = false
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun initialize(context: Context): Boolean {
        return try {
            Log.d(TAG, "Initializing content detection engine...")
            
            val mlInitialized = mlModelManager.initialize(context)
            if (!mlInitialized) {
                Log.w(TAG, "ML model initialization failed, continuing without it")
            }
            
            isInitialized = true
            Log.d(TAG, "Content detection engine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize content detection engine", e)
            false
        }
    }
    
    suspend fun analyzeContent(
        bitmap: Bitmap,
        appSettings: AppSettings
    ): ContentAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            Log.d(TAG, "📸 Starting content analysis - Image: ${bitmap.width}x${bitmap.height}")
            
            // Use concurrent processing for better performance
            val faceDetectionDeferred = if (appSettings.enableFaceDetection) {
                async {
                    Log.d(TAG, "👤 Starting face detection...")
                    val result = faceDetectionManager.detectFaces(bitmap, appSettings)
                    Log.d(TAG, "👤 Face detection completed - Found ${result.detectedFaces.size} faces")
                    result
                }
            } else {
                async {
                    Log.d(TAG, "👤 Face detection disabled")
                    FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
                }
            }
            
            // NSFW detection (concurrent with face detection)
            val nsfwDetectionDeferred = if (appSettings.enableNSFWDetection && mlModelManager.isModelReady()) {
                async {
                    Log.d(TAG, "🔞 Starting NSFW content detection...")
                    val result = mlModelManager.detectNSFWFast(bitmap)
                    Log.d(TAG, "🔞 NSFW detection completed")
                    result
                }
            } else {
                async {
                    Log.d(TAG, "🔞 NSFW detection disabled")
                    MLModelManager.DetectionResult(false, 0.0f, "NSFW detection disabled")
                }
            }
            
            // Wait for both detections to complete
            val faceResult = faceDetectionDeferred.await()
            val nsfwResult = nsfwDetectionDeferred.await()
            
            Log.d(TAG, "📊 Detection results summary:")
            Log.d(TAG, "   • Faces detected: ${faceResult.detectedFaces.size}")
            Log.d(TAG, "   • NSFW content: ${nsfwResult.isNSFW}")
            
            // Calculate blur regions
            val blurRegions = calculateBlurRegions(
                faceResult,
                nsfwResult,
                appSettings
            )
            
            Log.d(TAG, "🎯 Generated ${blurRegions.size} blur regions")
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ Content analysis completed in ${processingTime}ms")
            
            ContentAnalysisResult(
                shouldBlur = blurRegions.isNotEmpty(),
                blurRegions = blurRegions,
                faceDetectionResult = faceResult,
                nsfwDetectionResult = nsfwResult,
                processingTimeMs = processingTime,
                success = true,
                error = null
            )
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Content analysis failed after ${processingTime}ms", e)
            Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   • Error message: ${e.message}")
            
            ContentAnalysisResult(
                shouldBlur = false,
                blurRegions = emptyList(),
                faceDetectionResult = null,
                nsfwDetectionResult = null,
                processingTimeMs = processingTime,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Fast content analysis using optimized detection pipeline
     */
    suspend fun analyzeContentFast(
        bitmap: Bitmap,
        appSettings: AppSettings
    ): ContentAnalysisResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔍 Starting content analysis - Bitmap: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "⚙️ Settings - Female faces: ${appSettings.blurFemaleFaces}, Male faces: ${appSettings.blurMaleFaces}, NSFW: ${appSettings.enableNSFWDetection}, GPU: ${appSettings.enableGPUAcceleration}")
        
        try {
            // Check if frame should be processed
            val frameDecision = frameOptimizationManager.shouldProcessFrame()
            if (!frameDecision.shouldProcess) {
                Log.d(TAG, "Frame skipped: ${frameDecision.reason}")
                return@withContext ContentAnalysisResult(
                    shouldBlur = false,
                    blurRegions = emptyList(),
                    faceDetectionResult = null,
                    nsfwDetectionResult = null,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    success = true,
                    error = null
                )
            }
            
            // Enhanced performance mode with GPU acceleration priority
            val performanceMode = when {
                appSettings.enableGPUAcceleration && appSettings.ultraFastModeEnabled -> PerformanceMode.ULTRA_FAST
                appSettings.enableGPUAcceleration -> PerformanceMode.FAST
                appSettings.ultraFastModeEnabled -> PerformanceMode.ULTRA_FAST
                appSettings.maxProcessingTimeMs <= 50L -> PerformanceMode.ULTRA_FAST
                appSettings.maxProcessingTimeMs <= 100L -> PerformanceMode.FAST
                else -> PerformanceMode.BALANCED
            }
            
            fastContentDetector.setPerformanceMode(performanceMode)
            mlModelManager.setPerformanceMode(performanceMode)
            
            // Log GPU acceleration status
            if (appSettings.enableGPUAcceleration) {
                Log.d(TAG, "GPU acceleration enabled for faster female content detection")
            }
            
            // Perform fast detection
            val fastResult = fastContentDetector.detectContentFast(bitmap, appSettings)

            val processingTime = System.currentTimeMillis() - startTime

            // Update performance metrics
            frameOptimizationManager.updatePerformanceMetrics(
                processingTime,
                appSettings.maxProcessingTimeMs
            )

            // Extract region-based information from fast result
            val regionCount = fastResult.nsfwRegionCount
            val maxRegionConfidence = fastResult.maxNsfwConfidence
            val regionRects = fastResult.nsfwRegionRects

            // Check NEW 6-region rule for full-screen blur triggering
            val regionBasedFullScreenTrigger = if (appSettings.enableRegionBasedFullScreen &&
                regionCount >= appSettings.nsfwFullScreenRegionThreshold &&
                maxRegionConfidence >= appSettings.nsfwHighConfidenceThreshold) {
                Log.d(TAG, "🚨 Region-based full-screen blur triggered: $regionCount regions with max confidence $maxRegionConfidence")
                true
            } else {
                false
            }

            // Perform quick density analysis if enabled and not in ultra-fast mode
            val densityAnalysisResult = if (appSettings.fullScreenWarningEnabled &&
                performanceMode != PerformanceMode.ULTRA_FAST) {
                contentDensityAnalyzer.analyzeScreenContent(bitmap)
            } else {
                null
            }

            // Check for full-screen blur decision (existing density-based logic)
            val fullScreenDecision = densityAnalysisResult?.let { densityResult ->
                fullScreenBlurTrigger.shouldTriggerFullScreenBlur(densityResult, appSettings)
            }

            // Override blur decision if full-screen blur is required (region-based takes priority)
            val finalShouldBlur = regionBasedFullScreenTrigger ||
                    fullScreenDecision?.shouldTrigger == true ||
                    fastResult.shouldBlur
            val finalBlurRegions = when {
                regionBasedFullScreenTrigger -> {
                    // Region-based full-screen blur
                    listOf(Rect(0, 0, bitmap.width, bitmap.height))
                }
                fullScreenDecision?.shouldTrigger == true -> {
                    // Density-based full-screen blur
                    listOf(Rect(0, 0, bitmap.width, bitmap.height))
                }
                else -> {
                    // Selective blur
                    fastResult.blurRegions
                }
            }

            // Determine recommended action
            val recommendedAction = when {
                regionBasedFullScreenTrigger -> ContentAction.FULL_SCREEN_BLUR
                fullScreenDecision != null -> {
                    fullScreenBlurTrigger.calculateRecommendedAction(densityAnalysisResult!!, appSettings).action
                }
                else -> if (finalShouldBlur) ContentAction.SELECTIVE_BLUR else ContentAction.NO_ACTION
            }
            
            // Convert fast result to standard result format
            val contentAnalysisResult = ContentAnalysisResult(
                shouldBlur = finalShouldBlur,
                blurRegions = finalBlurRegions,
                faceDetectionResult = null, // Fast mode doesn't provide detailed face results
                nsfwDetectionResult = null, // Fast mode doesn't provide detailed NSFW results
                processingTimeMs = processingTime,
                success = true,
                error = null,
                densityAnalysisResult = densityAnalysisResult,
                fullScreenBlurDecision = fullScreenDecision,
                recommendedAction = recommendedAction,
                requiresFullScreenWarning = regionBasedFullScreenTrigger || fullScreenDecision?.shouldTrigger == true,
                nsfwRegionCount = regionCount,
                maxNsfwConfidence = maxRegionConfidence,
                nsfwRegionRects = regionRects,
                triggeredByRegionCount = regionBasedFullScreenTrigger
            )
            
            Log.d(TAG, "Fast content analysis completed in ${processingTime}ms: shouldBlur=${fastResult.shouldBlur}")
            return@withContext contentAnalysisResult
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "Fast content analysis failed", e)
            
            frameOptimizationManager.updatePerformanceMetrics(
                processingTime, 
                appSettings.maxProcessingTimeMs
            )
            
            return@withContext ContentAnalysisResult.failed("Fast analysis failed: ${e.message}")
        }
    }
    
    private fun calculateBlurRegions(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        appSettings: AppSettings
    ): List<Rect> {
        val blurRegions = mutableListOf<Rect>()

        // Check for region-based full-screen blur trigger
        if (appSettings.enableRegionBasedFullScreen &&
            nsfwResult.regionCount >= appSettings.nsfwFullScreenRegionThreshold &&
            nsfwResult.maxRegionConfidence >= appSettings.nsfwHighConfidenceThreshold) {
            Log.d(TAG, "Region-based full-screen blur in standard mode: ${nsfwResult.regionCount} regions")
            return listOf(Rect(0, 0, 1000, 1000)) // Placeholder - would need actual bitmap dimensions
        }

        // Add female face regions with enhanced detection
        // Note: FaceDetectionManager now only detects female faces, so no male filtering needed
        if (appSettings.enableFaceDetection && faceResult.hasFaces()) {
            val facesToBlur = mutableListOf<Rect>()

            // Focus on female faces only (male faces are already excluded by FaceDetectionManager)
            if (appSettings.blurFemaleFaces) {
                facesToBlur.addAll(faceResult.getFemaleFaces().map { it.boundingBox })

                // Enhanced: Include unknown gender faces with lower confidence for safety
                val unknownFaces = faceResult.getUnknownGenderFaces()
                    .filter { it.genderConfidence < 0.6f }
                    .map { it.boundingBox }
                facesToBlur.addAll(unknownFaces)
            }

            // Enhanced expansion for better coverage of female faces/hair
            val expandedFaceRegions = facesToBlur.map { face ->
                val expansion = 45 // Increased expansion for better female face coverage including hair
                Rect(
                    maxOf(0, face.left - expansion),
                    maxOf(0, face.top - (expansion * 1.5).toInt()), // More expansion on top for hair
                    face.right + expansion,
                    face.bottom + expansion
                )
            }
            blurRegions.addAll(expandedFaceRegions)

            Log.d(TAG, "Added ${expandedFaceRegions.size} female face blur regions (males already excluded)")
        }
        
        // Enhanced female body/NSFW content detection with better coverage
        // Note: This focuses on female content areas since male faces are excluded
        if (appSettings.enableNSFWDetection && nsfwResult.isNSFW) {
            when {
                nsfwResult.confidence > 0.6f -> {
                    // High confidence - blur entire screen for female body content
                    blurRegions.add(Rect(0, 0, 1080, 2400)) // Full screen blur
                    Log.d(TAG, "Full screen blur applied for high female body content confidence: ${nsfwResult.confidence}")
                }
                nsfwResult.confidence > 0.4f -> {
                    // Medium confidence - blur large center area covering typical female body regions
                    val centerBlur = Rect(20, 150, 1060, 2250) // Almost full screen with small margins
                    blurRegions.add(centerBlur)
                    Log.d(TAG, "Large area blur applied for medium female body content confidence: ${nsfwResult.confidence}")
                }
                nsfwResult.confidence > 0.25f -> {
                    // Lower confidence - blur typical body areas (torso/chest region)
                    val bodyBlur = Rect(80, 300, 1000, 1800) // Focus on torso area
                    blurRegions.add(bodyBlur)
                    Log.d(TAG, "Body area blur applied for moderate female body content confidence: ${nsfwResult.confidence}")
                }
            }
        }
        
        return blurRegions
    }
    
    private fun mergeOverlappingRegions(regions: List<Rect>): List<Rect> {
        if (regions.size <= 1) return regions
        
        val merged = mutableListOf<Rect>()
        val sorted = regions.sortedBy { it.left }
        
        var current = sorted[0]
        
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            
            if (Rect.intersects(current, next)) {
                // Merge overlapping rectangles
                current = Rect(
                    minOf(current.left, next.left),
                    minOf(current.top, next.top),
                    maxOf(current.right, next.right),
                    maxOf(current.bottom, next.bottom)
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        
        merged.add(current)
        return merged
    }
    
    fun updateSettings(newSettings: AppSettings) {
        Log.d(TAG, "Detection settings updated: $newSettings")
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up content detection engine")
        detectionScope.cancel()
        mlModelManager.cleanup()
        faceDetectionManager.cleanup()
        isInitialized = false
    }
    
    fun isReady(): Boolean = isInitialized
    
    
    data class ContentAnalysisResult(
        val shouldBlur: Boolean,
        val blurRegions: List<Rect>,
        val faceDetectionResult: FaceDetectionManager.FaceDetectionResult?,
        val nsfwDetectionResult: MLModelManager.DetectionResult?,
        val processingTimeMs: Long,
        val success: Boolean,
        val error: String?,
        val densityAnalysisResult: DensityAnalysisResult? = null,
        val fullScreenBlurDecision: FullScreenBlurDecision? = null,
        val recommendedAction: ContentAction = ContentAction.NO_ACTION,
        val requiresFullScreenWarning: Boolean = false,
        // Enhanced region-based information for full-screen blur triggering
        val nsfwRegionCount: Int = 0, // Number of NSFW regions detected
        val maxNsfwConfidence: Float = 0.0f, // Highest confidence among NSFW regions
        val nsfwRegionRects: List<Rect> = emptyList(), // Bounding boxes of NSFW regions
        val triggeredByRegionCount: Boolean = false // Whether full-screen was triggered by region count rule
    ) {
        companion object {
            fun failed(errorMessage: String) = ContentAnalysisResult(
                shouldBlur = false,
                blurRegions = emptyList(),
                faceDetectionResult = null,
                nsfwDetectionResult = null,
                processingTimeMs = 0L,
                success = false,
                error = errorMessage,
                densityAnalysisResult = null,
                fullScreenBlurDecision = null,
                recommendedAction = ContentAction.NO_ACTION,
                requiresFullScreenWarning = false,
                nsfwRegionCount = 0,
                maxNsfwConfidence = 0.0f,
                nsfwRegionRects = emptyList(),
                triggeredByRegionCount = false
            )
        }
        
        fun isSuccessful(): Boolean = success && error == null
    }
}