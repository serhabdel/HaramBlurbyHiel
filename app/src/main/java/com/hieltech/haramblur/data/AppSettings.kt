package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.detection.BlockingMethod

data class AppSettings(
    // Detection Settings - Female-focused with maximum performance defaults
    val enableFaceDetection: Boolean = true,
    val enableNSFWDetection: Boolean = true, // Enabled with improved error handling
    val blurMaleFaces: Boolean = false, // Disabled - focus on female content only
    val blurFemaleFaces: Boolean = true,
    val detectionSensitivity: Float = 0.8f, // Higher sensitivity for better female detection

    // Blur Settings - Enhanced privacy by default
    val blurIntensity: BlurIntensity = BlurIntensity.STRONG,
    val blurStyle: BlurStyle = BlurStyle.ARTISTIC,
    val expandBlurArea: Int = 30, // pixels to expand around detected areas

    // Performance Settings - Optimized for maximum performance
    val processingSpeed: ProcessingSpeed = ProcessingSpeed.BALANCED,
    val enableRealTimeProcessing: Boolean = true,
    val pauseInApps: Set<String> = emptySet(), // Package names to pause detection

    // Privacy Settings
    val enableFullScreenBlurForNSFW: Boolean = true,
    val showBlurBorders: Boolean = true,
    val enableHoverToReveal: Boolean = false, // Tap to temporarily reveal

    // Enhanced Detection Settings - Maximum performance and accuracy
    val genderDetectionAccuracy: GenderAccuracy = GenderAccuracy.BALANCED,
    
    // Pause/Resume Control - Global pause for all services
    val isServicePaused: Boolean = false, // Global pause state for all detection services
    val contentDensityThreshold: Float = 0.4f, // 40% threshold for full-screen blur
    val mandatoryReflectionTime: Int = 15, // seconds
    val enableSiteBlocking: Boolean = true,
    val enableQuranicGuidance: Boolean = true,
    val ultraFastModeEnabled: Boolean = false,
    val fullScreenWarningEnabled: Boolean = true,

    // Performance Enhancement Settings - Maximum performance by default
    val maxProcessingTimeMs: Long = 50L,
    val enableGPUAcceleration: Boolean = true,
    val frameSkipThreshold: Int = 3,
    val imageDownscaleRatio: Float = 0.5f,

    // Islamic Guidance Settings
    val preferredLanguage: Language = Language.ENGLISH,
    val verseDisplayDuration: Int = 10, // seconds
    val enableArabicText: Boolean = true,
    val customReflectionTime: Int = 15, // seconds for custom reflection periods

    // Advanced Detection Settings - Optimized for female detection with maximum performance
    val genderConfidenceThreshold: Float = 0.4f, // Lower threshold for more sensitive female detection
    val nsfwConfidenceThreshold: Float = 0.5f, // Lower threshold for better content detection
    val enableFallbackDetection: Boolean = true,
    val enablePerformanceMonitoring: Boolean = true,

    // NEW: Region-based Full-Screen Blur Settings
    val enableRegionBasedFullScreen: Boolean = true, // Enable/disable the 6+ regions rule
    val nsfwFullScreenRegionThreshold: Int = 6, // Number of NSFW regions required to trigger full-screen blur
    val nsfwHighConfidenceThreshold: Float = 0.7f, // Minimum confidence level for each region to be considered "high confidence"
    val regionDetectionTileSize: Int = 128, // Size of tiles used for region detection (adaptive based on screen size)
    val regionOverlapPercentage: Float = 0.5f, // Overlap percentage for sliding window region detection
    val maxRegionDetectionTime: Long = 100L, // Maximum time allowed for region detection in milliseconds



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
    val dhikrIntervalMinutes: Int = 60, // Interval between dhikr displays
    val dhikrDisplayDuration: Int = 10, // How long to display each dhikr (seconds)
    val dhikrShowTransliteration: Boolean = true, // Show transliteration
    val dhikrShowTranslation: Boolean = true, // Show English translation
    val dhikrPosition: String = "TOP_RIGHT", // Display position on screen
    val dhikrAnimationEnabled: Boolean = true, // Enable slide-in animation
    val dhikrSoundEnabled: Boolean = false, // Enable notification sound
    
    val settingsVersion: Int = 3 // Configuration version for compatibility tracking
)

enum class BlurIntensity(val displayName: String, val alphaValue: Int, val description: String) {
    LIGHT("Light", 150, "Subtle blur, content partially visible"),
    MEDIUM("Medium", 200, "Balanced blur, good privacy protection"),
    STRONG("Strong", 240, "Heavy blur, maximum privacy"),
    MAXIMUM("Maximum", 255, "Complete coverage, nothing visible")
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