package com.hieltech.haramblur.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.ui.components.GenderSelectionStep
import com.hieltech.haramblur.ui.components.IslamicOnboardingStep
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import org.junit.Assert.*
import org.robolectric.annotation.Config

/**
 * Comprehensive test plan for verifying first-time installation flow fixes
 * 
 * This test class verifies that:
 * 1. Gender selection only shows male/female options (no third option)
 * 2. Islamic features step shows only "Complete" button (not "Continue" and "Skip")
 * 3. High quality content detection is properly enabled after wizard completion
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class FirstTimeInstallationTestPlan {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var viewModel: PermissionWizardViewModel

    @Before
    fun setup() {
        // Reset settings before each test
        settingsRepository.resetOnboarding()
    }

    /**
     * Test 1: Verify gender selection only shows male/female options
     */
    @Test
    fun testGenderSelectionOnlyShowsMaleFemaleOptions() {
        composeTestRule.setContent {
            GenderSelectionStep(
                viewModel = viewModel,
                onGenderSelected = {}
            )
        }

        // Verify that only two gender options are shown
        composeTestRule
            .onAllNodesWithText("🧔 Male")
            .assertCountEquals(1)
        
        composeTestRule
            .onAllNodesWithText("👩 Female")
            .assertCountEquals(1)
        
        // Verify that "Prefer not to say" option is NOT shown
        composeTestRule
            .onAllNodesWithText("❓ Prefer not to say")
            .assertCountEquals(0)
        
        // Verify that "Skip" button is NOT shown
        composeTestRule
            .onAllNodesWithText("Skip (use safest settings)")
            .assertCountEquals(0)
        
        // Verify that "Continue" button is shown but disabled initially
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        
        // Test selecting male option
        composeTestRule
            .onNodeWithText("🧔 Male")
            .performClick()
        
        // Verify that Continue button is now enabled
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsEnabled()
        
        // Test selecting female option
        composeTestRule
            .onNodeWithText("👩 Female")
            .performClick()
        
        // Verify that Continue button is still enabled
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsEnabled()
    }

    /**
     * Test 2: Verify Islamic features step shows only "Complete" button
     */
    @Test
    fun testIslamicFeaturesStepShowsOnlyCompleteButton() {
        composeTestRule.setContent {
            IslamicOnboardingStep(
                onNext = {},
                onSkip = {}
            )
        }

        // Verify that "Complete" button is shown
        composeTestRule
            .onNodeWithText("Complete")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        // Verify that "Skip Islamic Features" button is NOT shown
        composeTestRule
            .onAllNodesWithText("Skip Islamic Features")
            .assertCountEquals(0)
        
        // Verify that "Continue" button is NOT shown
        composeTestRule
            .onAllNodesWithText("Continue")
            .assertCountEquals(0)
        
        // Test clicking Complete button
        composeTestRule
            .onNodeWithText("Complete")
            .performClick()
        
        // Verify the button click is handled (button should remain enabled)
        composeTestRule
            .onNodeWithText("Complete")
            .assertIsEnabled()
    }

    /**
     * Test 3: Verify high quality content detection is enabled after wizard completion
     */
    @Test
    fun testHighQualityContentDetectionEnabledAfterWizardCompletion() = runBlocking {
        // Complete the wizard
        viewModel.completeWizard()
        
        // Get the current settings
        val settings = settingsRepository.getCurrentSettings()
        
        // Verify that High Quality mode is applied
        assertEquals(QualityMode.HIGH_QUALITY, settings.qualityMode)
        
        // Verify that High Quality mode settings are correctly applied
        assertEquals(QualityMode.HIGH_QUALITY.detectionSensitivity, settings.detectionSensitivity)
        assertEquals(QualityMode.HIGH_QUALITY.processingSpeed, settings.processingSpeed)
        assertEquals(QualityMode.HIGH_QUALITY.blurIntensity, settings.blurIntensity)
        assertEquals(QualityMode.HIGH_QUALITY.maxProcessingTimeMs, settings.maxProcessingTimeMs)
        assertEquals(QualityMode.HIGH_QUALITY.frameSkipThreshold, settings.frameSkipThreshold)
        assertEquals(QualityMode.HIGH_QUALITY.imageDownscaleRatio, settings.imageDownscaleRatio)
        assertEquals(QualityMode.HIGH_QUALITY.enableGPUAcceleration, settings.enableGPUAcceleration)
        assertEquals(QualityMode.HIGH_QUALITY.enableRealTimeProcessing, settings.enableRealTimeProcessing)
        
        // Verify that content detection is enabled
        assertTrue(settings.enableFaceDetection)
        assertTrue(settings.enableNSFWDetection)
        assertTrue(settings.enableRealTimeProcessing)
        assertFalse(settings.isServicePaused)
        
        // Verify that optimized confidence thresholds are applied
        assertEquals(0.5f, settings.nsfwConfidenceThreshold)
        assertEquals(0.4f, settings.genderConfidenceThreshold)
        
        // Verify that onboarding is marked as completed
        assertTrue(settings.onboardingCompleted)
    }

    /**
     * Test 4: Verify quality mode update applies all settings correctly
     */
    @Test
    fun testQualityModeUpdateAppliesAllSettings() = runBlocking {
        // Test updating to each quality mode
        QualityMode.values().forEach { qualityMode ->
            settingsRepository.updateQualityMode(qualityMode)
            
            val settings = settingsRepository.getCurrentSettings()
            
            // Verify that the quality mode is set correctly
            assertEquals(qualityMode, settings.qualityMode)
            
            // Verify that all related settings are updated
            assertEquals(qualityMode.detectionSensitivity, settings.detectionSensitivity)
            assertEquals(qualityMode.processingSpeed, settings.processingSpeed)
            assertEquals(qualityMode.blurIntensity, settings.blurIntensity)
            assertEquals(qualityMode.maxProcessingTimeMs, settings.maxProcessingTimeMs)
            assertEquals(qualityMode.frameSkipThreshold, settings.frameSkipThreshold)
            assertEquals(qualityMode.imageDownscaleRatio, settings.imageDownscaleRatio)
            assertEquals(qualityMode.enableGPUAcceleration, settings.enableGPUAcceleration)
            assertEquals(qualityMode.enableRealTimeProcessing, settings.enableRealTimeProcessing)
            
            // Verify that detection is always enabled when updating quality mode
            assertTrue(settings.enableFaceDetection)
            assertTrue(settings.enableNSFWDetection)
            assertFalse(settings.isServicePaused)
        }
    }

    /**
     * Test 5: Verify first-time defaults apply High Quality mode correctly
     */
    @Test
    fun testFirstTimeDefaultsApplyHighQualityMode() = runBlocking {
        // Reset to simulate first-time install
        settingsRepository.resetToDefaults()
        
        // Apply first-time defaults
        settingsRepository.applyFirstTimeDefaults()
        
        val settings = settingsRepository.getCurrentSettings()
        
        // Verify that High Quality mode is applied
        assertEquals(QualityMode.HIGH_QUALITY, settings.qualityMode)
        
        // Verify that High Quality mode settings are correctly applied
        assertEquals(QualityMode.HIGH_QUALITY.detectionSensitivity, settings.detectionSensitivity)
        assertEquals(QualityMode.HIGH_QUALITY.processingSpeed, settings.processingSpeed)
        assertEquals(QualityMode.HIGH_QUALITY.blurIntensity, settings.blurIntensity)
        assertEquals(QualityMode.HIGH_QUALITY.maxProcessingTimeMs, settings.maxProcessingTimeMs)
        assertEquals(QualityMode.HIGH_QUALITY.frameSkipThreshold, settings.frameSkipThreshold)
        assertEquals(QualityMode.HIGH_QUALITY.imageDownscaleRatio, settings.imageDownscaleRatio)
        assertEquals(QualityMode.HIGH_QUALITY.enableGPUAcceleration, settings.enableGPUAcceleration)
        assertEquals(QualityMode.HIGH_QUALITY.enableRealTimeProcessing, settings.enableRealTimeProcessing)
        
        // Verify that detection is enabled
        assertTrue(settings.enableFaceDetection)
        assertTrue(settings.enableNSFWDetection)
        assertFalse(settings.isServicePaused)
        
        // Verify that optimized confidence thresholds are applied
        assertEquals(0.5f, settings.nsfwConfidenceThreshold)
        assertEquals(0.4f, settings.genderConfidenceThreshold)
    }

    /**
     * Test 6: Verify wizard state flow works correctly
     */
    @Test
    fun testWizardStateFlowWorksCorrectly() = runBlocking {
        // Test initial state
        val initialState = viewModel.wizardState.value
        assertFalse(initialState.isComplete)
        assertEquals(0, initialState.currentStepIndex)
        
        // Test completing gender selection
        viewModel.completeGenderSelection()
        
        var updatedState = viewModel.wizardState.value
        assertFalse(updatedState.isComplete)
        assertTrue(updatedState.canProceed)
        
        // Test completing Islamic features
        viewModel.completeIslamicFeaturesConfiguration()
        
        updatedState = viewModel.wizardState.value
        assertFalse(updatedState.isComplete)
        assertTrue(updatedState.canProceed)
        
        // Test completing wizard
        viewModel.completeWizard()
        
        val finalState = viewModel.wizardState.value
        assertTrue(finalState.isComplete)
    }

    /**
     * Integration Test: Verify complete wizard flow from start to finish
     */
    @Test
    fun testCompleteWizardFlowIntegration() = runBlocking {
        // Reset to initial state
        settingsRepository.resetOnboarding()
        
        // Simulate completing all wizard steps
        viewModel.completeLanguageSelection()
        viewModel.completeGenderSelection()
        viewModel.completeIslamicFeaturesConfiguration()
        viewModel.completeWizard()
        
        // Verify final state
        val settings = settingsRepository.getCurrentSettings()
        
        // Verify onboarding is completed
        assertTrue(settings.onboardingCompleted)
        
        // Verify High Quality mode is applied
        assertEquals(QualityMode.HIGH_QUALITY, settings.qualityMode)
        
        // Verify all High Quality settings are correct
        assertEquals(QualityMode.HIGH_QUALITY.detectionSensitivity, settings.detectionSensitivity)
        assertEquals(QualityMode.HIGH_QUALITY.processingSpeed, settings.processingSpeed)
        assertEquals(QualityMode.HIGH_QUALITY.blurIntensity, settings.blurIntensity)
        assertEquals(QualityMode.HIGH_QUALITY.maxProcessingTimeMs, settings.maxProcessingTimeMs)
        assertEquals(QualityMode.HIGH_QUALITY.frameSkipThreshold, settings.frameSkipThreshold)
        assertEquals(QualityMode.HIGH_QUALITY.imageDownscaleRatio, settings.imageDownscaleRatio)
        assertEquals(QualityMode.HIGH_QUALITY.enableGPUAcceleration, settings.enableGPUAcceleration)
        assertEquals(QualityMode.HIGH_QUALITY.enableRealTimeProcessing, settings.enableRealTimeProcessing)
        
        // Verify content detection is enabled
        assertTrue(settings.enableFaceDetection)
        assertTrue(settings.enableNSFWDetection)
        assertTrue(settings.enableRealTimeProcessing)
        assertFalse(settings.isServicePaused)
        
        // Verify optimized confidence thresholds
        assertEquals(0.5f, settings.nsfwConfidenceThreshold)
        assertEquals(0.4f, settings.genderConfidenceThreshold)
        
        // Verify wizard state is complete
        val wizardState = viewModel.wizardState.value
        assertTrue(wizardState.isComplete)
    }
}