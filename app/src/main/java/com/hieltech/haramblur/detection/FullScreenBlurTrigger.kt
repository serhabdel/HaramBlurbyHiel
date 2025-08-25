package com.hieltech.haramblur.detection

import android.util.Log
import com.hieltech.haramblur.data.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles full-screen blur triggering logic based on content density analysis
 * Determines when selective blur should escalate to full-screen blur with warnings
 */
@Singleton
class FullScreenBlurTrigger @Inject constructor() {
    
    companion object {
        private const val TAG = "FullScreenBlurTrigger"
        private const val DEFAULT_DENSITY_THRESHOLD = 0.4f
        private const val HIGH_DENSITY_THRESHOLD = 0.6f
        private const val CRITICAL_DENSITY_THRESHOLD = 0.8f
        private const val CRITICAL_REGIONS_THRESHOLD = 8
        private const val HIGH_REGIONS_THRESHOLD = 5
    }
    
    /**
     * Check if full-screen blur should be triggered based on density analysis
     * @param analysisResult Content density analysis result
     * @param settings Current app settings
     * @return Full-screen blur decision with reasoning
     */
    fun shouldTriggerFullScreenBlur(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings
    ): FullScreenBlurDecision {
        
        if (!settings.fullScreenWarningEnabled) {
            Log.d(TAG, "Full-screen warnings disabled in settings")
            return FullScreenBlurDecision(
                shouldTrigger = false,
                reason = "Full-screen warnings disabled",
                warningLevel = WarningLevel.NONE,
                recommendedAction = ContentAction.SELECTIVE_BLUR,
                reflectionTimeSeconds = 0
            )
        }
        
        val density = analysisResult.inappropriateContentDensity
        val threshold = settings.contentDensityThreshold
        val criticalRegions = analysisResult.criticalRegions.size
        val spatialDistribution = analysisResult.spatialDistribution
        val warningLevel = analysisResult.warningLevel
        
        Log.d(TAG, "Evaluating full-screen blur: density=$density, threshold=$threshold, regions=$criticalRegions, warning=$warningLevel")
        
        // Critical level - immediate full-screen blur
        if (density >= CRITICAL_DENSITY_THRESHOLD || warningLevel == WarningLevel.CRITICAL) {
            return FullScreenBlurDecision(
                shouldTrigger = true,
                reason = "Critical inappropriate content density detected",
                warningLevel = WarningLevel.CRITICAL,
                recommendedAction = ContentAction.IMMEDIATE_CLOSE,
                reflectionTimeSeconds = settings.mandatoryReflectionTime * 2 // Double reflection time for critical
            )
        }
        
        // High density threshold exceeded
        if (density >= HIGH_DENSITY_THRESHOLD || criticalRegions >= CRITICAL_REGIONS_THRESHOLD) {
            return FullScreenBlurDecision(
                shouldTrigger = true,
                reason = "High inappropriate content density (${(density * 100).toInt()}%) or critical regions ($criticalRegions)",
                warningLevel = WarningLevel.HIGH,
                recommendedAction = ContentAction.FULL_SCREEN_BLUR,
                reflectionTimeSeconds = settings.mandatoryReflectionTime
            )
        }
        
        // User-configured threshold exceeded
        if (density >= threshold) {
            return FullScreenBlurDecision(
                shouldTrigger = true,
                reason = "Content density (${(density * 100).toInt()}%) exceeds user threshold (${(threshold * 100).toInt()}%)",
                warningLevel = warningLevel,
                recommendedAction = ContentAction.FULL_SCREEN_BLUR,
                reflectionTimeSeconds = settings.mandatoryReflectionTime
            )
        }
        
        // Check for distributed inappropriate content
        if (spatialDistribution.isContentDistributed() && density > 0.25f) {
            return FullScreenBlurDecision(
                shouldTrigger = true,
                reason = "Inappropriate content distributed across screen (density: ${(density * 100).toInt()}%)",
                warningLevel = warningLevel,
                recommendedAction = ContentAction.FULL_SCREEN_BLUR,
                reflectionTimeSeconds = settings.mandatoryReflectionTime
            )
        }
        
        // Check for concentrated high-density content
        if (spatialDistribution.maxQuadrantDensity > 0.7f && criticalRegions >= HIGH_REGIONS_THRESHOLD) {
            return FullScreenBlurDecision(
                shouldTrigger = true,
                reason = "High concentration of inappropriate content in ${spatialDistribution.getHighestDensityQuadrant()} quadrant",
                warningLevel = warningLevel,
                recommendedAction = ContentAction.FULL_SCREEN_BLUR,
                reflectionTimeSeconds = settings.mandatoryReflectionTime
            )
        }
        
        // Default to selective blur
        Log.d(TAG, "Full-screen blur not triggered - using selective blur")
        return FullScreenBlurDecision(
            shouldTrigger = false,
            reason = "Content density below threshold for full-screen blur",
            warningLevel = warningLevel,
            recommendedAction = determineSelectiveAction(density, criticalRegions),
            reflectionTimeSeconds = 0
        )
    }
    
    /**
     * Calculate warning level based on content analysis
     * @param analysisResult Content density analysis result
     * @param settings Current app settings
     * @return Calculated warning level with details
     */
    fun calculateWarningLevel(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings
    ): WarningLevelDetails {
        
        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        val spatialDistribution = analysisResult.spatialDistribution
        val blurCoverage = analysisResult.blurCoveragePercentage
        
        val warningLevel = when {
            density >= CRITICAL_DENSITY_THRESHOLD || criticalRegions > 15 -> WarningLevel.CRITICAL
            density >= HIGH_DENSITY_THRESHOLD || criticalRegions > CRITICAL_REGIONS_THRESHOLD -> WarningLevel.HIGH
            density >= settings.contentDensityThreshold || criticalRegions > HIGH_REGIONS_THRESHOLD -> WarningLevel.MEDIUM
            density > 0.2f || criticalRegions > 2 -> WarningLevel.LOW
            density > 0.05f || criticalRegions > 0 -> WarningLevel.MINIMAL
            else -> WarningLevel.NONE
        }
        
        val severity = calculateSeverityScore(density, criticalRegions, spatialDistribution, blurCoverage)
        val urgency = calculateUrgencyLevel(warningLevel, spatialDistribution)
        
        return WarningLevelDetails(
            level = warningLevel,
            severity = severity,
            urgency = urgency,
            primaryReason = getPrimaryReason(density, criticalRegions, spatialDistribution),
            secondaryFactors = getSecondaryFactors(analysisResult),
            recommendedReflectionTime = calculateReflectionTime(warningLevel, settings)
        )
    }
    
    /**
     * Calculate recommended action based on content analysis
     * @param analysisResult Content density analysis result
     * @param settings Current app settings
     * @return Recommended content action with reasoning
     */
    fun calculateRecommendedAction(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings
    ): RecommendedActionResult {
        
        val fullScreenDecision = shouldTriggerFullScreenBlur(analysisResult, settings)
        
        if (fullScreenDecision.shouldTrigger) {
            return RecommendedActionResult(
                action = fullScreenDecision.recommendedAction,
                reason = fullScreenDecision.reason,
                confidence = calculateActionConfidence(analysisResult),
                alternativeActions = getAlternativeActions(analysisResult, settings),
                requiresUserConfirmation = fullScreenDecision.warningLevel >= WarningLevel.HIGH
            )
        }
        
        // Determine selective blur action
        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        
        val action = when {
            density > 0.1f || criticalRegions > 0 -> ContentAction.SELECTIVE_BLUR
            else -> ContentAction.NO_ACTION
        }
        
        return RecommendedActionResult(
            action = action,
            reason = "Content density (${(density * 100).toInt()}%) suitable for selective blur",
            confidence = calculateActionConfidence(analysisResult),
            alternativeActions = getAlternativeActions(analysisResult, settings),
            requiresUserConfirmation = false
        )
    }
    
    // Private helper methods
    
    private fun determineSelectiveAction(density: Float, criticalRegions: Int): ContentAction {
        return when {
            density > 0.1f || criticalRegions > 0 -> ContentAction.SELECTIVE_BLUR
            else -> ContentAction.NO_ACTION
        }
    }
    
    private fun calculateSeverityScore(
        density: Float,
        criticalRegions: Int,
        spatialDistribution: SpatialDistribution,
        blurCoverage: Float
    ): Float {
        var score = 0.0f
        
        // Density contribution (0-40 points)
        score += density * 40f
        
        // Critical regions contribution (0-30 points)
        score += minOf(30f, criticalRegions * 3f)
        
        // Spatial distribution contribution (0-20 points)
        if (spatialDistribution.isContentDistributed()) {
            score += 20f
        } else if (spatialDistribution.maxQuadrantDensity > 0.6f) {
            score += 15f
        }
        
        // Blur coverage contribution (0-10 points)
        score += blurCoverage * 10f
        
        return (score / 100f).coerceIn(0.0f, 1.0f)
    }
    
    private fun calculateUrgencyLevel(
        warningLevel: WarningLevel,
        spatialDistribution: SpatialDistribution
    ): UrgencyLevel {
        return when (warningLevel) {
            WarningLevel.CRITICAL -> UrgencyLevel.IMMEDIATE
            WarningLevel.HIGH -> UrgencyLevel.HIGH
            WarningLevel.MEDIUM -> {
                if (spatialDistribution.isContentDistributed()) {
                    UrgencyLevel.HIGH
                } else {
                    UrgencyLevel.MEDIUM
                }
            }
            WarningLevel.LOW -> UrgencyLevel.LOW
            WarningLevel.MINIMAL -> UrgencyLevel.LOW
            WarningLevel.NONE -> UrgencyLevel.NONE
        }
    }
    
    private fun getPrimaryReason(
        density: Float,
        criticalRegions: Int,
        spatialDistribution: SpatialDistribution
    ): String {
        return when {
            density >= CRITICAL_DENSITY_THRESHOLD -> "Critical content density: ${(density * 100).toInt()}%"
            criticalRegions > CRITICAL_REGIONS_THRESHOLD -> "High number of critical regions: $criticalRegions"
            spatialDistribution.isContentDistributed() -> "Inappropriate content distributed across screen"
            spatialDistribution.maxQuadrantDensity > 0.7f -> "High concentration in ${spatialDistribution.getHighestDensityQuadrant()} quadrant"
            density >= HIGH_DENSITY_THRESHOLD -> "High content density: ${(density * 100).toInt()}%"
            else -> "Moderate inappropriate content detected"
        }
    }
    
    private fun getSecondaryFactors(analysisResult: DensityAnalysisResult): List<String> {
        val factors = mutableListOf<String>()
        
        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        val spatialDistribution = analysisResult.spatialDistribution
        val gridAnalysis = analysisResult.gridAnalysis
        
        if (gridAnalysis.highDensityCells > 4) {
            factors.add("${gridAnalysis.highDensityCells} high-density grid cells")
        }
        
        if (spatialDistribution.distributionVariance > 0.3f) {
            factors.add("High content distribution variance")
        }
        
        if (analysisResult.blurCoveragePercentage > 0.5f) {
            factors.add("High blur coverage required: ${(analysisResult.blurCoveragePercentage * 100).toInt()}%")
        }
        
        if (spatialDistribution.center > 0.5f) {
            factors.add("High center region density")
        }
        
        return factors
    }
    
    private fun calculateReflectionTime(warningLevel: WarningLevel, settings: AppSettings): Int {
        val baseTime = settings.mandatoryReflectionTime
        return when (warningLevel) {
            WarningLevel.CRITICAL -> baseTime * 3
            WarningLevel.HIGH -> baseTime * 2
            WarningLevel.MEDIUM -> baseTime
            WarningLevel.LOW -> baseTime / 2
            WarningLevel.MINIMAL -> baseTime / 3
            WarningLevel.NONE -> 0
        }
    }
    
    private fun calculateActionConfidence(analysisResult: DensityAnalysisResult): Float {
        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        val gridAnalysis = analysisResult.gridAnalysis
        
        var confidence = 0.0f
        
        // High density gives high confidence
        confidence += density * 0.4f
        
        // Multiple critical regions increase confidence
        confidence += minOf(0.3f, criticalRegions * 0.05f)
        
        // Consistent grid analysis increases confidence
        if (gridAnalysis.highDensityCells > 2) {
            confidence += 0.2f
        }
        
        // Processing time factor (faster processing might be less accurate)
        if (analysisResult.processingTimeMs > 500L) {
            confidence += 0.1f
        }
        
        return confidence.coerceIn(0.0f, 1.0f)
    }
    
    private fun getAlternativeActions(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings
    ): List<ContentAction> {
        val alternatives = mutableListOf<ContentAction>()
        val density = analysisResult.inappropriateContentDensity
        val warningLevel = analysisResult.warningLevel
        
        when (warningLevel) {
            WarningLevel.CRITICAL -> {
                alternatives.add(ContentAction.FULL_SCREEN_BLUR)
                alternatives.add(ContentAction.BLOCK_AND_WARN)
            }
            WarningLevel.HIGH -> {
                alternatives.add(ContentAction.SELECTIVE_BLUR)
                alternatives.add(ContentAction.BLOCK_AND_WARN)
            }
            WarningLevel.MEDIUM -> {
                alternatives.add(ContentAction.SELECTIVE_BLUR)
                alternatives.add(ContentAction.FULL_SCREEN_BLUR)
            }
            WarningLevel.LOW -> {
                alternatives.add(ContentAction.NO_ACTION)
                alternatives.add(ContentAction.FULL_SCREEN_BLUR)
            }
            else -> {
                alternatives.add(ContentAction.SELECTIVE_BLUR)
            }
        }
        
        return alternatives.distinct()
    }
}

/**
 * Result of full-screen blur decision
 */
data class FullScreenBlurDecision(
    val shouldTrigger: Boolean,
    val reason: String,
    val warningLevel: WarningLevel,
    val recommendedAction: ContentAction,
    val reflectionTimeSeconds: Int
)

/**
 * Detailed warning level information
 */
data class WarningLevelDetails(
    val level: WarningLevel,
    val severity: Float, // 0.0 to 1.0
    val urgency: UrgencyLevel,
    val primaryReason: String,
    val secondaryFactors: List<String>,
    val recommendedReflectionTime: Int
)

/**
 * Recommended action result with reasoning
 */
data class RecommendedActionResult(
    val action: ContentAction,
    val reason: String,
    val confidence: Float, // 0.0 to 1.0
    val alternativeActions: List<ContentAction>,
    val requiresUserConfirmation: Boolean
)

/**
 * Urgency levels for content warnings
 */
enum class UrgencyLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    IMMEDIATE
}