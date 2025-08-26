package com.hieltech.haramblur.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository
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
    
    // Logging Settings Methods
    fun updateDetailedLogging(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableDetailedLogging = enabled))
        }
    }

    fun updateLogLevel(logLevel: LogLevel) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(logLevel = logLevel))
        }
    }

    fun updateLogCategory(category: LogCategory, enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            val updatedCategories = if (enabled) {
                current.enableLogCategories + category
            } else {
                current.enableLogCategories - category
            }
            settingsRepository.updateSettings(current.copy(enableLogCategories = updatedCategories))
        }
    }

    fun updateLogRetentionDays(days: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(maxLogRetentionDays = days))
        }
    }

    fun updatePerformanceLogging(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enablePerformanceLogging = enabled))
        }
    }

    fun updateErrorReporting(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableErrorReporting = enabled))
        }
    }

    fun updateUserActionLogging(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableUserActionLogging = enabled))
        }
    }

    // Log Export Methods
    suspend fun exportLogs(): String? {
        return try {
            viewModelScope.launch {
                logRepository.logInfo("SettingsViewModel", "Exporting logs from settings")
            }
            val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
            logRepository.exportLogsAsText(levels = levels)
        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("SettingsViewModel", "Failed to export logs", e)
            }
            null
        }
    }



    fun applyOptimalSettings() {
        viewModelScope.launch {
            val optimal = AppSettings(
                // Female-focused detection settings
                enableFaceDetection = true,
                enableNSFWDetection = true,
                blurMaleFaces = false, // Disabled - female-only focus
                blurFemaleFaces = true,
                detectionSensitivity = 0.7f, // High sensitivity for better female detection
                blurIntensity = BlurIntensity.STRONG,
                blurStyle = BlurStyle.COMBINED,
                expandBlurArea = 50, // Larger area for better female face coverage
                processingSpeed = ProcessingSpeed.BALANCED,
                enableRealTimeProcessing = true,
                enableFullScreenBlurForNSFW = true,
                showBlurBorders = false, // Cleaner look
                enableHoverToReveal = false,
                // Enhanced settings optimized for female detection
                genderDetectionAccuracy = GenderAccuracy.BALANCED,
                contentDensityThreshold = 0.3f, // Lower threshold for more coverage
                mandatoryReflectionTime = 15,
                ultraFastModeEnabled = false,
                fullScreenWarningEnabled = true,
                maxProcessingTimeMs = 75L, // Slightly higher for better accuracy
                enableGPUAcceleration = true, // Enable GPU by default
                frameSkipThreshold = 2, // Lower skip threshold for better detection
                imageDownscaleRatio = 0.6f, // Higher quality for better detection
                // Lower confidence thresholds for better female detection
                genderConfidenceThreshold = 0.4f,
                nsfwConfidenceThreshold = 0.5f,
                // Optimal logging settings
                enableDetailedLogging = true,
                logLevel = LogLevel.INFO,
                enableLogCategories = setOf(
                    LogCategory.DETECTION,
                    LogCategory.BLOCKING,
                    LogCategory.UI,
                    LogCategory.ACCESSIBILITY,
                    LogCategory.PERFORMANCE
                ),
                maxLogRetentionDays = 7,
                enablePerformanceLogging = true,
                enableErrorReporting = true,
                enableUserActionLogging = true
            )
            settingsRepository.updateSettings(optimal)
        }
    }
    
    // LLM Decision Making Settings
    fun updateLLMDecisionMaking(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableLLMDecisionMaking = enabled))
        }
    }
    
    fun updateOpenRouterApiKey(apiKey: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(openRouterApiKey = apiKey))
        }
    }
    
    fun updateLLMModel(model: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(llmModel = model))
        }
    }
    
    fun updateLLMTimeout(timeoutMs: Long) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(llmTimeoutMs = timeoutMs))
        }
    }
    
    fun updateLLMFallbackToRules(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(llmFallbackToRules = enabled))
        }
    }
}