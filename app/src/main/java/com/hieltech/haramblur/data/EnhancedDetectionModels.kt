package com.hieltech.haramblur.data

import android.graphics.Rect
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language

/**
 * Data models for enhanced detection features
 */

/**
 * Performance metrics for detection operations
 */
data class DetectionPerformanceMetrics(
    val processingTimeMs: Long,
    val memoryUsageMB: Float,
    val cpuUsagePercent: Float,
    val gpuAccelerationUsed: Boolean,
    val frameSkipped: Boolean,
    val qualityReduced: Boolean
)

/**
 * Enhanced detection result combining all detection types
 */
data class EnhancedDetectionResult(
    val shouldBlur: Boolean,
    val blurRegions: List<Rect>,
    val genderAnalysis: GenderAnalysisResult?,
    val contentDensity: ContentDensityMetrics?,
    val siteBlocking: SiteBlockingInfo?,
    val performanceMetrics: DetectionPerformanceMetrics,
    val recommendedAction: String,
    val warningLevel: Int // 0-5 scale
)

/**
 * Gender analysis result for enhanced detection
 */
data class GenderAnalysisResult(
    val maleCount: Int,
    val femaleCount: Int,
    val unknownCount: Int,
    val averageConfidence: Float,
    val shouldBlurMales: Boolean,
    val shouldBlurFemales: Boolean,
    val processingTimeMs: Long
)

/**
 * Content density metrics for full-screen blur decisions
 */
data class ContentDensityMetrics(
    val inappropriateContentPercentage: Float,
    val spatialDistribution: Map<String, Float>, // quadrant -> percentage
    val recommendsFullScreenBlur: Boolean,
    val criticalRegionCount: Int,
    val warningLevel: Int
)

/**
 * Site blocking information
 */
data class SiteBlockingInfo(
    val isBlocked: Boolean,
    val category: BlockingCategory?,
    val confidence: Float,
    val reflectionTimeSeconds: Int,
    val quranicVerseId: String?,
    val guidanceText: String?
)

/**
 * Enhanced settings validation result
 */
data class SettingsValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val recommendedChanges: Map<String, Any>
)

/**
 * Detection capability info for system status
 */
data class DetectionCapabilities(
    val genderDetectionAvailable: Boolean,
    val fastProcessingAvailable: Boolean,
    val siteBlockingAvailable: Boolean,
    val gpuAccelerationAvailable: Boolean,
    val quranicGuidanceAvailable: Boolean,
    val supportedLanguages: List<Language>,
    val maxProcessingTimeMs: Long,
    val recommendedSettings: AppSettings
)