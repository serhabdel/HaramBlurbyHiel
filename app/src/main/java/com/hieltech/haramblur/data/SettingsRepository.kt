package com.hieltech.haramblur.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
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

    fun resetToDefaults() {
        val defaultSettings = AppSettings()
        updateSettings(defaultSettings)
        Log.i(TAG, "Settings reset to defaults")
    }
}