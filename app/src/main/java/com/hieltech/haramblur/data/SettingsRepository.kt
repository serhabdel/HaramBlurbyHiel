package com.hieltech.haramblur.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SettingsRepository"
        private const val SETTINGS_VERSION_KEY = "settings_version"
        private const val CURRENT_SETTINGS_VERSION = 2
    }
    
    private val _settings = MutableStateFlow(loadSettingsWithMigration())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            // Basic Detection Settings
            enableFaceDetection = prefs.getBoolean("enable_face_detection", true),
            enableNSFWDetection = prefs.getBoolean("enable_nsfw_detection", true),
            blurMaleFaces = prefs.getBoolean("blur_male_faces", true),
            blurFemaleFaces = prefs.getBoolean("blur_female_faces", true),
            detectionSensitivity = prefs.getFloat("detection_sensitivity", 0.5f),
            
            // Blur Settings
            blurIntensity = BlurIntensity.valueOf(prefs.getString("blur_intensity", BlurIntensity.MEDIUM.name)!!),
            blurStyle = BlurStyle.valueOf(prefs.getString("blur_style", BlurStyle.PIXELATED.name)!!),
            expandBlurArea = prefs.getInt("expand_blur_area", 30),
            
            // Performance Settings
            processingSpeed = ProcessingSpeed.valueOf(prefs.getString("processing_speed", ProcessingSpeed.BALANCED.name)!!),
            enableRealTimeProcessing = prefs.getBoolean("enable_realtime_processing", true),
            pauseInApps = prefs.getStringSet("pause_in_apps", emptySet()) ?: emptySet(),
            
            // Privacy Settings
            enableFullScreenBlurForNSFW = prefs.getBoolean("enable_fullscreen_nsfw_blur", true),
            showBlurBorders = prefs.getBoolean("show_blur_borders", true),
            enableHoverToReveal = prefs.getBoolean("enable_hover_reveal", false),
            
            // Enhanced Detection Settings
            genderDetectionAccuracy = GenderAccuracy.valueOf(
                prefs.getString("gender_detection_accuracy", GenderAccuracy.HIGH.name)!!
            ),
            isServicePaused = prefs.getBoolean("is_service_paused", false),
            contentDensityThreshold = prefs.getFloat("content_density_threshold", 0.4f),
            mandatoryReflectionTime = prefs.getInt("mandatory_reflection_time", 15),
            enableSiteBlocking = prefs.getBoolean("enable_site_blocking", true),
            enableQuranicGuidance = prefs.getBoolean("enable_quranic_guidance", true),
            ultraFastModeEnabled = prefs.getBoolean("ultra_fast_mode_enabled", false),
            fullScreenWarningEnabled = prefs.getBoolean("fullscreen_warning_enabled", true),
            
            // Performance Enhancement Settings
            maxProcessingTimeMs = prefs.getLong("max_processing_time_ms", 50L),
            enableGPUAcceleration = prefs.getBoolean("enable_gpu_acceleration", true),
            frameSkipThreshold = prefs.getInt("frame_skip_threshold", 3),
            imageDownscaleRatio = prefs.getFloat("image_downscale_ratio", 0.5f),
            
            // Islamic Guidance Settings
            preferredLanguage = com.hieltech.haramblur.detection.Language.valueOf(
                prefs.getString("preferred_language", com.hieltech.haramblur.detection.Language.ENGLISH.name)!!
            ),
            verseDisplayDuration = prefs.getInt("verse_display_duration", 10),
            enableArabicText = prefs.getBoolean("enable_arabic_text", true),
            customReflectionTime = prefs.getInt("custom_reflection_time", 15),
            
            // Advanced Detection Settings
            genderConfidenceThreshold = prefs.getFloat("gender_confidence_threshold", 0.8f),
            nsfwConfidenceThreshold = prefs.getFloat("nsfw_confidence_threshold", 0.7f),
            enableFallbackDetection = prefs.getBoolean("enable_fallback_detection", true),
            enablePerformanceMonitoring = prefs.getBoolean("enable_performance_monitoring", true)
        )
    }
    
    fun updateSettings(newSettings: AppSettings) {
        val validatedSettings = validateSettings(newSettings)
        _settings.value = validatedSettings
        saveSettings(validatedSettings)
    }
    
    private fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            // Basic Detection Settings
            putBoolean("enable_face_detection", settings.enableFaceDetection)
            putBoolean("enable_nsfw_detection", settings.enableNSFWDetection)
            putBoolean("blur_male_faces", settings.blurMaleFaces)
            putBoolean("blur_female_faces", settings.blurFemaleFaces)
            putFloat("detection_sensitivity", settings.detectionSensitivity)
            
            // Blur Settings
            putString("blur_intensity", settings.blurIntensity.name)
            putString("blur_style", settings.blurStyle.name)
            putInt("expand_blur_area", settings.expandBlurArea)
            
            // Performance Settings
            putString("processing_speed", settings.processingSpeed.name)
            putBoolean("enable_realtime_processing", settings.enableRealTimeProcessing)
            putStringSet("pause_in_apps", settings.pauseInApps)
            
            // Privacy Settings
            putBoolean("enable_fullscreen_nsfw_blur", settings.enableFullScreenBlurForNSFW)
            putBoolean("show_blur_borders", settings.showBlurBorders)
            putBoolean("enable_hover_reveal", settings.enableHoverToReveal)
            
            // Enhanced Detection Settings
            putString("gender_detection_accuracy", settings.genderDetectionAccuracy.name)
            putBoolean("is_service_paused", settings.isServicePaused)
            putFloat("content_density_threshold", settings.contentDensityThreshold)
            putInt("mandatory_reflection_time", settings.mandatoryReflectionTime)
            putBoolean("enable_site_blocking", settings.enableSiteBlocking)
            putBoolean("enable_quranic_guidance", settings.enableQuranicGuidance)
            putBoolean("ultra_fast_mode_enabled", settings.ultraFastModeEnabled)
            putBoolean("fullscreen_warning_enabled", settings.fullScreenWarningEnabled)
            
            // Performance Enhancement Settings
            putLong("max_processing_time_ms", settings.maxProcessingTimeMs)
            putBoolean("enable_gpu_acceleration", settings.enableGPUAcceleration)
            putInt("frame_skip_threshold", settings.frameSkipThreshold)
            putFloat("image_downscale_ratio", settings.imageDownscaleRatio)
            
            // Islamic Guidance Settings
            putString("preferred_language", settings.preferredLanguage.name)
            putInt("verse_display_duration", settings.verseDisplayDuration)
            putBoolean("enable_arabic_text", settings.enableArabicText)
            putInt("custom_reflection_time", settings.customReflectionTime)
            
            // Advanced Detection Settings
            putFloat("gender_confidence_threshold", settings.genderConfidenceThreshold)
            putFloat("nsfw_confidence_threshold", settings.nsfwConfidenceThreshold)
            putBoolean("enable_fallback_detection", settings.enableFallbackDetection)
            putBoolean("enable_performance_monitoring", settings.enablePerformanceMonitoring)
            
            apply()
        }
    }
    
    // Quick access methods for common settings
    fun getCurrentSettings(): AppSettings = _settings.value
    
    fun updateBlurIntensity(intensity: BlurIntensity) {
        updateSettings(_settings.value.copy(blurIntensity = intensity))
    }
    
    fun updateDetectionSensitivity(sensitivity: Float) {
        updateSettings(_settings.value.copy(detectionSensitivity = sensitivity))
    }
    
    fun updateGenderBlurSettings(blurMales: Boolean, blurFemales: Boolean) {
        updateSettings(_settings.value.copy(blurMaleFaces = blurMales, blurFemaleFaces = blurFemales))
    }
    
    fun updateProcessingSpeed(speed: ProcessingSpeed) {
        updateSettings(_settings.value.copy(processingSpeed = speed))
    }
    
    // Settings Migration
    private fun loadSettingsWithMigration(): AppSettings {
        val currentVersion = prefs.getInt(SETTINGS_VERSION_KEY, 1)
        
        if (currentVersion < CURRENT_SETTINGS_VERSION) {
            Log.i(TAG, "Migrating settings from version $currentVersion to $CURRENT_SETTINGS_VERSION")
            migrateSettings(currentVersion)
            prefs.edit().putInt(SETTINGS_VERSION_KEY, CURRENT_SETTINGS_VERSION).apply()
        }
        
        return loadSettings()
    }
    
    private fun migrateSettings(fromVersion: Int) {
        when (fromVersion) {
            1 -> {
                // Migration from version 1 to 2: Add enhanced detection settings with defaults
                Log.i(TAG, "Migrating from version 1: Adding enhanced detection settings")
                
                // Set default values for new enhanced settings
                prefs.edit().apply {
                    putString("gender_detection_accuracy", GenderAccuracy.HIGH.name)
                    putFloat("content_density_threshold", 0.4f)
                    putBoolean("enable_site_blocking", true)
                    putBoolean("ultra_fast_mode_enabled", false)
                    putLong("max_processing_time_ms", 50L)
                    putBoolean("enable_gpu_acceleration", true)
                    putInt("frame_skip_threshold", 3)
                    putFloat("image_downscale_ratio", 0.5f)
                    putInt("verse_display_duration", 10)
                    putBoolean("enable_arabic_text", true)
                    putInt("custom_reflection_time", 15)
                    putFloat("gender_confidence_threshold", 0.8f)
                    putFloat("nsfw_confidence_threshold", 0.7f)
                    putBoolean("enable_fallback_detection", true)
                    putBoolean("enable_performance_monitoring", true)
                    apply()
                }
            }
        }
    }
    
    // Settings Validation
    fun validateSettings(settings: AppSettings): AppSettings {
        return settings.copy(
            detectionSensitivity = settings.detectionSensitivity.coerceIn(0f, 1f),
            contentDensityThreshold = settings.contentDensityThreshold.coerceIn(0.1f, 0.8f),
            mandatoryReflectionTime = settings.mandatoryReflectionTime.coerceIn(5, 30),
            maxProcessingTimeMs = settings.maxProcessingTimeMs.coerceIn(25L, 200L),
            frameSkipThreshold = settings.frameSkipThreshold.coerceIn(1, 10),
            imageDownscaleRatio = settings.imageDownscaleRatio.coerceIn(0.25f, 1.0f),
            verseDisplayDuration = settings.verseDisplayDuration.coerceIn(5, 30),
            customReflectionTime = settings.customReflectionTime.coerceIn(5, 60),
            genderConfidenceThreshold = settings.genderConfidenceThreshold.coerceIn(0.5f, 0.95f),
            nsfwConfidenceThreshold = settings.nsfwConfidenceThreshold.coerceIn(0.5f, 0.95f),
            expandBlurArea = settings.expandBlurArea.coerceIn(0, 100)
        )
    }
    
    // Settings Backup and Restore
    fun exportSettingsToJson(): String {
        val settings = _settings.value
        val jsonObject = JSONObject().apply {
            // Basic Detection Settings
            put("enableFaceDetection", settings.enableFaceDetection)
            put("enableNSFWDetection", settings.enableNSFWDetection)
            put("blurMaleFaces", settings.blurMaleFaces)
            put("blurFemaleFaces", settings.blurFemaleFaces)
            put("detectionSensitivity", settings.detectionSensitivity)
            
            // Blur Settings
            put("blurIntensity", settings.blurIntensity.name)
            put("blurStyle", settings.blurStyle.name)
            put("expandBlurArea", settings.expandBlurArea)
            
            // Performance Settings
            put("processingSpeed", settings.processingSpeed.name)
            put("enableRealTimeProcessing", settings.enableRealTimeProcessing)
            
            // Enhanced Detection Settings
            put("genderDetectionAccuracy", settings.genderDetectionAccuracy.name)
            put("contentDensityThreshold", settings.contentDensityThreshold)
            put("mandatoryReflectionTime", settings.mandatoryReflectionTime)
            put("enableQuranicGuidance", settings.enableQuranicGuidance)
            put("ultraFastModeEnabled", settings.ultraFastModeEnabled)
            put("fullScreenWarningEnabled", settings.fullScreenWarningEnabled)
            
            // Performance Enhancement Settings
            put("maxProcessingTimeMs", settings.maxProcessingTimeMs)
            put("enableGPUAcceleration", settings.enableGPUAcceleration)
            put("frameSkipThreshold", settings.frameSkipThreshold)
            put("imageDownscaleRatio", settings.imageDownscaleRatio)
            
            // Islamic Guidance Settings
            put("preferredLanguage", settings.preferredLanguage.name)
            put("verseDisplayDuration", settings.verseDisplayDuration)
            put("enableArabicText", settings.enableArabicText)
            put("customReflectionTime", settings.customReflectionTime)
            
            // Advanced Detection Settings
            put("genderConfidenceThreshold", settings.genderConfidenceThreshold)
            put("nsfwConfidenceThreshold", settings.nsfwConfidenceThreshold)
            put("enableFallbackDetection", settings.enableFallbackDetection)
            put("enablePerformanceMonitoring", settings.enablePerformanceMonitoring)
            
            // Metadata
            put("exportVersion", CURRENT_SETTINGS_VERSION)
            put("exportTimestamp", System.currentTimeMillis())
        }
        
        return jsonObject.toString(2)
    }
    
    fun importSettingsFromJson(jsonString: String): Boolean {
        return try {
            val jsonObject = JSONObject(jsonString)
            val exportVersion = jsonObject.optInt("exportVersion", 1)
            
            if (exportVersion > CURRENT_SETTINGS_VERSION) {
                Log.w(TAG, "Settings export version $exportVersion is newer than current version $CURRENT_SETTINGS_VERSION")
                return false
            }
            
            val importedSettings = AppSettings(
                // Basic Detection Settings
                enableFaceDetection = jsonObject.optBoolean("enableFaceDetection", true),
                enableNSFWDetection = jsonObject.optBoolean("enableNSFWDetection", true),
                blurMaleFaces = jsonObject.optBoolean("blurMaleFaces", true),
                blurFemaleFaces = jsonObject.optBoolean("blurFemaleFaces", true),
                detectionSensitivity = jsonObject.optDouble("detectionSensitivity", 0.5).toFloat(),
                
                // Blur Settings
                blurIntensity = try {
                    BlurIntensity.valueOf(jsonObject.optString("blurIntensity", BlurIntensity.MEDIUM.name))
                } catch (e: IllegalArgumentException) { BlurIntensity.MEDIUM },
                blurStyle = try {
                    BlurStyle.valueOf(jsonObject.optString("blurStyle", BlurStyle.PIXELATED.name))
                } catch (e: IllegalArgumentException) { BlurStyle.PIXELATED },
                expandBlurArea = jsonObject.optInt("expandBlurArea", 30),
                
                // Performance Settings
                processingSpeed = try {
                    ProcessingSpeed.valueOf(jsonObject.optString("processingSpeed", ProcessingSpeed.BALANCED.name))
                } catch (e: IllegalArgumentException) { ProcessingSpeed.BALANCED },
                enableRealTimeProcessing = jsonObject.optBoolean("enableRealTimeProcessing", true),
                
                // Enhanced Detection Settings
                genderDetectionAccuracy = try {
                    GenderAccuracy.valueOf(jsonObject.optString("genderDetectionAccuracy", GenderAccuracy.HIGH.name))
                } catch (e: IllegalArgumentException) { GenderAccuracy.HIGH },
                contentDensityThreshold = jsonObject.optDouble("contentDensityThreshold", 0.4).toFloat(),
                mandatoryReflectionTime = jsonObject.optInt("mandatoryReflectionTime", 15),
                enableQuranicGuidance = jsonObject.optBoolean("enableQuranicGuidance", true),
                ultraFastModeEnabled = jsonObject.optBoolean("ultraFastModeEnabled", false),
                fullScreenWarningEnabled = jsonObject.optBoolean("fullScreenWarningEnabled", true),
                
                // Performance Enhancement Settings
                maxProcessingTimeMs = jsonObject.optLong("maxProcessingTimeMs", 50L),
                enableGPUAcceleration = jsonObject.optBoolean("enableGPUAcceleration", true),
                frameSkipThreshold = jsonObject.optInt("frameSkipThreshold", 3),
                imageDownscaleRatio = jsonObject.optDouble("imageDownscaleRatio", 0.5).toFloat(),
                
                // Islamic Guidance Settings
                preferredLanguage = try {
                    com.hieltech.haramblur.detection.Language.valueOf(
                        jsonObject.optString("preferredLanguage", com.hieltech.haramblur.detection.Language.ENGLISH.name)
                    )
                } catch (e: IllegalArgumentException) { com.hieltech.haramblur.detection.Language.ENGLISH },
                verseDisplayDuration = jsonObject.optInt("verseDisplayDuration", 10),
                enableArabicText = jsonObject.optBoolean("enableArabicText", true),
                customReflectionTime = jsonObject.optInt("customReflectionTime", 15),
                
                // Advanced Detection Settings
                genderConfidenceThreshold = jsonObject.optDouble("genderConfidenceThreshold", 0.8).toFloat(),
                nsfwConfidenceThreshold = jsonObject.optDouble("nsfwConfidenceThreshold", 0.7).toFloat(),
                enableFallbackDetection = jsonObject.optBoolean("enableFallbackDetection", true),
                enablePerformanceMonitoring = jsonObject.optBoolean("enablePerformanceMonitoring", true)
            )
            
            val validatedSettings = validateSettings(importedSettings)
            updateSettings(validatedSettings)
            
            Log.i(TAG, "Settings imported successfully from version $exportVersion")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings", e)
            false
        }
    }

    fun updatePermissionStatus(permissionType: String, granted: Boolean) {
        val current = _settings.value
        val updated = when (permissionType) {
            "USAGE_STATS" -> current.copy(usageStatsPermissionGranted = granted)
            "DEVICE_ADMIN" -> current.copy(deviceAdminEnabled = granted)
            "ACCESSIBILITY_SERVICE" -> current.copy(accessibilityServiceEnabled = granted)
            else -> current
        }
        updateSettings(updated)
        Log.i(TAG, "Permission status updated: $permissionType = $granted")
    }

    fun syncPermissionStatus() {
        // This method would sync permission status with system state
        // For now, it's a placeholder for future implementation
        Log.i(TAG, "Permission status sync requested")
    }

    /**
     * Mark onboarding as completed
     */
    fun markOnboardingCompleted() {
        val current = _settings.value
        val updated = current.copy(
            onboardingCompleted = true,
            permissionWizardLastShown = System.currentTimeMillis()
        )
        updateSettings(updated)
        Log.i(TAG, "Onboarding marked as completed")
    }

    /**
     * Check if onboarding is completed reactively
     */
    fun isOnboardingCompleted(): Flow<Boolean> {
        return settings.map { it.onboardingCompleted }
    }

    /**
     * Get comprehensive permission statuses
     */
    fun getAllPermissionStatuses(): Flow<Map<String, Boolean>> {
        return settings.map { appSettings ->
            mapOf(
                "USAGE_STATS" to appSettings.usageStatsPermissionGranted,
                "DEVICE_ADMIN" to appSettings.deviceAdminEnabled,
                "ACCESSIBILITY_SERVICE" to appSettings.accessibilityServiceEnabled
            )
        }
    }

    /**
     * Determine if permission wizard should be shown
     */
    fun shouldShowPermissionWizard(): Flow<Boolean> {
        return settings.map { appSettings ->
            !appSettings.onboardingCompleted ||
            !appSettings.usageStatsPermissionGranted ||
            !appSettings.accessibilityServiceEnabled
        }
    }

    /**
     * Reset onboarding for testing/troubleshooting
     */
    fun resetOnboarding() {
        val current = _settings.value
        val updated = current.copy(
            onboardingCompleted = false,
            permissionWizardLastShown = 0L,
            skipOptionalPermissions = false
        )
        updateSettings(updated)
        Log.i(TAG, "Onboarding reset for testing/troubleshooting")
    }

    /**
     * Update permission wizard last shown timestamp
     */
    fun updatePermissionWizardLastShown() {
        val current = _settings.value
        val updated = current.copy(permissionWizardLastShown = System.currentTimeMillis())
        updateSettings(updated)
        Log.i(TAG, "Permission wizard last shown timestamp updated")
    }

    /**
     * Check if user has skipped optional permissions
     */
    fun hasSkippedOptionalPermissions(): Boolean {
        return _settings.value.skipOptionalPermissions
    }

    /**
     * Mark optional permissions as skipped
     */
    fun markOptionalPermissionsSkipped() {
        val current = _settings.value
        val updated = current.copy(
            skipOptionalPermissions = true,
            permissionWizardLastShown = System.currentTimeMillis()
        )
        updateSettings(updated)
        Log.i(TAG, "Optional permissions marked as skipped")
    }

    fun resetToDefaults() {
        val defaultSettings = AppSettings()
        updateSettings(defaultSettings)
        Log.i(TAG, "Settings reset to defaults")
    }

    // Enhanced Preset Management Methods

    /**
     * Export preset to file for sharing
     */
    fun exportPresetToFile(context: Context, preset: PresetData): Uri? {
        return try {
            val fileName = "${preset.name.replace("\\s+".toRegex(), "_")}_${System.currentTimeMillis()}.hbpreset"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                val presetJson = JSONObject().apply {
                    put("name", preset.name)
                    put("description", preset.description)
                    put("version", preset.version)
                    put("creationTimestamp", preset.creationTimestamp)

                    // Metadata
                    put("metadata", JSONObject().apply {
                        put("category", preset.metadata.category.name)
                        put("difficulty", preset.metadata.difficulty.name)
                        put("useCase", preset.metadata.useCase)
                        put("author", preset.metadata.author)
                        put("isBuiltIn", preset.metadata.isBuiltIn)
                        put("tags", preset.metadata.tags.joinToString(","))
                    })

                    // Settings
                    put("settings", JSONObject().apply {
                        val settings = preset.settings
                        put("enableFaceDetection", settings.enableFaceDetection)
                        put("enableNSFWDetection", settings.enableNSFWDetection)
                        put("blurMaleFaces", settings.blurMaleFaces)
                        put("blurFemaleFaces", settings.blurFemaleFaces)
                        put("detectionSensitivity", settings.detectionSensitivity)
                        put("blurIntensity", settings.blurIntensity.name)
                        put("blurStyle", settings.blurStyle.name)
                        put("expandBlurArea", settings.expandBlurArea)
                        put("processingSpeed", settings.processingSpeed.name)
                        put("enableRealTimeProcessing", settings.enableRealTimeProcessing)
                        put("enableFullScreenBlurForNSFW", settings.enableFullScreenBlurForNSFW)
                        put("showBlurBorders", settings.showBlurBorders)
                        put("enableHoverToReveal", settings.enableHoverToReveal)
                        put("genderDetectionAccuracy", settings.genderDetectionAccuracy.name)
                        put("contentDensityThreshold", settings.contentDensityThreshold)
                        put("mandatoryReflectionTime", settings.mandatoryReflectionTime)
                        put("enableSiteBlocking", settings.enableSiteBlocking)
                        put("enableQuranicGuidance", settings.enableQuranicGuidance)
                        put("ultraFastModeEnabled", settings.ultraFastModeEnabled)
                        put("fullScreenWarningEnabled", settings.fullScreenWarningEnabled)
                        put("maxProcessingTimeMs", settings.maxProcessingTimeMs)
                        put("enableGPUAcceleration", settings.enableGPUAcceleration)
                        put("frameSkipThreshold", settings.frameSkipThreshold)
                        put("imageDownscaleRatio", settings.imageDownscaleRatio)
                        put("preferredLanguage", settings.preferredLanguage.name)
                        put("verseDisplayDuration", settings.verseDisplayDuration)
                        put("enableArabicText", settings.enableArabicText)
                        put("customReflectionTime", settings.customReflectionTime)
                        put("genderConfidenceThreshold", settings.genderConfidenceThreshold)
                        put("nsfwConfidenceThreshold", settings.nsfwConfidenceThreshold)
                        put("enableFallbackDetection", settings.enableFallbackDetection)
                        put("enablePerformanceMonitoring", settings.enablePerformanceMonitoring)
                        put("enableLLMDecisionMaking", settings.enableLLMDecisionMaking)
                        put("openRouterApiKey", settings.openRouterApiKey)
                        put("llmModel", settings.llmModel)
                        put("llmTimeoutMs", settings.llmTimeoutMs)
                        put("llmFallbackToRules", settings.llmFallbackToRules)
                        put("enableDetailedLogging", settings.enableDetailedLogging)
                        put("logLevel", settings.logLevel.name)
                        put("enablePerformanceLogging", settings.enablePerformanceLogging)
                        put("enableErrorReporting", settings.enableErrorReporting)
                        put("enableUserActionLogging", settings.enableUserActionLogging)
                        put("maxLogRetentionDays", settings.maxLogRetentionDays)
                        put("enableEnhancedBlocking", settings.enableEnhancedBlocking)
                        put("preferredBlockingMethod", settings.preferredBlockingMethod.name)
                        put("forceCloseTimeout", settings.forceCloseTimeout)
                        put("settingsVersion", settings.settingsVersion)
                    })
                }
                writer.write(presetJson.toString(2))
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export preset to file", e)
            null
        }
    }

    /**
     * Import preset from file
     */
    fun importPresetFromFile(context: Context, uri: Uri): PresetImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return PresetImportResult.Error("Cannot open file", ImportErrorType.INVALID_JSON)

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val presetData = PresetManager.validatePresetData(jsonString)

            if (!presetData.isValid) {
                return PresetImportResult.Error(
                    presetData.errors.joinToString("\n"),
                    ImportErrorType.INVALID_JSON
                )
            }

            // Parse preset data
            val jsonObject = JSONObject(jsonString)
            val settingsJson = jsonObject.getJSONObject("settings")

            val importedSettings = AppSettings(
                enableFaceDetection = settingsJson.optBoolean("enableFaceDetection", true),
                enableNSFWDetection = settingsJson.optBoolean("enableNSFWDetection", true),
                blurMaleFaces = settingsJson.optBoolean("blurMaleFaces", false),
                blurFemaleFaces = settingsJson.optBoolean("blurFemaleFaces", true),
                detectionSensitivity = settingsJson.optDouble("detectionSensitivity", 0.8).toFloat(),
                blurIntensity = try {
                    BlurIntensity.valueOf(settingsJson.optString("blurIntensity", BlurIntensity.STRONG.name))
                } catch (e: IllegalArgumentException) { BlurIntensity.STRONG },
                blurStyle = try {
                    BlurStyle.valueOf(settingsJson.optString("blurStyle", BlurStyle.ARTISTIC.name))
                } catch (e: IllegalArgumentException) { BlurStyle.ARTISTIC },
                expandBlurArea = settingsJson.optInt("expandBlurArea", 30),
                processingSpeed = try {
                    ProcessingSpeed.valueOf(settingsJson.optString("processingSpeed", ProcessingSpeed.BALANCED.name))
                } catch (e: IllegalArgumentException) { ProcessingSpeed.BALANCED },
                enableRealTimeProcessing = settingsJson.optBoolean("enableRealTimeProcessing", true),
                enableFullScreenBlurForNSFW = settingsJson.optBoolean("enableFullScreenBlurForNSFW", true),
                showBlurBorders = settingsJson.optBoolean("showBlurBorders", true),
                enableHoverToReveal = settingsJson.optBoolean("enableHoverToReveal", false),
                genderDetectionAccuracy = try {
                    GenderAccuracy.valueOf(settingsJson.optString("genderDetectionAccuracy", GenderAccuracy.BALANCED.name))
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
                enableLLMDecisionMaking = settingsJson.optBoolean("enableLLMDecisionMaking", false),
                openRouterApiKey = settingsJson.optString("openRouterApiKey", ""),
                llmModel = settingsJson.optString("llmModel", "google/gemma-2-9b-it:free"),
                llmTimeoutMs = settingsJson.optLong("llmTimeoutMs", 3000L),
                llmFallbackToRules = settingsJson.optBoolean("llmFallbackToRules", true),
                enableDetailedLogging = settingsJson.optBoolean("enableDetailedLogging", true),
                logLevel = try {
                    LogLevel.valueOf(settingsJson.optString("logLevel", LogLevel.INFO.name))
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
            Log.e(TAG, "Failed to import preset from file", e)
            PresetImportResult.Error("Failed to read preset file: ${e.message}", ImportErrorType.INVALID_JSON)
        }
    }

    /**
     * Get built-in preset templates
     */
    fun getPresetTemplates(): List<PresetTemplate> {
        return PresetManager.getPresetTemplates()
    }

    /**
     * Validate preset data with enhanced validation
     */
    fun validatePresetData(presetJson: String): ValidationResult {
        return PresetManager.validatePresetData(presetJson)
    }

    /**
     * Merge preset with current settings using specified strategy
     */
    fun mergePresetWithCurrent(preset: PresetData, strategy: MergeStrategy): AppSettings {
        val current = _settings.value

        return when (strategy) {
            MergeStrategy.REPLACE_ALL -> preset.settings
            MergeStrategy.MERGE_COMPATIBLE -> {
                // Keep current values for certain sensitive settings
                preset.settings.copy(
                    openRouterApiKey = current.openRouterApiKey, // Preserve API key
                    enableDetailedLogging = current.enableDetailedLogging, // Keep logging preference
                    logLevel = current.logLevel
                )
            }
            MergeStrategy.USER_CHOICE -> {
                // This would be handled in the UI layer with user confirmation
                preset.settings
            }
        }
    }

    /**
     * Backup current settings as JSON string
     */
    fun backupCurrentSettings(): String {
        return exportSettingsToJson()
    }

    /**
     * Enhanced export with metadata
     */
    fun exportSettingsToJsonWithMetadata(): String {
        val settings = _settings.value
        val jsonObject = JSONObject().apply {
            // Basic settings
            put("enableFaceDetection", settings.enableFaceDetection)
            put("enableNSFWDetection", settings.enableNSFWDetection)
            put("blurMaleFaces", settings.blurMaleFaces)
            put("blurFemaleFaces", settings.blurFemaleFaces)
            put("detectionSensitivity", settings.detectionSensitivity)
            put("blurIntensity", settings.blurIntensity.name)
            put("blurStyle", settings.blurStyle.name)
            put("expandBlurArea", settings.expandBlurArea)
            put("processingSpeed", settings.processingSpeed.name)
            put("enableRealTimeProcessing", settings.enableRealTimeProcessing)
            put("enableFullScreenBlurForNSFW", settings.enableFullScreenBlurForNSFW)
            put("showBlurBorders", settings.showBlurBorders)
            put("enableHoverToReveal", settings.enableHoverToReveal)
            put("genderDetectionAccuracy", settings.genderDetectionAccuracy.name)
            put("contentDensityThreshold", settings.contentDensityThreshold)
            put("mandatoryReflectionTime", settings.mandatoryReflectionTime)
            put("enableSiteBlocking", settings.enableSiteBlocking)
            put("enableQuranicGuidance", settings.enableQuranicGuidance)
            put("ultraFastModeEnabled", settings.ultraFastModeEnabled)
            put("fullScreenWarningEnabled", settings.fullScreenWarningEnabled)
            put("maxProcessingTimeMs", settings.maxProcessingTimeMs)
            put("enableGPUAcceleration", settings.enableGPUAcceleration)
            put("frameSkipThreshold", settings.frameSkipThreshold)
            put("imageDownscaleRatio", settings.imageDownscaleRatio)
            put("preferredLanguage", settings.preferredLanguage.name)
            put("verseDisplayDuration", settings.verseDisplayDuration)
            put("enableArabicText", settings.enableArabicText)
            put("customReflectionTime", settings.customReflectionTime)
            put("genderConfidenceThreshold", settings.genderConfidenceThreshold)
            put("nsfwConfidenceThreshold", settings.nsfwConfidenceThreshold)
            put("enableFallbackDetection", settings.enableFallbackDetection)
            put("enablePerformanceMonitoring", settings.enablePerformanceMonitoring)
            put("enableLLMDecisionMaking", settings.enableLLMDecisionMaking)
            put("llmModel", settings.llmModel)
            put("llmTimeoutMs", settings.llmTimeoutMs)
            put("llmFallbackToRules", settings.llmFallbackToRules)
            put("enableDetailedLogging", settings.enableDetailedLogging)
            put("logLevel", settings.logLevel.name)
            put("enablePerformanceLogging", settings.enablePerformanceLogging)
            put("enableErrorReporting", settings.enableErrorReporting)
            put("enableUserActionLogging", settings.enableUserActionLogging)
            put("maxLogRetentionDays", settings.maxLogRetentionDays)
            put("enableEnhancedBlocking", settings.enableEnhancedBlocking)
            put("preferredBlockingMethod", settings.preferredBlockingMethod.name)
            put("forceCloseTimeout", settings.forceCloseTimeout)
            put("currentPreset", settings.currentPreset)
            put("lastPresetUpdate", settings.lastPresetUpdate)
            put("presetLockEnabled", settings.presetLockEnabled)
            put("settingsVersion", settings.settingsVersion)

            // Metadata
            put("exportVersion", CURRENT_SETTINGS_VERSION)
            put("exportTimestamp", System.currentTimeMillis())
            put("appVersion", "1.0.0") // TODO: Get from BuildConfig
            put("deviceModel", android.os.Build.MODEL)
            put("androidVersion", android.os.Build.VERSION.RELEASE)
        }

        return jsonObject.toString(2)
    }

    /**
     * Get settings difference between current and imported settings
     */
    fun getSettingsDiff(current: AppSettings, imported: AppSettings): SettingsDiff {
        return PresetManager.calculateSettingsDiff(current, imported)
    }

    /**
     * Save preset locally for user-created presets
     */
    fun savePresetLocally(name: String, settings: AppSettings) {
        val presetData = PresetData(
            name = name,
            description = "User-created preset",
            settings = settings,
            metadata = PresetMetadata(
                category = PresetCategory.CUSTOM,
                difficulty = PresetDifficulty.INTERMEDIATE,
                useCase = "Custom user configuration",
                isBuiltIn = false
            )
        )

        // Save to shared preferences as JSON
        val presetJson = JSONObject().apply {
            put("name", presetData.name)
            put("settings", JSONObject().apply {
                put("enableFaceDetection", settings.enableFaceDetection)
                put("enableNSFWDetection", settings.enableNSFWDetection)
                put("detectionSensitivity", settings.detectionSensitivity)
                put("blurIntensity", settings.blurIntensity.name)
                // Add other essential settings...
            })
        }

        prefs.edit().putString("user_preset_$name", presetJson.toString()).apply()
        Log.i(TAG, "User preset saved locally: $name")
    }

    suspend fun toggleServicePause(paused: Boolean) {
        val current = getCurrentSettings()
        updateSettings(current.copy(isServicePaused = paused))
    }
}