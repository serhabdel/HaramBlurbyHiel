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
}

/**
 * Result of density analysis with spatial distribution and recommendations
 */
data class DensityAnalysisResult(
    val inappropriateContentDensity: Float, // 0.0 to 1.0
    val spatialDistribution: SpatialDistribution,
    val recommendedAction: ContentAction,
    val warningLevel: WarningLevel,
    val criticalRegions: List<Rect>,
    val blurCoveragePercentage: Float,
    val processingTimeMs: Long,
    val gridAnalysis: GridAnalysis
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
    IMMEDIATE_CLOSE
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