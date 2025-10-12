package com.hieltech.haramblur.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.data.QualityMode
import com.hieltech.haramblur.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import com.hieltech.haramblur.utils.AutoCalculationMethodDetector

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val autoCalculationMethodDetector: AutoCalculationMethodDetector
) : ViewModel() {
    
    val settings: StateFlow<AppSettings> = settingsRepository.settings
    private val locationHelper: LocationHelper = LocationHelper(context)
    
    // One-time events for UI (activity recreation on language change)
    private val _languageChangeEvents = MutableSharedFlow<Unit>(replay = 0)
    val languageChangeEvents: SharedFlow<Unit> = _languageChangeEvents.asSharedFlow()
    
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
    
    fun updateLanguage(language: com.hieltech.haramblur.detection.Language) {
        updatePreferredLanguage(language)
    }

    suspend fun updateGenderSettings(gender: com.hieltech.haramblur.data.UserGender): Boolean {
        // Smart blur settings based on gender for Islamic compliance
        val (blurMale, blurFemale) = when (gender) {
            com.hieltech.haramblur.data.UserGender.MALE -> {
                // Males should have female faces blurred, but may see male faces
                Log.d("SettingsViewModel", "🧔 Applying MALE profile: blur female faces only")
                false to true
            }
            com.hieltech.haramblur.data.UserGender.FEMALE -> {
                // Females should have male faces blurred, but may see female faces
                Log.d("SettingsViewModel", "👩 Applying FEMALE profile: blur male faces only")
                true to false
            }
            com.hieltech.haramblur.data.UserGender.NOT_SPECIFIED -> {
                // Safest option: blur all faces
                Log.d("SettingsViewModel", "⚠️ Gender NOT_SPECIFIED: blur all faces as safest option")
                true to true
            }
        }

        Log.d("SettingsViewModel", "💾 Saving gender settings synchronously with verification...")
        Log.d("SettingsViewModel", "   Gender: $gender, Blur Male: $blurMale, Blur Female: $blurFemale")

        // Use synchronous persistence with verification
        val success = withContext(Dispatchers.IO) {
            settingsRepository.persistGenderSyncWithResult(
                gender = gender,
                blurMaleFaces = blurMale,
                blurFemaleFaces = blurFemale
            )
        }

        if (success) {
            Log.d("SettingsViewModel", "✅ Gender persistence verified successfully: $gender")
            Log.d("SettingsViewModel", "   Applied settings - Blur Male: $blurMale, Blur Female: $blurFemale")
            Log.d("SettingsViewModel", "   Face Detection: enabled, NSFW Detection: enabled")
            logRepository.logInfo("Gender settings persisted and verified: $gender, blur male: $blurMale, blur female: $blurFemale", "SettingsViewModel")
        } else {
            Log.e("SettingsViewModel", "❌ CRITICAL: Gender persistence FAILED!")
            Log.e("SettingsViewModel", "   Attempted to save: $gender with blur settings Male=$blurMale, Female=$blurFemale")
            Log.e("SettingsViewModel", "   User will need to retry gender selection")
            logRepository.logError("SettingsViewModel", "Gender persistence failed for $gender", null)
        }

        return success
    }
    
    fun updatePreferredLanguage(language: com.hieltech.haramblur.detection.Language) {
        viewModelScope.launch {
            try {
                // Persist synchronously in repository (updates StateFlow and commits SharedPreferences)
                settingsRepository.persistPreferredLanguageSync(language)
                
                // Log the language change
                logRepository.logInfo("Language changed to: ${language.displayName}", "SettingsViewModel")
                android.util.Log.d("SettingsViewModel", "Persisted language; requesting activity recreation")
                
                // Optional verification: read-back and ensure it matches
                val verified = try {
                    val prefs = context.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
                    val stored = prefs.getString("preferred_language", null)
                    stored == language.name
                } catch (e: Exception) { false }
                android.util.Log.d("SettingsViewModel", "Language persistence verification: $verified for ${language.name}")
                
                // Emit recreation event (suspending) after ensuring persistence
                _languageChangeEvents.emit(Unit)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error updating locale", e)
                // Log the error for debugging
                logRepository.logError("SettingsViewModel", "Failed to update language to: ${language.displayName}", e)
            }
        }
    }
    
    /**
     * Enhanced language update method with verification and return value for wizard flow
     * @param language The language to set
     * @param suppressRecreation Whether to suppress activity recreation (for wizard flow)
     * @return Boolean indicating success or failure of language persistence
     */
    suspend fun updatePreferredLanguageWithResult(language: com.hieltech.haramblur.detection.Language, suppressRecreation: Boolean = false): Boolean {
        return try {
            // Use the repository method with result and retry logic
            val success = settingsRepository.persistPreferredLanguageSyncWithResult(language)
            
            // Log the language change
            logRepository.logInfo("Language changed to: ${language.displayName}", "SettingsViewModel")
            android.util.Log.d("SettingsViewModel", "Persisted language: ${language.name}, success: $success")
            
            if (success && !suppressRecreation) {
                // Only emit recreation event for non-wizard flows
                // During wizard, we handle UI updates immediately without recreation
                try {
                    _languageChangeEvents.emit(Unit)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Error emitting language change event", e)
                }
            }
            
            success
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error updating locale", e)
            // Log the error for debugging
            logRepository.logError("SettingsViewModel", "Failed to update language to: ${language.displayName}", e)
            false
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

    fun updateDhikrSleepStartTime(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDhikrSleepStartTime(minutes)
        }
    }

    fun updateDhikrSleepEndTime(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDhikrSleepEndTime(minutes)
        }
    }

    fun updateDhikrSleepTimes(startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDhikrSleepTimes(startMinutes, endMinutes)
        }
    }

    // Theme Settings Methods
    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(appTheme = theme))
        }
    }

    // Islamic Calendar & Prayer Times Settings Methods
    fun updateIslamicCalendarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableIslamicCalendar = enabled))
        }
    }

    fun updatePrayerTimesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enablePrayerTimes = enabled))
        }
    }

    fun updatePrayerNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enablePrayerNotifications = enabled))
        }
    }

    

    // City selection (OpenStreetMap) methods
    fun updateSelectedCity(selection: com.hieltech.haramblur.data.cities.CitySelection) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                selectedCityName = selection.name,
                selectedCountry = selection.country,
                selectedCountryCode = selection.countryCode,
                selectedLatitude = selection.latitude,
                selectedLongitude = selection.longitude,
                // Keep legacy fields for backward compatibility in any UI still reading them
                preferredCity = selection.name,
                preferredCountry = selection.country,
                // Switch to manual city mode on selection
                locationMethod = LocationMethod.MANUAL_CITY,
                // If coordinates were provided with the selection, prefer them for more accuracy
                preferStoredCoordinates = selection.latitude != null && selection.longitude != null
            )
            settingsRepository.updateSettings(updated)
            // Invalidate cached prayer times so the widget and screens refresh immediately
            prayerTimesRepository.invalidateCache()
            // Trigger immediate refresh for reactive consumers
            prayerTimesRepository.triggerRefresh()
        }
    }

    fun updateCalculationMethod(method: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(prayerCalculationMethod = method))
        }
    }

    /**
     * Automatically detect and set the optimal calculation method based on current location
     */
    fun autoDetectCalculationMethod() {
        viewModelScope.launch {
            val current = settings.value
            val lat = current.locationLatitude
            val lon = current.locationLongitude

            if (lat != null && lon != null) {
                val recommendation = autoCalculationMethodDetector.getRecommendation(lat, lon)
                if (recommendation.confidence != AutoCalculationMethodDetector.ConfidenceLevel.LOW) {
                    updateCalculationMethod(recommendation.method.id)
                    Log.d("SettingsViewModel", "Auto-detected calculation method: ${recommendation.method.displayName} (${recommendation.reason})")
                }
            }
        }
    }

    /**
     * Check if current calculation method is optimal for location
     */
    fun isCalculationMethodOptimal(): Boolean {
        return autoCalculationMethodDetector.isOptimalForLocation(settings.value)
    }

    /**
     * Get suggestion message for better calculation method
     */
    fun getCalculationMethodSuggestion(): String? {
        return autoCalculationMethodDetector.getSuggestionMessage(settings.value)
    }

    fun updateNotificationAdvanceTime(minutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(prayerNotificationAdvanceTime = minutes))
        }
    }

    // City search and storage preferences
    fun updatePreferStoredCoordinates(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(preferStoredCoordinates = enabled))
        }
    }

    fun updateEnableCitySearchCache(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableCitySearchCache = enabled))
        }
    }

    fun updateEnableOfflineCityFallback(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableOfflineCityFallback = enabled))
        }
    }

    // Location Settings Methods
    fun updateAutoDetectLocation(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(autoDetectLocation = enabled))
        }
    }

    /**
     * Explicitly set the preferred location method (GPS vs Manual City)
     */
    fun updateLocationMethod(method: LocationMethod) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(locationMethod = method))
        }
    }

    /**
     * Sync the tracked permission status from the system.
     */
    fun syncLocationPermissionStatus() {
        viewModelScope.launch {
            val status = locationHelper.getLocationPermissionStatus()
            val current = settings.value
            settingsRepository.updateSettings(current.copy(locationPermissionStatus = status))
        }
    }

    /**
     * Try to fetch the best available location and persist coordinates, accuracy and timestamp.
     */
    fun refreshLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            val permission = locationHelper.getLocationPermissionStatus()
            val current = settings.value
            if (permission != LocationPermissionStatus.GRANTED) {
                settingsRepository.updateSettings(current.copy(locationPermissionStatus = permission))
                return@launch
            }

            val loc = locationHelper.getBestLocation()
            val updated = if (loc != null) {
                val ts = System.currentTimeMillis()

                // Auto-detect calculation method for new location
                val recommendedMethod = autoCalculationMethodDetector.detectCalculationMethod(loc.latitude, loc.longitude)
                val shouldUpdateMethod = current.prayerCalculationMethod != recommendedMethod.id

                current.copy(
                    locationLatitude = loc.latitude,
                    locationLongitude = loc.longitude,
                    // Keep existing city/country if we don't have reverse geocode here
                    locationAccuracy = loc.accuracy,
                    locationLastUpdated = ts,
                    locationPermissionStatus = LocationPermissionStatus.GRANTED,
                    // Auto-update calculation method if it's different and recommended
                    prayerCalculationMethod = if (shouldUpdateMethod) recommendedMethod.id else current.prayerCalculationMethod
                )
            } else {
                current.copy(locationPermissionStatus = permission)
            }
            settingsRepository.updateSettings(updated)

            // Log the auto-detection if method was changed
            if (loc != null) {
                val recommendedMethod = autoCalculationMethodDetector.detectCalculationMethod(loc.latitude, loc.longitude)
                if (current.prayerCalculationMethod != recommendedMethod.id) {
                    Log.d("SettingsViewModel", "Auto-updated calculation method to ${recommendedMethod.displayName} based on location")
                }
            }
        }
    }

    /**
     * Clear stored location coordinates and accuracy.
     */
    fun clearLocationData() {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    locationLatitude = null,
                    locationLongitude = null,
                    locationAccuracy = null,
                    locationLastUpdated = null
                )
            )
        }
    }

    /**
     * Helper: get current accuracy tier for UI.
     */
    fun getLocationAccuracyTier(): LocationAccuracy {
        return locationHelper.classifyAccuracy(settings.value.locationAccuracy)
    }

    /**
     * Helper: status summary string for UI.
     */
    fun getLocationStatusSummary(): String {
        val s = settings.value
        return when (s.locationMethod) {
            LocationMethod.GPS -> {
                val perm = s.locationPermissionStatus
                if (perm != LocationPermissionStatus.GRANTED) "GPS permission not granted"
                else if (s.locationLatitude != null && s.locationLongitude != null) {
                    val acc = s.locationAccuracy?.let { "±${it.toInt()}m" } ?: ""
                    "GPS: ${"%.4f".format(s.locationLatitude)} , ${"%.4f".format(s.locationLongitude)} $acc"
                } else "GPS location unavailable"
            }
            LocationMethod.MANUAL_CITY -> {
                val name = s.selectedCityName ?: s.preferredCity
                val country = s.selectedCountry ?: s.preferredCountry
                if (name != null) "Manual: $name${if (!country.isNullOrBlank()) ", $country" else ""}" else "Manual city not selected"
            }
        }
    }

    fun updatePreferredCity(city: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(preferredCity = city))
        }
    }

    fun updatePreferredCountry(country: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(preferredCountry = country))
        }
    }

    fun updateLocation(latitude: Double?, longitude: Double?, city: String?, country: String?) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(
                locationLatitude = latitude,
                locationLongitude = longitude,
                locationCity = city,
                locationCountry = country
            ))
        }
    }

    fun updatePrayerTimesUpdateInterval(minutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(prayerTimesUpdateInterval = minutes))
        }
    }

    fun updateIslamicCalendarUpdateInterval(minutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(islamicCalendarUpdateInterval = minutes))
        }
    }

    fun updateQiblaDirectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableQiblaDirection = enabled))
        }
    }

    // Compass Settings Methods
    fun updateQiblaCompassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(qiblaCompassEnabled = enabled))
        }
    }

    fun updateCompassCalibrationReminders(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassCalibrationReminders = enabled))
        }
    }

    fun updateCompassHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassHapticFeedback = enabled))
        }
    }

    fun updateCompassShowDegreeMarkings(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassShowDegreeMarkings = enabled))
        }
    }

    fun updateCompassSensitivity(value: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassSensitivity = value))
        }
    }

    fun updateCompassAccuracyThreshold(value: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassAccuracyThreshold = value))
        }
    }

    fun updateQiblaToleranceDegrees(value: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(qiblaToleranceDegrees = value))
        }
    }

    fun updateCompassAnimationSpeed(value: Float) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassAnimationSpeed = value))
        }
    }

    fun updateCompassPreferredSize(size: com.hieltech.haramblur.data.compass.CompassSize) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassPreferredSize = size))
        }
    }

    fun updateEnableMagneticDeclination(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableMagneticDeclination = enabled))
        }
    }

    fun updateCompassUpdateRate(hz: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(compassUpdateRate = hz))
        }
    }

    fun markCompassCalibrated() {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(lastCompassCalibration = System.currentTimeMillis()))
        }
    }

    // --- Islamic features health & recovery (lightweight stubs for integration) ---
    data class IslamicFeaturesStatus(
        val locationOk: Boolean,
        val sensorsOk: Boolean,
        val permissionsOk: Boolean,
        val apiOk: Boolean,
        val offlineMode: Boolean
    )

    fun getIslamicFeaturesStatus(): IslamicFeaturesStatus {
        val s = settings.value
        val locationOk = (s.locationLatitude != null && s.locationLongitude != null) || s.locationMethod == LocationMethod.MANUAL_CITY
        val sensorsOk = true // Placeholder; real sensor health is tracked by compass VM
        val permissionsOk = s.locationPermissionStatus == LocationPermissionStatus.GRANTED || s.locationMethod == LocationMethod.MANUAL_CITY
        val apiOk = true // Placeholder; real API status tracked in repositories
        val offline = false
        return IslamicFeaturesStatus(locationOk, sensorsOk, permissionsOk, apiOk, offline)
    }

    fun recoverIslamicFeatures() {
        // Minimal recovery attempts
        viewModelScope.launch {
            runCatching { syncLocationPermissionStatus() }
            runCatching { if (settings.value.locationMethod == LocationMethod.GPS) refreshLocation() }
        }
    }

    fun showIslamicFeatureMessage(message: String) {
        viewModelScope.launch {
            logRepository.logInfo("IslamicFeatures", message)
        }
    }

    // App-Specific Detection Update Methods
    fun updateAppSpecificDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableAppSpecificDetection = enabled))
        }
    }

    fun updateMonitoredAppCategories(categories: Set<com.hieltech.haramblur.data.models.AppCategory>) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(monitoredAppCategories = categories))
        }
    }

    fun updateCustomMonitoredApps(apps: Set<String>) {
        viewModelScope.launch {
            val current = settings.value
            // Remove any apps that are in the excluded list (excluded takes precedence)
            val filteredApps = apps.filter { !current.excludedApps.contains(it) }.toSet()
            settingsRepository.updateSettings(current.copy(customMonitoredApps = filteredApps))
        }
    }

    fun updateExcludedApps(apps: Set<String>) {
        viewModelScope.launch {
            val current = settings.value
            // Remove any apps that are in the monitored list (excluded takes precedence)
            val filteredApps = apps.filter { !current.customMonitoredApps.contains(it) }.toSet()
            settingsRepository.updateSettings(current.copy(excludedApps = filteredApps))
        }
    }

    /**
     * Check if an app can be added to the monitored list
     */
    fun canAddToMonitored(packageName: String): Boolean {
        val current = settings.value
        return !current.excludedApps.contains(packageName) &&
               !current.customMonitoredApps.contains(packageName)
    }

    /**
     * Check if an app can be added to the excluded list
     */
    fun canAddToExcluded(packageName: String): Boolean {
        val current = settings.value
        return !current.customMonitoredApps.contains(packageName) &&
               !current.excludedApps.contains(packageName) &&
               !isAppInMonitoredCategories(packageName)
    }

    /**
     * Check if an app is in any monitored category
     */
    private fun isAppInMonitoredCategories(packageName: String): Boolean {
        val current = settings.value
        return current.monitoredAppCategories.any { category ->
            category.defaultApps.contains(packageName)
        }
    }

    // Usage Time Tracking Update Methods
    fun updateUsageTimeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value

            // Prepopulate default app time limits on first enable if not already seeded
            val updatedLimits = if (enabled && !current.usageDefaultsSeeded) {
                val defaultLimits = current.customAppTimeLimits.toMutableMap()
                defaultLimits["com.instagram.android"] = 60 // Instagram - 1 hour
                defaultLimits["com.facebook.katana"] = 60 // Facebook - 1 hour
                defaultLimits["com.whatsapp"] = 120 // WhatsApp - 2 hours
                defaultLimits.toMap()
            } else {
                current.customAppTimeLimits
            }

            settingsRepository.updateSettings(current.copy(
                enableUsageTimeNotifications = enabled,
                customAppTimeLimits = updatedLimits,
                usageDefaultsSeeded = current.usageDefaultsSeeded || enabled
            ))
        }
    }

    fun updateDefaultSocialMediaTimeLimit(limitMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(defaultSocialMediaTimeLimit = limitMinutes))
        }
    }

    fun updateDefaultMessagingTimeLimit(limitMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(defaultMessagingTimeLimit = limitMinutes))
        }
    }

    fun updateCustomAppTimeLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            val updatedLimits = current.customAppTimeLimits.toMutableMap()
            updatedLimits[packageName] = limitMinutes
            settingsRepository.updateSettings(current.copy(customAppTimeLimits = updatedLimits))
        }
    }

    fun removeCustomAppTimeLimit(packageName: String) {
        viewModelScope.launch {
            val current = settings.value
            val updatedLimits = current.customAppTimeLimits.toMutableMap()
            updatedLimits.remove(packageName)
            settingsRepository.updateSettings(current.copy(customAppTimeLimits = updatedLimits))
        }
    }

    fun updateUsageNotificationFrequency(frequencyMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(usageNotificationFrequency = frequencyMinutes))
        }
    }

    fun updateDailyUsageReset(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(enableDailyUsageReset = enabled))
        }
    }

    // Helper Methods for UI
    fun getUsageTimeConfig(): com.hieltech.haramblur.data.models.UsageTimeConfig {
        val current = settings.value
        return com.hieltech.haramblur.data.models.UsageTimeConfig(
            enabled = current.enableUsageTimeNotifications,
            defaultLimits = mapOf(
                com.hieltech.haramblur.data.models.AppCategory.SOCIAL_MEDIA to current.defaultSocialMediaTimeLimit,
                com.hieltech.haramblur.data.models.AppCategory.MESSAGING to current.defaultMessagingTimeLimit,
                com.hieltech.haramblur.data.models.AppCategory.ENTERTAINMENT to 60,
                com.hieltech.haramblur.data.models.AppCategory.DATING to 30,
                com.hieltech.haramblur.data.models.AppCategory.BROWSERS to 180
            ),
            customAppLimits = current.customAppTimeLimits,
            notificationFrequencyMinutes = current.usageNotificationFrequency,
            enableDailyReset = current.enableDailyUsageReset
        )
    }

    fun getMonitoredAppCategoriesCount(): Int {
        return settings.value.monitoredAppCategories.size
    }

    fun getCustomMonitoredAppsCount(): Int {
        return settings.value.customMonitoredApps.size
    }

    fun isAppCategoryMonitored(category: com.hieltech.haramblur.data.models.AppCategory): Boolean {
        return settings.value.monitoredAppCategories.contains(category)
    }

    /**
     * Update quality mode - applies all related settings automatically
     */
    fun updateQualityMode(qualityMode: QualityMode) {
        viewModelScope.launch {
            settingsRepository.updateQualityMode(qualityMode)
            Log.d("SettingsViewModel", "Quality mode updated to: ${qualityMode.displayName}")
        }
    }

    /**
     * Get current quality mode
     */
    fun getCurrentQualityMode(): QualityMode {
        return settings.value.qualityMode
    }

    // Local Prayer Calculation Settings Methods
    fun updateEnableLocalCalculations(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableLocalCalculations(enabled)
            // Invalidate prayer times cache to ensure recalculation with new settings
            prayerTimesRepository.invalidateCache()
        }
    }

    fun updatePreferLocalOverApi(preferLocal: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePreferLocalOverApi(preferLocal)
            // Invalidate prayer times cache to ensure recalculation with new settings
            prayerTimesRepository.invalidateCache()
        }
    }

    fun updateShowCalculationMethod(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowCalculationMethod(show)
        }
    }

    fun updateMoroccoSpecificAdjustments(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMoroccoSpecificAdjustments(enabled)
            // Invalidate prayer times cache to ensure recalculation with new settings
            prayerTimesRepository.invalidateCache()
        }
    }

    fun updateAllLocalCalculationSettings(
        enableLocal: Boolean,
        preferLocal: Boolean,
        showMethod: Boolean,
        moroccoAdjustments: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateAllLocalCalculationSettings(
                enableLocal = enableLocal,
                preferLocal = preferLocal,
                showMethod = showMethod,
                moroccoAdjustments = moroccoAdjustments
            )
            // Invalidate prayer times cache to ensure recalculation with new settings
            prayerTimesRepository.invalidateCache()
        }
    }

    /**
     * Get list of installed apps for app picker dialog
     */
    suspend fun getInstalledApps(): List<com.hieltech.haramblur.detection.AppInfo> = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            
            packages.mapNotNull { appInfo ->
                try {
                    // Filter out system apps that users typically don't want to monitor
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    if (isSystemApp) {
                        null
                    } else {
                        com.hieltech.haramblur.detection.AppInfo(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            category = "user_app",
                            isSystemApp = false,
                            icon = null
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.appName }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error getting installed apps", e)
            emptyList()
        }
    }

}