package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.detection.BlockingMethod
import com.hieltech.haramblur.data.compass.CompassSize
import com.hieltech.haramblur.data.models.AppCategory

data class AppSettings(
    // Theme Settings
    val appTheme: AppTheme = AppTheme.ISLAMIC_LIGHT, // Default to Islamic Light theme

    // Quality Mode - Simple selection for users (defaults to High Quality for first install)
    val qualityMode: QualityMode = QualityMode.HIGH_QUALITY,
    
    // Detection Settings - Smart gender-based detection with maximum performance defaults
    val enableFaceDetection: Boolean = true,
    val enableNSFWDetection: Boolean = true, // Enabled with improved error handling
    val blurMaleFaces: Boolean = false, // Will be set based on user gender
    val blurFemaleFaces: Boolean = true, // Will be set based on user gender
    val detectionSensitivity: Float = 0.9f, // Higher sensitivity for better detection
    
    // NEW: Gender-based Settings for Islamic Compliance
    val userGender: UserGender = UserGender.NOT_SPECIFIED, // User's gender for appropriate content filtering

    // Blur Settings - Enhanced privacy by default
    val blurIntensity: BlurIntensity = BlurIntensity.STRONG,
    val blurStyle: BlurStyle = BlurStyle.ARTISTIC,
    val expandBlurArea: Int = 30, // pixels to expand around detected areas

    // Performance Settings - Optimized for maximum accuracy regardless of performance impact
    val processingSpeed: ProcessingSpeed = ProcessingSpeed.ULTRA_FAST, // Maximum accuracy mode
    val enableRealTimeProcessing: Boolean = true,
    val pauseInApps: Set<String> = emptySet(), // Package names to pause detection

    // Privacy Settings
    val enableFullScreenBlurForNSFW: Boolean = true,
    val showBlurBorders: Boolean = true,
    val enableHoverToReveal: Boolean = false, // Tap to temporarily reveal

    // Enhanced Detection Settings - Maximum performance and accuracy
    val genderDetectionAccuracy: GenderAccuracy = GenderAccuracy.HIGH, // Maximum accuracy
    
    // Pause/Resume Control - Global pause for all services
    val isServicePaused: Boolean = false, // Global pause state for all detection services
    val contentDensityThreshold: Float = 0.4f, // 40% threshold for full-screen blur
    val mandatoryReflectionTime: Int = 15, // seconds
    val enableSiteBlocking: Boolean = true,
    val enableQuranicGuidance: Boolean = true,
    val ultraFastModeEnabled: Boolean = true, // Enable ultra-fast mode for maximum accuracy
    val fullScreenWarningEnabled: Boolean = true,

    // Performance Enhancement Settings - Maximum accuracy regardless of performance impact
    val maxProcessingTimeMs: Long = 30L, // Maximum accuracy mode
    val enableGPUAcceleration: Boolean = true, // Already optimal
    val frameSkipThreshold: Int = 1, // Maximum accuracy mode
    val imageDownscaleRatio: Float = 0.8f, // Maximum accuracy mode

    // Islamic Guidance Settings
    val preferredLanguage: Language = Language.ENGLISH,
    val verseDisplayDuration: Int = 10, // seconds
    val enableArabicText: Boolean = true,
    val customReflectionTime: Int = 15, // seconds for custom reflection periods

    // Advanced Detection Settings - HIGH PRECISION defaults for maximum protection
    // See DetectionThresholds.kt for threshold documentation and rationale
    val genderConfidenceThreshold: Float = 0.35f, // High precision - lower threshold catches more faces
    val nsfwConfidenceThreshold: Float = 0.35f, // High precision - lower threshold for better NSFW detection
    val enableFallbackDetection: Boolean = true,
    val enablePerformanceMonitoring: Boolean = true,

    // NEW: Region-based Full-Screen Blur Settings - Aligned with HIGH_QUALITY defaults
    val enableRegionBasedFullScreen: Boolean = true, // Enable/disable the 6+ regions rule
    val nsfwFullScreenRegionThreshold: Int = 6, // Aligned with HIGH_QUALITY detection sensitivity
    val nsfwHighConfidenceThreshold: Float = 0.8f, // Aligned with HIGH_QUALITY detection sensitivity
    val regionDetectionTileSize: Int = 96, // Reduced for more granular region detection
    val regionOverlapPercentage: Float = 0.5f, // Overlap percentage for sliding window region detection
    val maxRegionDetectionTime: Long = 75L, // Reduced for faster region analysis

    // NEW: Blur Animation Settings
    val enableSmoothBlurAnimations: Boolean = true, // Enable/disable smooth fade and transition animations
    val blurAnimationDuration: Int = 250, // Duration in milliseconds for blur overlay fade animations
    val blurTransitionDuration: Int = 150, // Duration for blur intensity change transitions
    val enableBlurRegionInterpolation: Boolean = true, // Enable smooth interpolation between blur region changes

    // NEW: Performance Optimization Settings
    val enableHardwareBlurAcceleration: Boolean = true, // Enable hardware-accelerated blur rendering when available
    val blurRenderingMode: BlurRenderingMode = BlurRenderingMode.SMOOTH, // Choose between SMOOTH (cached) and QUALITY (full) rendering
    val maxBlurRegionsPerFrame: Int = 12, // Maximum number of blur regions to render per frame to maintain 60fps
    val enableBlurFrameRateLimiting: Boolean = true, // Limit blur rendering to 60fps to prevent excessive CPU usage

    // NEW: Edge Refinement Settings
    val enableBlurEdgeRefinement: Boolean = true, // Enable precise edge detection for blur boundaries
    val blurEdgeAntiAliasing: Boolean = true, // Enable anti-aliasing for smoother blur edges
    val blurBoundaryPrecision: Float = 0.5f, // Precision factor for blur boundary calculations (0.1-1.0)

    // Logging Settings - Enterprise/SaaS-style logging
    val enableDetailedLogging: Boolean = true, // Enable detailed logging for troubleshooting
    val logLevel: LogLevel = LogLevel.INFO, // Minimum log level to record
    val enableLogCategories: Set<LogCategory> = setOf(
        LogCategory.DETECTION,
        LogCategory.BLOCKING,
        LogCategory.UI,
        LogCategory.ACCESSIBILITY
    ), // Which categories to log
    val maxLogRetentionDays: Int = 7, // How long to keep logs
    val enablePerformanceLogging: Boolean = true, // Log performance metrics
    val enableErrorReporting: Boolean = true, // Log errors and crashes
    val enableUserActionLogging: Boolean = true, // Log user actions for troubleshooting

    // Enhanced Blocking Settings
    val enableEnhancedBlocking: Boolean = false, // Enable enhanced app blocking features
    val usageStatsPermissionGranted: Boolean = false, // Usage stats permission status
    val deviceAdminEnabled: Boolean = false, // Device admin permission status
    val preferredBlockingMethod: BlockingMethod = BlockingMethod.ADAPTIVE, // Preferred blocking method
    val forceCloseTimeout: Long = 5000L, // Timeout for force close operations in milliseconds

    // Onboarding and Permission Wizard Settings
    val onboardingCompleted: Boolean = false, // Track if user has completed initial setup wizard
    val accessibilityServiceEnabled: Boolean = false, // Accessibility service permission status
    val permissionWizardLastShown: Long = 0L, // Timestamp when wizard was last displayed
    val skipOptionalPermissions: Boolean = false, // Remember if user chose to skip optional permissions

    // Preset Management Settings
    val currentPreset: String = "Custom", // Name of currently active preset
    val lastPresetUpdate: Long = 0L, // Timestamp of last preset application
    val presetLockEnabled: Boolean = false, // Whether current preset is locked
    
    // Dhikr Settings - Islamic Remembrance Display
    val dhikrEnabled: Boolean = true, // Enable/disable dhikr feature
    val dhikrMorningEnabled: Boolean = true, // Show morning dhikr
    val dhikrEveningEnabled: Boolean = true, // Show evening dhikr
    val dhikrAnytimeEnabled: Boolean = true, // Show anytime dhikr
    val dhikrMorningStart: Int = 5, // Morning dhikr start time (24-hour format)
    val dhikrMorningEnd: Int = 10, // Morning dhikr end time
    val dhikrEveningStart: Int = 17, // Evening dhikr start time
    val dhikrEveningEnd: Int = 22, // Evening dhikr end time
    val dhikrIntervalMinutes: Int = 5, // Interval between dhikr displays (5 minutes)
    val dhikrDisplayDuration: Int = 30, // How long to display each dhikr (seconds)
    val dhikrShowTransliteration: Boolean = true, // Show transliteration
    val dhikrShowTranslation: Boolean = true, // Show English translation
    val dhikrPosition: String = "TOP_RIGHT", // Display position on screen
    val dhikrAnimationEnabled: Boolean = true, // Enable slide-in animation
    val dhikrSoundEnabled: Boolean = false, // Enable notification sound
    val dhikrSleepStartMinutes: Int = 1350, // 22:30 PM in minutes-of-day format
    val dhikrSleepEndMinutes: Int = 390,    // 6:30 AM in minutes-of-day format

    // Islamic Calendar & Prayer Times Settings
    val enableIslamicCalendar: Boolean = true, // Enable Islamic calendar display
    val enablePrayerTimes: Boolean = true, // Enable prayer times display
    val enablePrayerNotifications: Boolean = true, // Enable prayer time notifications
    val prayerCalculationMethod: Int = 2, // Calculation method (2 = ISNA)
    val prayerNotificationAdvanceTime: Int = 15, // Minutes before prayer to notify
    val locationLatitude: Double? = null, // Cached location latitude
    val locationLongitude: Double? = null, // Cached location longitude
    val locationCity: String? = null, // Cached city name
    val locationCountry: String? = null, // Cached country name
    val locationCountryCode: String? = null, // ISO country code for better accuracy
    val enableQiblaDirection: Boolean = true, // Enable Qibla direction feature
    val prayerTimesUpdateInterval: Int = 30, // Minutes between prayer times updates
    val islamicCalendarUpdateInterval: Int = 60, // Minutes between calendar updates
    val autoDetectLocation: Boolean = true, // Auto-detect location vs manual entry (legacy)
    /**
     * Preferred method for determining the user's location for prayer time calculation.
     * Replaces legacy autoDetectLocation with a more explicit choice.
     */
    val locationMethod: LocationMethod = LocationMethod.GPS,
    /**
     * Latest measured GPS accuracy in meters. Used for UI display of accuracy tier.
     */
    val locationAccuracy: Float? = null,
    /**
     * Epoch millis when location was last successfully obtained.
     */
    val locationLastUpdated: Long? = null,
    /**
     * Tracked permission state for location to simplify UI logic and avoid repeated checks.
     */
    val locationPermissionStatus: LocationPermissionStatus = LocationPermissionStatus.UNKNOWN,
    val preferredCity: String? = null, // User preferred city (legacy)
    val preferredCountry: String? = null, // User preferred country (legacy)
    val hijriCalendarMethod: String = "MOON_SIGHTING", // Hijri calendar method

    // New: City Selection fields (enhanced)
    val selectedCityName: String? = null,
    val selectedCountry: String? = null,
    val selectedCountryCode: String? = null,
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,

    // Preferences for city search behavior
    val enableCitySearchCache: Boolean = true,
    val enableOfflineCityFallback: Boolean = true,
    val preferStoredCoordinates: Boolean = true,

    // Prayer Enhancements: Offsets, history, caching and validation
    /** Per-prayer manual offsets in minutes. Positive values delay the prayer, negative advance it. */
    val fajrOffsetMinutes: Int = 0,
    /** Offset for Sunrise time display in minutes. */
    val sunriseOffsetMinutes: Int = 0,
    /** Offset for Dhuhr time display in minutes. */
    val dhuhrOffsetMinutes: Int = 0,
    /** Offset for Asr time display in minutes. */
    val asrOffsetMinutes: Int = 0,
    /** Offset for Maghrib time display in minutes. */
    val maghribOffsetMinutes: Int = 0,
    /** Offset for Isha time display in minutes. */
    val ishaOffsetMinutes: Int = 0,
    /** Enable storing fetched prayer times into local history for offline display and analysis. */
    val enablePrayerHistory: Boolean = true,
    /** Number of days to retain historical prayer times in the local database. */
    val prayerHistoryRetentionDays: Int = 60,
    /** Cache TTL in minutes for prayer times fetches (separate from display update interval). */
    val prayerCacheTtlMinutes: Int = 30,
    /** Consider location stale after this many minutes and trigger a refresh when using GPS. */
    val locationStaleAfterMinutes: Int = 60,
    /** Enable strict validation to detect obviously incorrect API results (timezone/date mismatches etc). */
    val strictPrayerAccuracyValidation: Boolean = true,
    /** Threshold in minutes to consider an API result suspicious when compared against previous data. */
    val maxAllowedPrayerShiftMinutes: Int = 20,
    /** GPS accuracy thresholds (meters) used to classify accuracy tiers for UI chips. */
    val gpsAccuracyHighThresholdM: Float = 30f,
    val gpsAccuracyMediumThresholdM: Float = 100f,
    val gpsAccuracyLowThresholdM: Float = 300f,

    // Local Prayer Calculation Settings
    /** Enable local prayer time calculations without API calls */
    val enableLocalCalculations: Boolean = false,
    /** Prefer local calculations over API calls when both are available */
    val preferLocalOverApi: Boolean = false,
    /** Show calculation method source (API vs Local) in the UI */
    val showCalculationMethod: Boolean = true,
    /** Enable Morocco-specific adjustments for prayer times */
    val moroccoSpecificAdjustments: Boolean = true,

    // Qibla Compass Settings
    /** Enable/disable the interactive Qibla compass feature */
    val qiblaCompassEnabled: Boolean = true,
    /** Show calibration reminders and guidance overlay when sensor accuracy is low */
    val compassCalibrationReminders: Boolean = true,
    /** Provide haptic feedback when device is aligned to Qibla within tolerance */
    val compassHapticFeedback: Boolean = true,
    /** Show degree markings and labels on the compass UI */
    val compassShowDegreeMarkings: Boolean = true,
    /** Adjust sensor responsiveness/smoothing. Valid range: 0.5..2.0 */
    val compassSensitivity: Float = 1.0f,
    /** Minimum acceptable sensor accuracy in degrees for green status */
    val compassAccuracyThreshold: Float = 20.0f,
    /** Tolerance in degrees to consider pointing toward Qibla */
    val qiblaToleranceDegrees: Float = 5.0f,
    /** Rotation animation speed multiplier for compass needle */
    val compassAnimationSpeed: Float = 1.0f,
    /** Epoch millis timestamp of last successful compass calibration */
    val lastCompassCalibration: Long = 0L,
    /** Preferred compass size in UI */
    val compassPreferredSize: CompassSize = CompassSize.MEDIUM,
    /** Enable magnetic declination correction to use true north */
    val enableMagneticDeclination: Boolean = true,
    /** Sensor update frequency in Hz (recommended ~15Hz) */
    val compassUpdateRate: Int = 15,

    // App-Specific Detection Settings
    val enableAppSpecificDetection: Boolean = false, // Toggle between monitoring all apps vs specific categories (disabled by default for maximum protection)
    val monitoredAppCategories: Set<AppCategory> = AppCategory.values().toSet(), // All categories monitored by default
    val customMonitoredApps: Set<String> = emptySet(), // User-added package names for monitoring
    val excludedApps: Set<String> = emptySet(), // Apps to exclude from detection even if in monitored categories

    // Usage Time Tracking Settings
    val enableUsageTimeNotifications: Boolean = true, // Enable/disable usage time notifications
    val defaultSocialMediaTimeLimit: Int = 60, // Default time limit in minutes for social media apps
    val defaultMessagingTimeLimit: Int = 120, // Default time limit in minutes for messaging apps
    val customAppTimeLimits: Map<String, Int> = emptyMap(), // Custom time limits per app package name
    val usageNotificationFrequency: Int = 30, // How often to show notifications after limit exceeded (minutes)
    val enableDailyUsageReset: Boolean = true, // Reset usage stats daily at midnight
    val lastUsageResetDate: Long? = null, // Epoch day of last usage reset (LocalDate.toEpochDay())
    val usageDefaultsSeeded: Boolean = false, // Whether default app time limits have been prepopulated

    // Settings schema version. Bump to 10 for local prayer calculation settings
    val settingsVersion: Int = 10 // Configuration version for compatibility tracking
)

enum class BlurIntensity(val displayName: String, val alphaValue: Int, val description: String) {
    LIGHT("Light", 150, "Subtle blur, content partially visible"),
    MEDIUM("Medium", 200, "Balanced blur, good privacy protection"),
    STRONG("Strong", 240, "Heavy blur, maximum privacy"),
    MAXIMUM("Maximum", 255, "Complete coverage, nothing visible")
}

/**
 * Explicit location method selection used across the app for clarity and type-safety.
 */
enum class LocationMethod {
    GPS,
    MANUAL_CITY
}

/**
 * Permission status for location. Helps the UI render appropriate states.
 */
enum class LocationPermissionStatus {
    GRANTED,
    DENIED,
    UNKNOWN
}

/**
 * User-friendly accuracy tiers for display purposes.
 */
enum class LocationAccuracy {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

enum class BlurStyle(val displayName: String, val description: String) {
    SOLID("Solid", "Simple gray overlay"),
    PIXELATED("Pixelated", "Mosaic-style blur effect"),
    NOISE("Noise", "Random pattern blur"),
    ARTISTIC("Artistic", "Film grain style blur effect"),
    COMBINED("Combined", "Multiple blur effects layered")
}

enum class ProcessingSpeed(val displayName: String, val intervalMs: Long, val description: String) {
    FAST("Fast", 500L, "Quick detection, higher battery usage"),
    BALANCED("Balanced", 800L, "Good balance of speed and efficiency"),
    BATTERY_SAVER("Battery Saver", 1500L, "Slower detection, better battery life"),
    ULTRA_FAST("Ultra Fast", 300L, "Maximum responsiveness, highest battery usage")
}

/**
 * Simple quality modes for easy user selection - replaces complex individual settings
 */
enum class QualityMode(
    val displayName: String,
    val description: String,
    val icon: String,
    val detectionSensitivity: Float,
    val processingSpeed: ProcessingSpeed,
    val blurIntensity: BlurIntensity,
    val maxProcessingTimeMs: Long,
    val frameSkipThreshold: Int,
    val imageDownscaleRatio: Float,
    val enableGPUAcceleration: Boolean,
    val enableRealTimeProcessing: Boolean,
    // NEW: Enhanced blur optimization defaults
    val enableSmoothBlurAnimations: Boolean = false,
    val enableHardwareBlurAcceleration: Boolean = true,
    val blurRenderingMode: BlurRenderingMode = BlurRenderingMode.SMOOTH,
    val enableBlurEdgeRefinement: Boolean = true,
    val blurEdgeAntiAliasing: Boolean = true,
    val blurBoundaryPrecision: Float = 0.5f,
    val maxBlurRegionsPerFrame: Int = 10,
    val enableBlurFrameRateLimiting: Boolean = true,
    val enableBlurRegionInterpolation: Boolean = true,
    val blurAnimationDuration: Int = 200,
    val blurTransitionDuration: Int = 300
) {
    /**
     * MAXIMUM_PRECISION: Optimized for the highest blur accuracy and detection precision.
     * Uses no image downscaling, highest sensitivity, and enhanced edge refinement.
     * Best for users who prioritize protection accuracy over battery life.
     */
    MAXIMUM_PRECISION(
        displayName = "Maximum Precision",
        description = "Highest blur accuracy with precise edge detection and no quality compromises",
        icon = "🎯",
        detectionSensitivity = 0.95f, // Maximum detection sensitivity
        processingSpeed = ProcessingSpeed.ULTRA_FAST, // Fastest processing
        blurIntensity = BlurIntensity.MAXIMUM, // Complete coverage
        maxProcessingTimeMs = 100L, // Allow more time for precision
        frameSkipThreshold = 0, // Process every single frame
        imageDownscaleRatio = 1.0f, // NO downscaling - full resolution
        enableGPUAcceleration = true,
        enableRealTimeProcessing = true,
        // Maximum precision blur optimization
        enableSmoothBlurAnimations = true,
        enableHardwareBlurAcceleration = true,
        blurRenderingMode = BlurRenderingMode.QUALITY, // Full quality rendering
        enableBlurEdgeRefinement = true,
        blurEdgeAntiAliasing = true,
        blurBoundaryPrecision = 1.0f, // Maximum precision for blur boundaries
        maxBlurRegionsPerFrame = 20, // Higher limit for precision mode
        enableBlurFrameRateLimiting = false, // No frame limiting for precision
        enableBlurRegionInterpolation = true,
        blurAnimationDuration = 150, // Fast animations
        blurTransitionDuration = 75 // Quick transitions
    ),
    HIGH_QUALITY(
        displayName = "High Quality",
        description = "Maximum protection and detection accuracy with enhanced blur optimizations",
        icon = "🛡️",
        detectionSensitivity = 0.9f, // Increased for maximum detection sensitivity
        processingSpeed = ProcessingSpeed.ULTRA_FAST, // Upgraded to ULTRA_FAST for maximum responsiveness
        blurIntensity = BlurIntensity.STRONG,
        maxProcessingTimeMs = 30L, // Reduced for faster processing cycles
        frameSkipThreshold = 1, // Already optimal
        imageDownscaleRatio = 0.8f, // Increased for higher quality image processing
        enableGPUAcceleration = true,
        enableRealTimeProcessing = true,
        // NEW: Enhanced blur optimization defaults for HIGH_QUALITY
        enableSmoothBlurAnimations = true,
        enableHardwareBlurAcceleration = true,
        blurRenderingMode = BlurRenderingMode.SMOOTH,
        enableBlurEdgeRefinement = true,
        blurEdgeAntiAliasing = true,
        blurBoundaryPrecision = 0.8f, // High precision for quality mode
        maxBlurRegionsPerFrame = 15, // Higher limit for quality mode
        enableBlurFrameRateLimiting = true,
        enableBlurRegionInterpolation = true,
        blurAnimationDuration = 200, // Fast animations for responsiveness
        blurTransitionDuration = 100 // Quick transitions
    ),
    BALANCED(
        displayName = "Balanced",
        description = "Good protection with optimal performance and balanced blur optimizations",
        icon = "⚖️",
        detectionSensitivity = 0.7f,
        processingSpeed = ProcessingSpeed.BALANCED,
        blurIntensity = BlurIntensity.MEDIUM,
        maxProcessingTimeMs = 75L,
        frameSkipThreshold = 2,
        imageDownscaleRatio = 0.5f,
        enableGPUAcceleration = true,
        enableRealTimeProcessing = true,
        // NEW: Balanced blur optimization defaults
        enableSmoothBlurAnimations = true,
        enableHardwareBlurAcceleration = true,
        blurRenderingMode = BlurRenderingMode.ADAPTIVE, // Adaptive for balanced performance
        enableBlurEdgeRefinement = true,
        blurEdgeAntiAliasing = true,
        blurBoundaryPrecision = 0.6f, // Medium precision
        maxBlurRegionsPerFrame = 12, // Standard limit
        enableBlurFrameRateLimiting = true,
        enableBlurRegionInterpolation = true,
        blurAnimationDuration = 250, // Standard animation duration
        blurTransitionDuration = 150 // Standard transition duration
    ),
    BATTERY_SAVER(
        displayName = "Battery Saver",
        description = "Essential protection with minimal battery usage and optimized blur performance",
        icon = "🔋",
        detectionSensitivity = 0.6f,
        processingSpeed = ProcessingSpeed.BATTERY_SAVER,
        blurIntensity = BlurIntensity.LIGHT,
        maxProcessingTimeMs = 50L,
        frameSkipThreshold = 3,
        imageDownscaleRatio = 0.4f,
        enableGPUAcceleration = false,
        enableRealTimeProcessing = false,
        // NEW: Battery-optimized blur settings
        enableSmoothBlurAnimations = false, // Disable animations to save battery
        enableHardwareBlurAcceleration = false, // Disable hardware acceleration to save battery
        blurRenderingMode = BlurRenderingMode.SMOOTH, // Use smooth cached patterns
        enableBlurEdgeRefinement = false, // Disable edge refinement to save processing
        blurEdgeAntiAliasing = false, // Disable anti-aliasing to save processing
        blurBoundaryPrecision = 0.3f, // Low precision for battery saving
        maxBlurRegionsPerFrame = 8, // Lower limit to reduce processing
        enableBlurFrameRateLimiting = true, // Strict frame rate limiting
        enableBlurRegionInterpolation = false, // Disable interpolation to save battery
        blurAnimationDuration = 300, // Slower animations
        blurTransitionDuration = 200 // Slower transitions
    );
    
    companion object {
        /**
         * Get recommended quality mode based on device performance tier
         */
        fun getRecommendedMode(isHighEndDevice: Boolean, prioritizeBattery: Boolean): QualityMode {
            return when {
                prioritizeBattery -> BATTERY_SAVER
                isHighEndDevice -> MAXIMUM_PRECISION
                else -> HIGH_QUALITY
            }
        }
    }
}

enum class GenderAccuracy(val confidenceThreshold: Float, val description: String) {
    FAST(0.75f, "Fast detection, 75% accuracy"),
    BALANCED(0.85f, "Balanced speed and accuracy"),
    HIGH(0.92f, "High accuracy, slower processing")
}

enum class LogLevel(val priority: Int, val displayName: String, val description: String) {
    DEBUG(0, "Debug", "All logs including debug information"),
    INFO(1, "Info", "Informational messages and warnings"),
    WARN(2, "Warning", "Warnings and errors only"),
    ERROR(3, "Error", "Errors only")
}

enum class LogCategory(val displayName: String, val description: String) {
    GENERAL("General", "General application logs"),
    DETECTION("Detection", "Face and content detection logs"),
    BLOCKING("Blocking", "Content blocking and blurring logs"),
    UI("User Interface", "UI interaction and navigation logs"),
    NETWORK("Network", "Network requests and connectivity logs"),
    DATABASE("Database", "Database operations and queries"),
    ACCESSIBILITY("Accessibility", "Accessibility service logs"),
    PERFORMANCE("Performance", "Performance metrics and timing logs")
}

/**
 * Blur rendering modes for performance optimization
 */
enum class BlurRenderingMode(val displayName: String, val description: String) {
    SMOOTH("Smooth", "Cached patterns for faster rendering"),
    QUALITY("Quality", "Full rendering for maximum quality"),
    ADAPTIVE("Adaptive", "Switches based on performance")
}

/**
 * App theme options with dark and light variants
 */
enum class AppTheme(val displayName: String, val description: String) {
    ISLAMIC_LIGHT("Islamic Light", "Traditional Islamic colors with light background"),
    ISLAMIC_DARK("Islamic Dark", "Traditional Islamic colors with dark background"),
    MODERN_LIGHT("Modern Light", "Clean modern design with light background"),
    MODERN_DARK("Modern Dark", "Clean modern design with dark background"),
    MINIMAL_LIGHT("Minimal Light", "Minimalist design with light background"),
    MINIMAL_DARK("Minimal Dark", "Minimalist design with dark background")
}