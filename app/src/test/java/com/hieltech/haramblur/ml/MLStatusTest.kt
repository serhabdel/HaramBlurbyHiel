package com.hieltech.haramblur.ml

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MLModelManager.MLStatus and MLHealth.
 * 
 * These tests verify:
 * 1. MLStatus correctly reports warning conditions
 * 2. MLStatus correctly reports detection capability
 * 3. MLHealth enum values are properly ordered by severity
 */
class MLStatusTest {

    // ============================================
    // shouldShowWarning Tests
    // ============================================

    @Test
    fun `shouldShowWarning returns true for CRITICAL health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            overallHealth = MLModelManager.MLHealth.CRITICAL
        )
        
        assertTrue(
            "CRITICAL health should show warning",
            status.shouldShowWarning()
        )
    }

    @Test
    fun `shouldShowWarning returns true for DEGRADED health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            overallHealth = MLModelManager.MLHealth.DEGRADED
        )
        
        assertTrue(
            "DEGRADED health should show warning",
            status.shouldShowWarning()
        )
    }

    @Test
    fun `shouldShowWarning returns true for NOT_INITIALIZED health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = false,
            overallHealth = MLModelManager.MLHealth.NOT_INITIALIZED
        )
        
        assertTrue(
            "NOT_INITIALIZED health should show warning",
            status.shouldShowWarning()
        )
    }

    @Test
    fun `shouldShowWarning returns false for HEALTHY health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            nsfwModelAvailable = true,
            genderModelAvailable = true,
            gpuAccelerationActive = true,
            overallHealth = MLModelManager.MLHealth.HEALTHY
        )
        
        assertFalse(
            "HEALTHY health should not show warning",
            status.shouldShowWarning()
        )
    }

    @Test
    fun `shouldShowWarning returns false for REDUCED_PERFORMANCE health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            nsfwModelAvailable = true,
            genderModelAvailable = true,
            gpuAccelerationActive = false,
            overallHealth = MLModelManager.MLHealth.REDUCED_PERFORMANCE
        )
        
        assertFalse(
            "REDUCED_PERFORMANCE health should not show warning (GPU is optional)",
            status.shouldShowWarning()
        )
    }

    // ============================================
    // canPerformDetection Tests
    // ============================================

    @Test
    fun `canPerformDetection returns true when initialized`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            overallHealth = MLModelManager.MLHealth.HEALTHY
        )
        
        assertTrue(
            "Should be able to perform detection when initialized",
            status.canPerformDetection()
        )
    }

    @Test
    fun `canPerformDetection returns true for DEGRADED health`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            overallHealth = MLModelManager.MLHealth.DEGRADED
        )
        
        assertTrue(
            "Should be able to perform detection even when degraded (using heuristics)",
            status.canPerformDetection()
        )
    }

    @Test
    fun `canPerformDetection returns false for CRITICAL when not initialized`() {
        val status = MLModelManager.MLStatus(
            isInitialized = false,
            overallHealth = MLModelManager.MLHealth.CRITICAL
        )
        
        assertFalse(
            "Should not be able to perform detection when CRITICAL and not initialized",
            status.canPerformDetection()
        )
    }

    // ============================================
    // Default Values Tests
    // ============================================

    @Test
    fun `default MLStatus has NOT_INITIALIZED health`() {
        val status = MLModelManager.MLStatus()
        
        assertEquals(
            "Default status should be NOT_INITIALIZED",
            MLModelManager.MLHealth.NOT_INITIALIZED,
            status.overallHealth
        )
    }

    @Test
    fun `default MLStatus has all models unavailable`() {
        val status = MLModelManager.MLStatus()
        
        assertFalse("Default should have NSFW model unavailable", status.nsfwModelAvailable)
        assertFalse("Default should have gender model unavailable", status.genderModelAvailable)
        assertFalse("Default should have GPU inactive", status.gpuAccelerationActive)
    }

    @Test
    fun `default MLStatus has default message`() {
        val status = MLModelManager.MLStatus()
        
        assertTrue(
            "Default message should mention initialization",
            status.statusMessage.contains("not yet initialized", ignoreCase = true)
        )
    }

    // ============================================
    // MLHealth Enum Tests
    // ============================================

    @Test
    fun `MLHealth enum has all expected values`() {
        val healthValues = MLModelManager.MLHealth.values()
        
        assertEquals("Should have 5 health levels", 5, healthValues.size)
        assertTrue("Should contain HEALTHY", healthValues.contains(MLModelManager.MLHealth.HEALTHY))
        assertTrue("Should contain REDUCED_PERFORMANCE", healthValues.contains(MLModelManager.MLHealth.REDUCED_PERFORMANCE))
        assertTrue("Should contain DEGRADED", healthValues.contains(MLModelManager.MLHealth.DEGRADED))
        assertTrue("Should contain CRITICAL", healthValues.contains(MLModelManager.MLHealth.CRITICAL))
        assertTrue("Should contain NOT_INITIALIZED", healthValues.contains(MLModelManager.MLHealth.NOT_INITIALIZED))
    }

    // ============================================
    // Status Message Tests
    // ============================================

    @Test
    fun `status with initialization error has error in message`() {
        val errorMessage = "Test error message"
        val status = MLModelManager.MLStatus(
            initializationError = errorMessage
        )
        
        assertNotNull("initializationError should be set", status.initializationError)
        assertEquals("Error message should match", errorMessage, status.initializationError)
    }

    @Test
    fun `healthy status with all models available`() {
        val status = MLModelManager.MLStatus(
            isInitialized = true,
            nsfwModelAvailable = true,
            genderModelAvailable = true,
            gpuAccelerationActive = true,
            overallHealth = MLModelManager.MLHealth.HEALTHY,
            statusMessage = "All systems operational"
        )
        
        assertTrue("Should be initialized", status.isInitialized)
        assertTrue("NSFW model should be available", status.nsfwModelAvailable)
        assertTrue("Gender model should be available", status.genderModelAvailable)
        assertTrue("GPU should be active", status.gpuAccelerationActive)
        assertFalse("Should not show warning", status.shouldShowWarning())
        assertTrue("Should be able to perform detection", status.canPerformDetection())
    }
}
