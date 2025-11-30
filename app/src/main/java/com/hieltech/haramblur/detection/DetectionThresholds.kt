package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.GenderAccuracy
import com.hieltech.haramblur.data.QualityMode

/**
 * Centralized detection thresholds configuration.
 * 
 * This object provides a single source of truth for all detection thresholds,
 * combining static defaults with user-configurable values from AppSettings.
 * 
 * ## Threshold Philosophy
 * - **Higher thresholds** = fewer false positives, but may miss some content
 * - **Lower thresholds** = more sensitive detection, but more false positives
 * 
 * ## Recommended Ranges
 * - NSFW: 0.35-0.60 (default 0.45)
 * - Gender: 0.45-0.70 (default 0.55)
 * - Skin tone: 0.20-0.40 (default 0.25)
 */
object DetectionThresholds {
    
    // ============================================
    // NSFW Detection Thresholds
    // ============================================
    
    /**
     * Default NSFW confidence threshold.
     * Values below this are considered safe.
     * 
     * Note: Previous ultra-low values (0.20-0.25) caused excessive false positives.
     * This balanced default reduces false positives while maintaining good detection.
     */
    const val DEFAULT_NSFW_THRESHOLD = 0.45f
    
    /**
     * Minimum allowed NSFW threshold (most sensitive).
     * Going below this causes too many false positives.
     */
    const val MIN_NSFW_THRESHOLD = 0.30f
    
    /**
     * Maximum allowed NSFW threshold (least sensitive).
     * Going above this may miss actual inappropriate content.
     */
    const val MAX_NSFW_THRESHOLD = 0.70f
    
    /**
     * High-confidence NSFW threshold for triggering full-screen blur.
     * Only trigger full-screen blur when very confident.
     */
    const val HIGH_CONFIDENCE_NSFW_THRESHOLD = 0.75f
    
    /**
     * Region-based NSFW threshold (slightly lower for granular detection).
     */
    const val REGION_NSFW_THRESHOLD = 0.40f
    
    // ============================================
    // Gender Detection Thresholds
    // ============================================
    
    /**
     * Default gender confidence threshold.
     * Faces with confidence below this are classified as UNKNOWN.
     */
    const val DEFAULT_GENDER_THRESHOLD = 0.55f
    
    /**
     * Minimum allowed gender threshold.
     */
    const val MIN_GENDER_THRESHOLD = 0.35f
    
    /**
     * Maximum allowed gender threshold.
     */
    const val MAX_GENDER_THRESHOLD = 0.80f
    
    /**
     * Male classification threshold.
     * Higher threshold for male to reduce false positives on female detection.
     */
    const val MALE_CLASSIFICATION_THRESHOLD = 0.70f
    
    /**
     * Female classification threshold.
     * Slightly lower to be more sensitive to female faces (Islamic compliance).
     */
    const val FEMALE_CLASSIFICATION_THRESHOLD = 0.50f
    
    // ============================================
    // Skin Tone Detection Thresholds
    // ============================================
    
    /**
     * Skin tone ratio threshold for triggering additional analysis.
     */
    const val SKIN_TONE_THRESHOLD = 0.25f
    
    /**
     * High skin tone ratio that may indicate inappropriate content.
     */
    const val HIGH_SKIN_TONE_THRESHOLD = 0.40f
    
    // ============================================
    // Content Density Thresholds
    // ============================================
    
    /**
     * Percentage of screen with inappropriate content to trigger full-screen blur.
     */
    const val FULL_SCREEN_DENSITY_THRESHOLD = 0.35f
    
    /**
     * Critical content density threshold.
     */
    const val CRITICAL_DENSITY_THRESHOLD = 0.60f
    
    /**
     * Warning level content density threshold.
     */
    const val WARNING_DENSITY_THRESHOLD = 0.25f
    
    // ============================================
    // Face Detection Thresholds
    // ============================================
    
    /**
     * Minimum face size as percentage of screen to consider for detection.
     * Faces smaller than this are likely too small to accurately classify.
     */
    const val MIN_FACE_SIZE_PERCENT = 0.02f // 2% of screen
    
    /**
     * Maximum number of faces to process per frame.
     */
    const val MAX_FACES_PER_FRAME = 10
    
    /**
     * Minimum face detection confidence from ML Kit.
     */
    const val MIN_FACE_DETECTION_CONFIDENCE = 0.5f
    
    // ============================================
    // Dynamic Threshold Calculation
    // ============================================
    
    /**
     * Get effective NSFW threshold based on user settings.
     */
    fun getEffectiveNsfwThreshold(settings: AppSettings): Float {
        val userThreshold = settings.nsfwConfidenceThreshold
        return userThreshold.coerceIn(MIN_NSFW_THRESHOLD, MAX_NSFW_THRESHOLD)
    }
    
    /**
     * Get effective gender threshold based on user settings and accuracy mode.
     */
    fun getEffectiveGenderThreshold(settings: AppSettings): Float {
        val baseThreshold = when (settings.genderDetectionAccuracy) {
            GenderAccuracy.HIGH -> 0.45f      // More sensitive
            GenderAccuracy.BALANCED -> 0.55f  // Default
            GenderAccuracy.FAST -> 0.65f      // Less sensitive, faster
        }
        
        // Allow user override within safe bounds
        val userThreshold = settings.genderConfidenceThreshold
        return if (userThreshold > 0f) {
            userThreshold.coerceIn(MIN_GENDER_THRESHOLD, MAX_GENDER_THRESHOLD)
        } else {
            baseThreshold
        }
    }
    
    /**
     * Get thresholds adjusted for quality mode.
     */
    fun getQualityAdjustedThresholds(settings: AppSettings): ThresholdSet {
        return when (settings.qualityMode) {
            QualityMode.MAXIMUM_PRECISION -> ThresholdSet(
                // Lowest thresholds for maximum detection sensitivity
                nsfwThreshold = (getEffectiveNsfwThreshold(settings) * 0.8f).coerceAtLeast(MIN_NSFW_THRESHOLD),
                genderThreshold = (getEffectiveGenderThreshold(settings) * 0.8f).coerceAtLeast(MIN_GENDER_THRESHOLD),
                skinToneThreshold = SKIN_TONE_THRESHOLD * 0.7f,
                fullScreenDensity = FULL_SCREEN_DENSITY_THRESHOLD * 0.75f
            )
            QualityMode.BATTERY_SAVER -> ThresholdSet(
                nsfwThreshold = 0.55f,      // Higher = fewer detections = less processing
                genderThreshold = 0.65f,
                skinToneThreshold = 0.35f,
                fullScreenDensity = 0.45f
            )
            QualityMode.BALANCED -> ThresholdSet(
                nsfwThreshold = getEffectiveNsfwThreshold(settings),
                genderThreshold = getEffectiveGenderThreshold(settings),
                skinToneThreshold = SKIN_TONE_THRESHOLD,
                fullScreenDensity = FULL_SCREEN_DENSITY_THRESHOLD
            )
            QualityMode.HIGH_QUALITY -> ThresholdSet(
                nsfwThreshold = (getEffectiveNsfwThreshold(settings) * 0.9f).coerceAtLeast(MIN_NSFW_THRESHOLD),
                genderThreshold = (getEffectiveGenderThreshold(settings) * 0.9f).coerceAtLeast(MIN_GENDER_THRESHOLD),
                skinToneThreshold = SKIN_TONE_THRESHOLD * 0.8f,
                fullScreenDensity = FULL_SCREEN_DENSITY_THRESHOLD * 0.85f
            )
        }
    }
    
    /**
     * Data class holding a complete set of thresholds for a detection pass.
     */
    data class ThresholdSet(
        val nsfwThreshold: Float,
        val genderThreshold: Float,
        val skinToneThreshold: Float,
        val fullScreenDensity: Float
    )
    
    // ============================================
    // Validation & Logging
    // ============================================
    
    /**
     * Validate that thresholds are within acceptable ranges.
     * Returns list of validation warnings.
     */
    fun validateThresholds(settings: AppSettings): List<String> {
        val warnings = mutableListOf<String>()
        
        if (settings.nsfwConfidenceThreshold < MIN_NSFW_THRESHOLD) {
            warnings.add("NSFW threshold (${settings.nsfwConfidenceThreshold}) is below minimum ($MIN_NSFW_THRESHOLD) - may cause excessive false positives")
        }
        
        if (settings.genderConfidenceThreshold < MIN_GENDER_THRESHOLD) {
            warnings.add("Gender threshold (${settings.genderConfidenceThreshold}) is below minimum ($MIN_GENDER_THRESHOLD) - may cause excessive false positives")
        }
        
        if (settings.nsfwConfidenceThreshold > MAX_NSFW_THRESHOLD) {
            warnings.add("NSFW threshold (${settings.nsfwConfidenceThreshold}) is above maximum ($MAX_NSFW_THRESHOLD) - may miss inappropriate content")
        }
        
        return warnings
    }
}
