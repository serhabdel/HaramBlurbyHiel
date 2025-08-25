package com.hieltech.haramblur.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val settings: StateFlow<AppSettings> = settingsRepository.settings
    
    fun updateFaceDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableFaceDetection = enabled))
        }
    }
    
    fun updateNSFWDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableNSFWDetection = enabled))
        }
    }
    
    fun updateMaleBlur(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(blurMaleFaces = enabled))
        }
    }
    
    fun updateFemaleBlur(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(blurFemaleFaces = enabled))
        }
    }
    
    fun updateSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(detectionSensitivity = sensitivity))
        }
    }
    
    fun updateBlurIntensity(intensity: BlurIntensity) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(blurIntensity = intensity))
        }
    }
    
    fun updateBlurStyle(style: BlurStyle) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(blurStyle = style))
        }
    }
    
    fun updateProcessingSpeed(speed: ProcessingSpeed) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(processingSpeed = speed))
        }
    }
    
    fun updateRealTimeProcessing(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableRealTimeProcessing = enabled))
        }
    }
    
    fun updateBlurExpansion(pixels: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(expandBlurArea = pixels))
        }
    }
    
    fun updateFullScreenBlur(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableFullScreenBlurForNSFW = enabled))
        }
    }
    
    fun updateBlurBorders(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(showBlurBorders = enabled))
        }
    }
    
    // Enhanced Detection Settings
    fun updateGenderDetectionAccuracy(accuracy: GenderAccuracy) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(genderDetectionAccuracy = accuracy))
        }
    }
    
    fun updateContentDensityThreshold(threshold: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(contentDensityThreshold = threshold))
        }
    }
    
    fun updateUltraFastMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(ultraFastModeEnabled = enabled))
        }
    }
    
    fun updateMandatoryReflectionTime(seconds: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(mandatoryReflectionTime = seconds))
        }
    }
    
    fun updateFullScreenWarning(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(fullScreenWarningEnabled = enabled))
        }
    }
    
    // Performance Enhancement Settings
    fun updateMaxProcessingTime(timeMs: Long) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(maxProcessingTimeMs = timeMs))
        }
    }
    
    fun updateGPUAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableGPUAcceleration = enabled))
        }
    }
    
    fun updateFrameSkipThreshold(threshold: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(frameSkipThreshold = threshold))
        }
    }
    
    fun updateImageDownscaleRatio(ratio: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(imageDownscaleRatio = ratio))
        }
    }
    
    // Advanced Settings
    fun updateGenderConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(genderConfidenceThreshold = threshold))
        }
    }
    
    fun updateNSFWConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(nsfwConfidenceThreshold = threshold))
        }
    }
    
    fun updateFallbackDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableFallbackDetection = enabled))
        }
    }
    
    fun updatePerformanceMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enablePerformanceMonitoring = enabled))
        }
    }
    
    // Islamic Guidance Settings
    fun updateQuranicGuidance(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableQuranicGuidance = enabled))
        }
    }
    
    fun updatePreferredLanguage(language: com.hieltech.haramblur.detection.Language) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(preferredLanguage = language))
        }
    }
    
    fun updateVerseDisplayDuration(duration: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(verseDisplayDuration = duration))
        }
    }
    
    fun updateArabicText(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableArabicText = enabled))
        }
    }
    
    fun updateCustomReflectionTime(seconds: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(customReflectionTime = seconds))
        }
    }
    
    // Settings Backup and Restore
    fun exportSettings(): String {
        return settingsRepository.exportSettingsToJson()
    }
    
    fun importSettings(jsonString: String): Boolean {
        return settingsRepository.importSettingsFromJson(jsonString)
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
    
    fun applyOptimalSettings() {
        viewModelScope.launch {
            val optimal = AppSettings(
                enableFaceDetection = true,
                enableNSFWDetection = true,
                blurMaleFaces = true,
                blurFemaleFaces = true,
                detectionSensitivity = 0.6f, // Slightly more sensitive
                blurIntensity = BlurIntensity.STRONG,
                blurStyle = BlurStyle.COMBINED,
                expandBlurArea = 40,
                processingSpeed = ProcessingSpeed.BALANCED,
                enableRealTimeProcessing = true,
                enableFullScreenBlurForNSFW = true,
                showBlurBorders = false, // Cleaner look
                enableHoverToReveal = false,
                // Enhanced settings
                genderDetectionAccuracy = GenderAccuracy.HIGH,
                contentDensityThreshold = 0.4f,
                mandatoryReflectionTime = 15,
                ultraFastModeEnabled = false,
                fullScreenWarningEnabled = true,
                maxProcessingTimeMs = 50L,
                enableGPUAcceleration = true,
                frameSkipThreshold = 3,
                imageDownscaleRatio = 0.5f
            )
            settingsRepository.updateSettings(optimal)
        }
    }
}