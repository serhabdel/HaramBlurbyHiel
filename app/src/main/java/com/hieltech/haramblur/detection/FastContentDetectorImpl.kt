package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ContentDensityMetrics
import com.hieltech.haramblur.data.DetectionPerformanceMetrics
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.ml.FaceDetectionManager
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastContentDetectorImpl @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val faceDetectionManager: FaceDetectionManager
) : FastContentDetector {
    
    companion object {
        private const val TAG = "FastContentDetector"
        private const val GRID_SIZE = 4 // 4x4 grid for density analysis
        private const val CACHE_EXPIRATION_MS = 2000L // 2 seconds cache
        private const val MAX_CACHE_SIZE = 50
        private const val SKIN_TONE_THRESHOLD = 0.3f
        private const val FULL_SCREEN_DENSITY_THRESHOLD = 0.4f
    }
    
    private var currentPerformanceMode = PerformanceMode.BALANCED
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Performance tracking
    private val totalProcessingTime = AtomicLong(0)
    private val totalDetections = AtomicLong(0)
    private val gpuAccelerationEnabled = AtomicLong(0)
    private val framesSkipped = AtomicLong(0)
    private val qualityReductions = AtomicLong(0)
    
    // Caching for performance
    private val detectionCache = ConcurrentHashMap<Int, CachedDetectionResult>()
    private val densityCache = ConcurrentHashMap<Int, CachedDensityResult>()
    
    // Frame skipping logic
    private var lastProcessedFrame = 0L
    private var consecutiveFrames = 0
    
    override suspend fun detectContentFast(bitmap: Bitmap, settings: AppSettings): FastDetectionResult = 
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            
            try {
                // Check if we should skip this frame for performance
                if (shouldSkipFrame()) {
                    framesSkipped.incrementAndGet()
                    return@withContext createSkippedFrameResult(startTime)
                }
                
                // Check cache first
                val bitmapHash = calculateBitmapHash(bitmap)
                detectionCache[bitmapHash]?.let { cached ->
                    if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRATION_MS) {
                        Log.d(TAG, "Using cached detection result")
                        return@withContext cached.result.copy(
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
                
                // Determine if we need to reduce quality for performance
                val shouldReduceQuality = shouldReduceQuality(settings)
                val processedBitmap = if (shouldReduceQuality) {
                    downscaleBitmap(bitmap, currentPerformanceMode.imageDownscaleRatio)
                } else {
                    bitmap
                }
                
                // Perform multi-threaded detection
                val detectionJobs = mutableListOf<Deferred<*>>()
                
                // Face detection job
                val faceDetectionJob = async {
                    if (settings.enableFaceDetection) {
                        faceDetectionManager.detectFaces(processedBitmap)
                    } else {
                        FaceDetectionManager.FaceDetectionResult(0, emptyList(), true, null)
                    }
                }
                detectionJobs.add(faceDetectionJob)
                
                // NSFW detection job
                val nsfwDetectionJob = async {
                    if (settings.enableNSFWDetection) {
                        mlModelManager.detectNSFW(processedBitmap)
                    } else {
                        MLModelManager.DetectionResult(false, 0.0f, "NSFW detection disabled")
                    }
                }
                detectionJobs.add(nsfwDetectionJob)
                
                // Fast skin tone analysis job
                val skinToneJob = async {
                    analyzeSkinToneDistributionFast(processedBitmap)
                }
                detectionJobs.add(skinToneJob)
                
                // Wait for all jobs with timeout
                val timeout = currentPerformanceMode.maxProcessingTimeMs
                val results = withTimeoutOrNull(timeout) {
                    val faceResult = faceDetectionJob.await()
                    val nsfwResult = nsfwDetectionJob.await()
                    val skinToneResult = skinToneJob.await()
                    Triple(faceResult, nsfwResult, skinToneResult)
                }
                
                if (results == null) {
                    Log.w(TAG, "Detection timed out after ${timeout}ms")
                    return@withContext createTimeoutResult(startTime)
                }
                
                val (faceResult, nsfwResult, skinToneResult) = results
                
                // Perform fast region detection for enhanced full-screen triggering
                val regionAnalysis = performFastRegionDetection(processedBitmap, settings, currentPerformanceMode)

                // Combine results and make blur decision
                val shouldBlur = determineBlurDecisionFast(faceResult, nsfwResult, skinToneResult, settings)
                val blurRegions = calculateBlurRegionsFast(faceResult, nsfwResult, processedBitmap, settings, regionAnalysis)
                val contentType = determineContentType(faceResult, nsfwResult, skinToneResult)
                val confidence = calculateOverallConfidence(faceResult, nsfwResult, skinToneResult)
                
                val processingTime = System.currentTimeMillis() - startTime
                totalProcessingTime.addAndGet(processingTime)
                totalDetections.incrementAndGet()
                
                val performanceMetrics = createPerformanceMetrics(
                    processingTime,
                    shouldReduceQuality,
                    false,
                    currentPerformanceMode.enableGPU
                )
                
                val result = FastDetectionResult(
                    shouldBlur = shouldBlur,
                    blurRegions = blurRegions,
                    contentType = contentType,
                    processingTimeMs = processingTime,
                    confidenceScore = confidence,
                    performanceMetrics = performanceMetrics,
                    qualityReduced = shouldReduceQuality,
                    frameSkipped = false,
                    nsfwRegionCount = regionAnalysis.regionCount,
                    maxNsfwConfidence = regionAnalysis.maxConfidence,
                    nsfwRegionRects = regionAnalysis.regionRects
                )
                
                // Cache the result
                cacheDetectionResult(bitmapHash, result)
                
                Log.d(TAG, "Fast detection completed in ${processingTime}ms: shouldBlur=$shouldBlur, type=$contentType")
                return@withContext result
                
            } catch (e: Exception) {
                Log.e(TAG, "Fast content detection failed", e)
                return@withContext createErrorResult(startTime, e)
            }
        }
    
    override suspend fun analyzeContentDensity(bitmap: Bitmap): ContentDensityResult = 
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            
            try {
                // Check cache first
                val bitmapHash = calculateBitmapHash(bitmap)
                densityCache[bitmapHash]?.let { cached ->
                    if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRATION_MS) {
                        Log.d(TAG, "Using cached density result")
                        return@withContext cached.result.copy(
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
                
                // Downscale for faster processing
                val scaledBitmap = downscaleBitmap(bitmap, 0.5f)
                
                // Create grid analysis
                val distributionMap = Array(GRID_SIZE) { Array(GRID_SIZE) { 0.0f } }
                val criticalRegions = mutableListOf<Rect>()
                
                val cellWidth = scaledBitmap.width / GRID_SIZE
                val cellHeight = scaledBitmap.height / GRID_SIZE
                
                var totalInappropriateContent = 0.0f
                var totalCells = 0
                
                // Analyze each grid cell
                for (row in 0 until GRID_SIZE) {
                    for (col in 0 until GRID_SIZE) {
                        val x = col * cellWidth
                        val y = row * cellHeight
                        val cellRect = Rect(x, y, x + cellWidth, y + cellHeight)
                        
                        // Extract cell bitmap
                        val cellBitmap = Bitmap.createBitmap(
                            scaledBitmap, 
                            x, 
                            y, 
                            minOf(cellWidth, scaledBitmap.width - x),
                            minOf(cellHeight, scaledBitmap.height - y)
                        )
                        
                        // Analyze cell content
                        val cellDensity = analyzeCellContent(cellBitmap)
                        distributionMap[row][col] = cellDensity
                        totalInappropriateContent += cellDensity
                        totalCells++
                        
                        // Mark as critical region if density is high
                        if (cellDensity > 0.6f) {
                            // Scale back to original bitmap coordinates
                            val originalRect = Rect(
                                (x * bitmap.width) / scaledBitmap.width,
                                (y * bitmap.height) / scaledBitmap.height,
                                ((x + cellWidth) * bitmap.width) / scaledBitmap.width,
                                ((y + cellHeight) * bitmap.height) / scaledBitmap.height
                            )
                            criticalRegions.add(originalRect)
                        }
                        
                        cellBitmap.recycle()
                    }
                }
                
                val averageDensity = totalInappropriateContent / totalCells
                val recommendsFullScreen = averageDensity > FULL_SCREEN_DENSITY_THRESHOLD
                
                val densityMetrics = ContentDensityMetrics(
                    inappropriateContentPercentage = averageDensity,
                    spatialDistribution = createSpatialDistributionMap(distributionMap),
                    recommendsFullScreenBlur = recommendsFullScreen,
                    criticalRegionCount = criticalRegions.size,
                    warningLevel = calculateWarningLevel(averageDensity, criticalRegions.size)
                )
                
                val processingTime = System.currentTimeMillis() - startTime
                
                val result = ContentDensityResult(
                    inappropriateContentPercentage = averageDensity,
                    distributionMap = distributionMap,
                    recommendsFullScreenBlur = recommendsFullScreen,
                    criticalRegions = criticalRegions,
                    densityMetrics = densityMetrics,
                    processingTimeMs = processingTime
                )
                
                // Cache the result
                cacheDensityResult(bitmapHash, result)
                
                Log.d(TAG, "Density analysis completed in ${processingTime}ms: density=${averageDensity}, fullScreen=$recommendsFullScreen")
                return@withContext result
                
            } catch (e: Exception) {
                Log.e(TAG, "Content density analysis failed", e)
                return@withContext createErrorDensityResult(startTime, e)
            }
        }
    
    override fun setPerformanceMode(mode: PerformanceMode) {
        currentPerformanceMode = mode
        Log.d(TAG, "Performance mode set to: ${mode.displayName}")
        
        // Clear caches when performance mode changes
        clearCaches()
    }
    
    override fun getPerformanceStats(): DetectionPerformanceMetrics {
        val totalDetectionsCount = totalDetections.get()
        val avgProcessingTime = if (totalDetectionsCount > 0) {
            totalProcessingTime.get() / totalDetectionsCount
        } else {
            0L
        }
        
        return DetectionPerformanceMetrics(
            processingTimeMs = avgProcessingTime,
            memoryUsageMB = getMemoryUsage(),
            cpuUsagePercent = getCPUUsage(),
            gpuAccelerationUsed = gpuAccelerationEnabled.get() > 0,
            frameSkipped = framesSkipped.get() > 0,
            qualityReduced = qualityReductions.get() > 0
        )
    }
    
    override fun clearCaches() {
        detectionCache.clear()
        densityCache.clear()
        Log.d(TAG, "Caches cleared")
    }
    
    override fun isGPUAccelerationEnabled(): Boolean {
        return currentPerformanceMode.enableGPU && mlModelManager.isModelReady()
    }
    
    // Private helper methods
    
    private fun shouldSkipFrame(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastProcessedFrame
        
        consecutiveFrames++
        
        val shouldSkip = consecutiveFrames > currentPerformanceMode.frameSkipThreshold &&
                timeSinceLastFrame < 100L // Skip if processing too frequently
        
        if (!shouldSkip) {
            lastProcessedFrame = currentTime
            consecutiveFrames = 0
        }
        
        return shouldSkip
    }
    
    private fun shouldReduceQuality(settings: AppSettings): Boolean {
        val avgProcessingTime = if (totalDetections.get() > 0) {
            totalProcessingTime.get() / totalDetections.get()
        } else {
            0L
        }
        
        return avgProcessingTime > currentPerformanceMode.maxProcessingTimeMs ||
                settings.ultraFastModeEnabled
    }
    
    private fun downscaleBitmap(bitmap: Bitmap, ratio: Float): Bitmap {
        if (ratio >= 1.0f) return bitmap
        
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun calculateBitmapHash(bitmap: Bitmap): Int {
        return try {
            // Check if bitmap is valid
            if (bitmap.isRecycled) {
                Log.w(TAG, "Bitmap is recycled, using fallback hash")
                return bitmap.hashCode()
            }
            
            // Simple hash based on bitmap properties and sample pixels
            var hash = bitmap.width * 31 + bitmap.height
            
            // Sample a few pixels for content-based hashing with bounds checking
            val samplePoints = listOf(
                Pair(bitmap.width / 4, bitmap.height / 4),
                Pair(bitmap.width / 2, bitmap.height / 2),
                Pair(3 * bitmap.width / 4, 3 * bitmap.height / 4)
            )
            
            samplePoints.forEach { (x, y) ->
                try {
                    // Double-check bounds and bitmap validity before accessing pixels
                    if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height && !bitmap.isRecycled) {
                        hash = hash * 31 + bitmap.getPixel(x, y)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error accessing pixel at ($x, $y): ${e.message}")
                }
            }
            
            hash
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating bitmap hash: ${e.message}")
            bitmap.hashCode() // Fallback to object hash
        }
    }    

    private suspend fun analyzeSkinToneDistributionFast(bitmap: Bitmap): Float = withContext(Dispatchers.Default) {
        try {
            // Check if bitmap is valid
            if (bitmap.isRecycled) {
                Log.w(TAG, "Bitmap is recycled in skin tone analysis")
                return@withContext 0.0f
            }
            
            val sampleSize = 20 // Reduced sample size for speed
            val step = maxOf(bitmap.width / sampleSize, bitmap.height / sampleSize, 1)
            
            var skinPixelCount = 0
            var totalPixels = 0
            
            for (x in 0 until bitmap.width step step) {
                for (y in 0 until bitmap.height step step) {
                    try {
                        if (!bitmap.isRecycled && x < bitmap.width && y < bitmap.height) {
                            val pixel = bitmap.getPixel(x, y)
                            if (isSkinTonePixelFast(pixel)) {
                                skinPixelCount++
                            }
                            totalPixels++
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error accessing pixel at ($x, $y) in skin tone analysis: ${e.message}")
                    }
                }
            }
            
            return@withContext if (totalPixels > 0) {
                skinPixelCount.toFloat() / totalPixels
            } else {
                0.0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in skin tone analysis: ${e.message}")
            return@withContext 0.0f
        }
    }
    
    private fun isSkinTonePixelFast(pixel: Int): Boolean {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Simplified skin tone detection for speed
        return red in 120..255 && green in 80..200 && blue in 60..150 &&
                red > green && green > blue
    }
    
    private fun determineBlurDecisionFast(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        skinToneRatio: Float,
        settings: AppSettings
    ): Boolean {
        // Fast blur decision logic
        
        // NSFW content check
        if (settings.enableNSFWDetection && nsfwResult.isNSFW && 
            nsfwResult.confidence >= settings.nsfwConfidenceThreshold) {
            return true
        }
        
        // Face detection check
        if (settings.enableFaceDetection && faceResult.hasFaces()) {
            return true // Simplified - actual gender filtering will be done in blur regions
        }
        
        // High skin tone ratio check
        if (skinToneRatio > SKIN_TONE_THRESHOLD) {
            return true
        }
        
        return false
    }
    
    private fun calculateBlurRegionsFast(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        bitmap: Bitmap,
        settings: AppSettings,
        regionAnalysis: FastRegionAnalysis
    ): List<Rect> {
        val regions = mutableListOf<Rect>()

        // Check for region-based full-screen blur trigger (6+ high-confidence regions)
        if (settings.enableRegionBasedFullScreen &&
            regionAnalysis.regionCount >= settings.nsfwFullScreenRegionThreshold &&
            regionAnalysis.maxConfidence >= settings.nsfwHighConfidenceThreshold) {
            // Trigger full-screen blur due to high region count
            Log.d(TAG, "Region-based full-screen blur triggered: ${regionAnalysis.regionCount} regions with max confidence ${regionAnalysis.maxConfidence}")
            return listOf(Rect(0, 0, bitmap.width, bitmap.height))
        }

        // Add face regions (simplified - no gender filtering for speed)
        if (settings.enableFaceDetection && faceResult.hasFaces()) {
            faceResult.detectedFaces.forEach { face ->
                val expandedRect = expandRect(face.boundingBox, 20)
                regions.add(expandedRect)
            }
        }
        
        // Add NSFW regions based on confidence
        if (settings.enableNSFWDetection && nsfwResult.isNSFW) {
            when {
                nsfwResult.confidence > 0.8f -> {
                    // Full screen blur for high confidence
                    regions.add(Rect(0, 0, bitmap.width, bitmap.height))
                }
                nsfwResult.confidence > 0.6f -> {
                    // Center region blur
                    val margin = bitmap.width / 10
                    regions.add(Rect(margin, margin, bitmap.width - margin, bitmap.height - margin))
                }
                nsfwResult.confidence > 0.4f -> {
                    // Middle section blur
                    val marginX = bitmap.width / 8
                    val marginY = bitmap.height / 8
                    regions.add(Rect(marginX, marginY, bitmap.width - marginX, bitmap.height - marginY))
                }
            }
        }
        
        return regions
    }
    
    private fun expandRect(rect: Rect, expansion: Int): Rect {
        return Rect(
            maxOf(0, rect.left - expansion),
            maxOf(0, rect.top - expansion),
            rect.right + expansion,
            rect.bottom + expansion
        )
    }
    
    private fun determineContentType(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        skinToneRatio: Float
    ): ContentType {
        return when {
            nsfwResult.isNSFW && nsfwResult.confidence > 0.7f -> ContentType.NSFW_CONTENT
            faceResult.hasFaces() && skinToneRatio > SKIN_TONE_THRESHOLD -> ContentType.MIXED_CONTENT
            faceResult.hasFaces() -> ContentType.FACES_DETECTED
            skinToneRatio > SKIN_TONE_THRESHOLD -> ContentType.HIGH_SKIN_TONE
            nsfwResult.isNSFW -> ContentType.NSFW_CONTENT
            else -> ContentType.SAFE
        }
    }
    
    private fun calculateOverallConfidence(
        faceResult: FaceDetectionManager.FaceDetectionResult,
        nsfwResult: MLModelManager.DetectionResult,
        skinToneRatio: Float
    ): Float {
        val confidences = mutableListOf<Float>()
        
        if (faceResult.hasFaces()) {
            confidences.add(0.8f) // Face detection is generally reliable
        }
        
        if (nsfwResult.isNSFW) {
            confidences.add(nsfwResult.confidence)
        }
        
        if (skinToneRatio > SKIN_TONE_THRESHOLD) {
            confidences.add(skinToneRatio)
        }
        
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0.0f
        }
    }
    
    private fun analyzeCellContent(cellBitmap: Bitmap): Float {
        // Fast cell content analysis
        val skinToneRatio = analyzeSkinToneInCell(cellBitmap)
        val colorVariance = analyzeColorVarianceInCell(cellBitmap)
        
        // Combine metrics for overall inappropriateness score
        var score = 0.0f
        
        // High skin tone ratio increases score
        if (skinToneRatio > 0.3f) score += skinToneRatio * 0.6f
        
        // Low color variance (smooth areas) might indicate skin
        if (colorVariance < 0.3f && skinToneRatio > 0.2f) score += 0.3f
        
        return minOf(1.0f, score)
    }
    
    private fun analyzeSkinToneInCell(cellBitmap: Bitmap): Float {
        var skinPixels = 0
        var totalPixels = 0
        val step = 2 // Sample every 2nd pixel for speed
        
        for (x in 0 until cellBitmap.width step step) {
            for (y in 0 until cellBitmap.height step step) {
                val pixel = cellBitmap.getPixel(x, y)
                if (isSkinTonePixelFast(pixel)) {
                    skinPixels++
                }
                totalPixels++
            }
        }
        
        return if (totalPixels > 0) skinPixels.toFloat() / totalPixels else 0.0f
    }
    
    private fun analyzeColorVarianceInCell(cellBitmap: Bitmap): Float {
        val colors = mutableListOf<Int>()
        val step = 3 // Sample every 3rd pixel for speed
        
        for (x in 0 until cellBitmap.width step step) {
            for (y in 0 until cellBitmap.height step step) {
                colors.add(cellBitmap.getPixel(x, y))
            }
        }
        
        if (colors.size < 2) return 0.0f
        
        // Calculate simple color variance
        val avgRed = colors.map { (it shr 16) and 0xFF }.average()
        val avgGreen = colors.map { (it shr 8) and 0xFF }.average()
        val avgBlue = colors.map { it and 0xFF }.average()
        
        val variance = colors.map { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            
            val redDiff = red - avgRed
            val greenDiff = green - avgGreen
            val blueDiff = blue - avgBlue
            
            redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff
        }.average()
        
        return (variance / 65536.0).toFloat() // Normalize to 0-1 range
    }
    
    private fun createSpatialDistributionMap(distributionMap: Array<Array<Float>>): Map<String, Float> {
        val quadrants = mapOf(
            "top_left" to Pair(0..1, 0..1),
            "top_right" to Pair(0..1, 2..3),
            "bottom_left" to Pair(2..3, 0..1),
            "bottom_right" to Pair(2..3, 2..3)
        )
        
        return quadrants.mapValues { (_, ranges) ->
            val (rowRange, colRange) = ranges
            var sum = 0.0f
            var count = 0
            
            for (row in rowRange) {
                for (col in colRange) {
                    sum += distributionMap[row][col]
                    count++
                }
            }
            
            if (count > 0) sum / count else 0.0f
        }
    }
    
    private fun calculateWarningLevel(density: Float, criticalRegions: Int): Int {
        return when {
            density > 0.8f || criticalRegions > 12 -> 5 // Maximum warning
            density > 0.6f || criticalRegions > 8 -> 4  // High warning
            density > 0.4f || criticalRegions > 4 -> 3  // Medium warning
            density > 0.2f || criticalRegions > 2 -> 2  // Low warning
            density > 0.1f || criticalRegions > 0 -> 1  // Minimal warning
            else -> 0 // No warning
        }
    }
    
    private fun createPerformanceMetrics(
        processingTime: Long,
        qualityReduced: Boolean,
        frameSkipped: Boolean,
        gpuUsed: Boolean
    ): DetectionPerformanceMetrics {
        if (qualityReduced) qualityReductions.incrementAndGet()
        if (gpuUsed) gpuAccelerationEnabled.incrementAndGet()
        
        return DetectionPerformanceMetrics(
            processingTimeMs = processingTime,
            memoryUsageMB = getMemoryUsage(),
            cpuUsagePercent = getCPUUsage(),
            gpuAccelerationUsed = gpuUsed,
            frameSkipped = frameSkipped,
            qualityReduced = qualityReduced
        )
    }
    
    private fun getMemoryUsage(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f) // Convert to MB
    }
    
    private fun getCPUUsage(): Float {
        // Simplified CPU usage estimation
        return minOf(100.0f, totalDetections.get().toFloat() / 10.0f)
    }
    
    private fun cacheDetectionResult(hash: Int, result: FastDetectionResult) {
        if (detectionCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries
            val oldestKey = detectionCache.keys.minByOrNull { 
                detectionCache[it]?.timestamp ?: Long.MAX_VALUE 
            }
            oldestKey?.let { detectionCache.remove(it) }
        }
        
        detectionCache[hash] = CachedDetectionResult(result, System.currentTimeMillis())
    }
    
    private fun cacheDensityResult(hash: Int, result: ContentDensityResult) {
        if (densityCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries
            val oldestKey = densityCache.keys.minByOrNull { 
                densityCache[it]?.timestamp ?: Long.MAX_VALUE 
            }
            oldestKey?.let { densityCache.remove(it) }
        }
        
        densityCache[hash] = CachedDensityResult(result, System.currentTimeMillis())
    }
    
    private fun createSkippedFrameResult(startTime: Long): FastDetectionResult {
        return FastDetectionResult(
            shouldBlur = false,
            blurRegions = emptyList(),
            contentType = ContentType.SAFE,
            processingTimeMs = System.currentTimeMillis() - startTime,
            confidenceScore = 0.0f,
            performanceMetrics = createPerformanceMetrics(0L, false, true, false),
            qualityReduced = false,
            frameSkipped = true
        )
    }
    
    private fun createTimeoutResult(startTime: Long): FastDetectionResult {
        return FastDetectionResult(
            shouldBlur = true, // Default to safe side
            blurRegions = emptyList(),
            contentType = ContentType.UNKNOWN,
            processingTimeMs = System.currentTimeMillis() - startTime,
            confidenceScore = 0.0f,
            performanceMetrics = createPerformanceMetrics(currentPerformanceMode.maxProcessingTimeMs, true, false, false),
            qualityReduced = true,
            frameSkipped = false
        )
    }
    
    private fun createErrorResult(startTime: Long, error: Exception): FastDetectionResult {
        Log.e(TAG, "Creating error result for exception", error)
        return FastDetectionResult(
            shouldBlur = true, // Default to safe side
            blurRegions = emptyList(),
            contentType = ContentType.UNKNOWN,
            processingTimeMs = System.currentTimeMillis() - startTime,
            confidenceScore = 0.0f,
            performanceMetrics = createPerformanceMetrics(0L, false, false, false),
            qualityReduced = false,
            frameSkipped = false
        )
    }
    
    private fun createErrorDensityResult(startTime: Long, error: Exception): ContentDensityResult {
        Log.e(TAG, "Creating error density result for exception", error)
        return ContentDensityResult(
            inappropriateContentPercentage = 0.0f,
            distributionMap = Array(GRID_SIZE) { Array(GRID_SIZE) { 0.0f } },
            recommendsFullScreenBlur = false,
            criticalRegions = emptyList(),
            densityMetrics = ContentDensityMetrics(
                inappropriateContentPercentage = 0.0f,
                spatialDistribution = emptyMap(),
                recommendsFullScreenBlur = false,
                criticalRegionCount = 0,
                warningLevel = 0
            ),
            processingTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Perform fast region detection for enhanced full-screen blur triggering
     * Uses simplified grid-based approach optimized for performance
     */
    private suspend fun performFastRegionDetection(
        bitmap: Bitmap,
        settings: AppSettings,
        performanceMode: PerformanceMode
    ): FastRegionAnalysis = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Determine grid size based on performance mode and bitmap dimensions
        val gridSize = when (performanceMode) {
            PerformanceMode.ULTRA_FAST -> 2 // 2x2 grid for maximum speed
            PerformanceMode.FAST -> 3        // 3x3 grid for good speed/accuracy balance
            else -> 4                         // 4x4 grid for better accuracy
        }

        val cellWidth = width / gridSize
        val cellHeight = height / gridSize

        val highConfidenceRegions = mutableListOf<Rect>()
        val regionConfidences = mutableListOf<Float>()
        var maxConfidence = 0.0f

        // Analyze each grid cell
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = col * cellWidth
                val y = row * cellHeight
                val cellRect = Rect(
                    x,
                    y,
                    minOf(x + cellWidth, width),
                    minOf(y + cellHeight, height)
                )

                // Skip if cell is too small
                if (cellRect.width() < 32 || cellRect.height() < 32) {
                    continue
                }

                // Extract cell bitmap
                val cellBitmap = Bitmap.createBitmap(
                    bitmap,
                    cellRect.left,
                    cellRect.top,
                    cellRect.width(),
                    cellRect.height()
                )

                // Analyze cell for NSFW content using fast heuristic
                val confidence = analyzeCellForNSFWFast(cellBitmap)
                cellBitmap.recycle()

                // Check if this cell meets high confidence threshold
                if (confidence >= settings.nsfwConfidenceThreshold) {
                    highConfidenceRegions.add(cellRect)
                    regionConfidences.add(confidence)
                    maxConfidence = maxOf(maxConfidence, confidence)
                }
            }
        }

        return@withContext FastRegionAnalysis(
            regionCount = highConfidenceRegions.size,
            regionRects = highConfidenceRegions,
            maxConfidence = maxConfidence
        )
    }

    /**
     * Fast NSFW analysis for a single cell
     */
    private fun analyzeCellForNSFWFast(cellBitmap: Bitmap): Float {
        // Quick skin tone analysis with reduced sampling
        val sampleStep = 4 // Sample every 4th pixel for speed
        var skinPixels = 0
        var totalPixels = 0

        for (x in 0 until cellBitmap.width step sampleStep) {
            for (y in 0 until cellBitmap.height step sampleStep) {
                val pixel = cellBitmap.getPixel(x, y)
                if (isSkinTonePixelFast(pixel)) {
                    skinPixels++
                }
                totalPixels++
            }
        }

        val skinRatio = if (totalPixels > 0) skinPixels.toFloat() / totalPixels else 0.0f

        // Convert skin ratio to confidence score
        return when {
            skinRatio > 0.4f -> 0.8f
            skinRatio > 0.3f -> 0.7f
            skinRatio > 0.2f -> 0.6f
            skinRatio > 0.15f -> 0.5f
            skinRatio > 0.1f -> 0.4f
            else -> 0.2f
        }
    }

    /**
     * Data class for fast region analysis results
     */
    private data class FastRegionAnalysis(
        val regionCount: Int,
        val regionRects: List<Rect>,
        val maxConfidence: Float
    )
    
    // Cache data classes
    private data class CachedDetectionResult(
        val result: FastDetectionResult,
        val timestamp: Long
    )
    
    private data class CachedDensityResult(
        val result: ContentDensityResult,
        val timestamp: Long
    )
}