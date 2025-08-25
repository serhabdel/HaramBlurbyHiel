package com.hieltech.haramblur.ui.effects

import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for EnhancedBlurEffects
 */
class EnhancedBlurEffectsTest {
    
    private lateinit var enhancedBlurEffects: EnhancedBlurEffects
    
    @Before
    fun setUp() {
        enhancedBlurEffects = EnhancedBlurEffects()
    }
    
    @Test
    fun `test blur intensity scaling for explicit content`() {
        val baseIntensity = BlurIntensity.MEDIUM
        val scaledIntensity = enhancedBlurEffects.getScaledIntensity(
            baseIntensity = baseIntensity,
            contentSensitivity = 0.9f,
            isExplicitContent = true,
            isFullScreen = false
        )
        
        // Should scale up to maximum for explicit content with high sensitivity
        assertEquals(BlurIntensity.MAXIMUM, scaledIntensity)
    }
    
    @Test
    fun `test blur intensity scaling for low sensitivity content`() {
        val baseIntensity = BlurIntensity.MEDIUM
        val scaledIntensity = enhancedBlurEffects.getScaledIntensity(
            baseIntensity = baseIntensity,
            contentSensitivity = 0.2f,
            isExplicitContent = false,
            isFullScreen = false
        )
        
        // Should remain the same for low sensitivity
        assertEquals(BlurIntensity.MEDIUM, scaledIntensity)
    }
    
    @Test
    fun `test blur intensity scaling for full screen`() {
        val baseIntensity = BlurIntensity.LIGHT
        val scaledIntensity = enhancedBlurEffects.getScaledIntensity(
            baseIntensity = baseIntensity,
            contentSensitivity = 0.5f,
            isExplicitContent = false,
            isFullScreen = true
        )
        
        // Should scale up for full screen
        assertTrue(scaledIntensity.alphaValue > baseIntensity.alphaValue)
    }
    
    @Test
    fun `test blur effectiveness validation for high privacy requirements`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.MAXIMUM,
            blurStyle = BlurStyle.COMBINED,
            contentSensitivity = 0.9f
        )
        
        assertTrue("Should be effective for high sensitivity content", result.isEffective)
        assertTrue("Privacy score should be high", result.privacyScore >= 0.8f)
        assertTrue("Overall score should be high", result.overallScore >= 0.8f)
    }
    
    @Test
    fun `test blur effectiveness validation for low privacy requirements`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.LIGHT,
            blurStyle = BlurStyle.SOLID,
            contentSensitivity = 0.2f
        )
        
        // Should still be reasonably effective for low sensitivity
        assertTrue("Privacy score should be reasonable", result.privacyScore >= 0.6f)
        assertTrue("Performance score should be high for solid style", result.performanceScore >= 0.9f)
    }
    
    @Test
    fun `test blur effectiveness recommendations for inadequate privacy`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.LIGHT,
            blurStyle = BlurStyle.SOLID,
            contentSensitivity = 0.9f
        )
        
        assertFalse("Should not be effective for high sensitivity with light blur", result.isEffective)
        assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
        assertTrue(
            "Should recommend stronger blur",
            result.recommendations.any { it.contains("stronger blur intensity") }
        )
    }
    
    @Test
    fun `test blur effectiveness for explicit content`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.MEDIUM,
            blurStyle = BlurStyle.PIXELATED,
            contentSensitivity = 0.85f
        )
        
        assertTrue(
            "Should recommend combined style for high sensitivity",
            result.recommendations.any { it.contains("COMBINED blur style") }
        )
    }
    
    @Test
    fun `test performance recommendations for complex blur styles`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.STRONG,
            blurStyle = BlurStyle.COMBINED,
            contentSensitivity = 0.3f
        )
        
        assertTrue(
            "Should recommend simpler style for better performance",
            result.recommendations.any { it.contains("SOLID or PIXELATED style") }
        )
    }
    
    @Test
    fun `test maximum privacy protection recommendation`() {
        val result = enhancedBlurEffects.validateBlurEffectiveness(
            blurIntensity = BlurIntensity.MEDIUM,
            blurStyle = BlurStyle.SOLID,
            contentSensitivity = 0.95f
        )
        
        assertTrue(
            "Should recommend maximum privacy for explicit content",
            result.recommendations.any { it.contains("maximum privacy protection") }
        )
    }
    
    @Test
    fun `test blur intensity progression`() {
        // Test that intensity scales appropriately
        val lightScaled = enhancedBlurEffects.getScaledIntensity(
            BlurIntensity.LIGHT, 0.5f, false, false
        )
        val mediumScaled = enhancedBlurEffects.getScaledIntensity(
            BlurIntensity.MEDIUM, 0.5f, false, false
        )
        val strongScaled = enhancedBlurEffects.getScaledIntensity(
            BlurIntensity.STRONG, 0.5f, false, false
        )
        
        // Should maintain or increase intensity, never decrease
        assertTrue(lightScaled.alphaValue >= BlurIntensity.LIGHT.alphaValue)
        assertTrue(mediumScaled.alphaValue >= BlurIntensity.MEDIUM.alphaValue)
        assertTrue(strongScaled.alphaValue >= BlurIntensity.STRONG.alphaValue)
    }
    
    @Test
    fun `test content sensitivity impact on scaling`() {
        val lowSensitivity = enhancedBlurEffects.getScaledIntensity(
            BlurIntensity.MEDIUM, 0.1f, false, false
        )
        val highSensitivity = enhancedBlurEffects.getScaledIntensity(
            BlurIntensity.MEDIUM, 0.9f, false, false
        )
        
        // High sensitivity should result in stronger blur
        assertTrue(
            "High sensitivity should result in stronger blur",
            highSensitivity.alphaValue >= lowSensitivity.alphaValue
        )
    }
}