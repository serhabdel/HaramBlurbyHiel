package com.hieltech.haramblur.accessibility

import android.graphics.Bitmap
import android.util.Log
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.detection.ContentAction
import com.hieltech.haramblur.utils.AppConstants.Tags

/**
 * Processes content detection results and determines blur/actions
 * Handles stability logic, consecutive detections, and sensitivity thresholds
 */
class DetectionProcessor(
    private val serviceLogger: ServiceLogger
) {
    private val TAG = Tags.ACCESSIBILITY_SERVICE
    
    // Detection tracking for stability
    private var consecutiveNSFWCount = 0
    private var consecutiveCleanCount = 0
    private val requiredConsecutiveDetections = 2
    
    // Adaptive thresholds
    private var adaptiveNSFWThreshold = 0.4f
    private var adaptiveGenderThreshold = 0.4f
    private var detectionHistory = mutableListOf<Pair<Long, Boolean>>()
    private var lastAdaptationTime = 0L
    private val adaptationIntervalMs = 30000L
    
    // State tracking (managed externally via callbacks)
    private var isCurrentlyBlurred = false
    private var lastBlurStartTime = 0L
    private var minBlurDuration = 2000L
    
    /**
     * Data class for detection processing result
     */
    data class ProcessingResult(
        val shouldShowBlur: Boolean,
        val shouldShowFullScreenWarning: Boolean,
        val recommendedAction: ContentAction?,
        val hasFemaleFaces: Boolean,
        val hasNSFWContent: Boolean,
        val blurRegions: List<android.graphics.Rect>,
        val nsfwRegionCount: Int,
        val maxNsfwConfidence: Float
    )
    
    /**
     * Process detection result with stability logic
     */
    fun processDetectionResult(
        result: ContentDetectionEngine.ContentAnalysisResult,
        settings: AppSettings,
        currentTime: Long,
        isCurrentlyBlurredState: Boolean,
        lastBlurStartTimeState: Long
    ): ProcessingResult {
        
        this.isCurrentlyBlurred = isCurrentlyBlurredState
        this.lastBlurStartTime = lastBlurStartTimeState
        
        Log.d(TAG, "📊 Analysis result: shouldBlur=${result.shouldBlur}, regions=${result.blurRegions.size}")
        
        // Adapt thresholds based on learning
        adaptThresholds(currentTime, settings)
        
        // Use adaptive thresholds
        val nsfwThreshold = minOf(adaptiveNSFWThreshold, settings.nsfwConfidenceThreshold)
        val genderThreshold = minOf(adaptiveGenderThreshold, settings.genderConfidenceThreshold)
        
        Log.d(TAG, "🧠 Using adaptive thresholds: NSFW=$nsfwThreshold, Gender=$genderThreshold")
        
        // Analyze female faces
        val hasFemaleFaces = analyzeFemaleFaces(result, settings, genderThreshold)
        
        // Analyze NSFW content
        val hasNSFWContent = analyzeNSFWContent(result, settings, nsfwThreshold)
        
        Log.d(TAG, "🔍 Detection summary: Female faces=$hasFemaleFaces, NSFW=$hasNSFWContent")
        
        // Record for learning
        val detectedInappropriate = hasFemaleFaces || hasNSFWContent
        detectionHistory.add(Pair(currentTime, detectedInappropriate))
        
        // Determine blur based on content
        val shouldBlurBasedOnContent = determineBlurBasedOnContent(
            hasFemaleFaces, hasNSFWContent, settings, result
        )
        
        // Update consecutive counters
        updateConsecutiveCounters(shouldBlurBasedOnContent)
        
        // Determine final blur decision
        val shouldShowBlur = determineShowBlur(
            shouldBlurBasedOnContent, currentTime
        )
        
        // Determine action
        val recommendedAction = result.fullScreenBlurDecision?.recommendedAction
        
        return ProcessingResult(
            shouldShowBlur = shouldShowBlur,
            shouldShowFullScreenWarning = result.requiresFullScreenWarning && shouldShowBlur,
            recommendedAction = recommendedAction,
            hasFemaleFaces = hasFemaleFaces,
            hasNSFWContent = hasNSFWContent,
            blurRegions = result.blurRegions,
            nsfwRegionCount = result.nsfwRegionCount,
            maxNsfwConfidence = result.maxNsfwConfidence
        )
    }
    
    /**
     * Analyze female faces in detection result
     */
    private fun analyzeFemaleFaces(
        result: ContentDetectionEngine.ContentAnalysisResult,
        settings: AppSettings,
        genderThreshold: Float
    ): Boolean {
        if (!settings.blurFemaleFaces) {
            Log.d(TAG, "👩 Female face detection disabled in settings")
            return false
        }
        
        return result.faceDetectionResult?.detectedFaces?.any { face ->
            val isConfidentFemale = face.genderConfidence > genderThreshold &&
                    face.estimatedGender.toString().contains("FEMALE", ignoreCase = true)
            
            val isModerateConfidenceMale = face.estimatedGender.toString().contains("MALE", ignoreCase = true) &&
                    face.genderConfidence >= 0.4f &&
                    face.genderConfidence <= 0.8f
            
            val isLowConfidenceUnknown = face.genderConfidence < 0.4f
            val isUnknownGender = face.estimatedGender.toString().contains("UNKNOWN", ignoreCase = true)
            
            serviceLogger.debug(
                "👩 Female analysis: confidence=${face.genderConfidence}, gender=${face.estimatedGender}",
                LogRepository.LogCategory.DETECTION
            )
            
            isConfidentFemale ||
            (isModerateConfidenceMale && settings.detectionSensitivity > 0.5f) ||
            (isLowConfidenceUnknown && settings.detectionSensitivity > 0.6f) ||
            (isUnknownGender && settings.detectionSensitivity > 0.4f)
        } ?: false
    }
    
    /**
     * Analyze NSFW content in detection result
     */
    private fun analyzeNSFWContent(
        result: ContentDetectionEngine.ContentAnalysisResult,
        settings: AppSettings,
        nsfwThreshold: Float
    ): Boolean {
        return result.nsfwDetectionResult?.let { nsfwResult ->
            val isHighConfidenceNSFW = nsfwResult.isNSFW && nsfwResult.confidence > nsfwThreshold
            val isMediumConfidenceNSFW = nsfwResult.confidence > (nsfwThreshold * 0.7f)
            val isAnyNSFWIndicator = nsfwResult.confidence > 0.2f
            
            Log.d(TAG, "🔞 NSFW analysis: confidence=${nsfwResult.confidence}, threshold=$nsfwThreshold")
            
            serviceLogger.debug(
                "🔞 NSFW analysis: confidence=${nsfwResult.confidence}, isNSFW=${nsfwResult.isNSFW}",
                LogRepository.LogCategory.DETECTION
            )
            
            when {
                isHighConfidenceNSFW -> true
                isMediumConfidenceNSFW && settings.detectionSensitivity > 0.6f -> true
                isAnyNSFWIndicator && settings.detectionSensitivity > 0.8f -> true
                else -> false
            }
        } ?: false
    }
    
    /**
     * Determine if blur should be applied based on content
     */
    private fun determineBlurBasedOnContent(
        hasFemaleFaces: Boolean,
        hasNSFWContent: Boolean,
        settings: AppSettings,
        result: ContentDetectionEngine.ContentAnalysisResult
    ): Boolean {
        return when {
            hasFemaleFaces && settings.blurFemaleFaces -> {
                Log.d(TAG, "👩 ❗ Female face detected - TRIGGERING BLUR")
                true
            }
            hasNSFWContent && settings.enableNSFWDetection -> {
                Log.d(TAG, "🔞 ❗ NSFW content detected - TRIGGERING BLUR")
                true
            }
            result.blurRegions.isNotEmpty() -> {
                Log.d(TAG, "⚠️ Fallback - blur regions detected")
                true
            }
            else -> {
                Log.d(TAG, "✅ No inappropriate content detected")
                false
            }
        }
    }
    
    /**
     * Update consecutive detection counters
     */
    private fun updateConsecutiveCounters(shouldBlurBasedOnContent: Boolean) {
        if (shouldBlurBasedOnContent) {
            consecutiveNSFWCount++
            consecutiveCleanCount = 0
            Log.d(TAG, "🔴 Inappropriate content count: $consecutiveNSFWCount")
        } else {
            consecutiveCleanCount++
            consecutiveNSFWCount = 0
            Log.d(TAG, "✅ Clean content count: $consecutiveCleanCount")
        }
    }
    
    /**
     * Determine if blur should be shown based on consecutive detections
     */
    private fun determineShowBlur(
        shouldBlurBasedOnContent: Boolean,
        currentTime: Long
    ): Boolean {
        return when {
            shouldBlurBasedOnContent && consecutiveNSFWCount >= requiredConsecutiveDetections -> {
                if (!isCurrentlyBlurred) {
                    Log.w(TAG, "🛑 ⚡ BLUR TRIGGERED - $consecutiveNSFWCount consecutive detections!")
                    isCurrentlyBlurred = true
                    lastBlurStartTime = currentTime
                }
                true
            }
            isCurrentlyBlurred -> {
                val timeSinceBlurStart = currentTime - lastBlurStartTime
                if (timeSinceBlurStart < minBlurDuration) {
                    Log.d(TAG, "⏰ Maintaining blur (duration: ${timeSinceBlurStart}ms)")
                    true
                } else {
                    Log.d(TAG, "🔓 Stopping blur - content appears clean")
                    isCurrentlyBlurred = false
                    false
                }
            }
            else -> false
        }
    }
    
    /**
     * Adapt thresholds based on detection history
     */
    private fun adaptThresholds(currentTime: Long, settings: AppSettings) {
        if (currentTime - lastAdaptationTime < adaptationIntervalMs) return
        
        lastAdaptationTime = currentTime
        
        // Clean old history
        detectionHistory.removeAll { (timestamp, _) ->
            currentTime - timestamp > 60000 // 1 minute
        }
        
        if (detectionHistory.size < 10) return
        
        val detectionRate = detectionHistory.count { it.second }.toFloat() / detectionHistory.size
        
        // Adapt based on detection rate
        when {
            detectionRate > 0.7f -> {
                adaptiveNSFWThreshold = minOf(adaptiveNSFWThreshold * 1.1f, 0.6f)
                adaptiveGenderThreshold = minOf(adaptiveGenderThreshold * 1.1f, 0.6f)
                Log.d(TAG, "📈 High detection rate ($detectionRate), increasing thresholds")
            }
            detectionRate < 0.2f -> {
                adaptiveNSFWThreshold = maxOf(adaptiveNSFWThreshold * 0.9f, 0.3f)
                adaptiveGenderThreshold = maxOf(adaptiveGenderThreshold * 0.9f, 0.3f)
                Log.d(TAG, "📉 Low detection rate ($detectionRate), decreasing thresholds")
            }
        }
    }
    
    /**
     * Reset detection state
     */
    fun reset() {
        consecutiveNSFWCount = 0
        consecutiveCleanCount = 0
        isCurrentlyBlurred = false
        detectionHistory.clear()
        Log.d(TAG, "🔄 Detection processor state reset")
    }
    
    fun getConsecutiveNSFWCount(): Int = consecutiveNSFWCount
    fun isCurrentlyBlurred(): Boolean = isCurrentlyBlurred
    fun getLastBlurStartTime(): Long = lastBlurStartTime
}
