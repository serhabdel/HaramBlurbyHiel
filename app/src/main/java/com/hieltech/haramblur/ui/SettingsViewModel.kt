package com.hieltech.haramblur.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    
    fun toggleServicePause() {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(isServicePaused = !current.isServicePaused))
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
    


    // Enhanced Preset Management Methods

    /**
     * Apply Maximum Protection preset with highest security settings
     */
    fun applyMaximumProtectionPreset() {
        viewModelScope.launch {
            val preset = PresetManager.createMaximumProtectionPreset()
            settingsRepository.updateSettings(preset.settings)
            Log.i("SettingsViewModel", "Applied Maximum Protection preset")
        }
    }

    /**
     * Apply Optimal Performance preset balancing performance and protection
     */
    fun applyOptimalPerformancePreset() {
        viewModelScope.launch {
            val preset = PresetManager.createOptimalPerformancePreset()
            settingsRepository.updateSettings(preset.settings)
            Log.i("SettingsViewModel", "Applied Optimal Performance preset")
        }
    }

    /**
     * Apply custom preset with user-defined settings
     */
    fun applyCustomPreset(presetData: PresetData) {
        viewModelScope.launch {
            settingsRepository.updateSettings(presetData.settings)
            Log.i("SettingsViewModel", "Applied custom preset: ${presetData.name}")
        }
    }

    /**
     * Get available preset templates
     */
    fun getAvailablePresets(): List<PresetData> {
        return listOf(
            PresetManager.createMaximumProtectionPreset(),
            PresetManager.createOptimalPerformancePreset()
        )
    }

    /**
     * Export settings with enhanced preset metadata
     */
    fun exportSettingsWithPresetInfo(): String {
        return settingsRepository.exportSettingsToJsonWithMetadata()
    }

    /**
     * Export custom preset for sharing
     */
    fun exportPreset(name: String, description: String): String {
        val currentSettings = settings.value
        val presetData = PresetData(
            name = name,
            description = description,
            settings = currentSettings,
            metadata = PresetMetadata(
                category = PresetCategory.CUSTOM,
                difficulty = PresetDifficulty.INTERMEDIATE,
                useCase = "Custom user configuration",
                isBuiltIn = false
            )
        )

        return PresetManager.exportPresetToJson(presetData)
    }

    /**
     * Import preset with validation and conflict resolution
     */
    fun importPreset(presetJson: String): PresetImportResult {
        val validation = settingsRepository.validatePresetData(presetJson)
        if (!validation.isValid) {
            return PresetImportResult.Error(
                validation.errors.joinToString("\n"),
                ImportErrorType.INVALID_JSON
            )
        }

        return try {
            val jsonObject = org.json.JSONObject(presetJson)
            val settingsJson = jsonObject.getJSONObject("settings")

            val importedSettings = com.hieltech.haramblur.data.AppSettings(
                enableFaceDetection = settingsJson.optBoolean("enableFaceDetection", true),
                enableNSFWDetection = settingsJson.optBoolean("enableNSFWDetection", true),
                blurMaleFaces = settingsJson.optBoolean("blurMaleFaces", false),
                blurFemaleFaces = settingsJson.optBoolean("blurFemaleFaces", true),
                detectionSensitivity = settingsJson.optDouble("detectionSensitivity", 0.8).toFloat(),
                blurIntensity = try {
                    com.hieltech.haramblur.data.BlurIntensity.valueOf(settingsJson.optString("blurIntensity", BlurIntensity.STRONG.name))
                } catch (e: IllegalArgumentException) { BlurIntensity.STRONG },
                blurStyle = try {
                    com.hieltech.haramblur.data.BlurStyle.valueOf(settingsJson.optString("blurStyle", BlurStyle.ARTISTIC.name))
                } catch (e: IllegalArgumentException) { BlurStyle.ARTISTIC },
                expandBlurArea = settingsJson.optInt("expandBlurArea", 30),
                processingSpeed = try {
                    com.hieltech.haramblur.data.ProcessingSpeed.valueOf(settingsJson.optString("processingSpeed", ProcessingSpeed.BALANCED.name))
                } catch (e: IllegalArgumentException) { ProcessingSpeed.BALANCED },
                enableRealTimeProcessing = settingsJson.optBoolean("enableRealTimeProcessing", true),
                enableFullScreenBlurForNSFW = settingsJson.optBoolean("enableFullScreenBlurForNSFW", true),
                showBlurBorders = settingsJson.optBoolean("showBlurBorders", true),
                enableHoverToReveal = settingsJson.optBoolean("enableHoverToReveal", false),
                genderDetectionAccuracy = try {
                    com.hieltech.haramblur.data.GenderAccuracy.valueOf(settingsJson.optString("genderDetectionAccuracy", GenderAccuracy.BALANCED.name))
                } catch (e: IllegalArgumentException) { GenderAccuracy.BALANCED },
                contentDensityThreshold = settingsJson.optDouble("contentDensityThreshold", 0.4).toFloat(),
                mandatoryReflectionTime = settingsJson.optInt("mandatoryReflectionTime", 15),
                enableSiteBlocking = settingsJson.optBoolean("enableSiteBlocking", true),
                enableQuranicGuidance = settingsJson.optBoolean("enableQuranicGuidance", true),
                ultraFastModeEnabled = settingsJson.optBoolean("ultraFastModeEnabled", false),
                fullScreenWarningEnabled = settingsJson.optBoolean("fullScreenWarningEnabled", true),
                maxProcessingTimeMs = settingsJson.optLong("maxProcessingTimeMs", 50L),
                enableGPUAcceleration = settingsJson.optBoolean("enableGPUAcceleration", true),
                frameSkipThreshold = settingsJson.optInt("frameSkipThreshold", 3),
                imageDownscaleRatio = settingsJson.optDouble("imageDownscaleRatio", 0.5).toFloat(),
                preferredLanguage = try {
                    com.hieltech.haramblur.detection.Language.valueOf(
                        settingsJson.optString("preferredLanguage", com.hieltech.haramblur.detection.Language.ENGLISH.name)
                    )
                } catch (e: IllegalArgumentException) { com.hieltech.haramblur.detection.Language.ENGLISH },
                verseDisplayDuration = settingsJson.optInt("verseDisplayDuration", 10),
                enableArabicText = settingsJson.optBoolean("enableArabicText", true),
                customReflectionTime = settingsJson.optInt("customReflectionTime", 15),
                genderConfidenceThreshold = settingsJson.optDouble("genderConfidenceThreshold", 0.4).toFloat(),
                nsfwConfidenceThreshold = settingsJson.optDouble("nsfwConfidenceThreshold", 0.5).toFloat(),
                enableFallbackDetection = settingsJson.optBoolean("enableFallbackDetection", true),
                enablePerformanceMonitoring = settingsJson.optBoolean("enablePerformanceMonitoring", true),

                enableDetailedLogging = settingsJson.optBoolean("enableDetailedLogging", true),
                logLevel = try {
                    com.hieltech.haramblur.data.LogLevel.valueOf(settingsJson.optString("logLevel", LogLevel.INFO.name))
                } catch (e: IllegalArgumentException) { LogLevel.INFO },
                enablePerformanceLogging = settingsJson.optBoolean("enablePerformanceLogging", true),
                enableErrorReporting = settingsJson.optBoolean("enableErrorReporting", true),
                enableUserActionLogging = settingsJson.optBoolean("enableUserActionLogging", true),
                maxLogRetentionDays = settingsJson.optInt("maxLogRetentionDays", 7),
                enableEnhancedBlocking = settingsJson.optBoolean("enableEnhancedBlocking", false),
                preferredBlockingMethod = try {
                    com.hieltech.haramblur.detection.BlockingMethod.valueOf(
                        settingsJson.optString("preferredBlockingMethod", com.hieltech.haramblur.detection.BlockingMethod.ADAPTIVE.name)
                    )
                } catch (e: IllegalArgumentException) { com.hieltech.haramblur.detection.BlockingMethod.ADAPTIVE },
                forceCloseTimeout = settingsJson.optLong("forceCloseTimeout", 5000L),
                settingsVersion = settingsJson.optInt("settingsVersion", 3)
            )

            val preset = PresetData(
                name = jsonObject.getString("name"),
                description = jsonObject.optString("description", ""),
                version = jsonObject.optInt("version", 3),
                settings = importedSettings,
                metadata = PresetMetadata(
                    category = try {
                        PresetCategory.valueOf(jsonObject.getJSONObject("metadata").optString("category", PresetCategory.CUSTOM.name))
                    } catch (e: Exception) { PresetCategory.CUSTOM },
                    difficulty = try {
                        PresetDifficulty.valueOf(jsonObject.getJSONObject("metadata").optString("difficulty", PresetDifficulty.INTERMEDIATE.name))
                    } catch (e: Exception) { PresetDifficulty.INTERMEDIATE },
                    useCase = jsonObject.getJSONObject("metadata").optString("useCase", "Custom configuration"),
                    author = jsonObject.getJSONObject("metadata").optString("author", "Unknown"),
                    isBuiltIn = jsonObject.getJSONObject("metadata").optBoolean("isBuiltIn", false),
                    tags = jsonObject.getJSONObject("metadata").optString("tags", "").split(",").filter { it.isNotBlank() }.toSet()
                ),
                creationTimestamp = jsonObject.optLong("creationTimestamp", System.currentTimeMillis())
            )

            PresetImportResult.Success(preset)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Failed to import preset", e)
            PresetImportResult.Error("Failed to parse preset: ${e.message}", ImportErrorType.INVALID_JSON)
        }
    }

    /**
     * Validate preset compatibility
     */
    fun validatePresetCompatibility(preset: PresetData): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Version compatibility check
        if (preset.version > 3) {
            errors.add("Preset version (${preset.version}) is newer than current app version (3)")
        } else if (preset.version < 2) {
            warnings.add("Preset version (${preset.version}) is older and may have compatibility issues")
        }

        // Settings bounds validation
        val settings = preset.settings
        if (settings.detectionSensitivity !in 0.3f..0.9f) {
            errors.add("Detection sensitivity (${settings.detectionSensitivity}) is outside valid range (0.3-0.9)")
        }

        if (settings.genderConfidenceThreshold !in 0.3f..0.8f) {
            errors.add("Gender confidence threshold (${settings.genderConfidenceThreshold}) is outside valid range (0.3-0.8)")
        }

        if (settings.nsfwConfidenceThreshold !in 0.4f..0.7f) {
            errors.add("NSFW confidence threshold (${settings.nsfwConfidenceThreshold}) is outside valid range (0.4-0.7)")
        }

        // Dependency checks


        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            compatibility = if (preset.version > 3) CompatibilityStatus.INCOMPATIBLE else CompatibilityStatus.COMPATIBLE
        )
    }

    /**
     * Get settings organized by category for dynamic UI generation
     */
    fun getSettingsCategories(): Map<SettingsCategory, List<SettingItem>> {
        return PresetManager.getSettingsCategories()
    }

    /**
     * Search settings by query
     */
    fun searchSettings(query: String): List<SettingItem> {
        return PresetManager.searchSettings(query)
    }

    /**
     * Create preset from current settings
     */
    fun createPresetFromCurrent(name: String, description: String): PresetData {
        val currentSettings = settings.value
        return PresetData(
            name = name,
            description = description,
            settings = currentSettings,
            metadata = PresetMetadata(
                category = PresetCategory.CUSTOM,
                difficulty = PresetDifficulty.INTERMEDIATE,
                useCase = "Custom user configuration",
                isBuiltIn = false
            )
        )
    }

    /**
     * Get preset templates for UI display
     */
    fun getPresetTemplates(): List<PresetTemplate> {
        return settingsRepository.getPresetTemplates()
    }

    /**
     * Apply preset with backup and validation
     */
    fun applyPresetWithBackup(preset: PresetData) {
        viewModelScope.launch {
            try {
                // Backup current settings
                val backup = settingsRepository.backupCurrentSettings()

                // Validate preset
                val validation = validatePresetCompatibility(preset)
                if (!validation.isValid) {
                    Log.e("SettingsViewModel", "Preset validation failed: ${validation.errors.joinToString()}")
                    return@launch
                }

                // Apply preset
                settingsRepository.updateSettings(preset.settings)

                // Update preset tracking
                val current = settings.value
                settingsRepository.updateSettings(current.copy(
                    currentPreset = preset.name,
                    lastPresetUpdate = System.currentTimeMillis()
                ))

                Log.i("SettingsViewModel", "Successfully applied preset: ${preset.name}")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to apply preset", e)
            }
        }
    }

    /**
     * Get settings difference for UI display
     */
    fun getSettingsDiff(imported: AppSettings): SettingsDiff {
        val current = settings.value
        return settingsRepository.getSettingsDiff(current, imported)
    }
    
    // Dhikr Settings Methods
    fun updateDhikrEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrEnabled = enabled))
        }
    }
    
    fun updateDhikrMorningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrMorningEnabled = enabled))
        }
    }
    
    fun updateDhikrEveningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrEveningEnabled = enabled))
        }
    }
    
    fun updateDhikrAnytimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrAnytimeEnabled = enabled))
        }
    }
    
    fun updateDhikrInterval(intervalMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrIntervalMinutes = intervalMinutes))
        }
    }
    
    fun updateDhikrDisplayDuration(durationSeconds: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrDisplayDuration = durationSeconds))
        }
    }
    
    fun updateDhikrPosition(position: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrPosition = position))
        }
    }
    
    fun updateDhikrShowTransliteration(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrShowTransliteration = enabled))
        }
    }
    
    fun updateDhikrShowTranslation(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrShowTranslation = enabled))
        }
    }
    
    fun updateDhikrAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(dhikrAnimationEnabled = enabled))
        }
    }
}