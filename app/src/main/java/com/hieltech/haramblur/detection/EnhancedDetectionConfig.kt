package com.hieltech.haramblur.detection

/**
 * Configuration constants and defaults for enhanced detection system
 */
object EnhancedDetectionConfig {
    
    // Performance Thresholds
    const val ULTRA_FAST_PROCESSING_TIME_MS = 50L
    const val FAST_PROCESSING_TIME_MS = 100L
    const val BALANCED_PROCESSING_TIME_MS = 200L
    const val HIGH_QUALITY_PROCESSING_TIME_MS = 500L
    
    // Gender Detection
    const val GENDER_CONFIDENCE_THRESHOLD_HIGH = 0.92f
    const val GENDER_CONFIDENCE_THRESHOLD_BALANCED = 0.85f
    const val GENDER_CONFIDENCE_THRESHOLD_FAST = 0.75f
    
    // Content Density
    const val FULL_SCREEN_BLUR_THRESHOLD = 0.4f // 40% of screen
    const val CRITICAL_CONTENT_THRESHOLD = 0.6f // 60% of screen
    const val WARNING_CONTENT_THRESHOLD = 0.25f // 25% of screen
    
    // Site Blocking
    const val SITE_BLOCKING_CONFIDENCE_THRESHOLD = 0.8f
    const val DEFAULT_REFLECTION_TIME_SECONDS = 15
    const val MINIMUM_REFLECTION_TIME_SECONDS = 5
    const val MAXIMUM_REFLECTION_TIME_SECONDS = 30
    
    // Performance Optimization
    const val MAX_CONCURRENT_DETECTIONS = 3
    const val CACHE_SIZE_MB = 50
    const val IMAGE_DOWNSCALE_ULTRA_FAST = 0.25f // 224x224 -> 56x56
    const val IMAGE_DOWNSCALE_FAST = 0.5f // 224x224 -> 112x112
    const val IMAGE_DOWNSCALE_BALANCED = 0.75f // 224x224 -> 168x168
    
    // Error Recovery
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
    const val FALLBACK_TIMEOUT_MS = 2000L
    
    // Islamic Guidance
    const val DEFAULT_VERSE_DISPLAY_DURATION_SECONDS = 10
    const val MINIMUM_VERSE_DISPLAY_DURATION_SECONDS = 5
    const val MAXIMUM_VERSE_DISPLAY_DURATION_SECONDS = 30
    
    // Model Paths
    const val GENDER_DETECTION_MODEL_PATH = "models/model_lite_gender_q.tflite"
    const val ENHANCED_NSFW_MODEL_PATH = "models/enhanced_nsfw.tflite"
    const val FAST_DETECTION_MODEL_PATH = "models/fast_detection.tflite"
    
    // Database
    const val SITE_BLOCKING_DB_NAME = "site_blocking.db"
    const val QURANIC_VERSES_DB_NAME = "quranic_verses.db"
    const val DB_VERSION = 1
    
    // Default Settings
    val DEFAULT_DETECTION_SETTINGS = mapOf(
        "enableGPUAcceleration" to true,
        "maxProcessingTimeMs" to FAST_PROCESSING_TIME_MS,
        "imageDownscaleRatio" to IMAGE_DOWNSCALE_FAST,
        "skipFrameThreshold" to 3,
        "confidenceThreshold" to 0.7f
    )
}