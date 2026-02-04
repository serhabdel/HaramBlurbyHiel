package com.hieltech.haramblur.utils

/**
 * Centralized constants for HaramBlur application
 * This file contains all magic strings, timeouts, and configuration values
 * to ensure consistency and easy maintenance across the codebase.
 */
object AppConstants {

    /**
     * Logging tags used across the application
     */
    object Tags {
        const val ACCESSIBILITY_SERVICE = "HaramBlurAccessibilityService"
        const val BLUR_DEBOUNCER = "BlurUpdateDebouncer"
        const val ML_MODEL_MANAGER = "MLModelManager"
        const val CONTENT_DETECTION = "ContentDetectionEngine"
        const val FACE_DETECTION = "FaceDetectionManager"
        const val SITE_BLOCKING = "SiteBlockingManager"
        const val PRAYER_TIMES = "PrayerTimesRepository"
        const val DHIKR_MANAGER = "DhikrManager"
        const val SETTINGS = "SettingsRepository"
    }

    /**
     * ML Model configuration
     */
    object ML {
        // Model file paths (relative to assets/)
        const val NSFW_MODEL_PATH = "models/nsfw_mobilenet_v2_140_224.1.tflite"
        const val GENDER_MODEL_PATH = "models/model_lite_gender_q.tflite"

        // Input sizes
        const val NSFW_INPUT_SIZE = 224
        const val GENDER_INPUT_SIZE = 96

        // Thresholds
        const val DEFAULT_NSFW_THRESHOLD = 0.45f
        const val DEFAULT_GENDER_THRESHOLD = 0.55f
        const val MIN_NSFW_THRESHOLD = 0.30f
        const val FACE_DETECTION_THRESHOLD = 0.7f
        const val MIN_FACE_SIZE = 64

        // Timeouts (milliseconds)
        const val DEFAULT_ML_TIMEOUT_MS = 5000L
        const val FAST_ML_TIMEOUT_MS = 100L
        const val ULTRA_FAST_ML_TIMEOUT_MS = 50L

        // Processing
        const val MIN_TILE_SIZE = 64
        const val MAX_OVERLAP_PERCENTAGE = 0.6f

        // Cache
        const val CACHE_EXPIRATION_MS = 5000L
    }

    /**
     * Performance and timing configuration
     */
    object Performance {
        // Capture intervals (milliseconds)
        const val DEFAULT_CAPTURE_INTERVAL_MS = 2000L
        const val FAST_CAPTURE_INTERVAL_MS = 1000L
        const val ULTRA_FAST_CAPTURE_INTERVAL_MS = 500L
        const val HIGH_QUALITY_CAPTURE_INTERVAL_MS = 3000L

        // Debounce delays
        const val BLUR_DEBOUNCE_MS = 100L
        const val SCROLL_DEBOUNCE_MS = 150L

        // Memory management
        const val BITMAP_CACHE_SIZE = 5
        const val MAX_BITMAP_DIMENSION = 2048
    }

    /**
     * Broadcast actions and intent extras
     */
    object BroadcastActions {
        const val EMERGENCY_RESET = "com.hieltech.haramblur.EMERGENCY_RESET"
        const val DHIKR_DISMISS = "com.hieltech.haramblur.DHIKR_DISMISS"
        const val DHIKR_NEXT = "com.hieltech.haramblur.DHIKR_NEXT"
        const val DHIKR_SHOW_NOW = "com.hieltech.haramblur.DHIKR_SHOW_NOW"
        const val PRAYER_COMPLETED = "com.hieltech.haramblur.PRAYER_COMPLETED"
        const val PRAYER_NOT_COMPLETED = "com.hieltech.haramblur.PRAYER_NOT_COMPLETED"
        const val PRAYER_WILL_DO_NOW = "com.hieltech.haramblur.PRAYER_WILL_DO_NOW"
        const val REFLECT_AND_CONTINUE = "com.hieltech.haramblur.REFLECT_AND_CONTINUE"
        const val CLOSE_APP = "com.hieltech.haramblur.CLOSE_APP"
        const val OPEN_SETTINGS = "com.hieltech.haramblur.OPEN_SETTINGS"
        const val DISMISS_USAGE_NOTIFICATION = "com.hieltech.haramblur.DISMISS_USAGE_NOTIFICATION"
        const val SHOW_GUIDANCE = "com.hieltech.haramblur.SHOW_GUIDANCE"

        // Widget actions
        const val WIDGET_INCREMENT_TASBIH = "com.hieltech.haramblur.widget.INCREMENT_TASBIH"
        const val WIDGET_RESET_TASBIH = "com.hieltech.haramblur.widget.RESET_TASBIH"
        const val WIDGET_UPDATE = "com.hieltech.haramblur.widget.UPDATE_WIDGET"
        const val WIDGET_DHIKR_INCREMENT = "com.hieltech.haramblur.widget.DHIKR_INCREMENT"
        const val WIDGET_DHIKR_RESET = "com.hieltech.haramblur.widget.DHIKR_RESET"
    }

    /**
     * Notification channel IDs
     */
    object NotificationChannels {
        const val DHIKR_CHANNEL_ID = "haramblur_dhikr_channel"
        const val PRAYER_CHANNEL_ID = "haramblur_prayer_channel"
        const val SERVICE_CHANNEL_ID = "haramblur_service_channel"
        const val USAGE_TIME_CHANNEL_ID = "haramblur_usage_time_channel"
        const val GENERAL_CHANNEL_ID = "haramblur_general_channel"
    }

    /**
     * SharedPreferences keys
     */
    object PreferenceKeys {
        const val PREFS_NAME = "HaramBlurPrefs"
        const val FIRST_RUN_COMPLETED = "first_run_completed"
        const val USER_GENDER = "user_gender"
        const val DETECTION_ENABLED = "detection_enabled"
        const val BLUR_INTENSITY = "blur_intensity"
        const val PERFORMANCE_MODE = "performance_mode"
        const val LANGUAGE = "app_language"
        const val PRAYER_NOTIFICATIONS_ENABLED = "prayer_notifications_enabled"
        const val DHIKR_ENABLED = "dhikr_enabled"
        const val LAST_PRAYER_CALCULATION_DATE = "last_prayer_calculation_date"
    }

    /**
     * Database configuration
     */
    object Database {
        const val DATABASE_NAME = "site_blocking_database.db"
        const val DATABASE_VERSION = 1
        const val MAX_LOG_ENTRIES = 1000
    }

    /**
     * Watchdog and service monitoring
     */
    object ServiceMonitoring {
        const val WATCHDOG_CHECK_INTERVAL_MS = 30000L // 30 seconds
        const val WATCHDOG_REASON_DESTROY = "service_destroyed"
        const val WATCHDOG_REASON_TASK_REMOVED = "task_removed"
        const val WATCHDOG_REASON_ACTIVE = "service_active"
        const val WATCHDOG_REASON_RESTART = "service_restart"
    }

    /**
     * API configuration
     */
    object API {
        const val ALADHAN_API_BASE_URL = "https://api.aladhan.com/v1/"
        const val DEFAULT_API_TIMEOUT_MS = 30000L
        const val API_RETRY_COUNT = 3
        const val API_RETRY_DELAY_MS = 1000L
    }

    /**
     * Content blocking categories
     */
    object BlockingCategories {
        const val CATEGORY_ADULT = "adult_content"
        const val CATEGORY_GAMBLING = "gambling"
        const val CATEGORY_DATING = "dating"
        const val CATEGORY_SOCIAL_MEDIA = "social_media"
        const val CATEGORY_SUSPICIOUS = "suspicious"
        const val CATEGORY_QURANIC = "quranic"
    }

    /**
     * Blur configuration defaults
     */
    object BlurDefaults {
        const val DEFAULT_BLUR_INTENSITY = 0.8f
        const val MIN_BLUR_INTENSITY = 0.3f
        const val MAX_BLUR_INTENSITY = 1.0f
        const val DEFAULT_ANIMATION_DURATION_MS = 200L
    }
}
