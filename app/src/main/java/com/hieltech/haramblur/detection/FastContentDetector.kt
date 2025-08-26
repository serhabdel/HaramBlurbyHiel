package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ContentDensityMetrics
import com.hieltech.haramblur.data.DetectionPerformanceMetrics

/**
 * Ultra-fast content detection interface for performance-optimized blur processing
 * Implements multi-threaded processing, image downscaling, and GPU acceleration
 */
interface FastContentDetector {
    
    /**
     * Perform ultra-fast content detection with performance optimizations
     * @param bitmap Input image to analyze
     * @param settings Detection settings and thresholds
     * @return Fast detection result with blur regions and performance metrics
     */
    suspend fun detectContentFast(bitmap: Bitmap, settings: AppSettings): FastDetectionResult
    
    /**
     * Analyze content density for full-screen blur decisions
     * @param bitmap Input image to analyze
     * @return Content density analysis result
     */
    suspend fun analyzeContentDensity(bitmap: Bitmap): ContentDensityResult
    
    /**
     * Set performance mode for detection processing
     * @param mode Performance mode to use
     */
    fun setPerformanceMode(mode: PerformanceMode)
    
    /**
     * Get current performance statistics
     * @return Current performance metrics
     */
    fun getPerformanceStats(): DetectionPerformanceMetrics
    
    /**
     * Clear internal caches and reset performance counters
     */
    fun clearCaches()
    
    /**
     * Check if GPU acceleration is available and enabled
     * @return True if GPU acceleration is active
     */
    fun isGPUAccelerationEnabled(): Boolean
}

/**
 * Result of fast content detection
 */
data class FastDetectionResult(
    val shouldBlur: Boolean,
    val blurRegions: List<Rect>,
    val contentType: ContentType,
    val processingTimeMs: Long,
    val confidenceScore: Float,
    val performanceMetrics: DetectionPerformanceMetrics,
    val qualityReduced: Boolean,
    val frameSkipped: Boolean,
    // Enhanced region-based information for full-screen blur triggering
    val nsfwRegionCount: Int = 0, // Number of NSFW regions detected in fast mode
    val maxNsfwConfidence: Float = 0.0f, // Highest confidence among NSFW regions
    val nsfwRegionRects: List<Rect> = emptyList() // Bounding boxes of NSFW regions
)

/**
 * Result of content density analysis
 */
data class ContentDensityResult(
    val inappropriateContentPercentage: Float,
    val distributionMap: Array<Array<Float>>, // 4x4 grid of content density
    val recommendsFullScreenBlur: Boolean,
    val criticalRegions: List<Rect>,
    val densityMetrics: ContentDensityMetrics,
    val processingTimeMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ContentDensityResult
        
        if (inappropriateContentPercentage != other.inappropriateContentPercentage) return false
        if (!distributionMap.contentDeepEquals(other.distributionMap)) return false
        if (recommendsFullScreenBlur != other.recommendsFullScreenBlur) return false
        if (criticalRegions != other.criticalRegions) return false
        if (densityMetrics != other.densityMetrics) return false
        if (processingTimeMs != other.processingTimeMs) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = inappropriateContentPercentage.hashCode()
        result = 31 * result + distributionMap.contentDeepHashCode()
        result = 31 * result + recommendsFullScreenBlur.hashCode()
        result = 31 * result + criticalRegions.hashCode()
        result = 31 * result + densityMetrics.hashCode()
        result = 31 * result + processingTimeMs.hashCode()
        return result
    }
}

/**
 * Performance modes for fast detection
 */
enum class PerformanceMode(
    val displayName: String,
    val maxProcessingTimeMs: Long,
    val imageDownscaleRatio: Float,
    val enableGPU: Boolean,
    val frameSkipThreshold: Int,
    val description: String
) {
    ULTRA_FAST(
        "Ultra Fast",
        50L,
        0.6f, // Better resolution for face detection: 1080x2400 → 648x1440
        true,
        2,
        "Maximum speed, better face detection accuracy"
    ),
    FAST(
        "Fast", 
        100L,
        0.75f, // Better resolution: 1080x2400 → 810x1800
        true,
        3,
        "High speed with good accuracy"
    ),
    BALANCED(
        "Balanced",
        200L,
        0.85f, // Higher resolution for better detection: 1080x2400 → 918x2040
        true,
        5,
        "Balance between speed and accuracy"
    ),
    QUALITY(
        "Quality",
        500L,
        1.0f,
        false,
        10,
        "Best accuracy, slower processing"
    )
}

/**
 * Content types detected by fast detector
 */
enum class ContentType {
    SAFE,
    FACES_DETECTED,
    NSFW_CONTENT,
    HIGH_SKIN_TONE,
    MIXED_CONTENT,
    UNKNOWN
}