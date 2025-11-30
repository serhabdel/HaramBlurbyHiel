package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.GenderAccuracy
import com.hieltech.haramblur.data.QualityMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DetectionThresholds centralized configuration.
 * 
 * These tests verify:
 * 1. Default threshold values are within acceptable ranges
 * 2. Dynamic threshold calculation respects bounds
 * 3. Quality mode adjustments work correctly
 * 4. Validation catches out-of-range values
 */
class DetectionThresholdsTest {

    // ============================================
    // Default Value Tests
    // ============================================

    @Test
    fun `default NSFW threshold is within acceptable range`() {
        assertTrue(
            "Default NSFW threshold should be >= MIN",
            DetectionThresholds.DEFAULT_NSFW_THRESHOLD >= DetectionThresholds.MIN_NSFW_THRESHOLD
        )
        assertTrue(
            "Default NSFW threshold should be <= MAX",
            DetectionThresholds.DEFAULT_NSFW_THRESHOLD <= DetectionThresholds.MAX_NSFW_THRESHOLD
        )
    }

    @Test
    fun `default gender threshold is within acceptable range`() {
        assertTrue(
            "Default gender threshold should be >= MIN",
            DetectionThresholds.DEFAULT_GENDER_THRESHOLD >= DetectionThresholds.MIN_GENDER_THRESHOLD
        )
        assertTrue(
            "Default gender threshold should be <= MAX",
            DetectionThresholds.DEFAULT_GENDER_THRESHOLD <= DetectionThresholds.MAX_GENDER_THRESHOLD
        )
    }

    @Test
    fun `high confidence NSFW threshold is higher than default`() {
        assertTrue(
            "High confidence threshold should be > default",
            DetectionThresholds.HIGH_CONFIDENCE_NSFW_THRESHOLD > DetectionThresholds.DEFAULT_NSFW_THRESHOLD
        )
    }

    @Test
    fun `male classification threshold is higher than female`() {
        assertTrue(
            "Male threshold should be >= female threshold for Islamic compliance",
            DetectionThresholds.MALE_CLASSIFICATION_THRESHOLD >= DetectionThresholds.FEMALE_CLASSIFICATION_THRESHOLD
        )
    }

    // ============================================
    // Dynamic Threshold Calculation Tests
    // ============================================

    @Test
    fun `getEffectiveNsfwThreshold respects minimum bound`() {
        val settings = createSettingsWithNsfwThreshold(0.10f) // Below minimum
        val effective = DetectionThresholds.getEffectiveNsfwThreshold(settings)
        
        assertEquals(
            "Should clamp to minimum threshold",
            DetectionThresholds.MIN_NSFW_THRESHOLD,
            effective,
            0.001f
        )
    }

    @Test
    fun `getEffectiveNsfwThreshold respects maximum bound`() {
        val settings = createSettingsWithNsfwThreshold(0.95f) // Above maximum
        val effective = DetectionThresholds.getEffectiveNsfwThreshold(settings)
        
        assertEquals(
            "Should clamp to maximum threshold",
            DetectionThresholds.MAX_NSFW_THRESHOLD,
            effective,
            0.001f
        )
    }

    @Test
    fun `getEffectiveNsfwThreshold passes through valid values`() {
        val validThreshold = 0.50f
        val settings = createSettingsWithNsfwThreshold(validThreshold)
        val effective = DetectionThresholds.getEffectiveNsfwThreshold(settings)
        
        assertEquals(
            "Should pass through valid threshold unchanged",
            validThreshold,
            effective,
            0.001f
        )
    }

    @Test
    fun `getEffectiveGenderThreshold varies by accuracy mode`() {
        val highAccuracySettings = createSettingsWithGenderAccuracy(GenderAccuracy.HIGH)
        val balancedSettings = createSettingsWithGenderAccuracy(GenderAccuracy.BALANCED)
        val fastSettings = createSettingsWithGenderAccuracy(GenderAccuracy.FAST)
        
        val highThreshold = DetectionThresholds.getEffectiveGenderThreshold(highAccuracySettings)
        val balancedThreshold = DetectionThresholds.getEffectiveGenderThreshold(balancedSettings)
        val fastThreshold = DetectionThresholds.getEffectiveGenderThreshold(fastSettings)
        
        // HIGH accuracy should have lower threshold (more sensitive)
        assertTrue(
            "HIGH accuracy should have lower threshold than BALANCED",
            highThreshold < balancedThreshold
        )
        
        // FAST should have higher threshold (less sensitive)
        assertTrue(
            "FAST should have higher threshold than BALANCED",
            fastThreshold > balancedThreshold
        )
    }

    // ============================================
    // Quality Mode Adjustment Tests
    // ============================================

    @Test
    fun `battery saver mode uses higher thresholds`() {
        val batterySaverSettings = createSettingsWithQualityMode(QualityMode.BATTERY_SAVER)
        val balancedSettings = createSettingsWithQualityMode(QualityMode.BALANCED)
        
        val batterySaverThresholds = DetectionThresholds.getQualityAdjustedThresholds(batterySaverSettings)
        val balancedThresholds = DetectionThresholds.getQualityAdjustedThresholds(balancedSettings)
        
        assertTrue(
            "Battery saver should have higher NSFW threshold",
            batterySaverThresholds.nsfwThreshold >= balancedThresholds.nsfwThreshold
        )
    }

    @Test
    fun `maximum protection mode uses minimum thresholds`() {
        val maxProtectionSettings = createSettingsWithQualityMode(QualityMode.MAXIMUM_PROTECTION)
        val thresholds = DetectionThresholds.getQualityAdjustedThresholds(maxProtectionSettings)
        
        assertEquals(
            "Maximum protection should use minimum NSFW threshold",
            DetectionThresholds.MIN_NSFW_THRESHOLD,
            thresholds.nsfwThreshold,
            0.001f
        )
        
        assertEquals(
            "Maximum protection should use minimum gender threshold",
            DetectionThresholds.MIN_GENDER_THRESHOLD,
            thresholds.genderThreshold,
            0.001f
        )
    }

    @Test
    fun `high quality mode uses slightly lower thresholds`() {
        val highQualitySettings = createSettingsWithQualityMode(QualityMode.HIGH_QUALITY)
        val balancedSettings = createSettingsWithQualityMode(QualityMode.BALANCED)
        
        val highQualityThresholds = DetectionThresholds.getQualityAdjustedThresholds(highQualitySettings)
        val balancedThresholds = DetectionThresholds.getQualityAdjustedThresholds(balancedSettings)
        
        assertTrue(
            "High quality should have lower or equal NSFW threshold",
            highQualityThresholds.nsfwThreshold <= balancedThresholds.nsfwThreshold
        )
    }

    // ============================================
    // Validation Tests
    // ============================================

    @Test
    fun `validateThresholds returns empty for valid settings`() {
        val validSettings = AppSettings(
            nsfwConfidenceThreshold = 0.50f,
            genderConfidenceThreshold = 0.55f
        )
        
        val warnings = DetectionThresholds.validateThresholds(validSettings)
        
        assertTrue(
            "Valid settings should produce no warnings",
            warnings.isEmpty()
        )
    }

    @Test
    fun `validateThresholds warns for too-low NSFW threshold`() {
        val lowThresholdSettings = AppSettings(
            nsfwConfidenceThreshold = 0.15f // Below minimum
        )
        
        val warnings = DetectionThresholds.validateThresholds(lowThresholdSettings)
        
        assertTrue(
            "Should warn about low NSFW threshold",
            warnings.any { it.contains("NSFW") && it.contains("below") }
        )
    }

    @Test
    fun `validateThresholds warns for too-high NSFW threshold`() {
        val highThresholdSettings = AppSettings(
            nsfwConfidenceThreshold = 0.85f // Above maximum
        )
        
        val warnings = DetectionThresholds.validateThresholds(highThresholdSettings)
        
        assertTrue(
            "Should warn about high NSFW threshold",
            warnings.any { it.contains("NSFW") && it.contains("above") }
        )
    }

    @Test
    fun `validateThresholds warns for too-low gender threshold`() {
        val lowThresholdSettings = AppSettings(
            genderConfidenceThreshold = 0.20f // Below minimum
        )
        
        val warnings = DetectionThresholds.validateThresholds(lowThresholdSettings)
        
        assertTrue(
            "Should warn about low gender threshold",
            warnings.any { it.contains("Gender") && it.contains("below") }
        )
    }

    // ============================================
    // ThresholdSet Tests
    // ============================================

    @Test
    fun `ThresholdSet contains all required fields`() {
        val thresholdSet = DetectionThresholds.ThresholdSet(
            nsfwThreshold = 0.5f,
            genderThreshold = 0.5f,
            skinToneThreshold = 0.25f,
            fullScreenDensity = 0.35f
        )
        
        assertNotNull("nsfwThreshold should not be null", thresholdSet.nsfwThreshold)
        assertNotNull("genderThreshold should not be null", thresholdSet.genderThreshold)
        assertNotNull("skinToneThreshold should not be null", thresholdSet.skinToneThreshold)
        assertNotNull("fullScreenDensity should not be null", thresholdSet.fullScreenDensity)
    }

    // ============================================
    // Helper Methods
    // ============================================

    private fun createSettingsWithNsfwThreshold(threshold: Float): AppSettings {
        return AppSettings(nsfwConfidenceThreshold = threshold)
    }

    private fun createSettingsWithGenderAccuracy(accuracy: GenderAccuracy): AppSettings {
        return AppSettings(
            genderDetectionAccuracy = accuracy,
            genderConfidenceThreshold = 0f // Use 0 to let accuracy mode determine threshold
        )
    }

    private fun createSettingsWithQualityMode(mode: QualityMode): AppSettings {
        return AppSettings(qualityMode = mode)
    }
}
