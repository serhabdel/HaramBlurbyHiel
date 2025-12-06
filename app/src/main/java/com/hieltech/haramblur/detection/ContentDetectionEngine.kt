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
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.models.DetectionScope
import com.hieltech.haramblur.data.AppRegistry
import com.hieltech.haramblur.data.AppCategoryDetector
import com.hieltech.haramblur.data.AppFilteringManager
import com.hieltech.haramblur.detection.Gender
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.hieltech.haramblur.data.QualityMode
import com.hieltech.haramblur.data.ProcessingSpeed

@Singleton
class ContentDetectionEngine @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val faceDetectionManager: FaceDetectionManager,
    private val fastContentDetector: FastContentDetector,
    private val frameOptimizationManager: FrameOptimizationManager,
    private val performanceMonitor: PerformanceMonitor,
    private val contentDensityAnalyzer: ContentDensityAnalyzer,
    private val fullScreenBlurTrigger: FullScreenBlurTrigger,
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val appCategoryDetector: AppCategoryDetector,
    private val appFilteringManager: AppFilteringManager
) {
    
    companion object {
        private const val TAG = "ContentDetectionEngine"
        private const val DETECTION_TIMEOUT_MS = 5000L
        private const val LOGGING_SAMPLE_RATE = 10 // Log detailed events every 10 detections
        
        /**
         * Derive PerformanceMode from QualityMode settings
         */
        fun derivePerformanceMode(qualityMode: QualityMode): PerformanceMode {
            return when (qualityMode) {
                QualityMode.MAXIMUM_PRECISION -> PerformanceMode.QUALITY
                QualityMode.HIGH_QUALITY -> PerformanceMode.QUALITY
                QualityMode.BALANCED -> PerformanceMode.BALANCED
                QualityMode.BATTERY_SAVER -> PerformanceMode.FAST
            }
        }
    }

    /**
     * Determine the category of an app based on its package name
     */
    private fun determineAppCategory(packageName: String): AppCategory? {
        return appCategoryDetector.determineAppCategory(packageName)
    }

    /**
     * Check if an app is a browser based on package name and common patterns
     */
    private fun isBrowserApp(packageName: String): Boolean {
        return appCategoryDetector.isBrowserApp(packageName)
    }

    /**
     * Check if a specific app should be monitored for content detection
     */
    private suspend fun shouldProcessAppContent(packageName: String?, appSettings: AppSettings): Boolean {
        // Short-circuit: if app-specific detection is disabled, monitor all apps
        if (!appSettings.enableAppSpecificDetection) {
            return true
        }

        return appFilteringManager.shouldMonitorApp(packageName)
    }

    /**
     * Log detection event with structured information for analytics
     * Uses sampling to reduce database overhead
     */
    private suspend fun logDetectionEvent(
        result: ContentAnalysisResult,
        performanceMode: PerformanceMode? = null,
        appSettings: AppSettings? = null,
        currentAppPackage: String? = null
    ) {
        // Implement sampling to reduce database overhead
        detectionCounter++
        val shouldLogDetailed = detectionCounter % LOGGING_SAMPLE_RATE == 0
        val shouldLogBasic = detectionCounter % (LOGGING_SAMPLE_RATE * 2) == 0 // Log basic stats every 20 detections

        try {
            // Always log essential detection data (faces/NSFW) for stats - no sampling
            val appCategory = currentAppPackage?.let { determineAppCategory(it) }
            
            // Extract detailed face stats from detection result
            // Use allDetectedFaces for accurate stats (includes all faces before filtering)
            val faceResult = result.faceDetectionResult
            val totalFaces = faceResult?.allDetectedFaces?.size ?: 0
            val maleCount = faceResult?.allDetectedFaces?.count { it.estimatedGender == Gender.MALE } ?: 0
            val femaleCount = faceResult?.allDetectedFaces?.count { it.estimatedGender == Gender.FEMALE } ?: 0
            val unknownCount = faceResult?.allDetectedFaces?.count { it.estimatedGender == Gender.UNKNOWN } ?: 0
            // Use detectedFaces for blur count (faces selected for blurring after filtering)
            val blurredCount = faceResult?.detectedFaces?.size ?: 0
            val skippedCount = maxOf(0, totalFaces - blurredCount)
            
            val essentialLogMessage = buildString {
                append("DETECTION|")
                append("faces:$totalFaces|")
                append("male:$maleCount|")
                append("female:$femaleCount|")
                append("unknown:$unknownCount|")
                append("blurred:$blurredCount|")
                append("skipped:$skippedCount|")
                append("nsfw:${result.nsfwDetectionResult?.isNSFW ?: false}|")
                append("nsfw_confidence:${result.nsfwDetectionResult?.confidence ?: 0.0f}|")
                append("processing_time:${result.processingTimeMs}ms|")
                append("success:${result.success}|")
                append("performance_mode:${performanceMode ?: "unknown"}")
                if (currentAppPackage != null) {
                    append("|app:$currentAppPackage")
                    if (appCategory != null) {
                        append("|category:${appCategory.name.lowercase()}")
                    }
                }
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
        appSettings: AppSettings,
        currentAppPackage: String? = null
    ): ContentAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        return@withContext try {
            Log.d(TAG, "📸 Starting content analysis - Image: ${bitmap.width}x${bitmap.height}")

            // Check app filtering first - early exit if app should not be monitored
            if (!shouldProcessAppContent(currentAppPackage, appSettings)) {
                Log.d(TAG, "Content detection skipped for app: $currentAppPackage (not in monitored categories)")
                return@withContext ContentAnalysisResult(
                    shouldBlur = false,
                    blurRegions = emptyList(),
                    faceDetectionResult = null,
                    nsfwDetectionResult = null,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    success = true,
                    error = null,
                    recommendedAction = ContentAction.NO_ACTION
                )
            }

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
            val faceDetectionDeferred = if (appSettings.enableFaceDetection && isInitialized) {
                async {
                    try {
                        Log.d(TAG, "👤 Starting face detection...")
                        val result = faceDetectionManager.detectFaces(bitmap, appSettings)
                        Log.d(TAG, "👤 Face detection completed - Found ${result.detectedFaces.size} faces")
                        result
                    } catch (e: Exception) {
                        Log.e(TAG, "Face detection failed", e)
                        FaceDetectionManager.FaceDetectionResult(0, emptyList(), emptyList(), false, e.message)
                    }
                }
            } else {
                async {
                    Log.d(TAG, "👤 Face detection disabled or not ready")
                    FaceDetectionManager.FaceDetectionResult(0, emptyList(), emptyList(), true, null)
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
                appSettings,
                bitmap.width,
                bitmap.height,
                bitmap
            )
            
            Log.d(TAG, "🎯 Generated ${blurRegions.size} blur regions")
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ Content analysis completed in ${processingTime}ms")

            // Derive performance mode from settings
            val performanceMode = derivePerformanceMode(appSettings.qualityMode)
            
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
                logDetectionEvent(result, performanceMode, appSettings, currentAppPackage)
            }

            return@withContext result
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Content analysis failed after ${processingTime}ms", e)
            Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   • Error message: ${e.message}")

            // Derive performance mode from settings for error logging
            val performanceMode = derivePerformanceMode(appSettings.qualityMode)
            
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
                logDetectionEvent(failedResult, performanceMode, appSettings, currentAppPackage)
            }

            return@withContext failedResult
        }
    }
    
    /**
     * Fast content analysis using optimized detection pipeline
     */
    suspend fun analyzeContentFast(
        bitmap: Bitmap,
        appSettings: AppSettings,
        currentAppPackage: String? = null
    ): ContentAnalysisResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔍 Starting content analysis - Bitmap: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "⚙️ Settings - Female faces: ${appSettings.blurFemaleFaces}, Male faces: ${appSettings.blurMaleFaces}, NSFW: ${appSettings.enableNSFWDetection}, GPU: ${appSettings.enableGPUAcceleration}")

        // Check app filtering first - early exit if app should not be monitored
        if (!shouldProcessAppContent(currentAppPackage, appSettings)) {
            Log.d(TAG, "Content detection skipped for app: $currentAppPackage (not in monitored categories)")
            return@withContext ContentAnalysisResult(
                shouldBlur = false,
                blurRegions = emptyList(),
                faceDetectionResult = null,
                nsfwDetectionResult = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                success = true,
                error = null,
                recommendedAction = ContentAction.NO_ACTION
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
                logDetectionEvent(contentAnalysisResult, performanceMode, appSettings, currentAppPackage)
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
                logDetectionEvent(failedResult, performanceMode, appSettings, currentAppPackage)
            }

            return@withContext failedResult
        }
    }
    
    /**
     * Refine blur region using edge detection for precise boundaries
     * Now uses blurBoundaryPrecision setting to control threshold and expansion
     */
    private fun refineBlurRegionWithEdgeDetection(rect: Rect, bitmap: Bitmap, appSettings: AppSettings? = null): Rect {
        try {
            // Get precision setting - higher precision = lower threshold (more sensitive)
            val precision = appSettings?.blurBoundaryPrecision ?: 0.5f
            
            // Dynamic edge threshold based on precision setting
            // precision 1.0 -> threshold 0.15 (very sensitive)
            // precision 0.5 -> threshold 0.30 (balanced)
            // precision 0.3 -> threshold 0.40 (less sensitive)
            val edgeThreshold = 0.45f - (precision * 0.3f)
            
            // Dynamic expansion based on precision
            // Higher precision = smaller expansion for tighter boundaries
            val expansion = ((1.0f - precision) * 10 + 2).toInt().coerceIn(2, 10)
            
            Log.d(TAG, "Edge detection with precision=$precision, threshold=$edgeThreshold, expansion=$expansion")
            
            // Analyze border pixels for content boundaries
            val leftBoundary = findLeftContentBoundary(rect, bitmap, edgeThreshold)
            val rightBoundary = findRightContentBoundary(rect, bitmap, edgeThreshold)
            val topBoundary = findTopContentBoundary(rect, bitmap, edgeThreshold)
            val bottomBoundary = findBottomContentBoundary(rect, bitmap, edgeThreshold)
            
            // Create refined rectangle with precise boundaries
            val refinedRect = Rect(
                maxOf(0, leftBoundary - expansion),
                maxOf(0, topBoundary - expansion),
                minOf(bitmap.width, rightBoundary + expansion),
                minOf(bitmap.height, bottomBoundary + expansion)
            )
            
            Log.d(TAG, "Edge detection refined region: $rect -> $refinedRect (precision=$precision)")
            return refinedRect
        } catch (e: Exception) {
            Log.w(TAG, "Edge detection refinement failed, using original region", e)
            return rect
        }
    }
    
    /**
     * Find left content boundary using edge detection
     */
    private fun findLeftContentBoundary(rect: Rect, bitmap: Bitmap, threshold: Float): Int {
        var leftBoundary = rect.left
        for (x in rect.left until minOf(rect.right, bitmap.width - 1)) { // Iterate up to right-1 to avoid OOB
            if (x >= bitmap.width) break
            var edgeFound = false
            for (y in rect.top until minOf(rect.bottom, bitmap.height)) { // Clamp y to bitmap height
                if (y >= bitmap.height) break
                val pixel = bitmap.getPixel(x, y)
                val neighborPixel = if (x + 1 < bitmap.width) bitmap.getPixel(x + 1, y) else pixel // Defensive check
                val edgeStrength = calculateEdgeStrength(pixel, neighborPixel)
                if (edgeStrength > threshold) {
                    edgeFound = true
                    break
                }
            }
            if (edgeFound) {
                leftBoundary = x
                break
            }
        }
        return leftBoundary
    }
    
    /**
     * Find right content boundary using edge detection
     */
    private fun findRightContentBoundary(rect: Rect, bitmap: Bitmap, threshold: Float): Int {
        var rightBoundary = rect.right
        for (x in minOf(rect.right, bitmap.width - 1) downTo maxOf(rect.left, 1)) { // Clamp x to valid range
            if (x <= 0) break
            var edgeFound = false
            for (y in rect.top until minOf(rect.bottom, bitmap.height)) { // Clamp y to bitmap height
                if (y >= bitmap.height) break
                val pixel = bitmap.getPixel(x, y)
                val neighborPixel = if (x - 1 >= 0) bitmap.getPixel(x - 1, y) else pixel // Defensive check
                val edgeStrength = calculateEdgeStrength(pixel, neighborPixel)
                if (edgeStrength > threshold) {
                    edgeFound = true
                    break
                }
            }
            if (edgeFound) {
                rightBoundary = x
                break
            }
        }
        return rightBoundary
    }
    
    /**
     * Find top content boundary using edge detection
     */
    private fun findTopContentBoundary(rect: Rect, bitmap: Bitmap, threshold: Float): Int {
        var topBoundary = rect.top
        for (y in rect.top until minOf(rect.bottom, bitmap.height - 1)) { // Iterate up to bottom-1 to avoid OOB
            if (y >= bitmap.height) break
            var edgeFound = false
            for (x in rect.left until minOf(rect.right, bitmap.width)) { // Clamp x to bitmap width
                if (x >= bitmap.width) break
                val pixel = bitmap.getPixel(x, y)
                val neighborPixel = if (y + 1 < bitmap.height) bitmap.getPixel(x, y + 1) else pixel // Defensive check
                val edgeStrength = calculateEdgeStrength(pixel, neighborPixel)
                if (edgeStrength > threshold) {
                    edgeFound = true
                    break
                }
            }
            if (edgeFound) {
                topBoundary = y
                break
            }
        }
        return topBoundary
    }
    
    /**
     * Find bottom content boundary using edge detection
     */
    private fun findBottomContentBoundary(rect: Rect, bitmap: Bitmap, threshold: Float): Int {
        var bottomBoundary = rect.bottom
        for (y in minOf(rect.bottom, bitmap.height - 1) downTo maxOf(rect.top, 1)) { // Clamp y to valid range
            if (y <= 0) break
            var edgeFound = false
            for (x in rect.left until minOf(rect.right, bitmap.width)) { // Clamp x to bitmap width
                if (x >= bitmap.width) break
                val pixel = bitmap.getPixel(x, y)
                val neighborPixel = if (y - 1 >= 0) bitmap.getPixel(x, y - 1) else pixel // Defensive check
                val edgeStrength = calculateEdgeStrength(pixel, neighborPixel)
                if (edgeStrength > threshold) {
                    edgeFound = true
                    break
                }
            }
            if (edgeFound) {
                bottomBoundary = y
                break
            }
        }
        return bottomBoundary
    }
    
    /**
     * Calculate edge strength between two pixels
     */
    private fun calculateEdgeStrength(pixel1: Int, pixel2: Int): Float {
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF
        
        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF
        
        val diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
        return minOf(1.0f, diff / 255.0f)
    }
    
    private fun calculateBlurRegions(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        appSettings: AppSettings,
        bitmapWidth: Int,
        bitmapHeight: Int,
        bitmap: Bitmap? = null
    ): List<Rect> {
        val blurRegions = mutableListOf<Rect>()

        // Enhanced region-based full-screen blur trigger with lower thresholds
        if (appSettings.enableRegionBasedFullScreen &&
            nsfwResult.regionCount >= (appSettings.nsfwFullScreenRegionThreshold - 1) &&
            nsfwResult.maxRegionConfidence >= (appSettings.nsfwHighConfidenceThreshold - 0.05f)) {
            Log.d(TAG, "Region-based full-screen blur in standard mode: ${nsfwResult.regionCount} regions")
            return listOf(Rect(0, 0, bitmapWidth, bitmapHeight))
        }

        // Add female face regions with enhanced detection and edge refinement
        if (appSettings.enableFaceDetection && faceResult.hasFaces()) {
            val facesToBlur = mutableListOf<Rect>()

            // Focus on female faces only (male faces are already excluded by FaceDetectionManager)
            if (appSettings.blurFemaleFaces) {
                facesToBlur.addAll(faceResult.getFemaleFaces().map { it.boundingBox })

                // Enhanced: Include unknown gender faces with lower confidence for safety
                val unknownFaces = faceResult.getUnknownGenderFaces()
                    .filter { it.genderConfidence < 0.5f }
                    .map { it.boundingBox }
                facesToBlur.addAll(unknownFaces)
            }

            // Enhanced expansion for better coverage of female faces/hair with edge refinement
            val expandedFaceRegions = facesToBlur.map { face ->
                // Apply edge detection refinement before expansion using blurBoundaryPrecision
                val refinedFace = if (bitmap != null && appSettings.enableBlurEdgeRefinement) {
                    refineBlurRegionWithEdgeDetection(face, bitmap, appSettings)
                } else {
                    face
                }
                // SMART BLUR: Adaptive expansion with reduced top expansion for precision
                val adaptiveExpansion = calculateAdaptiveFaceExpansion(refinedFace, appSettings)
                Rect(
                    maxOf(0, refinedFace.left - adaptiveExpansion),
                    maxOf(0, refinedFace.top - (adaptiveExpansion * 1.3).toInt()), // Reduced from 1.8 to 1.3 for top
                    minOf(bitmapWidth, refinedFace.right + adaptiveExpansion),
                    minOf(bitmapHeight, refinedFace.bottom + (adaptiveExpansion * 0.8).toInt()) // Slightly less at bottom
                )
            }
            blurRegions.addAll(expandedFaceRegions)

            Log.d(TAG, "Added ${expandedFaceRegions.size} female face blur regions with edge refinement (males already excluded)")
        }
        
        // SMART TARGETED NSFW BLUR - Use detected regions instead of large screen areas
        if (appSettings.enableNSFWDetection && nsfwResult.isNSFW) {
            // Check if we have specific NSFW regions from the detection
            if (nsfwResult.regionCount > 0 && nsfwResult.regionRects.isNotEmpty()) {
                // TARGETED BLUR: Use actual detected regions instead of blanket screen blur
                val targetedRegions = nsfwResult.regionRects.mapNotNull { detectedRect ->
                    // Apply modest expansion to detected regions (not huge screen areas)
                    val expansion = when {
                        nsfwResult.confidence > 0.6f -> 40 // High confidence: moderate expansion
                        nsfwResult.confidence > 0.4f -> 30 // Medium confidence: small expansion
                        else -> 20 // Low confidence: minimal expansion
                    }
                    
                    val expandedRect = Rect(
                        maxOf(0, detectedRect.left - expansion),
                        maxOf(0, detectedRect.top - expansion),
                        minOf(bitmapWidth, detectedRect.right + expansion),
                        minOf(bitmapHeight, detectedRect.bottom + expansion)
                    )
                    
                    // Only include regions that are meaningful in size
                    if (expandedRect.width() > 30 && expandedRect.height() > 30) {
                        // Apply edge refinement to targeted region using blurBoundaryPrecision
                        if (bitmap != null && appSettings.enableBlurEdgeRefinement) {
                            refineBlurRegionWithEdgeDetection(expandedRect, bitmap, appSettings)
                        } else {
                            expandedRect
                        }
                    } else {
                        null
                    }
                }
                
                blurRegions.addAll(targetedRegions)
                Log.d(TAG, "Smart targeted blur: ${targetedRegions.size} NSFW regions with ${nsfwResult.confidence} confidence")
                
            } else {
                // FALLBACK: Only if no specific regions detected, use confidence-based areas
                when {
                    nsfwResult.confidence > 0.7f -> {
                        // Very high confidence - blur center 80% of screen
                        val margin = (bitmapWidth * 0.1f).toInt()
                        val topMargin = (bitmapHeight * 0.15f).toInt()
                        val bottomMargin = (bitmapHeight * 0.15f).toInt()
                        val centerBlur = Rect(margin, topMargin, bitmapWidth - margin, bitmapHeight - bottomMargin)
                        blurRegions.add(centerBlur)
                        Log.d(TAG, "Fallback: Center area blur for very high confidence: ${nsfwResult.confidence}")
                    }
                    nsfwResult.confidence > 0.5f -> {
                        // High confidence - blur center 60% of screen
                        val margin = (bitmapWidth * 0.2f).toInt()
                        val topMargin = (bitmapHeight * 0.2f).toInt()
                        val bottomMargin = (bitmapHeight * 0.2f).toInt()
                        val centerBlur = Rect(margin, topMargin, bitmapWidth - margin, bitmapHeight - bottomMargin)
                        blurRegions.add(centerBlur)
                        Log.d(TAG, "Fallback: Center area blur for high confidence: ${nsfwResult.confidence}")
                    }
                    // Lower confidence: Don't add any region (rely on face detection only)
                }
            }
        }
        
        // Apply edge-aware merging to final blur regions with precision settings
        return mergeOverlappingRegionsWithEdgeAwareness(blurRegions, bitmap, appSettings)
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
    
    /**
     * Enhanced region merging with edge awareness for better precision
     * @param regions List of regions to merge
     * @param bitmap Optional bitmap for edge refinement (can be null)
     * @param appSettings App settings for precision control
     */
    private fun mergeOverlappingRegionsWithEdgeAwareness(regions: List<Rect>, bitmap: Bitmap? = null, appSettings: AppSettings? = null): List<Rect> {
        if (regions.size <= 1) return regions
        
        val merged = mutableListOf<Rect>()
        val sorted = regions.sortedBy { it.left }
        
        var current = sorted[0]
        
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            
            // Use smaller overlap threshold for more precise region separation
            val overlapThreshold = 0.1f // 10% overlap required for merging
            val currentArea = current.width() * current.height().toFloat()
            val nextArea = next.width() * next.height().toFloat()
            
            if (Rect.intersects(current, next)) {
                val intersection = Rect()
                intersection.setIntersect(current, next)
                val intersectionArea = intersection.width() * intersection.height().toFloat()
                val overlapRatio = intersectionArea / minOf(currentArea, nextArea)
                
                if (overlapRatio > overlapThreshold) {
                    // Merge overlapping rectangles with edge awareness
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
            } else {
                merged.add(current)
                current = next
            }
        }
        
        merged.add(current)
        
        // Apply final edge refinement pass after merging using blurBoundaryPrecision
        return merged.map { region ->
            // Apply edge detection refinement to merged regions if bitmap is available
            // This provides more precise boundaries for the final blur regions
            if (bitmap != null && appSettings?.enableBlurEdgeRefinement == true) {
                refineBlurRegionWithEdgeDetection(region, bitmap, appSettings)
            } else {
                region
            }
        }
    }
    
    /**
     * Calculate adaptive face expansion based on face dimensions and settings
     */
    private fun calculateAdaptiveFaceExpansion(faceRect: Rect, appSettings: AppSettings): Int {
        val faceWidth = faceRect.width()
        val faceHeight = faceRect.height()
        val faceSize = maxOf(faceWidth, faceHeight)
        
        // SMART BLUR: Reduced expansion for more precise targeting
        // Base expansion from settings or calculate from face dimensions (reduced by ~40%)
        val baseExpansion = when {
            appSettings.expandBlurArea > 0 -> (appSettings.expandBlurArea * 0.6f).toInt() // Use 60% of setting
            faceSize >= 200 -> 35 // Larger faces: reduced from 60 to 35
            faceSize >= 150 -> 25 // Medium faces: reduced from 50 to 25
            faceSize >= 100 -> 20 // Small faces: reduced from 40 to 20
            else -> 15 // Very small faces: reduced from 30 to 15
        }
        
        // Scale with face dimensions for better coverage (reduced multiplier)
        val widthScale = faceWidth / 150f // Increased baseline from 100 to 150 (reduces expansion)
        val heightScale = faceHeight / 180f // Increased baseline from 120 to 180 (reduces expansion)
        val dimensionScale = (widthScale + heightScale) / 2
        
        // Apply scaling factor (clamp between 0.7 and 1.5)
        val scaledExpansion = (baseExpansion * dimensionScale.coerceIn(0.7f, 1.5f)).toInt()
        
        Log.d(TAG, "Adaptive face expansion: base=$baseExpansion, scale=$dimensionScale, final=$scaledExpansion for ${faceWidth}x${faceHeight} face")
        
        return scaledExpansion
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