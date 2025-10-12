package com.hieltech.haramblur.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.models.DetectionScope
import com.hieltech.haramblur.data.models.DetectionMode
import com.hieltech.haramblur.data.models.RecentSetting
import com.hieltech.haramblur.data.models.UsageTimeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
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
        private const val CURRENT_SETTINGS_VERSION = 12
    }
    
    private val _settings = MutableStateFlow(loadSettingsWithMigration())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    /**
     * Helper function to safely get nullable string from JSONObject
     */
    private fun getNullableString(jsonObject: JSONObject, key: String): String? {
        return if (jsonObject.has(key) && !jsonObject.isNull(key)) {
            val value = jsonObject.getString(key)
            if (value.isNullOrBlank()) null else value
        } else {
            null
        }
    }
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            // Quality Mode
            qualityMode = try {
                val modeName = prefs.getString("quality_mode", QualityMode.HIGH_QUALITY.name)
                if (modeName != null) QualityMode.valueOf(modeName) else QualityMode.HIGH_QUALITY
            } catch (e: Exception) {
                QualityMode.HIGH_QUALITY // Default to high quality on error
            },
            
            // Basic Detection Settings
            enableFaceDetection = prefs.getBoolean("enable_face_detection", true),
            enableNSFWDetection = prefs.getBoolean("enable_nsfw_detection", true),
            blurMaleFaces = prefs.getBoolean("blur_male_faces", true),
            blurFemaleFaces = prefs.getBoolean("blur_female_faces", true),
            detectionSensitivity = prefs.getFloat("detection_sensitivity", 0.8f),
            userGender = try {
                UserGender.valueOf(prefs.getString("user_gender", UserGender.NOT_SPECIFIED.name)!!)
            } catch (e: Exception) {
                UserGender.NOT_SPECIFIED // Fallback to NOT_SPECIFIED on error
            },
            
            // Blur Settings
            blurIntensity = BlurIntensity.valueOf(prefs.getString("blur_intensity", BlurIntensity.MEDIUM.name)!!),
            blurStyle = BlurStyle.valueOf(prefs.getString("blur_style", BlurStyle.PIXELATED.name)!!),
            expandBlurArea = prefs.getInt("expand_blur_area", 30),
            
            // Performance Settings
            processingSpeed = ProcessingSpeed.valueOf(prefs.getString("processing_speed", QualityMode.HIGH_QUALITY.processingSpeed.name)!!), // Aligned with HIGH_QUALITY
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
            maxProcessingTimeMs = prefs.getLong("max_processing_time_ms", QualityMode.HIGH_QUALITY.maxProcessingTimeMs), // Aligned with HIGH_QUALITY
            enableGPUAcceleration = prefs.getBoolean("enable_gpu_acceleration", QualityMode.HIGH_QUALITY.enableGPUAcceleration), // Aligned with HIGH_QUALITY
            frameSkipThreshold = prefs.getInt("frame_skip_threshold", QualityMode.HIGH_QUALITY.frameSkipThreshold), // Aligned with HIGH_QUALITY
            imageDownscaleRatio = prefs.getFloat("image_downscale_ratio", QualityMode.HIGH_QUALITY.imageDownscaleRatio), // Aligned with HIGH_QUALITY
            
            // Islamic Guidance Settings
            preferredLanguage = com.hieltech.haramblur.detection.Language.valueOf(
                prefs.getString("preferred_language", com.hieltech.haramblur.detection.Language.ENGLISH.name)!!
            ),
            verseDisplayDuration = prefs.getInt("verse_display_duration", 10),
            enableArabicText = prefs.getBoolean("enable_arabic_text", true),
            customReflectionTime = prefs.getInt("custom_reflection_time", 15),
            
            // Advanced Detection Settings
            genderConfidenceThreshold = prefs.getFloat("gender_confidence_threshold", 0.4f),
            nsfwConfidenceThreshold = prefs.getFloat("nsfw_confidence_threshold", 0.7f),
            enableFallbackDetection = prefs.getBoolean("enable_fallback_detection", true),
            enablePerformanceMonitoring = prefs.getBoolean("enable_performance_monitoring", true),
            
            // Dhikr Settings
            dhikrEnabled = prefs.getBoolean("dhikr_enabled", true),
            dhikrMorningEnabled = prefs.getBoolean("dhikr_morning_enabled", true),
            dhikrEveningEnabled = prefs.getBoolean("dhikr_evening_enabled", true),
            dhikrAnytimeEnabled = prefs.getBoolean("dhikr_anytime_enabled", true),
            dhikrMorningStart = prefs.getInt("dhikr_morning_start", 5),
            dhikrMorningEnd = prefs.getInt("dhikr_morning_end", 10),
            dhikrEveningStart = prefs.getInt("dhikr_evening_start", 17),
            dhikrEveningEnd = prefs.getInt("dhikr_evening_end", 22),
            dhikrIntervalMinutes = prefs.getInt("dhikr_interval_minutes", 15),
            dhikrDisplayDuration = prefs.getInt("dhikr_display_duration", 30),
            dhikrShowTransliteration = prefs.getBoolean("dhikr_show_transliteration", true),
            dhikrShowTranslation = prefs.getBoolean("dhikr_show_translation", true),
            dhikrPosition = prefs.getString("dhikr_position", "TOP_RIGHT") ?: "TOP_RIGHT",
            dhikrAnimationEnabled = prefs.getBoolean("dhikr_animation_enabled", true),
            dhikrSoundEnabled = prefs.getBoolean("dhikr_sound_enabled", false),
            dhikrSleepStartMinutes = prefs.getInt("dhikr_sleep_start_minutes", 1350),
            dhikrSleepEndMinutes = prefs.getInt("dhikr_sleep_end_minutes", 390),

            // Islamic Calendar & Prayer Times (persist previously missing fields)
            enableIslamicCalendar = prefs.getBoolean("enable_islamic_calendar", true),
            enablePrayerTimes = prefs.getBoolean("enable_prayer_times", true),
            enablePrayerNotifications = prefs.getBoolean("enable_prayer_notifications", true),
            prayerCalculationMethod = prefs.getInt("prayer_calculation_method", 2),
            prayerNotificationAdvanceTime = prefs.getInt("prayer_notification_advance_time", 15),
            locationLatitude = prefs.getString("location_latitude", null)?.toDoubleOrNull(),
            locationLongitude = prefs.getString("location_longitude", null)?.toDoubleOrNull(),
            locationCity = prefs.getString("location_city", null),
            locationCountry = prefs.getString("location_country", null),
            locationCountryCode = prefs.getString("location_country_code", null),
            enableQiblaDirection = prefs.getBoolean("enable_qibla_direction", true),
            prayerTimesUpdateInterval = prefs.getInt("prayer_times_update_interval", 30),
            islamicCalendarUpdateInterval = prefs.getInt("islamic_calendar_update_interval", 60),
            autoDetectLocation = prefs.getBoolean("auto_detect_location", true),
            locationMethod = try {
                LocationMethod.valueOf(prefs.getString("location_method", LocationMethod.GPS.name)!!)
            } catch (e: IllegalArgumentException) { LocationMethod.GPS },
            locationAccuracy = prefs.getString("location_accuracy", null)?.toFloatOrNull(),
            locationLastUpdated = prefs.getLong("location_last_updated", 0L).let { if (it <= 0L) null else it },
            locationPermissionStatus = try {
                LocationPermissionStatus.valueOf(
                    prefs.getString("location_permission_status", LocationPermissionStatus.UNKNOWN.name)!!
                )
            } catch (e: IllegalArgumentException) { LocationPermissionStatus.UNKNOWN },

            // New city selection fields
            selectedCityName = prefs.getString("selected_city_name", null),
            selectedCountry = prefs.getString("selected_country", null),
            selectedCountryCode = prefs.getString("selected_country_code", null),
            selectedLatitude = prefs.getString("selected_latitude", null)?.toDoubleOrNull(),
            selectedLongitude = prefs.getString("selected_longitude", null)?.toDoubleOrNull(),

            // City search behavior
            enableCitySearchCache = prefs.getBoolean("enable_city_search_cache", true),
            enableOfflineCityFallback = prefs.getBoolean("enable_offline_city_fallback", true),
            preferStoredCoordinates = prefs.getBoolean("prefer_stored_coordinates", true),

            // Prayer Enhancements: Offsets, history, caching and validation
            fajrOffsetMinutes = prefs.getInt("fajr_offset_minutes", 0),
            sunriseOffsetMinutes = prefs.getInt("sunrise_offset_minutes", 0),
            dhuhrOffsetMinutes = prefs.getInt("dhuhr_offset_minutes", 0),
            asrOffsetMinutes = prefs.getInt("asr_offset_minutes", 0),
            maghribOffsetMinutes = prefs.getInt("maghrib_offset_minutes", 0),
            ishaOffsetMinutes = prefs.getInt("isha_offset_minutes", 0),
            enablePrayerHistory = prefs.getBoolean("enable_prayer_history", true),
            prayerHistoryRetentionDays = prefs.getInt("prayer_history_retention_days", 60),
            prayerCacheTtlMinutes = prefs.getInt("prayer_cache_ttl_minutes", 30),
            locationStaleAfterMinutes = prefs.getInt("location_stale_after_minutes", 60),
            strictPrayerAccuracyValidation = prefs.getBoolean("strict_prayer_accuracy_validation", true),
            maxAllowedPrayerShiftMinutes = prefs.getInt("max_allowed_prayer_shift_minutes", 20),
            gpsAccuracyHighThresholdM = prefs.getFloat("gps_accuracy_high_threshold_m", 30f),
            gpsAccuracyMediumThresholdM = prefs.getFloat("gps_accuracy_medium_threshold_m", 100f),
            gpsAccuracyLowThresholdM = prefs.getFloat("gps_accuracy_low_threshold_m", 300f),

            // Qibla Compass Settings
            qiblaCompassEnabled = prefs.getBoolean("qibla_compass_enabled", true),
            compassCalibrationReminders = prefs.getBoolean("compass_calibration_reminders", true),
            compassHapticFeedback = prefs.getBoolean("compass_haptic_feedback", true),
            compassShowDegreeMarkings = prefs.getBoolean("compass_show_degree_markings", true),
            compassSensitivity = prefs.getFloat("compass_sensitivity", 1.0f),
            compassAccuracyThreshold = prefs.getFloat("compass_accuracy_threshold", 20.0f),
            qiblaToleranceDegrees = prefs.getFloat("qibla_tolerance_degrees", 5.0f),
            compassAnimationSpeed = prefs.getFloat("compass_animation_speed", 1.0f),
            lastCompassCalibration = prefs.getLong("last_compass_calibration", 0L),
            compassPreferredSize = try {
                com.hieltech.haramblur.data.compass.CompassSize.valueOf(
                    prefs.getString("compass_preferred_size", com.hieltech.haramblur.data.compass.CompassSize.MEDIUM.name)!!
                )
            } catch (e: IllegalArgumentException) { com.hieltech.haramblur.data.compass.CompassSize.MEDIUM },
            enableMagneticDeclination = prefs.getBoolean("enable_magnetic_declination", true),
            compassUpdateRate = prefs.getInt("compass_update_rate", 15),

            // App-Specific Detection Settings
            enableAppSpecificDetection = prefs.getBoolean("enable_app_specific_detection", true),
            monitoredAppCategories = loadMonitoredAppCategories(),
            customMonitoredApps = prefs.getStringSet("custom_monitored_apps", emptySet()) ?: emptySet(),
            excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet(),

            // Usage Time Tracking Settings
            enableUsageTimeNotifications = prefs.getBoolean("enable_usage_time_notifications", true),
            defaultSocialMediaTimeLimit = prefs.getInt("default_social_media_time_limit", 60),
            defaultMessagingTimeLimit = prefs.getInt("default_messaging_time_limit", 120),
            customAppTimeLimits = loadCustomAppTimeLimits(),
            usageNotificationFrequency = prefs.getInt("usage_notification_frequency", 30),
            enableDailyUsageReset = prefs.getBoolean("enable_daily_usage_reset", true),
            lastUsageResetDate = prefs.getLong("last_usage_reset_date", 0L).let { if (it <= 0L) null else it },
            
            // Local Prayer Calculation Settings
            enableLocalCalculations = prefs.getBoolean("enable_local_calculations", false),
            preferLocalOverApi = prefs.getBoolean("prefer_local_over_api", false),
            showCalculationMethod = prefs.getBoolean("show_calculation_method", true),
            moroccoSpecificAdjustments = prefs.getBoolean("morocco_specific_adjustments", true),

            // Blur Animation Settings
            enableSmoothBlurAnimations = prefs.getBoolean("enable_smooth_blur_animations", true),
            blurAnimationDuration = prefs.getInt("blur_animation_duration", 250),
            blurTransitionDuration = prefs.getInt("blur_transition_duration", 150),
            enableBlurRegionInterpolation = prefs.getBoolean("enable_blur_region_interpolation", true),

            // Blur Performance Optimization Settings
            enableHardwareBlurAcceleration = prefs.getBoolean("enable_hardware_blur_acceleration", true),
            blurRenderingMode = try {
                BlurRenderingMode.valueOf(prefs.getString("blur_rendering_mode", BlurRenderingMode.SMOOTH.name)!!)
            } catch (e: Exception) {
                BlurRenderingMode.SMOOTH // Default to SMOOTH on error
            },
            maxBlurRegionsPerFrame = prefs.getInt("max_blur_regions_per_frame", 12),
            enableBlurFrameRateLimiting = prefs.getBoolean("enable_blur_frame_rate_limiting", true),

            // Blur Edge Refinement Settings
            enableBlurEdgeRefinement = prefs.getBoolean("enable_blur_edge_refinement", true),
            blurEdgeAntiAliasing = prefs.getBoolean("blur_edge_anti_aliasing", true),
            blurBoundaryPrecision = prefs.getFloat("blur_boundary_precision", 0.5f)
        )
    }

    /**
     * Load monitored app categories from SharedPreferences
     */
    private fun loadMonitoredAppCategories(): Set<AppCategory> {
        val categoriesJson = prefs.getString("monitored_app_categories", null)
        if (categoriesJson.isNullOrBlank()) {
            return setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)
        }

        return try {
            val jsonObject = JSONObject(categoriesJson)
            val categories = mutableSetOf<AppCategory>()
            val keys = jsonObject.keys()
            var foundUnknownCategory = false

            while (keys.hasNext()) {
                val categoryName = keys.next()
                if (jsonObject.optBoolean(categoryName, false)) {
                    try {
                        categories.add(AppCategory.valueOf(categoryName))
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown AppCategory in stored preferences: $categoryName - skipping")
                        foundUnknownCategory = true
                    }
                }
            }

            // If we found unknown categories, sanitize the stored preferences
            if (foundUnknownCategory && categories.isNotEmpty()) {
                prefs.edit().putString("monitored_app_categories", saveMonitoredAppCategories(categories)).apply()
                Log.i(TAG, "Sanitized monitored app categories - removed unknown categories")
            }

            // Ensure we have at least the defaults if everything was filtered out
            if (categories.isEmpty()) {
                Log.w(TAG, "No valid categories found, falling back to defaults")
                setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)
            } else {
                categories
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load monitored app categories", e)
            setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)
        }
    }

    /**
     * Load a presence map from SharedPreferences and convert to Set<String>.
     *
     * Storage format: JSON object where keys are set elements and values are boolean 'true'.
     * Example: {"com.instagram.android": true, "com.facebook.katana": true}
     * This format allows for efficient lookups and easy extension to store additional metadata.
     */
    private fun loadPresenceMapAsSet(key: String): Set<String> {
        val jsonString = prefs.getString(key, null)
        if (jsonString.isNullOrBlank()) return emptySet()

        return try {
            val jsonObject = JSONObject(jsonString)
            val set = mutableSetOf<String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                set.add(keys.next())
            }
            set
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load presence map as set for key: $key", e)
            emptySet()
        }
    }

    /**
     * Load custom app time limits from SharedPreferences
     */
    private fun loadCustomAppTimeLimits(): Map<String, Int> {
        val jsonString = prefs.getString("custom_app_time_limits", null)
        if (jsonString.isNullOrBlank()) return emptyMap()

        return try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Int>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.optInt(key, 0)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom app time limits", e)
            emptyMap()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        try {
            val validatedSettings = validateSettings(newSettings)
            
            // Update state first
            _settings.value = validatedSettings
            
            // Force immediate save to prevent loss
            saveSettings(validatedSettings)
            
            Log.v(TAG, "Settings updated and saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update settings", e)
            throw e // Re-throw to let caller handle
        }
    }
    
    private fun saveSettings(settings: AppSettings) {
        try {
            Log.v(TAG, "Saving settings to SharedPreferences")
            prefs.edit().apply {
            // Quality Mode
            putString("quality_mode", settings.qualityMode.name)
            
            // Basic Detection Settings
            putBoolean("enable_face_detection", settings.enableFaceDetection)
            putBoolean("enable_nsfw_detection", settings.enableNSFWDetection)
            putBoolean("blur_male_faces", settings.blurMaleFaces)
            putBoolean("blur_female_faces", settings.blurFemaleFaces)
            putFloat("detection_sensitivity", settings.detectionSensitivity)
            putString("user_gender", settings.userGender.name) // CRITICAL: Save gender preference
            
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
            
            // Dhikr Settings
            putBoolean("dhikr_enabled", settings.dhikrEnabled)
            putBoolean("dhikr_morning_enabled", settings.dhikrMorningEnabled)
            putBoolean("dhikr_evening_enabled", settings.dhikrEveningEnabled)
            putBoolean("dhikr_anytime_enabled", settings.dhikrAnytimeEnabled)
            putInt("dhikr_morning_start", settings.dhikrMorningStart)
            putInt("dhikr_morning_end", settings.dhikrMorningEnd)
            putInt("dhikr_evening_start", settings.dhikrEveningStart)
            putInt("dhikr_evening_end", settings.dhikrEveningEnd)
            putInt("dhikr_interval_minutes", settings.dhikrIntervalMinutes)
            putInt("dhikr_display_duration", settings.dhikrDisplayDuration)
            putBoolean("dhikr_show_transliteration", settings.dhikrShowTransliteration)
            putBoolean("dhikr_show_translation", settings.dhikrShowTranslation)
            putString("dhikr_position", settings.dhikrPosition)
            putBoolean("dhikr_animation_enabled", settings.dhikrAnimationEnabled)
            putBoolean("dhikr_sound_enabled", settings.dhikrSoundEnabled)
            putInt("dhikr_sleep_start_minutes", settings.dhikrSleepStartMinutes)
            putInt("dhikr_sleep_end_minutes", settings.dhikrSleepEndMinutes)

            // Islamic Calendar & Prayer Times (persist previously missing fields)
            putBoolean("enable_islamic_calendar", settings.enableIslamicCalendar)
            putBoolean("enable_prayer_times", settings.enablePrayerTimes)
            putBoolean("enable_prayer_notifications", settings.enablePrayerNotifications)
            putInt("prayer_calculation_method", settings.prayerCalculationMethod)
            putInt("prayer_notification_advance_time", settings.prayerNotificationAdvanceTime)
            putString("location_latitude", settings.locationLatitude?.toString())
            putString("location_longitude", settings.locationLongitude?.toString())
            putString("location_city", settings.locationCity)
            putString("location_country", settings.locationCountry)
            putString("location_country_code", settings.locationCountryCode)
            putBoolean("enable_qibla_direction", settings.enableQiblaDirection)
            putInt("prayer_times_update_interval", settings.prayerTimesUpdateInterval)
            putInt("islamic_calendar_update_interval", settings.islamicCalendarUpdateInterval)
            putBoolean("auto_detect_location", settings.autoDetectLocation)
            putString("location_method", settings.locationMethod.name)
            putString("location_accuracy", settings.locationAccuracy?.toString())
            putLong("location_last_updated", settings.locationLastUpdated ?: 0L)
            putString("location_permission_status", settings.locationPermissionStatus.name)

            // New city selection fields
            putString("selected_city_name", settings.selectedCityName)
            putString("selected_country", settings.selectedCountry)
            putString("selected_country_code", settings.selectedCountryCode)
            putString("selected_latitude", settings.selectedLatitude?.toString())
            putString("selected_longitude", settings.selectedLongitude?.toString())

            // City search behavior
            putBoolean("enable_city_search_cache", settings.enableCitySearchCache)
            putBoolean("enable_offline_city_fallback", settings.enableOfflineCityFallback)
            putBoolean("prefer_stored_coordinates", settings.preferStoredCoordinates)

            // Prayer Enhancements: Offsets, history, caching and validation
            putInt("fajr_offset_minutes", settings.fajrOffsetMinutes)
            putInt("sunrise_offset_minutes", settings.sunriseOffsetMinutes)
            putInt("dhuhr_offset_minutes", settings.dhuhrOffsetMinutes)
            putInt("asr_offset_minutes", settings.asrOffsetMinutes)
            putInt("maghrib_offset_minutes", settings.maghribOffsetMinutes)
            putInt("isha_offset_minutes", settings.ishaOffsetMinutes)
            putBoolean("enable_prayer_history", settings.enablePrayerHistory)
            putInt("prayer_history_retention_days", settings.prayerHistoryRetentionDays)
            putInt("prayer_cache_ttl_minutes", settings.prayerCacheTtlMinutes)
            putInt("location_stale_after_minutes", settings.locationStaleAfterMinutes)
            putBoolean("strict_prayer_accuracy_validation", settings.strictPrayerAccuracyValidation)
            putInt("max_allowed_prayer_shift_minutes", settings.maxAllowedPrayerShiftMinutes)
            putFloat("gps_accuracy_high_threshold_m", settings.gpsAccuracyHighThresholdM)
            putFloat("gps_accuracy_medium_threshold_m", settings.gpsAccuracyMediumThresholdM)
            putFloat("gps_accuracy_low_threshold_m", settings.gpsAccuracyLowThresholdM)

            // Qibla Compass Settings
            putBoolean("qibla_compass_enabled", settings.qiblaCompassEnabled)
            putBoolean("compass_calibration_reminders", settings.compassCalibrationReminders)
            putBoolean("compass_haptic_feedback", settings.compassHapticFeedback)
            putBoolean("compass_show_degree_markings", settings.compassShowDegreeMarkings)
            putFloat("compass_sensitivity", settings.compassSensitivity)
            putFloat("compass_accuracy_threshold", settings.compassAccuracyThreshold)
            putFloat("qibla_tolerance_degrees", settings.qiblaToleranceDegrees)
            putFloat("compass_animation_speed", settings.compassAnimationSpeed)
            putLong("last_compass_calibration", settings.lastCompassCalibration)
            putString("compass_preferred_size", settings.compassPreferredSize.name)
            putBoolean("enable_magnetic_declination", settings.enableMagneticDeclination)
            putInt("compass_update_rate", settings.compassUpdateRate)

            // App-Specific Detection Settings
            putBoolean("enable_app_specific_detection", settings.enableAppSpecificDetection)
            putString("monitored_app_categories", saveMonitoredAppCategories(settings.monitoredAppCategories))
            putStringSet("custom_monitored_apps", settings.customMonitoredApps)
            putStringSet("excluded_apps", settings.excludedApps)

            // Usage Time Tracking Settings
            putBoolean("enable_usage_time_notifications", settings.enableUsageTimeNotifications)
            putInt("default_social_media_time_limit", settings.defaultSocialMediaTimeLimit)
            putInt("default_messaging_time_limit", settings.defaultMessagingTimeLimit)
            putString("custom_app_time_limits", saveCustomAppTimeLimits(settings.customAppTimeLimits))
            putInt("usage_notification_frequency", settings.usageNotificationFrequency)
            putBoolean("enable_daily_usage_reset", settings.enableDailyUsageReset)
            putLong("last_usage_reset_date", settings.lastUsageResetDate ?: 0L)
            putBoolean("usage_defaults_seeded", settings.usageDefaultsSeeded)
            putInt("settings_version", settings.settingsVersion)
            
            // Local Prayer Calculation Settings
            putBoolean("enable_local_calculations", settings.enableLocalCalculations)
            putBoolean("prefer_local_over_api", settings.preferLocalOverApi)
            putBoolean("show_calculation_method", settings.showCalculationMethod)
            putBoolean("morocco_specific_adjustments", settings.moroccoSpecificAdjustments)

            // Blur Animation Settings
            putBoolean("enable_smooth_blur_animations", settings.enableSmoothBlurAnimations)
            putInt("blur_animation_duration", settings.blurAnimationDuration)
            putInt("blur_transition_duration", settings.blurTransitionDuration)
            putBoolean("enable_blur_region_interpolation", settings.enableBlurRegionInterpolation)

            // Blur Performance Optimization Settings
            putBoolean("enable_hardware_blur_acceleration", settings.enableHardwareBlurAcceleration)
            putString("blur_rendering_mode", settings.blurRenderingMode.name)
            putInt("max_blur_regions_per_frame", settings.maxBlurRegionsPerFrame)
            putBoolean("enable_blur_frame_rate_limiting", settings.enableBlurFrameRateLimiting)

            // Blur Edge Refinement Settings
            putBoolean("enable_blur_edge_refinement", settings.enableBlurEdgeRefinement)
            putBoolean("blur_edge_anti_aliasing", settings.blurEdgeAntiAliasing)
            putFloat("blur_boundary_precision", settings.blurBoundaryPrecision)

            // Force immediate commit to prevent data loss
            apply()
            }
            Log.v(TAG, "Settings saved successfully to SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            throw e
        }
    }

    /**
     * Save monitored app categories to SharedPreferences
     */
    private fun saveMonitoredAppCategories(categories: Set<AppCategory>): String {
        val jsonObject = JSONObject()
        AppCategory.values().forEach { category ->
            jsonObject.put(category.name, categories.contains(category))
        }
        return jsonObject.toString()
    }

    /**
     * Save a Set<String> as a presence map JSON string for SharedPreferences storage.
     *
     * Storage format: JSON object where keys are set elements and values are boolean 'true'.
     * Example: {"com.instagram.android": true, "com.facebook.katana": true}
     * This format allows for efficient lookups and easy extension to store additional metadata.
     */
    private fun saveSetAsPresenceMapJson(set: Set<String>): String {
        val jsonObject = JSONObject()
        set.forEach { item ->
            jsonObject.put(item, true)
        }
        return jsonObject.toString()
    }

    /**
     * Save custom app time limits to SharedPreferences
     */
    private fun saveCustomAppTimeLimits(limits: Map<String, Int>): String {
        val jsonObject = JSONObject()
        limits.forEach { (packageName, limit) ->
            jsonObject.put(packageName, limit)
        }
        return jsonObject.toString()
    }

    // Quick access methods for common settings
    fun getCurrentSettings(): AppSettings = _settings.value
    
    /**
     * Persist preferred language synchronously (commit) and update in-memory state.
     * This avoids race conditions when the UI must recreate immediately after change.
     */
    fun persistPreferredLanguageSync(language: Language) {
        val updated = _settings.value.copy(preferredLanguage = language)
        _settings.value = updated
        // Commit synchronously to ensure durability before consumers act on it
        prefs.edit().putString("preferred_language", language.name).commit()
        Log.d(TAG, "persistPreferredLanguageSync committed: ${language.name}")
    }
    
    /**
     * Enhanced language persistence method with verification and return value
     * @param language The language to persist
     * @return Boolean indicating success or failure of language persistence
     */
    fun persistPreferredLanguageSyncWithResult(language: Language): Boolean {
        return try {
            val updated = _settings.value.copy(preferredLanguage = language)
            _settings.value = updated

            // First attempt
            var success = prefs.edit().putString("preferred_language", language.name).commit()
            Log.d(TAG, "persistPreferredLanguageSyncWithResult first attempt: ${language.name}, success: $success")

            // Retry logic: single retry on failure
            if (!success) {
                Log.w(TAG, "First language persistence attempt failed, retrying...")
                Thread.sleep(100) // Small delay before retry
                success = prefs.edit().putString("preferred_language", language.name).commit()
                Log.d(TAG, "persistPreferredLanguageSyncWithResult retry attempt: ${language.name}, success: $success")
            }

            // Verification: read-back and ensure it matches
            if (success) {
                val verified = try {
                    val stored = prefs.getString("preferred_language", null)
                    val matches = stored == language.name
                    if (!matches) {
                        Log.w(TAG, "Language verification failed: expected ${language.name}, got $stored")
                    }
                    matches
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying language persistence", e)
                    false
                }

                Log.d(TAG, "Language persistence verification: $verified for ${language.name}")
                verified
            } else {
                Log.e(TAG, "Language persistence failed after retry for ${language.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in persistPreferredLanguageSyncWithResult", e)
            false
        }
    }

    /**
     * Synchronously persist gender and related blur settings with verification.
     * This method uses commit() instead of apply() to ensure immediate persistence
     * and includes retry logic and read-back verification.
     *
     * @param gender The user's gender
     * @param blurMaleFaces Whether to blur male faces
     * @param blurFemaleFaces Whether to blur female faces
     * @return true if persistence was verified successful, false otherwise
     */
    fun persistGenderSyncWithResult(
        gender: UserGender,
        blurMaleFaces: Boolean,
        blurFemaleFaces: Boolean
    ): Boolean {
        return try {
            // Update in-memory StateFlow first
            val updated = _settings.value.copy(
                userGender = gender,
                blurMaleFaces = blurMaleFaces,
                blurFemaleFaces = blurFemaleFaces,
                enableFaceDetection = true,
                enableNSFWDetection = true
            )
            _settings.value = updated

            // First attempt - synchronous commit
            var success = prefs.edit()
                .putString("user_gender", gender.name)
                .putBoolean("blur_male_faces", blurMaleFaces)
                .putBoolean("blur_female_faces", blurFemaleFaces)
                .putBoolean("enable_face_detection", true)
                .putBoolean("enable_nsfw_detection", true)
                .commit()

            Log.d(TAG, "persistGenderSyncWithResult first attempt: gender=${gender.name}, blurMale=$blurMaleFaces, blurFemale=$blurFemaleFaces, success=$success")

            // Retry logic: single retry on failure
            if (!success) {
                Log.w(TAG, "First gender persistence attempt failed, retrying...")
                Thread.sleep(100) // Small delay before retry
                success = prefs.edit()
                    .putString("user_gender", gender.name)
                    .putBoolean("blur_male_faces", blurMaleFaces)
                    .putBoolean("blur_female_faces", blurFemaleFaces)
                    .putBoolean("enable_face_detection", true)
                    .putBoolean("enable_nsfw_detection", true)
                    .commit()
                Log.d(TAG, "persistGenderSyncWithResult retry attempt: gender=${gender.name}, success=$success")
            }

            // Verification: read-back and ensure it matches
            if (success) {
                val verified = try {
                    val storedGender = prefs.getString("user_gender", null)
                    val storedBlurMale = prefs.getBoolean("blur_male_faces", false)
                    val storedBlurFemale = prefs.getBoolean("blur_female_faces", false)
                    val storedFaceDetection = prefs.getBoolean("enable_face_detection", false)
                    val storedNsfwDetection = prefs.getBoolean("enable_nsfw_detection", false)

                    val genderMatches = storedGender == gender.name
                    val blurMaleMatches = storedBlurMale == blurMaleFaces
                    val blurFemaleMatches = storedBlurFemale == blurFemaleFaces
                    val faceDetectionMatches = storedFaceDetection == true
                    val nsfwDetectionMatches = storedNsfwDetection == true

                    val allMatch = genderMatches && blurMaleMatches && blurFemaleMatches &&
                                   faceDetectionMatches && nsfwDetectionMatches

                    if (!allMatch) {
                        Log.w(TAG, "Gender verification failed:")
                        Log.w(TAG, "  Expected gender: ${gender.name}, got: $storedGender (match: $genderMatches)")
                        Log.w(TAG, "  Expected blurMale: $blurMaleFaces, got: $storedBlurMale (match: $blurMaleMatches)")
                        Log.w(TAG, "  Expected blurFemale: $blurFemaleFaces, got: $storedBlurFemale (match: $blurFemaleMatches)")
                        Log.w(TAG, "  Expected faceDetection: true, got: $storedFaceDetection (match: $faceDetectionMatches)")
                        Log.w(TAG, "  Expected nsfwDetection: true, got: $storedNsfwDetection (match: $nsfwDetectionMatches)")
                    }
                    allMatch
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying gender persistence", e)
                    false
                }

                Log.d(TAG, "Gender persistence verification: $verified for ${gender.name}")
                verified
            } else {
                Log.e(TAG, "Gender persistence failed after retry for ${gender.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in persistGenderSyncWithResult for ${gender.name}", e)
            false
        }
    }
    
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
    
    // Location Settings - Atomic Helper Methods
    /**
     * Update the explicit location method (GPS vs MANUAL_CITY).
     * Resets incompatible fields when switching methods to keep state coherent.
     */
    fun updateLocationMethod(method: LocationMethod) {
        val current = _settings.value
        val updated = if (method == LocationMethod.GPS) {
            // Switching to GPS: keep GPS fields, clear selected manual city if it was set
            current.copy(
                locationMethod = LocationMethod.GPS,
                autoDetectLocation = true, // backward-compat mirror
                // keep any existing GPS cache
                // clear manual selection so UI reflects GPS choice
                selectedCityName = null,
                selectedCountry = null,
                selectedCountryCode = null,
                selectedLatitude = null,
                selectedLongitude = null
            )
        } else {
            // Switching to MANUAL_CITY: keep manual fields, do not silently clear stored GPS
            current.copy(
                locationMethod = LocationMethod.MANUAL_CITY,
                autoDetectLocation = false // backward-compat mirror
            )
        }
        updateSettings(updated)
    }

    /**
     * Update stored GPS location and associated metadata.
     * Pass null for city/country if reverse-geocode not available.
     */
    fun updateGpsLocation(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        country: String? = null,
        accuracyMeters: Float? = null,
        permissionStatus: LocationPermissionStatus = _settings.value.locationPermissionStatus
    ) {
        val current = _settings.value
        val updated = current.copy(
            locationLatitude = latitude,
            locationLongitude = longitude,
            locationCity = city ?: current.locationCity,
            locationCountry = country ?: current.locationCountry,
            locationAccuracy = accuracyMeters,
            locationLastUpdated = System.currentTimeMillis(),
            locationPermissionStatus = permissionStatus
        )
        updateSettings(updated)
    }

    /**
     * Clear stored GPS location cache (coordinates and labels).
     */
    fun clearGpsLocation() {
        val current = _settings.value
        val updated = current.copy(
            locationLatitude = null,
            locationLongitude = null,
            locationCity = null,
            locationCountry = null,
            locationAccuracy = null,
            locationLastUpdated = null
        )
        updateSettings(updated)
    }

    /**
     * Update manual city selection (city/country and optional coords).
     */
    fun updateManualCitySelection(
        city: String,
        country: String,
        countryCode: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val current = _settings.value
        val updated = current.copy(
            selectedCityName = city,
            selectedCountry = country,
            selectedCountryCode = countryCode,
            selectedLatitude = latitude,
            selectedLongitude = longitude
        )
        updateSettings(updated)
    }

    /**
     * Clear manual city selection.
     */
    fun clearManualCitySelection() {
        val current = _settings.value
        val updated = current.copy(
            selectedCityName = null,
            selectedCountry = null,
            selectedCountryCode = null,
            selectedLatitude = null,
            selectedLongitude = null
        )
        updateSettings(updated)
    }

    /**
     * Update location permission status enum.
     */
    fun updateLocationPermissionStatus(status: LocationPermissionStatus) {
        val current = _settings.value
        val updated = current.copy(locationPermissionStatus = status)
        updateSettings(updated)
    }

    /**
     * Update only location accuracy value (meters) and optionally bump timestamp.
     */
    fun updateLocationAccuracy(accuracyMeters: Float?, setTimestampNow: Boolean = false) {
        val current = _settings.value
        val updated = current.copy(
            locationAccuracy = accuracyMeters,
            locationLastUpdated = if (setTimestampNow) System.currentTimeMillis() else current.locationLastUpdated
        )
        updateSettings(updated)
    }

    /**
     * Convenience to mark the location as refreshed "now" without changing coordinates.
     */
    fun markLocationUpdatedNow() {
        val current = _settings.value
        val updated = current.copy(locationLastUpdated = System.currentTimeMillis())
        updateSettings(updated)
    }

    /**
     * Backward-compat setter to mirror legacy auto-detect flag into the new enum.
     */
    fun setAutoDetectCompat(autoDetect: Boolean) {
        updateLocationMethod(if (autoDetect) LocationMethod.GPS else LocationMethod.MANUAL_CITY)
    }

    // App-Specific Detection Helper Methods
    fun getDetectionScope(): DetectionScope = DetectionScope(
        mode = if (settings.value.enableAppSpecificDetection) DetectionMode.SPECIFIC_CATEGORIES else DetectionMode.ALL_APPS,
        monitoredCategories = settings.value.monitoredAppCategories,
        customIncludedApps = settings.value.customMonitoredApps,
        excludedApps = settings.value.excludedApps
    )

    fun getUsageTimeConfig(): UsageTimeConfig = UsageTimeConfig(
        enabled = settings.value.enableUsageTimeNotifications,
        defaultLimits = mapOf(
            AppCategory.SOCIAL_MEDIA to settings.value.defaultSocialMediaTimeLimit,
            AppCategory.MESSAGING to settings.value.defaultMessagingTimeLimit,
            AppCategory.ENTERTAINMENT to settings.value.defaultSocialMediaTimeLimit, // Same as social media for now
            AppCategory.DATING to 30, // Fixed 30 minutes for dating apps
            AppCategory.BROWSERS to 180 // Fixed 3 hours for browsers
        ),
        customAppLimits = settings.value.customAppTimeLimits,
        notificationFrequencyMinutes = settings.value.usageNotificationFrequency,
        enableDailyReset = settings.value.enableDailyUsageReset
    )

    // App-Specific Detection Update Methods
    fun updateAppSpecificDetectionEnabled(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableAppSpecificDetection = enabled)
        updateSettings(updated)
    }

    fun updateMonitoredAppCategories(categories: Set<AppCategory>) {
        val current = _settings.value
        val updated = current.copy(monitoredAppCategories = categories)
        updateSettings(updated)
    }

    fun updateCustomMonitoredApps(apps: Set<String>) {
        val current = _settings.value
        val updated = current.copy(customMonitoredApps = apps)
        updateSettings(updated)
    }

    fun updateExcludedApps(apps: Set<String>) {
        val current = _settings.value
        val updated = current.copy(excludedApps = apps)
        updateSettings(updated)
    }

    // Usage Time Tracking Update Methods
    fun updateUsageTimeNotifications(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableUsageTimeNotifications = enabled)
        updateSettings(updated)
    }

    fun updateAppTimeLimit(packageName: String, limitMinutes: Int) {
        val current = _settings.value
        val updatedLimits = current.customAppTimeLimits.toMutableMap()
        updatedLimits[packageName] = limitMinutes
        val updated = current.copy(customAppTimeLimits = updatedLimits)
        updateSettings(updated)
    }

    fun removeAppTimeLimit(packageName: String) {
        val current = _settings.value
        val updatedLimits = current.customAppTimeLimits.toMutableMap()
        updatedLimits.remove(packageName)
        val updated = current.copy(customAppTimeLimits = updatedLimits)
        updateSettings(updated)
    }

    fun updateDefaultSocialMediaTimeLimit(limitMinutes: Int) {
        val current = _settings.value
        val updated = current.copy(defaultSocialMediaTimeLimit = limitMinutes)
        updateSettings(updated)
    }

    fun updateDefaultMessagingTimeLimit(limitMinutes: Int) {
        val current = _settings.value
        val updated = current.copy(defaultMessagingTimeLimit = limitMinutes)
        updateSettings(updated)
    }

    fun updateUsageNotificationFrequency(frequencyMinutes: Int) {
        val current = _settings.value
        val updated = current.copy(usageNotificationFrequency = frequencyMinutes)
        updateSettings(updated)
    }

    fun updateDailyUsageReset(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableDailyUsageReset = enabled)
        updateSettings(updated)
    }

    // Dhikr Sleep Time Update Methods
    fun updateDhikrSleepStartTime(minutes: Int) {
        require(minutes in 0..1439) { "Sleep start time must be between 0 and 1439 minutes" }
        val current = _settings.value
        val updated = current.copy(dhikrSleepStartMinutes = minutes)
        updateSettings(updated)
    }

    fun updateDhikrSleepEndTime(minutes: Int) {
        require(minutes in 0..1439) { "Sleep end time must be between 0 and 1439 minutes" }
        val current = _settings.value
        val updated = current.copy(dhikrSleepEndMinutes = minutes)
        updateSettings(updated)
    }

    fun updateDhikrSleepTimes(startMinutes: Int, endMinutes: Int) {
        require(startMinutes in 0..1439) { "Sleep start time must be between 0 and 1439 minutes" }
        require(endMinutes in 0..1439) { "Sleep end time must be between 0 and 1439 minutes" }
        val current = _settings.value
        val updated = current.copy(
            dhikrSleepStartMinutes = startMinutes,
            dhikrSleepEndMinutes = endMinutes
        )
        updateSettings(updated)
    }

    // Local Prayer Calculation Settings Helper Methods
    fun updateEnableLocalCalculations(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableLocalCalculations = enabled)
        updateSettings(updated)
    }

    fun updatePreferLocalOverApi(preferLocal: Boolean) {
        val current = _settings.value
        val updated = current.copy(preferLocalOverApi = preferLocal)
        updateSettings(updated)
    }

    fun updateShowCalculationMethod(show: Boolean) {
        val current = _settings.value
        val updated = current.copy(showCalculationMethod = show)
        updateSettings(updated)
    }

    fun updateMoroccoSpecificAdjustments(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(moroccoSpecificAdjustments = enabled)
        updateSettings(updated)
    }

    // Blur Animation Settings Helper Methods
    fun updateSmoothBlurAnimations(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableSmoothBlurAnimations = enabled)
        updateSettings(updated)
    }

    fun updateBlurAnimationDuration(duration: Int) {
        val current = _settings.value
        val updated = current.copy(blurAnimationDuration = duration.coerceIn(50, 1000))
        updateSettings(updated)
    }

    fun updateBlurTransitionDuration(duration: Int) {
        val current = _settings.value
        val updated = current.copy(blurTransitionDuration = duration.coerceIn(50, 1000))
        updateSettings(updated)
    }

    fun updateBlurRenderingMode(mode: BlurRenderingMode) {
        val current = _settings.value
        val updated = current.copy(blurRenderingMode = mode)
        updateSettings(updated)
    }

    fun updateHardwareBlurAcceleration(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableHardwareBlurAcceleration = enabled)
        updateSettings(updated)
    }

    fun updateBlurEdgeRefinement(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableBlurEdgeRefinement = enabled)
        updateSettings(updated)
    }

    fun updateBlurEdgeAntiAliasing(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(blurEdgeAntiAliasing = enabled)
        updateSettings(updated)
    }

    fun updateBlurBoundaryPrecision(precision: Float) {
        val current = _settings.value
        val updated = current.copy(blurBoundaryPrecision = precision.coerceIn(0.1f, 1.0f))
        updateSettings(updated)
    }

    fun updateMaxBlurRegionsPerFrame(maxRegions: Int) {
        val current = _settings.value
        val updated = current.copy(maxBlurRegionsPerFrame = maxRegions.coerceIn(1, 50))
        updateSettings(updated)
    }

    fun updateBlurFrameRateLimiting(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableBlurFrameRateLimiting = enabled)
        updateSettings(updated)
    }

    fun updateBlurRegionInterpolation(enabled: Boolean) {
        val current = _settings.value
        val updated = current.copy(enableBlurRegionInterpolation = enabled)
        updateSettings(updated)
    }

    fun updateAllLocalCalculationSettings(
        enableLocal: Boolean,
        preferLocal: Boolean,
        showMethod: Boolean,
        moroccoAdjustments: Boolean
    ) {
        val current = _settings.value
        val updated = current.copy(
            enableLocalCalculations = enableLocal,
            preferLocalOverApi = preferLocal,
            showCalculationMethod = showMethod,
            moroccoSpecificAdjustments = moroccoAdjustments
        )
        updateSettings(updated)
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
                    putFloat("gender_confidence_threshold", 0.4f)
                    putFloat("nsfw_confidence_threshold", 0.7f)
                    putBoolean("enable_fallback_detection", true)
                    putBoolean("enable_performance_monitoring", true)
                    apply()
                }
            }
            2, 3, 4 -> {
                // Migration to version 5: Introduce city selection fields and defaults
                Log.i(TAG, "Migrating from version $fromVersion: Adding city selection fields and defaults + location method")
                prefs.edit().apply {
                    // Defaults
                    putBoolean("enable_city_search_cache", true)
                    putBoolean("enable_offline_city_fallback", true)
                    putBoolean("prefer_stored_coordinates", true)

                    // Map legacy preferredCity/preferredCountry to new fields if present
                    val legacyCity = prefs.getString("preferred_city", null) ?: prefs.getString("preferredCity", null)
                    val legacyCountry = prefs.getString("preferred_country", null) ?: prefs.getString("preferredCountry", null)
                    if (!legacyCity.isNullOrBlank()) putString("selected_city_name", legacyCity)
                    if (!legacyCountry.isNullOrBlank()) putString("selected_country", legacyCountry)

                    // Map legacy auto-detect boolean to new explicit location method
                    val legacyAuto = prefs.getBoolean("auto_detect_location", true)
                    putString("location_method", if (legacyAuto) LocationMethod.GPS.name else LocationMethod.MANUAL_CITY.name)
                    if (!prefs.contains("location_permission_status")) putString("location_permission_status", LocationPermissionStatus.UNKNOWN.name)

                    apply()
                }
            }
            5 -> {
                // Migration to version 6: Introduce compass settings with sensible defaults
                Log.i(TAG, "Migrating from version 5: Adding compass settings defaults")
                prefs.edit().apply {
                    putBoolean("qibla_compass_enabled", true)
                    putBoolean("compass_calibration_reminders", true)
                    putBoolean("compass_haptic_feedback", true)
                    putBoolean("compass_show_degree_markings", true)
                    putFloat("compass_sensitivity", 1.0f)
                    putFloat("compass_accuracy_threshold", 20.0f)
                    putFloat("qibla_tolerance_degrees", 5.0f)
                    putFloat("compass_animation_speed", 1.0f)
                    putLong("last_compass_calibration", 0L)
                    putString("compass_preferred_size", com.hieltech.haramblur.data.compass.CompassSize.MEDIUM.name)
                    putBoolean("enable_magnetic_declination", true)
                    putInt("compass_update_rate", 15)
                    apply()
                }
            }
            6 -> {
                // Migration to version 7: Add prayer enhancement defaults
                Log.i(TAG, "Migrating from version 6: Adding prayer enhancement defaults")
                prefs.edit().apply {
                    putInt("fajr_offset_minutes", 0)
                    putInt("sunrise_offset_minutes", 0)
                    putInt("dhuhr_offset_minutes", 0)
                    putInt("asr_offset_minutes", 0)
                    putInt("maghrib_offset_minutes", 0)
                    putInt("isha_offset_minutes", 0)
                    putBoolean("enable_prayer_history", true)
                    putInt("prayer_history_retention_days", 60)
                    putInt("prayer_cache_ttl_minutes", 30)
                    putInt("location_stale_after_minutes", 60)
                    putBoolean("strict_prayer_accuracy_validation", true)
                    putInt("max_allowed_prayer_shift_minutes", 20)
                    putFloat("gps_accuracy_high_threshold_m", 30f)
                    putFloat("gps_accuracy_medium_threshold_m", 100f)
                    putFloat("gps_accuracy_low_threshold_m", 300f)
                    apply()
                }
            }
            7 -> {
                // Migration to version 8: Add app-specific detection and usage time settings
                Log.i(TAG, "Migrating from version 7: Adding app-specific detection and usage time settings")
                prefs.edit().apply {
                    putBoolean("enable_app_specific_detection", true)
                    putString("monitored_app_categories", saveMonitoredAppCategories(setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)))
                    putStringSet("custom_monitored_apps", emptySet<String>())
                    putStringSet("excluded_apps", emptySet<String>())
                    putBoolean("enable_usage_time_notifications", true)
                    putInt("default_social_media_time_limit", 60)
                    putInt("default_messaging_time_limit", 120)
                    putString("custom_app_time_limits", saveCustomAppTimeLimits(emptyMap()))
                    putInt("usage_notification_frequency", 30)
                    putBoolean("enable_daily_usage_reset", true)
                    apply()
                }
            }
            8 -> {
                // Migration to version 9: Add dhikr sleep time settings
                Log.i(TAG, "Migrating from version 8: Adding dhikr sleep time settings")
                prefs.edit().apply {
                    putInt("dhikr_sleep_start_minutes", 1350) // 22:30 PM
                    putInt("dhikr_sleep_end_minutes", 390)    // 6:30 AM
                    apply()
                }
            }
            9 -> {
                // Migration to version 10: Lower gender threshold for reliable female detection
                Log.i(TAG, "Migrating from version 9: Adjusting gender detection thresholds")
                val currentThreshold = prefs.getFloat("gender_confidence_threshold", 0.8f)
                val adjustedThreshold = if (currentThreshold > 0.5f) 0.4f else currentThreshold
                prefs.edit().apply {
                    putFloat("gender_confidence_threshold", adjustedThreshold)
                    apply()
                }
            }
            10 -> {
                // Migration to version 11: Add local prayer calculation settings
                Log.i(TAG, "Migrating from version 10: Adding local prayer calculation settings")
                prefs.edit().apply {
                    putBoolean("enable_local_calculations", false)
                    putBoolean("prefer_local_over_api", false)
                    putBoolean("show_calculation_method", true)
                    putBoolean("morocco_specific_adjustments", true)
                    apply()
                }
            }
            11 -> {
                // Migration to version 12: Add blur animation and performance optimization settings
                Log.i(TAG, "Migrating from version 11: Adding blur animation and performance settings")
                prefs.edit().apply {
                    // Blur Animation Settings
                    putBoolean("enable_smooth_blur_animations", true)
                    putInt("blur_animation_duration", 250)
                    putInt("blur_transition_duration", 150)
                    putBoolean("enable_blur_region_interpolation", true)
                    
                    // Blur Performance Optimization Settings
                    putBoolean("enable_hardware_blur_acceleration", true)
                    putString("blur_rendering_mode", BlurRenderingMode.SMOOTH.name)
                    putInt("max_blur_regions_per_frame", 12)
                    putBoolean("enable_blur_frame_rate_limiting", true)
                    
                    // Blur Edge Refinement Settings
                    putBoolean("enable_blur_edge_refinement", true)
                    putBoolean("blur_edge_anti_aliasing", true)
                    putFloat("blur_boundary_precision", 0.5f)
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
            genderConfidenceThreshold = settings.genderConfidenceThreshold.coerceIn(0.3f, 0.9f),
            nsfwConfidenceThreshold = settings.nsfwConfidenceThreshold.coerceIn(0.5f, 0.95f),
            expandBlurArea = settings.expandBlurArea.coerceIn(0, 100),
            // Prayer enhancement ranges
            fajrOffsetMinutes = settings.fajrOffsetMinutes.coerceIn(-60, 60),
            sunriseOffsetMinutes = settings.sunriseOffsetMinutes.coerceIn(-60, 60),
            dhuhrOffsetMinutes = settings.dhuhrOffsetMinutes.coerceIn(-60, 60),
            asrOffsetMinutes = settings.asrOffsetMinutes.coerceIn(-60, 60),
            maghribOffsetMinutes = settings.maghribOffsetMinutes.coerceIn(-60, 60),
            ishaOffsetMinutes = settings.ishaOffsetMinutes.coerceIn(-60, 60),
            prayerHistoryRetentionDays = settings.prayerHistoryRetentionDays.coerceIn(7, 365),
            prayerCacheTtlMinutes = settings.prayerCacheTtlMinutes.coerceIn(5, 180),
            locationStaleAfterMinutes = settings.locationStaleAfterMinutes.coerceIn(5, 240),
            maxAllowedPrayerShiftMinutes = settings.maxAllowedPrayerShiftMinutes.coerceIn(0, 60),
            gpsAccuracyHighThresholdM = settings.gpsAccuracyHighThresholdM.coerceAtLeast(5f),
            gpsAccuracyMediumThresholdM = settings.gpsAccuracyMediumThresholdM.coerceAtLeast(settings.gpsAccuracyHighThresholdM + 1f),
            gpsAccuracyLowThresholdM = settings.gpsAccuracyLowThresholdM.coerceAtLeast(settings.gpsAccuracyMediumThresholdM + 1f),

            // App-Specific Detection and Usage Time validation
            defaultSocialMediaTimeLimit = settings.defaultSocialMediaTimeLimit.coerceIn(1, 1440), // 1 minute to 24 hours
            defaultMessagingTimeLimit = settings.defaultMessagingTimeLimit.coerceIn(1, 1440), // 1 minute to 24 hours
            usageNotificationFrequency = settings.usageNotificationFrequency.coerceIn(5, 120), // 5 minutes to 2 hours

            // Blur Animation and Performance Settings validation
            blurAnimationDuration = settings.blurAnimationDuration.coerceIn(50, 1000), // 50ms to 1000ms
            blurTransitionDuration = settings.blurTransitionDuration.coerceIn(50, 1000), // 50ms to 1000ms
            maxBlurRegionsPerFrame = settings.maxBlurRegionsPerFrame.coerceIn(1, 50), // 1 to 50 regions
            blurBoundaryPrecision = settings.blurBoundaryPrecision.coerceIn(0.1f, 1.0f) // 0.1 to 1.0 precision
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

            // Islamic Calendar & Prayer Times
            put("enableIslamicCalendar", settings.enableIslamicCalendar)
            put("enablePrayerTimes", settings.enablePrayerTimes)
            put("enablePrayerNotifications", settings.enablePrayerNotifications)
            put("prayerCalculationMethod", settings.prayerCalculationMethod)
            put("prayerNotificationAdvanceTime", settings.prayerNotificationAdvanceTime)
            put("locationLatitude", settings.locationLatitude)
            put("locationLongitude", settings.locationLongitude)
            put("locationCity", settings.locationCity)
            put("locationCountry", settings.locationCountry)
            put("locationCountryCode", settings.locationCountryCode)
            put("enableQiblaDirection", settings.enableQiblaDirection)
            put("prayerTimesUpdateInterval", settings.prayerTimesUpdateInterval)
            put("islamicCalendarUpdateInterval", settings.islamicCalendarUpdateInterval)
            put("autoDetectLocation", settings.autoDetectLocation)
            put("locationMethod", settings.locationMethod.name)
            put("locationAccuracy", settings.locationAccuracy)
            put("locationLastUpdated", settings.locationLastUpdated)
            put("locationPermissionStatus", settings.locationPermissionStatus.name)

            // New city selection fields
            put("selectedCityName", settings.selectedCityName)
            put("selectedCountry", settings.selectedCountry)
            put("selectedCountryCode", settings.selectedCountryCode)
            put("selectedLatitude", settings.selectedLatitude)
            put("selectedLongitude", settings.selectedLongitude)
            put("enableCitySearchCache", settings.enableCitySearchCache)
            put("enableOfflineCityFallback", settings.enableOfflineCityFallback)
            put("preferStoredCoordinates", settings.preferStoredCoordinates)

            // Prayer Enhancements: Offsets, history, caching and validation
            put("fajrOffsetMinutes", settings.fajrOffsetMinutes)
            put("sunriseOffsetMinutes", settings.sunriseOffsetMinutes)
            put("dhuhrOffsetMinutes", settings.dhuhrOffsetMinutes)
            put("asrOffsetMinutes", settings.asrOffsetMinutes)
            put("maghribOffsetMinutes", settings.maghribOffsetMinutes)
            put("ishaOffsetMinutes", settings.ishaOffsetMinutes)
            put("enablePrayerHistory", settings.enablePrayerHistory)
            put("prayerHistoryRetentionDays", settings.prayerHistoryRetentionDays)
            put("prayerCacheTtlMinutes", settings.prayerCacheTtlMinutes)
            put("locationStaleAfterMinutes", settings.locationStaleAfterMinutes)
            put("strictPrayerAccuracyValidation", settings.strictPrayerAccuracyValidation)
            put("maxAllowedPrayerShiftMinutes", settings.maxAllowedPrayerShiftMinutes)
            put("gpsAccuracyHighThresholdM", settings.gpsAccuracyHighThresholdM)
            put("gpsAccuracyMediumThresholdM", settings.gpsAccuracyMediumThresholdM)
            put("gpsAccuracyLowThresholdM", settings.gpsAccuracyLowThresholdM)

            // Qibla Compass Settings
            put("qiblaCompassEnabled", settings.qiblaCompassEnabled)
            put("compassCalibrationReminders", settings.compassCalibrationReminders)
            put("compassHapticFeedback", settings.compassHapticFeedback)
            put("compassShowDegreeMarkings", settings.compassShowDegreeMarkings)
            put("compassSensitivity", settings.compassSensitivity)
            put("compassAccuracyThreshold", settings.compassAccuracyThreshold)
            put("qiblaToleranceDegrees", settings.qiblaToleranceDegrees)
            put("compassAnimationSpeed", settings.compassAnimationSpeed)
            put("lastCompassCalibration", settings.lastCompassCalibration)
            put("compassPreferredSize", settings.compassPreferredSize.name)
            put("enableMagneticDeclination", settings.enableMagneticDeclination)
            put("compassUpdateRate", settings.compassUpdateRate)

            // App-Specific Detection Settings
            put("enableAppSpecificDetection", settings.enableAppSpecificDetection)
            put("monitoredAppCategories", JSONArray(settings.monitoredAppCategories.map { it.name }))
            put("customMonitoredApps", JSONArray(settings.customMonitoredApps.toList()))
            put("excludedApps", JSONArray(settings.excludedApps.toList()))

            // Usage Time Tracking Settings
            put("enableUsageTimeNotifications", settings.enableUsageTimeNotifications)
            put("defaultSocialMediaTimeLimit", settings.defaultSocialMediaTimeLimit)
            put("defaultMessagingTimeLimit", settings.defaultMessagingTimeLimit)
            put("customAppTimeLimits", JSONObject().apply {
                settings.customAppTimeLimits.forEach { (packageName, limit) ->
                    put(packageName, limit)
                }
            })
            put("usageNotificationFrequency", settings.usageNotificationFrequency)
            put("enableDailyUsageReset", settings.enableDailyUsageReset)

            // Local Prayer Calculation Settings
            put("enableLocalCalculations", settings.enableLocalCalculations)
            put("preferLocalOverApi", settings.preferLocalOverApi)
            put("showCalculationMethod", settings.showCalculationMethod)
            put("moroccoSpecificAdjustments", settings.moroccoSpecificAdjustments)

            // Blur Animation Settings
            put("enableSmoothBlurAnimations", settings.enableSmoothBlurAnimations)
            put("blurAnimationDuration", settings.blurAnimationDuration)
            put("blurTransitionDuration", settings.blurTransitionDuration)
            put("enableBlurRegionInterpolation", settings.enableBlurRegionInterpolation)

            // Blur Performance Optimization Settings
            put("enableHardwareBlurAcceleration", settings.enableHardwareBlurAcceleration)
            put("blurRenderingMode", settings.blurRenderingMode.name)
            put("maxBlurRegionsPerFrame", settings.maxBlurRegionsPerFrame)
            put("enableBlurFrameRateLimiting", settings.enableBlurFrameRateLimiting)

            // Blur Edge Refinement Settings
            put("enableBlurEdgeRefinement", settings.enableBlurEdgeRefinement)
            put("blurEdgeAntiAliasing", settings.blurEdgeAntiAliasing)
            put("blurBoundaryPrecision", settings.blurBoundaryPrecision)

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
                    ProcessingSpeed.valueOf(jsonObject.optString("processingSpeed", ProcessingSpeed.FAST.name))
                } catch (e: IllegalArgumentException) { ProcessingSpeed.FAST },
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
                frameSkipThreshold = jsonObject.optInt("frameSkipThreshold", 1),
                imageDownscaleRatio = jsonObject.optDouble("imageDownscaleRatio", 0.7).toFloat(),
                
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
                ,
                // Islamic Calendar & Prayer Times
                enableIslamicCalendar = jsonObject.optBoolean("enableIslamicCalendar", true),
                enablePrayerTimes = jsonObject.optBoolean("enablePrayerTimes", true),
                enablePrayerNotifications = jsonObject.optBoolean("enablePrayerNotifications", true),
                prayerCalculationMethod = jsonObject.optInt("prayerCalculationMethod", 2),
                prayerNotificationAdvanceTime = jsonObject.optInt("prayerNotificationAdvanceTime", 15),
                locationLatitude = if (jsonObject.has("locationLatitude")) jsonObject.optDouble("locationLatitude").let { if (it.isNaN()) null else it } else null,
                locationLongitude = if (jsonObject.has("locationLongitude")) jsonObject.optDouble("locationLongitude").let { if (it.isNaN()) null else it } else null,
                locationCity = getNullableString(jsonObject, "locationCity"),
                locationCountry = getNullableString(jsonObject, "locationCountry"),
                locationCountryCode = getNullableString(jsonObject, "locationCountryCode"),
                enableQiblaDirection = jsonObject.optBoolean("enableQiblaDirection", true),
                prayerTimesUpdateInterval = jsonObject.optInt("prayerTimesUpdateInterval", 30),
                islamicCalendarUpdateInterval = jsonObject.optInt("islamicCalendarUpdateInterval", 60),
                autoDetectLocation = jsonObject.optBoolean("autoDetectLocation", true),
                locationMethod = try {
                    LocationMethod.valueOf(jsonObject.optString("locationMethod", LocationMethod.GPS.name))
                } catch (e: IllegalArgumentException) { LocationMethod.GPS },
                locationAccuracy = if (jsonObject.has("locationAccuracy")) jsonObject.optDouble("locationAccuracy").toFloat() else null,
                locationLastUpdated = if (jsonObject.has("locationLastUpdated")) jsonObject.optLong("locationLastUpdated") else null,
                locationPermissionStatus = try {
                    LocationPermissionStatus.valueOf(jsonObject.optString("locationPermissionStatus", LocationPermissionStatus.UNKNOWN.name))
                } catch (e: IllegalArgumentException) { LocationPermissionStatus.UNKNOWN }
                ,
                // New city selection fields
                selectedCityName = getNullableString(jsonObject, "selectedCityName"),
                selectedCountry = getNullableString(jsonObject, "selectedCountry"),
                selectedCountryCode = getNullableString(jsonObject, "selectedCountryCode"),
                selectedLatitude = if (jsonObject.has("selectedLatitude")) jsonObject.optDouble("selectedLatitude").let { if (it.isNaN()) null else it } else null,
                selectedLongitude = if (jsonObject.has("selectedLongitude")) jsonObject.optDouble("selectedLongitude").let { if (it.isNaN()) null else it } else null,
                enableCitySearchCache = jsonObject.optBoolean("enableCitySearchCache", true),
                enableOfflineCityFallback = jsonObject.optBoolean("enableOfflineCityFallback", true),
                preferStoredCoordinates = jsonObject.optBoolean("preferStoredCoordinates", true),

                // Prayer Enhancements: Offsets, history, caching and validation
                fajrOffsetMinutes = jsonObject.optInt("fajrOffsetMinutes", 0),
                sunriseOffsetMinutes = jsonObject.optInt("sunriseOffsetMinutes", 0),
                dhuhrOffsetMinutes = jsonObject.optInt("dhuhrOffsetMinutes", 0),
                asrOffsetMinutes = jsonObject.optInt("asrOffsetMinutes", 0),
                maghribOffsetMinutes = jsonObject.optInt("maghribOffsetMinutes", 0),
                ishaOffsetMinutes = jsonObject.optInt("ishaOffsetMinutes", 0),
                enablePrayerHistory = jsonObject.optBoolean("enablePrayerHistory", true),
                prayerHistoryRetentionDays = jsonObject.optInt("prayerHistoryRetentionDays", 60),
                prayerCacheTtlMinutes = jsonObject.optInt("prayerCacheTtlMinutes", 30),
                locationStaleAfterMinutes = jsonObject.optInt("locationStaleAfterMinutes", 60),
                strictPrayerAccuracyValidation = jsonObject.optBoolean("strictPrayerAccuracyValidation", true),
                maxAllowedPrayerShiftMinutes = jsonObject.optInt("maxAllowedPrayerShiftMinutes", 20),
                gpsAccuracyHighThresholdM = jsonObject.optDouble("gpsAccuracyHighThresholdM", 30.0).toFloat(),
                gpsAccuracyMediumThresholdM = jsonObject.optDouble("gpsAccuracyMediumThresholdM", 100.0).toFloat(),
                gpsAccuracyLowThresholdM = jsonObject.optDouble("gpsAccuracyLowThresholdM", 300.0).toFloat(),

                // Qibla Compass Settings
                qiblaCompassEnabled = jsonObject.optBoolean("qiblaCompassEnabled", true),
                compassCalibrationReminders = jsonObject.optBoolean("compassCalibrationReminders", true),
                compassHapticFeedback = jsonObject.optBoolean("compassHapticFeedback", true),
                compassShowDegreeMarkings = jsonObject.optBoolean("compassShowDegreeMarkings", true),
                compassSensitivity = jsonObject.optDouble("compassSensitivity", 1.0).toFloat(),
                compassAccuracyThreshold = jsonObject.optDouble("compassAccuracyThreshold", 20.0).toFloat(),
                qiblaToleranceDegrees = jsonObject.optDouble("qiblaToleranceDegrees", 5.0).toFloat(),
                compassAnimationSpeed = jsonObject.optDouble("compassAnimationSpeed", 1.0).toFloat(),
                lastCompassCalibration = jsonObject.optLong("lastCompassCalibration", 0L),
                compassPreferredSize = try {
                    com.hieltech.haramblur.data.compass.CompassSize.valueOf(
                        jsonObject.optString("compassPreferredSize", com.hieltech.haramblur.data.compass.CompassSize.MEDIUM.name)
                    )
                } catch (e: IllegalArgumentException) { com.hieltech.haramblur.data.compass.CompassSize.MEDIUM },
                enableMagneticDeclination = jsonObject.optBoolean("enableMagneticDeclination", true),
                compassUpdateRate = jsonObject.optInt("compassUpdateRate", 15),

                // App-Specific Detection Settings
                enableAppSpecificDetection = jsonObject.optBoolean("enableAppSpecificDetection", true),
                monitoredAppCategories = parseMonitoredAppCategories(jsonObject),
                customMonitoredApps = parseStringArray(jsonObject, "customMonitoredApps"),
                excludedApps = parseStringArray(jsonObject, "excludedApps"),

                // Usage Time Tracking Settings
                enableUsageTimeNotifications = jsonObject.optBoolean("enableUsageTimeNotifications", true),
                defaultSocialMediaTimeLimit = validateTimeLimit(jsonObject.optInt("defaultSocialMediaTimeLimit", 60)),
                defaultMessagingTimeLimit = validateTimeLimit(jsonObject.optInt("defaultMessagingTimeLimit", 120)),
                customAppTimeLimits = parseCustomAppTimeLimits(jsonObject),
                usageNotificationFrequency = validateNotificationFrequency(jsonObject.optInt("usageNotificationFrequency", 30)),
                enableDailyUsageReset = jsonObject.optBoolean("enableDailyUsageReset", true),
                
                // Local Prayer Calculation Settings
                enableLocalCalculations = jsonObject.optBoolean("enableLocalCalculations", false),
                preferLocalOverApi = jsonObject.optBoolean("preferLocalOverApi", false),
                showCalculationMethod = jsonObject.optBoolean("showCalculationMethod", true),
                moroccoSpecificAdjustments = jsonObject.optBoolean("moroccoSpecificAdjustments", true),
                
                // Blur Animation Settings
                enableSmoothBlurAnimations = jsonObject.optBoolean("enableSmoothBlurAnimations", true),
                blurAnimationDuration = jsonObject.optInt("blurAnimationDuration", 250).coerceIn(50, 1000),
                blurTransitionDuration = jsonObject.optInt("blurTransitionDuration", 150).coerceIn(50, 1000),
                enableBlurRegionInterpolation = jsonObject.optBoolean("enableBlurRegionInterpolation", true),
                
                // Blur Performance Optimization Settings
                enableHardwareBlurAcceleration = jsonObject.optBoolean("enableHardwareBlurAcceleration", true),
                blurRenderingMode = try {
                    BlurRenderingMode.valueOf(jsonObject.optString("blurRenderingMode", BlurRenderingMode.SMOOTH.name))
                } catch (e: IllegalArgumentException) { BlurRenderingMode.SMOOTH },
                maxBlurRegionsPerFrame = jsonObject.optInt("maxBlurRegionsPerFrame", 12).coerceIn(1, 50),
                enableBlurFrameRateLimiting = jsonObject.optBoolean("enableBlurFrameRateLimiting", true),
                
                // Blur Edge Refinement Settings
                enableBlurEdgeRefinement = jsonObject.optBoolean("enableBlurEdgeRefinement", true),
                blurEdgeAntiAliasing = jsonObject.optBoolean("blurEdgeAntiAliasing", true),
                blurBoundaryPrecision = jsonObject.optDouble("blurBoundaryPrecision", 0.5).toFloat().coerceIn(0.1f, 1.0f)
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

    /**
     * Parse monitored app categories from JSON array
     */
    private fun parseMonitoredAppCategories(jsonObject: JSONObject): Set<AppCategory> {
        val categoriesArray = jsonObject.optJSONArray("monitoredAppCategories")
        if (categoriesArray == null) {
            return setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)
        }

        val categories = mutableSetOf<AppCategory>()
        for (i in 0 until categoriesArray.length()) {
            try {
                val categoryName = categoriesArray.getString(i)
                val category = AppCategory.valueOf(categoryName)
                categories.add(category)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown AppCategory in import: ${categoriesArray.getString(i)}")
            }
        }

        return if (categories.isEmpty()) {
            setOf(AppCategory.SOCIAL_MEDIA, AppCategory.BROWSERS, AppCategory.DATING)
        } else {
            categories
        }
    }

    /**
     * Parse string array from JSON
     */
    private fun parseStringArray(jsonObject: JSONObject, key: String): Set<String> {
        val array = jsonObject.optJSONArray(key)
        if (array == null) return emptySet()

        val set = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i, "")
            if (item.isNotBlank() && isValidPackageName(item)) {
                set.add(item)
            }
        }
        return set
    }

    /**
     * Parse custom app time limits from JSON object
     */
    private fun parseCustomAppTimeLimits(jsonObject: JSONObject): Map<String, Int> {
        val limitsObject = jsonObject.optJSONObject("customAppTimeLimits")
        if (limitsObject == null) return emptyMap()

        val map = mutableMapOf<String, Int>()
        val keys = limitsObject.keys()
        while (keys.hasNext()) {
            val packageName = keys.next()
            if (isValidPackageName(packageName)) {
                val limit = limitsObject.optInt(packageName, 0)
                if (limit > 0) {
                    map[packageName] = validateTimeLimit(limit)
                }
            }
        }
        return map
    }

    /**
     * Validate time limit (1 minute to 24 hours)
     */
    private fun validateTimeLimit(limit: Int): Int {
        return limit.coerceIn(1, 1440) // 1 minute to 24 hours
    }

    /**
     * Validate notification frequency (5 minutes to 2 hours)
     */
    private fun validateNotificationFrequency(frequency: Int): Int {
        return frequency.coerceIn(5, 120) // 5 minutes to 2 hours
    }

    /**
     * Basic validation for Android package names
     */
    private fun isValidPackageName(packageName: String): Boolean {
        // Simple validation: contains dots, starts with letter, reasonable length
        return packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9._]{0,254}$")) &&
               packageName.contains(".") &&
               packageName.length <= 255
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
                    ProcessingSpeed.valueOf(settingsJson.optString("processingSpeed", ProcessingSpeed.FAST.name))
                } catch (e: IllegalArgumentException) { ProcessingSpeed.FAST },
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
                frameSkipThreshold = settingsJson.optInt("frameSkipThreshold", 1),
                imageDownscaleRatio = settingsJson.optDouble("imageDownscaleRatio", 0.7).toFloat(),
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
        try {
            Log.d(TAG, "Toggling service pause state to: $paused")
            
            // Force immediate persistence to SharedPreferences
            prefs.edit().apply {
                putBoolean("is_service_paused", paused)
                apply() // Use apply() for immediate synchronous write
            }
            
            // Update the state flow
            val current = getCurrentSettings()
            _settings.value = current.copy(isServicePaused = paused)
            
            Log.i(TAG, "Service pause state updated successfully: $paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle service pause state", e)
        }
    }

    /**
     * Get recent settings changes
     */
    fun getRecentSettings(limit: Int): List<RecentSetting> {
        // This would typically query a database or log file
        // For now, return empty list
        return emptyList()
    }

    /**
     * Get recent settings changes as Flow
     */
    fun getRecentSettingsFlow(limit: Int): Flow<List<RecentSetting>> {
        // This would typically observe a database or log file
        // For now, return empty flow
        return kotlinx.coroutines.flow.flowOf(emptyList<RecentSetting>())
    }

    /**
     * Record a toggle change
     */
    suspend fun recordToggleChange(
        settingName: String,
        category: String,
        isEnabled: Boolean,
        settingId: String
    ) {
        // This would typically save to a database or log file
        // For now, just log
        Log.d(TAG, "Toggle recorded: $settingName = $isEnabled")
    }

    /**
     * Record a value change
     */
    suspend fun recordValueChange(
        settingName: String,
        category: String,
        previousValue: String,
        newValue: String,
        settingId: String
    ) {
        // This would typically save to a database or log file
        // For now, just log
        Log.d(TAG, "Value change recorded: $settingName = $newValue")
    }

    /**
     * Record a reset action
     */
    suspend fun recordResetAction(
        settingName: String,
        category: String,
        previousValue: String,
        settingId: String
    ) {
        // This would typically save to a database or log file
        // For now, just log
        Log.d(TAG, "Reset recorded: $settingName")
    }

    /**
     * Export recent settings
     */
    suspend fun exportRecentSettings(days: Int): List<RecentSetting> {
        // This would typically query recent changes from database
        // For now, return empty list
        return emptyList<RecentSetting>()
    }

    /**
     * Update quality mode - applies all related settings automatically including new blur optimization defaults
     */
    suspend fun updateQualityMode(qualityMode: QualityMode) {
        val currentSettings = getCurrentSettings()
        val updatedSettings = currentSettings.copy(
            qualityMode = qualityMode,
            detectionSensitivity = qualityMode.detectionSensitivity,
            processingSpeed = qualityMode.processingSpeed,
            blurIntensity = qualityMode.blurIntensity,
            maxProcessingTimeMs = qualityMode.maxProcessingTimeMs,
            frameSkipThreshold = qualityMode.frameSkipThreshold,
            imageDownscaleRatio = qualityMode.imageDownscaleRatio,
            enableGPUAcceleration = qualityMode.enableGPUAcceleration,
            enableRealTimeProcessing = qualityMode.enableRealTimeProcessing,
            // NEW: Apply blur optimization defaults based on QualityMode
            enableSmoothBlurAnimations = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> true
                QualityMode.BALANCED -> true
                QualityMode.BATTERY_SAVER -> false // Disable animations for battery saving
            },
            enableHardwareBlurAcceleration = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> true
                QualityMode.BALANCED -> true
                QualityMode.BATTERY_SAVER -> false // Disable hardware acceleration for battery saving
            },
            blurRenderingMode = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> BlurRenderingMode.SMOOTH
                QualityMode.BALANCED -> BlurRenderingMode.ADAPTIVE
                QualityMode.BATTERY_SAVER -> BlurRenderingMode.SMOOTH // Use smooth cached patterns
            },
            enableBlurEdgeRefinement = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> true
                QualityMode.BALANCED -> true
                QualityMode.BATTERY_SAVER -> false // Disable edge refinement for battery saving
            },
            blurEdgeAntiAliasing = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> true
                QualityMode.BALANCED -> true
                QualityMode.BATTERY_SAVER -> false // Disable anti-aliasing for battery saving
            },
            blurBoundaryPrecision = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> 0.8f // High precision for quality mode
                QualityMode.BALANCED -> 0.6f // Medium precision for balanced mode
                QualityMode.BATTERY_SAVER -> 0.3f // Low precision for battery saving
            },
            maxBlurRegionsPerFrame = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> 15 // Higher limit for quality mode
                QualityMode.BALANCED -> 12 // Standard limit for balanced mode
                QualityMode.BATTERY_SAVER -> 8 // Lower limit for battery saving
            },
            enableBlurFrameRateLimiting = true, // Always enable frame rate limiting
            enableBlurRegionInterpolation = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> true
                QualityMode.BALANCED -> true
                QualityMode.BATTERY_SAVER -> false // Disable interpolation for battery saving
            },
            blurAnimationDuration = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> 200 // Fast animations for responsiveness
                QualityMode.BALANCED -> 250 // Standard animation duration
                QualityMode.BATTERY_SAVER -> 300 // Slower animations for battery saving
            },
            blurTransitionDuration = when (qualityMode) {
                QualityMode.HIGH_QUALITY -> 100 // Quick transitions
                QualityMode.BALANCED -> 150 // Standard transition duration
                QualityMode.BATTERY_SAVER -> 200 // Slower transitions for battery saving
            },
            // Ensure detection is enabled when applying quality mode
            enableFaceDetection = true,
            enableNSFWDetection = true,
            isServicePaused = false
        )
        updateSettings(updatedSettings)
        Log.d(TAG, "Quality mode updated to: ${qualityMode.displayName} with blur optimization defaults")
    }

    /**
     * Get current quality mode
     */
    fun getCurrentQualityMode(): QualityMode {
        return getCurrentSettings().qualityMode
    }

    /**
     * Check if this is a first-time install (no settings saved yet)
     */
    fun isFirstTimeInstall(): Boolean {
        return !prefs.contains("onboarding_completed")
    }

    /**
     * Apply maximum performance settings using QualityMode.HIGH_QUALITY as source of truth
     */
    suspend fun applyMaximumPerformanceSettings() {
        val currentSettings = getCurrentSettings()
        val maxPerformanceSettings = currentSettings.copy(
            qualityMode = QualityMode.HIGH_QUALITY,
            // Apply High Quality mode settings with maximum performance
            detectionSensitivity = QualityMode.HIGH_QUALITY.detectionSensitivity,
            processingSpeed = QualityMode.HIGH_QUALITY.processingSpeed,
            blurIntensity = QualityMode.HIGH_QUALITY.blurIntensity,
            maxProcessingTimeMs = QualityMode.HIGH_QUALITY.maxProcessingTimeMs,
            frameSkipThreshold = QualityMode.HIGH_QUALITY.frameSkipThreshold,
            imageDownscaleRatio = QualityMode.HIGH_QUALITY.imageDownscaleRatio,
            enableGPUAcceleration = QualityMode.HIGH_QUALITY.enableGPUAcceleration,
            enableRealTimeProcessing = QualityMode.HIGH_QUALITY.enableRealTimeProcessing,
            // Ensure detection is enabled when applying quality mode
            enableFaceDetection = true,
            enableNSFWDetection = true,
            isServicePaused = false,
            ultraFastModeEnabled = false, // Disable ultra fast mode for better quality
            fullScreenWarningEnabled = true,
            enableRegionBasedFullScreen = true,
            // Align thresholds with HIGH_QUALITY detection sensitivity
            nsfwConfidenceThreshold = if (QualityMode.HIGH_QUALITY.detectionSensitivity > 0.8f) 0.5f else 0.4f,
            genderConfidenceThreshold = if (QualityMode.HIGH_QUALITY.detectionSensitivity > 0.8f) 0.4f else 0.3f,
            nsfwFullScreenRegionThreshold = if (QualityMode.HIGH_QUALITY.detectionSensitivity > 0.8f) 6 else 5,
            nsfwHighConfidenceThreshold = if (QualityMode.HIGH_QUALITY.detectionSensitivity > 0.8f) 0.8f else 0.65f
        )
        updateSettings(maxPerformanceSettings)
        Log.d(TAG, "Applied maximum performance settings using QualityMode.HIGH_QUALITY constants")
    }

    /**
     * Apply optimal first-time defaults (High Quality mode)
     * Now delegates to applyMaximumPerformanceSettings() to avoid duplication
     */
    suspend fun applyFirstTimeDefaults() {
        if (isFirstTimeInstall()) {
            applyMaximumPerformanceSettings()
            Log.d(TAG, "Applied first-time defaults with Maximum Performance settings")
        }
    }
}
