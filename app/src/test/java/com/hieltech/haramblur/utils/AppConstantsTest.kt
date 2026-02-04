package com.hieltech.haramblur.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppConstants
 */
class AppConstantsTest {

    @Test
    fun `ML constants are valid`() {
        assertTrue("NSFW input size should be positive", AppConstants.ML.NSFW_INPUT_SIZE > 0)
        assertTrue("Gender input size should be positive", AppConstants.ML.GENDER_INPUT_SIZE > 0)
        
        assertTrue("NSFW threshold should be between 0 and 1", 
            AppConstants.ML.DEFAULT_NSFW_THRESHOLD in 0.0f..1.0f)
        assertTrue("Gender threshold should be between 0 and 1",
            AppConstants.ML.DEFAULT_GENDER_THRESHOLD in 0.0f..1.0f)
    }
    
    @Test
    fun `Performance constants are positive`() {
        assertTrue("Default capture interval should be positive",
            AppConstants.Performance.DEFAULT_CAPTURE_INTERVAL_MS > 0)
        assertTrue("Fast capture interval should be positive",
            AppConstants.Performance.FAST_CAPTURE_INTERVAL_MS > 0)
    }
    
    @Test
    fun `Broadcast actions are not empty`() {
        assertTrue("Emergency reset action should not be empty",
            AppConstants.BroadcastActions.EMERGENCY_RESET.isNotEmpty())
        assertTrue("Prayer completed action should not be empty",
            AppConstants.BroadcastActions.PRAYER_COMPLETED.isNotEmpty())
    }
    
    @Test
    fun `Tags are not empty`() {
        assertTrue("Accessibility service tag should not be empty",
            AppConstants.Tags.ACCESSIBILITY_SERVICE.isNotEmpty())
        assertTrue("ML model manager tag should not be empty",
            AppConstants.Tags.ML_MODEL_MANAGER.isNotEmpty())
    }
    
    @Test
    fun `Preference keys are not empty`() {
        assertTrue("First run key should not be empty",
            AppConstants.PreferenceKeys.FIRST_RUN_COMPLETED.isNotEmpty())
        assertTrue("User gender key should not be empty",
            AppConstants.PreferenceKeys.USER_GENDER.isNotEmpty())
    }
    
    @Test
    fun `ML model paths have correct format`() {
        assertTrue("NSFW model path should start with models/",
            AppConstants.ML.NSFW_MODEL_PATH.startsWith("models/"))
        assertTrue("Gender model path should start with models/",
            AppConstants.ML.GENDER_MODEL_PATH.startsWith("models/"))
        assertTrue("NSFW model path should have .tflite extension",
            AppConstants.ML.NSFW_MODEL_PATH.endsWith(".tflite"))
    }
}
