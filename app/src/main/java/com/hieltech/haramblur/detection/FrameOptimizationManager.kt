package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages frame optimization including frame skipping, ROI processing, and adaptive quality
 */
@Singleton
class FrameOptimizationManager @Inject constructor() {
    
    companion object {
        private const val TAG = "FrameOptimizationManager"
        private const val PERFORMANCE_WINDOW_MS = 5000L // 5 second performance window
        private const val MIN_FRAME_INTERVAL_MS = 33L // ~30 FPS max
        private const val RAPID_SCROLLING_THRESHOLD = 100L // ms between frames
        private const val ROI_MARGIN_RATIO = 0.1f // 10% margin around ROI
    }
    
    private val mutex = Mutex()
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var skippedFrameCount = 0L
    private var totalProcessingTime = AtomicLong(0)
    private var currentQualityLevel = QualityLevel.HIGH
    
    // Performance tracking
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    private var isRapidScrolling = false
    private var consecutiveSlowFrames = 0
    
    // ROI tracking
    private var lastROI: Rect? = null
    private var roiStabilityCount = 0
    
    /**
     * Determine if current frame should be processed or skipped
     */
    suspend fun shouldProcessFrame(forceProcess: Boolean = false): FrameDecision = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastFrameTime
        
        frameCount++
        
        // Always process if forced or first frame
        if (forceProcess || lastFrameTime == 0L) {
            lastFrameTime = currentTime
            return FrameDecision(
                shouldProcess = true,
                qualityLevel = currentQualityLevel,
                reason = if (forceProcess) "Forced processing" else "First frame"
            )
        }
        
        // Skip if processing too frequently
        if (timeSinceLastFrame < MIN_FRAME_INTERVAL_MS) {
            skippedFrameCount++
            return FrameDecision(
                shouldProcess = false,
                qualityLevel = currentQualityLevel,
                reason = "Frame rate limiting (${timeSinceLastFrame}ms < ${MIN_FRAME_INTERVAL_MS}ms)"
            )
        }
        
        // Detect rapid scrolling
        val wasRapidScrolling = isRapidScrolling
        isRapidScrolling = timeSinceLastFrame < RAPID_SCROLLING_THRESHOLD
        
        // Skip frames during rapid scrolling based on quality level
        if (isRapidScrolling) {
            val skipRatio = when (currentQualityLevel) {
                QualityLevel.ULTRA_FAST -> 1 // Process every frame
                QualityLevel.FAST -> 2 // Process every 2nd frame
                QualityLevel.BALANCED -> 3 // Process every 3rd frame
                QualityLevel.HIGH -> 4 // Process every 4th frame
            }
            
            if (frameCount % skipRatio != 0L) {
                skippedFrameCount++
                return FrameDecision(
                    shouldProcess = false,
                    qualityLevel = currentQualityLevel,
                    reason = "Rapid scrolling frame skip (ratio: $skipRatio)"
                )
            }
        }
        
        lastFrameTime = currentTime
        return FrameDecision(
            shouldProcess = true,
            qualityLevel = currentQualityLevel,
            reason = "Normal processing"
        )
    }
    
    /**
     * Calculate optimal Region of Interest for processing
     */
    fun calculateROI(bitmap: Bitmap, previousBlurRegions: List<Rect>): ROIResult {
        val fullRect = Rect(0, 0, bitmap.width, bitmap.height)
        
        // If no previous regions, process full image
        if (previousBlurRegions.isEmpty()) {
            return ROIResult(
                roi = fullRect,
                reductionRatio = 1.0f,
                reason = "No previous regions, full processing"
            )
        }
        
        // Calculate bounding box of all blur regions
        val boundingBox = calculateBoundingBox(previousBlurRegions)
        
        // Expand bounding box with margin
        val margin = (minOf(bitmap.width, bitmap.height) * ROI_MARGIN_RATIO).toInt()
        val expandedROI = Rect(
            maxOf(0, boundingBox.left - margin),
            maxOf(0, boundingBox.top - margin),
            minOf(bitmap.width, boundingBox.right + margin),
            minOf(bitmap.height, boundingBox.bottom + margin)
        )
        
        // Check ROI stability
        val isStable = lastROI?.let { last ->
            val overlap = calculateOverlap(last, expandedROI)
            overlap > 0.7f // 70% overlap considered stable
        } ?: false
        
        if (isStable) {
            roiStabilityCount++
        } else {
            roiStabilityCount = 0
        }
        
        lastROI = expandedROI
        
        val fullArea = bitmap.width * bitmap.height
        val roiArea = expandedROI.width() * expandedROI.height()
        val reductionRatio = roiArea.toFloat() / fullArea
        
        return ROIResult(
            roi = expandedROI,
            reductionRatio = reductionRatio,
            reason = "ROI optimization (${(reductionRatio * 100).toInt()}% of image, stable: $isStable)"
        )
    }
    
    /**
     * Update performance metrics and adjust quality if needed
     */
    suspend fun updatePerformanceMetrics(processingTimeMs: Long, targetTimeMs: Long) = mutex.withLock {
        totalProcessingTime.addAndGet(processingTimeMs)
        
        val snapshot = PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            processingTimeMs = processingTimeMs,
            targetTimeMs = targetTimeMs,
            qualityLevel = currentQualityLevel
        )
        
        performanceHistory.add(snapshot)
        
        // Keep only recent history
        val cutoffTime = System.currentTimeMillis() - PERFORMANCE_WINDOW_MS
        performanceHistory.removeAll { it.timestamp < cutoffTime }
        
        // Analyze performance and adjust quality
        analyzeAndAdjustQuality(processingTimeMs, targetTimeMs)
        
        Log.d(TAG, "Performance updated: ${processingTimeMs}ms (target: ${targetTimeMs}ms), quality: $currentQualityLevel")
    }
    
    /**
     * Get current performance statistics
     */
    suspend fun getPerformanceStats(): FrameOptimizationStats = mutex.withLock {
        val totalFrames = frameCount
        val processedFrames = totalFrames - skippedFrameCount
        val avgProcessingTime = if (processedFrames > 0) {
            totalProcessingTime.get() / processedFrames
        } else {
            0L
        }
        
        val recentPerformance = performanceHistory.takeLast(10)
        val avgRecentTime = if (recentPerformance.isNotEmpty()) {
            recentPerformance.map { it.processingTimeMs }.average().toLong()
        } else {
            0L
        }
        
        return FrameOptimizationStats(
            totalFrames = totalFrames,
            processedFrames = processedFrames,
            skippedFrames = skippedFrameCount,
            skipRatio = if (totalFrames > 0) skippedFrameCount.toFloat() / totalFrames else 0f,
            avgProcessingTimeMs = avgProcessingTime,
            recentAvgProcessingTimeMs = avgRecentTime,
            currentQualityLevel = currentQualityLevel,
            isRapidScrolling = isRapidScrolling,
            roiStabilityCount = roiStabilityCount
        )
    }
    
    /**
     * Force quality level change
     */
    suspend fun setQualityLevel(level: QualityLevel) = mutex.withLock {
        currentQualityLevel = level
        consecutiveSlowFrames = 0
        Log.d(TAG, "Quality level manually set to: $level")
    }
    
    /**
     * Reset all performance counters
     */
    suspend fun reset() = mutex.withLock {
        frameCount = 0L
        skippedFrameCount = 0L
        totalProcessingTime.set(0)
        performanceHistory.clear()
        lastFrameTime = 0L
        isRapidScrolling = false
        consecutiveSlowFrames = 0
        lastROI = null
        roiStabilityCount = 0
        currentQualityLevel = QualityLevel.HIGH
        Log.d(TAG, "Frame optimization manager reset")
    }
    
    private fun analyzeAndAdjustQuality(processingTimeMs: Long, targetTimeMs: Long) {
        val isSlowFrame = processingTimeMs > targetTimeMs * 1.2f // 20% tolerance
        
        if (isSlowFrame) {
            consecutiveSlowFrames++
        } else {
            consecutiveSlowFrames = 0
        }
        
        // Adjust quality based on performance
        val newQualityLevel = when {
            consecutiveSlowFrames >= 3 -> {
                // Multiple slow frames, reduce quality
                when (currentQualityLevel) {
                    QualityLevel.HIGH -> QualityLevel.BALANCED
                    QualityLevel.BALANCED -> QualityLevel.FAST
                    QualityLevel.FAST -> QualityLevel.ULTRA_FAST
                    QualityLevel.ULTRA_FAST -> QualityLevel.ULTRA_FAST
                }
            }
            consecutiveSlowFrames == 0 && performanceHistory.size >= 5 -> {
                // Good performance, potentially increase quality
                val recentAvg = performanceHistory.takeLast(5).map { it.processingTimeMs }.average()
                if (recentAvg < targetTimeMs * 0.7f) { // Well under target
                    when (currentQualityLevel) {
                        QualityLevel.ULTRA_FAST -> QualityLevel.FAST
                        QualityLevel.FAST -> QualityLevel.BALANCED
                        QualityLevel.BALANCED -> QualityLevel.HIGH
                        QualityLevel.HIGH -> QualityLevel.HIGH
                    }
                } else {
                    currentQualityLevel
                }
            }
            else -> currentQualityLevel
        }
        
        if (newQualityLevel != currentQualityLevel) {
            Log.d(TAG, "Quality level adjusted: $currentQualityLevel -> $newQualityLevel (consecutive slow: $consecutiveSlowFrames)")
            currentQualityLevel = newQualityLevel
        }
    }
    
    private fun calculateBoundingBox(regions: List<Rect>): Rect {
        if (regions.isEmpty()) return Rect()
        
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        
        regions.forEach { rect ->
            left = minOf(left, rect.left)
            top = minOf(top, rect.top)
            right = maxOf(right, rect.right)
            bottom = maxOf(bottom, rect.bottom)
        }
        
        return Rect(left, top, right, bottom)
    }
    
    private fun calculateOverlap(rect1: Rect, rect2: Rect): Float {
        val intersection = Rect()
        if (!intersection.setIntersect(rect1, rect2)) {
            return 0f
        }
        
        val intersectionArea = intersection.width() * intersection.height()
        val unionArea = (rect1.width() * rect1.height()) + (rect2.width() * rect2.height()) - intersectionArea
        
        return if (unionArea > 0) intersectionArea.toFloat() / unionArea else 0f
    }
}

/**
 * Decision about whether to process current frame
 */
data class FrameDecision(
    val shouldProcess: Boolean,
    val qualityLevel: QualityLevel,
    val reason: String
)

/**
 * Result of ROI calculation
 */
data class ROIResult(
    val roi: Rect,
    val reductionRatio: Float, // 0.0 to 1.0, how much of the image to process
    val reason: String
)

/**
 * Performance snapshot for tracking
 */
private data class PerformanceSnapshot(
    val timestamp: Long,
    val processingTimeMs: Long,
    val targetTimeMs: Long,
    val qualityLevel: QualityLevel
)

/**
 * Frame optimization statistics
 */
data class FrameOptimizationStats(
    val totalFrames: Long,
    val processedFrames: Long,
    val skippedFrames: Long,
    val skipRatio: Float,
    val avgProcessingTimeMs: Long,
    val recentAvgProcessingTimeMs: Long,
    val currentQualityLevel: QualityLevel,
    val isRapidScrolling: Boolean,
    val roiStabilityCount: Int
)

/**
 * Quality levels for adaptive processing
 */
enum class QualityLevel(
    val displayName: String,
    val imageScale: Float,
    val maxProcessingTimeMs: Long,
    val description: String
) {
    ULTRA_FAST("Ultra Fast", 0.25f, 50L, "Maximum speed, minimum accuracy"),
    FAST("Fast", 0.5f, 100L, "High speed, reduced accuracy"),
    BALANCED("Balanced", 0.75f, 200L, "Balance of speed and accuracy"),
    HIGH("High Quality", 1.0f, 500L, "Best accuracy, slower processing")
}