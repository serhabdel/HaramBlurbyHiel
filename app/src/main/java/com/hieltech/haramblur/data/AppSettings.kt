package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.Language

data class AppSettings(
    // Detection Settings
    val enableFaceDetection: Boolean = true,
    val enableNSFWDetection: Boolean = true,
    val blurMaleFaces: Boolean = true,
    val blurFemaleFaces: Boolean = true,
    val detectionSensitivity: Float = 0.5f, // 0.0 = least sensitive, 1.0 = most sensitive
    
    // Blur Settings
    val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    val blurStyle: BlurStyle = BlurStyle.PIXELATED,
    val expandBlurArea: Int = 30, // pixels to expand around detected areas
    
    // Performance Settings
    val processingSpeed: ProcessingSpeed = ProcessingSpeed.BALANCED,
    val enableRealTimeProcessing: Boolean = true,
    val pauseInApps: Set<String> = emptySet(), // Package names to pause detection
    
    // Privacy Settings
    val enableFullScreenBlurForNSFW: Boolean = true,
    val showBlurBorders: Boolean = true,
    val enableHoverToReveal: Boolean = false, // Tap to temporarily reveal
    
    // Enhanced Detection Settings
    val genderDetectionAccuracy: GenderAccuracy = GenderAccuracy.HIGH,
    val contentDensityThreshold: Float = 0.4f, // 40% threshold for full-screen blur
    val mandatoryReflectionTime: Int = 15, // seconds
    val enableSiteBlocking: Boolean = true,
    val enableQuranicGuidance: Boolean = true,
    val ultraFastModeEnabled: Boolean = false,
    val fullScreenWarningEnabled: Boolean = true,
    
    // Performance Enhancement Settings
    val maxProcessingTimeMs: Long = 50L,
    val enableGPUAcceleration: Boolean = true,
    val frameSkipThreshold: Int = 3,
    val imageDownscaleRatio: Float = 0.5f,
    
    // Islamic Guidance Settings
    val preferredLanguage: Language = Language.ENGLISH,
    val verseDisplayDuration: Int = 10, // seconds
    val enableArabicText: Boolean = true,
    val customReflectionTime: Int = 15, // seconds for custom reflection periods
    
    // Advanced Detection Settings
    val genderConfidenceThreshold: Float = 0.8f,
    val nsfwConfidenceThreshold: Float = 0.7f,
    val enableFallbackDetection: Boolean = true,
    val enablePerformanceMonitoring: Boolean = true
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