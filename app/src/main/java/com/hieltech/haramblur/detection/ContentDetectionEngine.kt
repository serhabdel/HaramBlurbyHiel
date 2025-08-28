package com.hieltech.haramblur.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.LogRepository.LogCategory
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
    private val fullScreenBlurTrigger: FullScreenBlurTrigger,
    private val logRepository: LogRepository
) {
    
    companion object {
        private const val TAG = "ContentDetectionEngine"
        private const val DETECTION_TIMEOUT_MS = 5000L
        private const val LOGGING_SAMPLE_RATE = 10 // Log detailed events every 10 detections
    }

    /**
     * Log detection event with structured information for analytics
     * Uses sampling to reduce database overhead
     */
    private suspend fun logDetectionEvent(
        result: ContentAnalysisResult,
        performanceMode: PerformanceMode? = null,
        appSettings: AppSettings? = null
    ) {
        // Implement sampling to reduce database overhead
        detectionCounter++
        val shouldLogDetailed = detectionCounter % LOGGING_SAMPLE_RATE == 0
        val shouldLogBasic = detectionCounter % (LOGGING_SAMPLE_RATE * 2) == 0 // Log basic stats every 20 detections

        try {
            // Always log essential detection data (faces/NSFW) for stats - no sampling
            val essentialLogMessage = buildString {
                append("DETECTION|")
                append("faces:${result.faceDetectionResult?.detectedFaces?.size ?: 0}|")
                append("nsfw:${result.nsfwDetectionResult?.isNSFW ?: false}|")
                append("processing_time:${result.processingTimeMs}ms|")
                append("success:${result.success}")
                append("performance_mode:${performanceMode ?: "unknown"}")
                if (result.error != null) {
                    append("|error:${result.error}")
                }
            }

            logRepository.logDebug(
                tag = "ContentDetectionEngine",
                message = essentialLogMessage,
                category = LogCategory.DETECTION,
                userAction = "content_detection"
            )

            // Log detailed detection event with sampling for additional debugging info
            if (shouldLogDetailed) {
                val detailedLogMessage = buildString {
                    append("DETECTION_DETAILED|")
                    append("faces:${result.faceDetectionResult?.detectedFaces?.size ?: 0}|")
                    append("nsfw:${result.nsfwDetectionResult?.isNSFW ?: false}|")
                    append("nsfw_confidence:${result.nsfwDetectionResult?.confidence ?: 0.0f}|")
                    append("processing_time:${result.processingTimeMs}ms|")
                    append("blur_regions:${result.blurRegions.size}|")
                    append("should_blur:${result.shouldBlur}|")
                    append("action:${result.recommendedAction}|")
                    append("regions:${result.nsfwRegionCount}|")
                    append("max_confidence:${result.maxNsfwConfidence}|")
                    append("performance_mode:${performanceMode ?: "unknown"}|")
                    append("success:${result.success}")
                    if (result.error != null) {
                        append("|error:${result.error}")
                    }
                }

                logRepository.logDebug(
                    tag = "ContentDetectionEngine",
                    message = detailedLogMessage,
                    category = LogCategory.DETECTION,
                    userAction = "detailed_detection"
                )
            }

            // Log performance metrics separately for better analytics (less frequently)
            if (shouldLogBasic && result.processingTimeMs > 0) {
                val performanceMessage = buildString {
                    append("PERFORMANCE|")
                    append("detection_time:${result.processingTimeMs}ms|")
                    append("mode:${performanceMode ?: "unknown"}|")
                    append("success:${result.success}|")
                    append("faces:${result.faceDetectionResult?.detectedFaces?.size ?: 0}|")
                    append("regions:${result.nsfwRegionCount}")
                }

                logRepository.logDebug(
                    tag = "ContentDetectionEngine",
                    message = performanceMessage,
                    category = LogCategory.PERFORMANCE,
                    userAction = "performance_measurement"
                )
            }
        } catch (e: Exception) {
            // Don't let logging failures crash the detection process
            Log.w(TAG, "Failed to log detection event", e)
        }
    }
    
    private var isInitialized = false
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var detectionCounter = 0 // Counter for sampling/throttling detection logging
    
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
            isInitialized = false
            false
        }
    }

    fun isEngineReady(): Boolean {
        return isInitialized && mlModelManager.isModelReady()
    }
    
    suspend fun analyzeContent(
        bitmap: Bitmap,
        appSettings: AppSettings
    ): ContentAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        return@withContext try {
            Log.d(TAG, "📸 Starting content analysis - Image: ${bitmap.width}x${bitmap.height}")

            // Check if bitmap is valid
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                Log.w(TAG, "Invalid bitmap provided for analysis")
                return@withContext ContentAnalysisResult(
                    shouldBlur = false,
                    blurRegions = emptyList(),
                    faceDetectionResult = null,
                    nsfwDetectionResult = null,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    success = false,
                    error = "Invalid bitmap",
                    recommendedAction = ContentAction.NO_ACTION
                )
            }

            // Use concurrent processing for better performance
            val faceDetectionDeferred = if (appSettings.enableFaceDetection && isEngineReady()) {
                async {
                    try {
                        Log.d(TAG, "👤 Starting face detection...")
                        val result = faceDetectionManager.detectFaces(bitmap, appSettings)
                        Log.d(TAG, "👤 Face detection completed - Found ${result.detectedFaces.size} faces")
                        result
                    } catch (e: Exception) {
                        Log.e(TAG, "Face detection failed", e)
                        FaceDetectionManager.FaceDetectionResult(0, emptyList(), false, e.message)
                    }
                }
            } else {
                async {
                    Log.d(TAG, "👤 Face detection disabled or not ready")
                    FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
                }
            }

            // NSFW detection (concurrent with face detection)
            val nsfwDetectionDeferred = if (appSettings.enableNSFWDetection && mlModelManager.isModelReady()) {
                async {
                    try {
                        Log.d(TAG, "🔞 Starting NSFW content detection...")
                        val result = mlModelManager.detectNSFWFast(bitmap)
                        Log.d(TAG, "🔞 NSFW detection completed")
                        result
                    } catch (e: Exception) {
                        Log.e(TAG, "NSFW detection failed", e)
                        MLModelManager.DetectionResult(false, 0.0f, "NSFW detection failed: ${e.message}")
                    }
                }
            } else {
                async {
                    Log.d(TAG, "🔞 NSFW detection disabled or not ready")
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

            val result = ContentAnalysisResult(
                shouldBlur = blurRegions.isNotEmpty(),
                blurRegions = blurRegions,
                faceDetectionResult = faceResult,
                nsfwDetectionResult = nsfwResult,
                processingTimeMs = processingTime,
                success = true,
                error = null
            )

            // Log detection event for analytics
            detectionScope.launch {
                logDetectionEvent(result, PerformanceMode.BALANCED, appSettings)
            }

            return@withContext result
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Content analysis failed after ${processingTime}ms", e)
            Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   • Error message: ${e.message}")

            val failedResult = ContentAnalysisResult(
                shouldBlur = false,
                blurRegions = emptyList(),
                faceDetectionResult = null,
                nsfwDetectionResult = null,
                processingTimeMs = processingTime,
                success = false,
                error = e.message
            )

            // Log failed detection event for analytics
            detectionScope.launch {
                logDetectionEvent(failedResult, PerformanceMode.BALANCED, appSettings)
            }

            return@withContext failedResult
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
        
        // Enhanced performance mode with GPU acceleration priority
        val performanceMode = when {
            appSettings.enableGPUAcceleration && appSettings.ultraFastModeEnabled -> PerformanceMode.ULTRA_FAST
            appSettings.enableGPUAcceleration -> PerformanceMode.FAST
            appSettings.ultraFastModeEnabled -> PerformanceMode.ULTRA_FAST
            appSettings.maxProcessingTimeMs <= 50L -> PerformanceMode.ULTRA_FAST
            appSettings.maxProcessingTimeMs <= 100L -> PerformanceMode.FAST
            else -> PerformanceMode.BALANCED
        }
        
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

            // Enhanced region-based full-screen blur triggering with ML model integration
            val regionBasedFullScreenTrigger = evaluateRegionBasedBlurTrigger(
                regionCount, maxRegionConfidence, regionRects, appSettings
            )

            // Perform quick density analysis if enabled and not in ultra-fast mode
            val densityAnalysisResult = if (appSettings.fullScreenWarningEnabled &&
                performanceMode != PerformanceMode.ULTRA_FAST) {
                contentDensityAnalyzer.analyzeScreenContent(bitmap)
            } else {
                null
            }

            // Check for full-screen blur decision with region-based information
            val fullScreenDecision = densityAnalysisResult?.let { densityResult ->
                fullScreenBlurTrigger.shouldTriggerFullScreenBlur(
                    densityResult, 
                    appSettings, 
                    regionCount, 
                    maxRegionConfidence
                )
            }

            // Use new graduated response system instead of traditional full-screen blur
            val finalShouldBlur: Boolean
            val finalBlurRegions: List<Rect>
            val requiresFullScreenWarning: Boolean
            val usesGraduatedResponse: Boolean
            
            when {
                // NEW: Region-based graduated response (6+ regions triggers actions, not blur)
                fullScreenDecision?.recommendedAction in listOf(
                    ContentAction.SCROLL_AWAY, 
                    ContentAction.NAVIGATE_BACK, 
                    ContentAction.AUTO_CLOSE_APP, 
                    ContentAction.GENTLE_REDIRECT
                ) -> {
                    finalShouldBlur = false  // Don't show blur - take action instead
                    finalBlurRegions = emptyList()
                    requiresFullScreenWarning = false
                    usesGraduatedResponse = true
                    Log.d(TAG, "🎯 Using graduated response: ${fullScreenDecision?.recommendedAction} for $regionCount regions")
                }
                
                // Traditional full-screen blur
                regionBasedFullScreenTrigger || fullScreenDecision?.shouldTrigger == true -> {
                    finalShouldBlur = true
                    finalBlurRegions = listOf(Rect(0, 0, bitmap.width, bitmap.height))
                    requiresFullScreenWarning = true
                    usesGraduatedResponse = false
                }
                
                // Selective blur
                else -> {
                    finalShouldBlur = fastResult.shouldBlur
                    finalBlurRegions = fastResult.blurRegions
                    requiresFullScreenWarning = false
                    usesGraduatedResponse = false
                }
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
                recommendedAction = fullScreenDecision?.recommendedAction ?: ContentAction.NO_ACTION,
                requiresFullScreenWarning = requiresFullScreenWarning,
                nsfwRegionCount = regionCount,
                maxNsfwConfidence = maxRegionConfidence,
                nsfwRegionRects = regionRects,
                triggeredByRegionCount = usesGraduatedResponse
            )
            
            Log.d(TAG, "Fast content analysis completed in ${processingTime}ms: shouldBlur=${fastResult.shouldBlur}")

            // Log detection event for analytics
            detectionScope.launch {
                logDetectionEvent(contentAnalysisResult, performanceMode, appSettings)
            }

            return@withContext contentAnalysisResult
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "Fast content analysis failed", e)

            frameOptimizationManager.updatePerformanceMetrics(
                processingTime,
                appSettings.maxProcessingTimeMs
            )

            val failedResult = ContentAnalysisResult.failed("Fast analysis failed: ${e.message}")

            // Log failed detection event for analytics
            detectionScope.launch {
                logDetectionEvent(failedResult, performanceMode, appSettings)
            }

            return@withContext failedResult
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

    /**
     * Enhanced region-based blur trigger evaluation with ML model integration
     * Provides sophisticated decision making based on multiple factors
     */
    private fun evaluateRegionBasedBlurTrigger(
        regionCount: Int,
        maxRegionConfidence: Float,
        regionRects: List<Rect>,
        settings: AppSettings
    ): Boolean {
        if (!settings.enableRegionBasedFullScreen) {
            return false
        }

        // Primary region-based trigger (6+ regions with high confidence)
        val basicRegionTrigger = regionCount >= settings.nsfwFullScreenRegionThreshold &&
                                maxRegionConfidence >= settings.nsfwHighConfidenceThreshold

        if (basicRegionTrigger) {
            Log.d(TAG, "🚨 PRIMARY: Region-based trigger - $regionCount regions, confidence: $maxRegionConfidence")
            return true
        }

        // Enhanced trigger: Based on region analysis patterns
        val mlModelTrigger = evaluateEnhancedRegionTrigger(regionCount, maxRegionConfidence)
        if (mlModelTrigger) {
            Log.d(TAG, "🚨 ENHANCED: Region pattern trigger activated")
            return true
        }

        // Spatial distribution trigger: Check if regions are concentrated in critical areas
        val spatialTrigger = evaluateSpatialDistributionTrigger(regionRects)
        if (spatialTrigger && regionCount >= 3) {
            Log.d(TAG, "🚨 SPATIAL: Region concentration trigger activated")
            return true
        }

        // Progressive trigger: Lower thresholds for persistent detection
        val progressiveTrigger = regionCount >= 4 && maxRegionConfidence >= 0.6f
        if (progressiveTrigger) {
            Log.d(TAG, "🚨 PROGRESSIVE: Lower threshold trigger - $regionCount regions, confidence: $maxRegionConfidence")
            return true
        }

        return false
    }

    /**
     * Evaluate enhanced region trigger based on region analysis patterns
     */
    private fun evaluateEnhancedRegionTrigger(
        regionCount: Int,
        maxRegionConfidence: Float
    ): Boolean {
        // High confidence with moderate region count
        if (maxRegionConfidence >= 0.85f && regionCount >= 4) {
            return true
        }

        // Very high region count with moderate confidence
        if (regionCount >= 8 && maxRegionConfidence >= 0.7f) {
            return true
        }

        // Medium confidence with high region density
        if (maxRegionConfidence >= 0.75f && regionCount >= 6) {
            return true
        }

        return false
    }

    /**
     * Evaluate spatial distribution of regions for critical area concentration
     */
    private fun evaluateSpatialDistributionTrigger(regionRects: List<Rect>): Boolean {
        if (regionRects.size < 3) return false

        // Calculate average region size to determine if regions are clustered
        val avgWidth = regionRects.map { it.width() }.average()
        val avgHeight = regionRects.map { it.height() }.average()

        // Check if regions are relatively close to each other (within 2x average size)
        var clusterCount = 0
        for (i in regionRects.indices) {
            for (j in i + 1 until regionRects.size) {
                val rect1 = regionRects[i]
                val rect2 = regionRects[j]

                // Check if rectangles are close (overlapping or within proximity)
                val proximityThreshold = (avgWidth + avgHeight) / 2 * 2
                val distance = Math.sqrt(
                    Math.pow((rect1.centerX() - rect2.centerX()).toDouble(), 2.0) +
                    Math.pow((rect1.centerY() - rect2.centerY()).toDouble(), 2.0)
                )

                if (distance <= proximityThreshold) {
                    clusterCount++
                }
            }
        }

        // If we have significant clustering (at least 3 pairs close together)
        return clusterCount >= 3
    }
    
    /**
     * Make LLM-enhanced decision for content actions
     * Delegates to the FullScreenBlurTrigger's LLM capabilities
     */
    suspend fun makeLLMEnhancedDecision(
        nsfwRegionCount: Int,
        maxNsfwConfidence: Float,
        settings: AppSettings,
        currentApp: String = "browser"
    ): com.hieltech.haramblur.llm.LLMDecisionResult {
        return fullScreenBlurTrigger.makeLLMEnhancedDecision(
            nsfwRegionCount, maxNsfwConfidence, settings, currentApp
        )
    }
    
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