package com.hieltech.haramblur.detection

import android.util.Log
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.llm.OpenRouterLLMService
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles full-screen blur triggering logic based on content density analysis
 * Determines when selective blur should escalate to full-screen blur with warnings
 * Enhanced with OpenRouter LLM for intelligent decision making
 */
@Singleton
class FullScreenBlurTrigger @Inject constructor(
    private val llmService: OpenRouterLLMService
) {
    
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
     * Enhanced with region-count-based triggering
     * @param analysisResult Content density analysis result
     * @param settings Current app settings
     * @param nsfwRegionCount Number of NSFW regions detected (optional, for region-based triggering)
     * @param maxNsfwConfidence Maximum confidence among NSFW regions (optional, for region-based triggering)
     * @return Full-screen blur decision with reasoning
     */
    fun shouldTriggerFullScreenBlur(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
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

        // NEW: High priority region-count-based triggering (6+ regions with high confidence)
        if (settings.enableRegionBasedFullScreen &&
            nsfwRegionCount >= settings.nsfwFullScreenRegionThreshold &&
            maxNsfwConfidence >= settings.nsfwHighConfidenceThreshold) {

            Log.d(TAG, "🚨 CRITICAL: Region-based trigger activated - $nsfwRegionCount regions with confidence $maxNsfwConfidence")

            // Check if LLM decision making is enabled for faster, smarter decisions
            if (settings.enableLLMDecisionMaking && settings.openRouterApiKey.isNotBlank()) {
                Log.d(TAG, "🤖 Using LLM for fast decision making")
                // Note: LLM call will be made asynchronously by caller
                return FullScreenBlurDecision(
                    shouldTrigger = false,
                    reason = "LLM_ENHANCED: $nsfwRegionCount regions detected - using AI decision",
                    warningLevel = WarningLevel.CRITICAL,
                    recommendedAction = ContentAction.SCROLL_AWAY, // Default while LLM decides
                    reflectionTimeSeconds = 0,
                    useLLMDecision = true
                )
            }

            // Use graduated response instead of immediate full screen blur
            val recommendedAction = when {
                nsfwRegionCount >= 10 -> ContentAction.AUTO_CLOSE_APP
                nsfwRegionCount >= 8 -> ContentAction.NAVIGATE_BACK
                nsfwRegionCount >= 6 -> ContentAction.SCROLL_AWAY
                else -> ContentAction.GENTLE_REDIRECT
            }

            Log.d(TAG, "🎯 Using graduated response: $recommendedAction for $nsfwRegionCount regions")

            return FullScreenBlurDecision(
                shouldTrigger = false, // Don't trigger full screen blur - use navigation instead
                reason = "ADAPTIVE: $nsfwRegionCount high-confidence NSFW regions detected - using $recommendedAction",
                warningLevel = WarningLevel.CRITICAL,
                recommendedAction = recommendedAction,
                reflectionTimeSeconds = 0 // No reflection time for automatic actions
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
     * Enhanced with region-based analysis
     * @param analysisResult Content density analysis result
     * @param settings Current app settings
     * @param nsfwRegionCount Number of NSFW regions detected (optional)
     * @param maxNsfwConfidence Maximum confidence among NSFW regions (optional)
     * @return Calculated warning level with details
     */
    fun calculateWarningLevel(
        analysisResult: DensityAnalysisResult,
        settings: AppSettings,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): WarningLevelDetails {
        
        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        val spatialDistribution = analysisResult.spatialDistribution
        val blurCoverage = analysisResult.blurCoveragePercentage

        // NEW: Region-based warning level calculation (highest priority)
        val regionBasedWarningLevel = when {
            nsfwRegionCount >= 10 && maxNsfwConfidence >= settings.nsfwHighConfidenceThreshold -> WarningLevel.CRITICAL
            nsfwRegionCount >= settings.nsfwFullScreenRegionThreshold && maxNsfwConfidence >= settings.nsfwHighConfidenceThreshold -> WarningLevel.CRITICAL
            nsfwRegionCount >= 4 && maxNsfwConfidence >= settings.nsfwHighConfidenceThreshold -> WarningLevel.HIGH
            nsfwRegionCount >= 2 && maxNsfwConfidence >= 0.6f -> WarningLevel.MEDIUM
            nsfwRegionCount >= 1 && maxNsfwConfidence >= 0.5f -> WarningLevel.LOW
            else -> WarningLevel.NONE
        }

        val warningLevel = when {
            // Region-based critical takes highest priority
            regionBasedWarningLevel == WarningLevel.CRITICAL -> WarningLevel.CRITICAL

            // Density-based critical
            density >= CRITICAL_DENSITY_THRESHOLD || criticalRegions > 15 -> WarningLevel.CRITICAL

            // Region-based high or density-based high
            regionBasedWarningLevel == WarningLevel.HIGH ||
            density >= HIGH_DENSITY_THRESHOLD || criticalRegions > CRITICAL_REGIONS_THRESHOLD -> WarningLevel.HIGH

            // Medium levels
            regionBasedWarningLevel == WarningLevel.MEDIUM ||
            density >= settings.contentDensityThreshold || criticalRegions > HIGH_REGIONS_THRESHOLD -> WarningLevel.MEDIUM

            // Low levels
            regionBasedWarningLevel == WarningLevel.LOW ||
            density > 0.2f || criticalRegions > 2 -> WarningLevel.LOW

            // Minimal levels
            density > 0.05f || criticalRegions > 0 -> WarningLevel.MINIMAL

            else -> WarningLevel.NONE
        }
        
        val severity = calculateSeverityScore(density, criticalRegions, spatialDistribution, blurCoverage, nsfwRegionCount, maxNsfwConfidence)
        val urgency = calculateUrgencyLevel(warningLevel, spatialDistribution, nsfwRegionCount, maxNsfwConfidence)

        return WarningLevelDetails(
            level = warningLevel,
            severity = severity,
            urgency = urgency,
            primaryReason = getPrimaryReason(density, criticalRegions, spatialDistribution, nsfwRegionCount, maxNsfwConfidence),
            secondaryFactors = getSecondaryFactors(analysisResult, nsfwRegionCount, maxNsfwConfidence),
            recommendedReflectionTime = calculateReflectionTime(warningLevel, settings, nsfwRegionCount, maxNsfwConfidence)
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
        blurCoverage: Float,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): Float {
        var score = 0.0f

        // NEW: Region-based severity contribution (highest priority)
        if (nsfwRegionCount >= 6 && maxNsfwConfidence >= 0.7f) {
            score += 50f // Critical region-based score
        } else if (nsfwRegionCount >= 4 && maxNsfwConfidence >= 0.6f) {
            score += 35f // High region-based score
        } else if (nsfwRegionCount >= 2 && maxNsfwConfidence >= 0.5f) {
            score += 20f // Medium region-based score
        }

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
        spatialDistribution: SpatialDistribution,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): UrgencyLevel {
        // NEW: Region-based urgency takes priority
        if (nsfwRegionCount >= 6 && maxNsfwConfidence >= 0.7f) {
            return UrgencyLevel.IMMEDIATE
        } else if (nsfwRegionCount >= 4 && maxNsfwConfidence >= 0.6f) {
            return UrgencyLevel.HIGH
        }

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
        spatialDistribution: SpatialDistribution,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): String {
        return when {
            // NEW: Region-based reasons (highest priority)
            nsfwRegionCount >= 10 && maxNsfwConfidence >= 0.8f -> "EXTREME: $nsfwRegionCount very high-confidence NSFW regions detected"
            nsfwRegionCount >= 6 && maxNsfwConfidence >= 0.7f -> "CRITICAL: $nsfwRegionCount high-confidence NSFW regions detected"
            nsfwRegionCount >= 4 && maxNsfwConfidence >= 0.6f -> "HIGH: $nsfwRegionCount moderate-confidence NSFW regions detected"
            nsfwRegionCount >= 2 && maxNsfwConfidence >= 0.5f -> "MODERATE: Multiple NSFW regions with $maxNsfwConfidence confidence"

            // Existing density-based reasons
            density >= CRITICAL_DENSITY_THRESHOLD -> "Critical content density: ${(density * 100).toInt()}%"
            criticalRegions > CRITICAL_REGIONS_THRESHOLD -> "High number of critical regions: $criticalRegions"
            spatialDistribution.isContentDistributed() -> "Inappropriate content distributed across screen"
            spatialDistribution.maxQuadrantDensity > 0.7f -> "High concentration in ${spatialDistribution.getHighestDensityQuadrant()} quadrant"
            density >= HIGH_DENSITY_THRESHOLD -> "High content density: ${(density * 100).toInt()}%"
            else -> "Moderate inappropriate content detected"
        }
    }
    
    private fun getSecondaryFactors(
        analysisResult: DensityAnalysisResult,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): List<String> {
        val factors = mutableListOf<String>()

        val density = analysisResult.inappropriateContentDensity
        val criticalRegions = analysisResult.criticalRegions.size
        val spatialDistribution = analysisResult.spatialDistribution
        val gridAnalysis = analysisResult.gridAnalysis

        // NEW: Region-based factors (highest priority)
        if (nsfwRegionCount > 0) {
            factors.add("$nsfwRegionCount NSFW regions detected")
        }
        if (maxNsfwConfidence > 0.5f) {
            factors.add("Max region confidence: ${(maxNsfwConfidence * 100).toInt()}%")
        }
        if (nsfwRegionCount >= 6) {
            factors.add("CRITICAL: Multiple NSFW regions (6+)")
        }

        // Existing density-based factors
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
    
    private fun calculateReflectionTime(
        warningLevel: WarningLevel,
        settings: AppSettings,
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f
    ): Int {
        val baseTime = settings.mandatoryReflectionTime

        // NEW: Region-based reflection time calculation (highest priority)
        if (nsfwRegionCount >= 10 && maxNsfwConfidence >= 0.8f) {
            return baseTime * 5 // Extreme case - 5x reflection time
        } else if (nsfwRegionCount >= 6 && maxNsfwConfidence >= 0.7f) {
            return baseTime * 4 // Critical case - 4x reflection time
        } else if (nsfwRegionCount >= 4 && maxNsfwConfidence >= 0.6f) {
            return baseTime * 3 // High case - 3x reflection time
        }

        // Existing density-based calculation
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
    
    /**
     * Make LLM-enhanced decision for fast, intelligent action selection
     * @param nsfwRegionCount Number of NSFW regions detected
     * @param maxNsfwConfidence Maximum confidence among NSFW regions
     * @param settings Current app settings
     * @param currentApp Current app package name (optional)
     * @return LLM decision result with enhanced reasoning
     */
    suspend fun makeLLMEnhancedDecision(
        nsfwRegionCount: Int,
        maxNsfwConfidence: Float,
        settings: AppSettings,
        currentApp: String = "browser"
    ): com.hieltech.haramblur.llm.LLMDecisionResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "🤖 Making LLM-enhanced decision for $nsfwRegionCount regions")
            
            // Determine content description based on region count and confidence
            val contentDescription = when {
                nsfwRegionCount >= 10 && maxNsfwConfidence >= 0.9f -> "extremely explicit web content"
                nsfwRegionCount >= 8 && maxNsfwConfidence >= 0.8f -> "highly inappropriate content"
                nsfwRegionCount >= 6 && maxNsfwConfidence >= 0.7f -> "multiple NSFW regions"
                nsfwRegionCount >= 4 && maxNsfwConfidence >= 0.6f -> "moderate inappropriate content"
                else -> "potentially inappropriate content"
            }
            
            // Get LLM decision with timeout
            val llmResult = withTimeoutOrNull(settings.llmTimeoutMs) {
                llmService.getFastDecision(
                    nsfwRegionCount = nsfwRegionCount,
                    maxConfidence = maxNsfwConfidence,
                    contentDescription = contentDescription,
                    currentApp = currentApp,
                    apiKey = settings.openRouterApiKey
                )
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            if (llmResult != null) {
                Log.d(TAG, "✅ LLM decision: ${llmResult.action} (${processingTime}ms)")
                return@withContext llmResult.copy(responseTimeMs = processingTime)
            } else {
                Log.w(TAG, "⏰ LLM timeout after ${settings.llmTimeoutMs}ms, using fallback")
                // Return fallback decision
                return@withContext createFallbackLLMDecision(nsfwRegionCount, maxNsfwConfidence, processingTime)
            }
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ LLM decision failed after ${processingTime}ms", e)
            return@withContext createFallbackLLMDecision(nsfwRegionCount, maxNsfwConfidence, processingTime)
        }
    }
    
    /**
     * Create fallback decision when LLM fails
     */
    private fun createFallbackLLMDecision(
        regionCount: Int, 
        confidence: Float, 
        processingTime: Long
    ): com.hieltech.haramblur.llm.LLMDecisionResult {
        val (action, reasoning, urgency) = when {
            regionCount >= 10 -> Triple(
                ContentAction.AUTO_CLOSE_APP,
                "Extreme content - close app",
                com.hieltech.haramblur.llm.UrgencyLevel.CRITICAL
            )
            regionCount >= 8 -> Triple(
                ContentAction.NAVIGATE_BACK,
                "High NSFW content - go back",
                com.hieltech.haramblur.llm.UrgencyLevel.HIGH
            )
            regionCount >= 6 -> Triple(
                ContentAction.SCROLL_AWAY,
                "Multiple regions - scroll away",
                com.hieltech.haramblur.llm.UrgencyLevel.MEDIUM
            )
            else -> Triple(
                ContentAction.SELECTIVE_BLUR,
                "Moderate content - selective blur",
                com.hieltech.haramblur.llm.UrgencyLevel.LOW
            )
        }
        
        return com.hieltech.haramblur.llm.LLMDecisionResult(
            action = action,
            reasoning = reasoning,
            confidence = 0.8f,
            urgency = urgency,
            isLLMDecision = false,
            responseTimeMs = processingTime
        )
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
    val reflectionTimeSeconds: Int,
    val useLLMDecision: Boolean = false // Whether to use LLM for enhanced decision making
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