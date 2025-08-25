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
        
        if (!isInitialized) {
            return@withContext ContentAnalysisResult.failed("Engine not initialized")
        }
        
        Log.d(TAG, "Starting content analysis on ${bitmap.width}x${bitmap.height} image")
        
        return@withContext try {
            // Check if we should use fast processing
            if (appSettings.ultraFastModeEnabled || appSettings.maxProcessingTimeMs <= 100L) {
                return@withContext analyzeContentFast(bitmap, appSettings)
            }
            
            withTimeout(DETECTION_TIMEOUT_MS) {
                val analysisJobs = mutableListOf<Deferred<*>>()
                
                // Face detection based on settings
                val faceDetectionJob = async {
                    if (appSettings.enableFaceDetection) {
                        faceDetectionManager.detectFaces(bitmap)
                    } else {
                        FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
                    }
                }
                analysisJobs.add(faceDetectionJob)
                
                // NSFW detection based on settings
                val nsfwDetectionJob = async {
                    if (appSettings.enableNSFWDetection && mlModelManager.isModelReady()) {
                        mlModelManager.detectNSFW(bitmap)
                    } else {
                        MLModelManager.DetectionResult(false, 0.0f, "NSFW detection disabled")
                    }
                }
                analysisJobs.add(nsfwDetectionJob)
                
                // Wait for all detection tasks to complete
                val faceResult = faceDetectionJob.await()
                val nsfwResult = nsfwDetectionJob.await()
                
                // Perform density analysis if enabled
                val densityAnalysisResult = if (appSettings.fullScreenWarningEnabled) {
                    contentDensityAnalyzer.analyzeScreenContent(bitmap)
                } else {
                    null
                }
                
                // Determine full-screen blur decision
                val fullScreenDecision = densityAnalysisResult?.let { densityResult ->
                    fullScreenBlurTrigger.shouldTriggerFullScreenBlur(densityResult, appSettings)
                }
                
                // Combine results based on app settings and density analysis
                val shouldBlur = determineBlurDecisionWithDensity(faceResult, nsfwResult, densityAnalysisResult, appSettings)
                val blurRegions = calculateBlurRegionsWithDensity(faceResult, nsfwResult, densityAnalysisResult, fullScreenDecision, bitmap, appSettings)
                
                // Determine recommended action
                val recommendedAction = densityAnalysisResult?.let { densityResult ->
                    fullScreenBlurTrigger.calculateRecommendedAction(densityResult, appSettings).action
                } ?: if (shouldBlur) ContentAction.SELECTIVE_BLUR else ContentAction.NO_ACTION
                
                val requiresFullScreenWarning = fullScreenDecision?.shouldTrigger == true
                
                val processingTime = System.currentTimeMillis() - System.currentTimeMillis() // Will be calculated properly
                
                Log.d(TAG, "Content analysis completed: shouldBlur=$shouldBlur, regions=${blurRegions.size}, fullScreen=$requiresFullScreenWarning, action=$recommendedAction")
                
                ContentAnalysisResult(
                    shouldBlur = shouldBlur,
                    blurRegions = blurRegions,
                    faceDetectionResult = faceResult,
                    nsfwDetectionResult = nsfwResult,
                    processingTimeMs = processingTime,
                    success = true,
                    error = null,
                    densityAnalysisResult = densityAnalysisResult,
                    fullScreenBlurDecision = fullScreenDecision,
                    recommendedAction = recommendedAction,
                    requiresFullScreenWarning = requiresFullScreenWarning
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Content analysis timed out")
            ContentAnalysisResult.failed("Analysis timed out")
        } catch (e: Exception) {
            Log.e(TAG, "Content analysis failed", e)
            ContentAnalysisResult.failed("Analysis failed: ${e.message}")
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
        
        return@withContext try {
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
            
            // Set performance mode based on settings
            val performanceMode = when {
                appSettings.ultraFastModeEnabled -> PerformanceMode.ULTRA_FAST
                appSettings.maxProcessingTimeMs <= 50L -> PerformanceMode.ULTRA_FAST
                appSettings.maxProcessingTimeMs <= 100L -> PerformanceMode.FAST
                else -> PerformanceMode.BALANCED
            }
            
            fastContentDetector.setPerformanceMode(performanceMode)
            mlModelManager.setPerformanceMode(performanceMode)
            
            // Perform fast detection
            val fastResult = fastContentDetector.detectContentFast(bitmap, appSettings)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Update performance metrics
            frameOptimizationManager.updatePerformanceMetrics(
                processingTime, 
                appSettings.maxProcessingTimeMs
            )
            
            // Perform quick density analysis if enabled and not in ultra-fast mode
            val densityAnalysisResult = if (appSettings.fullScreenWarningEnabled && 
                performanceMode != PerformanceMode.ULTRA_FAST) {
                contentDensityAnalyzer.analyzeScreenContent(bitmap)
            } else {
                null
            }
            
            // Check for full-screen blur decision
            val fullScreenDecision = densityAnalysisResult?.let { densityResult ->
                fullScreenBlurTrigger.shouldTriggerFullScreenBlur(densityResult, appSettings)
            }
            
            // Override blur decision if full-screen blur is required
            val finalShouldBlur = fullScreenDecision?.shouldTrigger == true || fastResult.shouldBlur
            val finalBlurRegions = if (fullScreenDecision?.shouldTrigger == true) {
                listOf(Rect(0, 0, bitmap.width, bitmap.height))
            } else {
                fastResult.blurRegions
            }
            
            // Determine recommended action
            val recommendedAction = densityAnalysisResult?.let { densityResult ->
                fullScreenBlurTrigger.calculateRecommendedAction(densityResult, appSettings).action
            } ?: if (finalShouldBlur) ContentAction.SELECTIVE_BLUR else ContentAction.NO_ACTION
            
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
                requiresFullScreenWarning = fullScreenDecision?.shouldTrigger == true
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
    
    private fun determineBlurDecisionWithDensity(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        densityResult: DensityAnalysisResult?,
        settings: AppSettings
    ): Boolean {
        // Check density analysis first if available
        densityResult?.let { density ->
            val fullScreenDecision = fullScreenBlurTrigger.shouldTriggerFullScreenBlur(density, settings)
            if (fullScreenDecision.shouldTrigger) {
                Log.d(TAG, "Blur decision: Full-screen blur triggered by density analysis")
                return true
            }
            
            // Check if density analysis recommends any blur action
            if (density.recommendedAction != ContentAction.NO_ACTION) {
                Log.d(TAG, "Blur decision: Density analysis recommends ${density.recommendedAction}")
                return true
            }
        }
        
        // Fall back to traditional blur decision logic
        return determineBlurDecision(faceResult, nsfwResult, settings)
    }
    
    private fun determineBlurDecision(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        settings: AppSettings
    ): Boolean {
        // Decision logic based on settings and detection results
        
        if (settings.enableNSFWDetection && nsfwResult.isNSFW && nsfwResult.confidence >= settings.detectionSensitivity) {
            Log.d(TAG, "Blur decision: NSFW content detected (confidence: ${nsfwResult.confidence}, threshold: ${settings.detectionSensitivity})")
            return true
        }
        
        if (settings.enableFaceDetection && faceResult.hasFaces()) {
            val maleFaces = faceResult.getMaleFaces()
            val femaleFaces = faceResult.getFemaleFaces()
            
            val shouldBlurMales = settings.blurMaleFaces && maleFaces.isNotEmpty()
            val shouldBlurFemales = settings.blurFemaleFaces && femaleFaces.isNotEmpty()
            
            if (shouldBlurMales || shouldBlurFemales) {
                Log.d(TAG, "Blur decision: Gender-specific faces detected (males: ${maleFaces.size}, females: ${femaleFaces.size})")
                return true
            }
        }
        
        return false
    }
    
    private fun calculateBlurRegionsWithDensity(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        densityResult: DensityAnalysisResult?,
        fullScreenDecision: FullScreenBlurDecision?,
        bitmap: Bitmap,
        settings: AppSettings
    ): List<Rect> {
        // Check if full-screen blur is required
        if (fullScreenDecision?.shouldTrigger == true) {
            Log.d(TAG, "Applying full-screen blur: ${fullScreenDecision.reason}")
            return listOf(Rect(0, 0, bitmap.width, bitmap.height))
        }
        
        // Check if density analysis provides critical regions
        densityResult?.let { density ->
            if (density.criticalRegions.isNotEmpty() && density.inappropriateContentDensity > 0.3f) {
                Log.d(TAG, "Using ${density.criticalRegions.size} critical regions from density analysis")
                
                // Combine critical regions with traditional blur regions
                val traditionalRegions = calculateBlurRegions(faceResult, nsfwResult, settings)
                val allRegions = mutableListOf<Rect>()
                allRegions.addAll(density.criticalRegions)
                allRegions.addAll(traditionalRegions)
                
                return mergeOverlappingRegions(allRegions)
            }
        }
        
        // Fall back to traditional blur region calculation
        return calculateBlurRegions(faceResult, nsfwResult, settings)
    }
    
    private fun calculateBlurRegions(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        settings: AppSettings
    ): List<Rect> {
        val regions = mutableListOf<Rect>()
        
        // Add face regions based on gender-specific settings
        if (settings.enableFaceDetection && faceResult.hasFaces()) {
            val facesToBlur = mutableListOf<Rect>()
            
            // Add male faces if enabled
            if (settings.blurMaleFaces) {
                facesToBlur.addAll(faceResult.getMaleFaces().map { it.boundingBox })
            }
            
            // Add female faces if enabled
            if (settings.blurFemaleFaces) {
                facesToBlur.addAll(faceResult.getFemaleFaces().map { it.boundingBox })
            }
            
            // Expand face rectangles slightly for better coverage
            val expandedFaceRegions = facesToBlur.map { face ->
                val expansion = 30 // pixels expansion for better coverage
                Rect(
                    maxOf(0, face.left - expansion),
                    maxOf(0, face.top - expansion),
                    face.right + expansion,
                    face.bottom + expansion
                )
            }
            regions.addAll(expandedFaceRegions)
            
            Log.d(TAG, "Added ${expandedFaceRegions.size} face blur regions (males: ${settings.blurMaleFaces}, females: ${settings.blurFemaleFaces})")
        }
        
        // If NSFW content is detected, blur significant portions or full screen
        if (settings.enableNSFWDetection && nsfwResult.isNSFW) {
            when {
                nsfwResult.confidence > 0.7f -> {
                    // Very high confidence - blur entire screen
                    regions.add(Rect(0, 0, 1080, 2400)) // Full screen blur
                    Log.d(TAG, "Full screen blur applied for high NSFW confidence: ${nsfwResult.confidence}")
                }
                nsfwResult.confidence > 0.5f -> {
                    // Medium confidence - blur center portion (likely content area)
                    val centerBlur = Rect(50, 200, 1030, 2200) // Most of screen except edges
                    regions.add(centerBlur)
                    Log.d(TAG, "Center screen blur applied for medium NSFW confidence: ${nsfwResult.confidence}")
                }
                nsfwResult.confidence > 0.3f -> {
                    // Lower confidence - blur middle section where inappropriate content likely appears
                    val middleBlur = Rect(100, 400, 980, 2000)
                    regions.add(middleBlur)
                    Log.d(TAG, "Middle screen blur applied for moderate NSFW confidence: ${nsfwResult.confidence}")
                }
            }
        }
        
        return regions
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
        val requiresFullScreenWarning: Boolean = false
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
                requiresFullScreenWarning = false
            )
        }
        
        fun isSuccessful(): Boolean = success && error == null
    }
}