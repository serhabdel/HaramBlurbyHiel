package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.hieltech.haramblur.data.AppSettings

/**
 * Interface for analyzing content density to determine full-screen blur requirements
 * Provides spatial distribution analysis and blur coverage calculations
 */
interface ContentDensityAnalyzer {
    
    /**
     * Analyze screen content density for inappropriate content distribution
     * @param bitmap Input screen bitmap to analyze
     * @return Density analysis result with spatial distribution and recommendations
     */
    suspend fun analyzeScreenContent(bitmap: Bitmap): DensityAnalysisResult
    
    /**
     * Calculate blur coverage percentage based on blur regions and screen size
     * @param regions List of blur regions
     * @param screenSize Screen dimensions
     * @return Percentage of screen covered by blur regions (0.0 to 1.0)
     */
    fun calculateBlurCoverage(regions: List<Rect>, screenSize: Size): Float
    
    /**
     * Determine if full-screen warning should be triggered based on density and settings
     * @param density Content density percentage (0.0 to 1.0)
     * @param settings Current app settings with thresholds
     * @return True if full-screen warning should be shown
     */
    fun shouldTriggerFullScreenWarning(density: Float, settings: AppSettings): Boolean
    
    /**
     * Analyze spatial distribution of inappropriate content across screen quadrants
     * @param bitmap Input bitmap to analyze
     * @return Spatial distribution map with quadrant percentages
     */
    suspend fun analyzeSpatialDistribution(bitmap: Bitmap): SpatialDistribution
    
    /**
     * Calculate recommended blur action based on content analysis
     * @param analysisResult Density analysis result
     * @param settings Current app settings
     * @return Recommended content action
     */
    fun calculateRecommendedAction(analysisResult: DensityAnalysisResult, settings: AppSettings): ContentAction

    /**
     * NEW: Analyze spatial distribution of NSFW regions
     * @param nsfwRegionRects List of NSFW region bounding boxes
     * @param bitmap Input bitmap for size reference
     * @return Spatial distribution analysis for regions
     */
    suspend fun analyzeRegionSpatialDistribution(nsfwRegionRects: List<Rect>, bitmap: Bitmap): RegionSpatialDistribution

    /**
     * NEW: Check if region-based full-screen blur should be triggered
     * @param nsfwRegionCount Number of NSFW regions detected
     * @param maxNsfwConfidence Maximum confidence among regions
     * @param settings Current app settings
     * @return True if region-based full-screen blur should be triggered
     */
    fun shouldTriggerByRegionCount(
        nsfwRegionCount: Int,
        maxNsfwConfidence: Float,
        settings: AppSettings
    ): Boolean

    /**
     * NEW: Merge overlapping NSFW regions to avoid double-counting
     * @param regionRects List of region bounding boxes
     * @param regionConfidences List of region confidence scores
     * @return Merged list of non-overlapping regions with consolidated confidence
     */
    fun mergeOverlappingRegions(
        regionRects: List<Rect>,
        regionConfidences: List<Float>
    ): List<Pair<Rect, Float>>

    /**
     * NEW: Calculate coverage percentage of NSFW regions on screen
     * @param regionRects List of NSFW region bounding boxes
     * @param screenSize Screen dimensions
     * @return Percentage of screen covered by NSFW regions (0.0 to 1.0)
     */
    fun calculateRegionCoverage(regionRects: List<Rect>, screenSize: Size): Float
}

/**
 * Result of density analysis with spatial distribution and recommendations
 * Enhanced with region-based information
 */
data class DensityAnalysisResult(
    val inappropriateContentDensity: Float, // 0.0 to 1.0
    val spatialDistribution: SpatialDistribution,
    val recommendedAction: ContentAction,
    val warningLevel: WarningLevel,
    val criticalRegions: List<Rect>,
    val blurCoveragePercentage: Float,
    val processingTimeMs: Long,
    val gridAnalysis: GridAnalysis,
    // NEW: Enhanced region-based information
    val nsfwRegionCount: Int = 0, // Number of NSFW regions detected
    val maxNsfwConfidence: Float = 0.0f, // Highest confidence among NSFW regions
    val nsfwRegionRects: List<Rect> = emptyList(), // Bounding boxes of NSFW regions
    val regionBasedTrigger: Boolean = false // Whether region-based rule was triggered
)

/**
 * Spatial distribution of content across screen quadrants
 */
data class SpatialDistribution(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float,
    val center: Float,
    val edges: Float,
    val maxQuadrantDensity: Float,
    val distributionVariance: Float
) {
    /**
     * Check if content is concentrated in specific areas
     */
    fun isContentConcentrated(): Boolean = maxQuadrantDensity > 0.6f
    
    /**
     * Check if content is evenly distributed across screen
     */
    fun isContentDistributed(): Boolean = distributionVariance < 0.2f
    
    /**
     * Get the quadrant with highest content density
     */
    fun getHighestDensityQuadrant(): String {
        val densities = mapOf(
            "top_left" to topLeft,
            "top_right" to topRight,
            "bottom_left" to bottomLeft,
            "bottom_right" to bottomRight,
            "center" to center
        )
        return densities.maxByOrNull { it.value }?.key ?: "unknown"
    }
}

/**
 * Grid-based analysis of screen content
 */
data class GridAnalysis(
    val gridSize: Int,
    val cellDensities: Array<Array<Float>>,
    val highDensityCells: Int,
    val mediumDensityCells: Int,
    val lowDensityCells: Int,
    val averageCellDensity: Float,
    val maxCellDensity: Float,
    val minCellDensity: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as GridAnalysis
        
        if (gridSize != other.gridSize) return false
        if (!cellDensities.contentDeepEquals(other.cellDensities)) return false
        if (highDensityCells != other.highDensityCells) return false
        if (mediumDensityCells != other.mediumDensityCells) return false
        if (lowDensityCells != other.lowDensityCells) return false
        if (averageCellDensity != other.averageCellDensity) return false
        if (maxCellDensity != other.maxCellDensity) return false
        if (minCellDensity != other.minCellDensity) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = gridSize
        result = 31 * result + cellDensities.contentDeepHashCode()
        result = 31 * result + highDensityCells
        result = 31 * result + mediumDensityCells
        result = 31 * result + lowDensityCells
        result = 31 * result + averageCellDensity.hashCode()
        result = 31 * result + maxCellDensity.hashCode()
        result = 31 * result + minCellDensity.hashCode()
        return result
    }
}

/**
 * Recommended actions based on content analysis
 */
enum class ContentAction {
    NO_ACTION,
    SELECTIVE_BLUR,
    FULL_SCREEN_BLUR,
    BLOCK_AND_WARN,
    IMMEDIATE_CLOSE,
    SCROLL_AWAY,        // Scroll page to move inappropriate content out of view
    NAVIGATE_BACK,      // Go back to previous page/screen
    AUTO_CLOSE_APP,     // Automatically close the current app
    GENTLE_REDIRECT     // Show warning and redirect after short delay
}

/**
 * Warning levels for content density
 */
enum class WarningLevel(val level: Int, val description: String) {
    NONE(0, "No warning needed"),
    MINIMAL(1, "Minimal inappropriate content detected"),
    LOW(2, "Low level inappropriate content"),
    MEDIUM(3, "Medium level inappropriate content"),
    HIGH(4, "High level inappropriate content"),
    CRITICAL(5, "Critical level - immediate action required")
}

/**
 * NEW: Spatial distribution analysis for NSFW regions
 */
data class RegionSpatialDistribution(
    val totalRegions: Int,
    val clusteredRegions: Int, // Regions that are close to each other
    val distributedRegions: Int, // Regions that are spread out
    val centerWeightedRegions: Int, // Regions concentrated in center
    val edgeWeightedRegions: Int, // Regions concentrated on edges
    val clusteringScore: Float, // 0.0 = evenly distributed, 1.0 = highly clustered
    val coveragePercentage: Float, // Percentage of screen covered by regions
    val dominantLocation: String // "center", "top", "bottom", "left", "right", "distributed"
) {
    /**
     * Check if regions are highly clustered
     */
    fun isHighlyClustered(): Boolean = clusteringScore > 0.7f

    /**
     * Check if regions are evenly distributed
     */
    fun isEvenlyDistributed(): Boolean = clusteringScore < 0.3f

    /**
     * Get clustering description
     */
    fun getClusteringDescription(): String = when {
        clusteringScore > 0.8f -> "Highly clustered"
        clusteringScore > 0.6f -> "Moderately clustered"
        clusteringScore > 0.4f -> "Somewhat clustered"
        clusteringScore > 0.2f -> "Somewhat distributed"
        else -> "Evenly distributed"
    }
}