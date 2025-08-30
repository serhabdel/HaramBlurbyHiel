package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.Language
import org.json.JSONObject

/**
 * Data classes and utilities for preset management
 */

/**
 * Preset data structure for user-defined and built-in presets
 */
data class PresetData(
    val name: String,
    val description: String,
    val version: Int = 3,
    val settings: AppSettings,
    val metadata: PresetMetadata,
    val creationTimestamp: Long = System.currentTimeMillis()
)

/**
 * Metadata associated with a preset
 */
data class PresetMetadata(
    val category: PresetCategory,
    val difficulty: PresetDifficulty,
    val useCase: String,
    val author: String = "System",
    val tags: Set<String> = emptySet(),
    val isBuiltIn: Boolean = false
)

/**
 * Preset categories for organization
 */
enum class PresetCategory(val displayName: String, val description: String) {
    PROTECTION("Protection", "Maximum privacy and content blocking"),
    PERFORMANCE("Performance", "Optimized for speed and efficiency"),
    BALANCED("Balanced", "Good balance of protection and performance"),
    CUSTOM("Custom", "User-defined configurations")
}

/**
 * Preset difficulty levels
 */
enum class PresetDifficulty(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert")
}

/**
 * Template for built-in presets with configuration hints
 */
data class PresetTemplate(
    val name: String,
    val description: String,
    val category: PresetCategory,
    val difficulty: PresetDifficulty,
    val useCase: String,
    val icon: String,
    val colorHex: String
)

/**
 * Settings category enum for UI organization
 */
enum class SettingsCategory(val displayName: String, val description: String, val icon: String) {
    ESSENTIAL("Essential Settings", "Core functionality that everyone needs", "⚙️"),
    DETECTION("Detection & Privacy", "Face and content detection settings", "👁️"),
    PERFORMANCE("Performance & Advanced", "Speed and resource optimization", "⚡"),
    ISLAMIC("Islamic Guidance", "Quranic verses and spiritual guidance", "🕌"),
    AI("AI & Automation", "OpenRouter LLM and intelligent decisions", "🤖"),
    DEVELOPER("Developer & Logging", "Debugging and troubleshooting tools", "📊")
}

/**
 * Setting item for dynamic UI generation
 */
data class SettingItem(
    val key: String,
    val displayName: String,
    val description: String,
    val category: SettingsCategory,
    val valueType: SettingValueType,
    val defaultValue: Any? = null,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val options: List<Pair<String, String>>? = null
)

/**
 * Value type for settings
 */
enum class SettingValueType {
    BOOLEAN,
    FLOAT_SLIDER,
    INT_SLIDER,
    ENUM_SINGLE,
    ENUM_MULTI,
    TEXT_INPUT,
    COLOR_PICKER
}

/**
 * Result of preset import operation
 */
sealed class PresetImportResult {
    data class Success(val preset: PresetData) : PresetImportResult()
    data class Error(val message: String, val errorType: ImportErrorType) : PresetImportResult()
    data class PartialSuccess(val preset: PresetData, val warnings: List<String>) : PresetImportResult()
}

/**
 * Types of import errors
 */
enum class ImportErrorType {
    INVALID_JSON,
    MISSING_REQUIRED_FIELDS,
    VERSION_TOO_NEW,
    INVALID_SETTING_VALUES,
    COMPATIBILITY_ISSUE,
    PERMISSION_REQUIRED
}

/**
 * Validation result for preset data
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val compatibility: CompatibilityStatus = CompatibilityStatus.COMPATIBLE
)

/**
 * Compatibility status for presets
 */
enum class CompatibilityStatus {
    COMPATIBLE,
    NEEDS_UPDATE,
    INCOMPATIBLE
}

/**
 * Merge strategy for importing presets
 */
enum class MergeStrategy {
    REPLACE_ALL,
    MERGE_COMPATIBLE,
    USER_CHOICE
}

/**
 * Settings difference for showing changes during import
 */
data class SettingsDiff(
    val added: Map<String, Any> = emptyMap(),
    val modified: Map<String, Pair<Any, Any>> = emptyMap(),
    val removed: Set<String> = emptySet()
)

/**
 * Preset manager object with utility methods
 */
object PresetManager {

    /**
     * Get all available built-in preset templates
     */
    fun getPresetTemplates(): List<PresetTemplate> = listOf(
        PresetTemplate(
            name = "Maximum Protection",
            description = "Ultimate privacy with maximum content blocking and detection sensitivity",
            category = PresetCategory.PROTECTION,
            difficulty = PresetDifficulty.ADVANCED,
            useCase = "For users who prioritize maximum privacy and protection",
            icon = "🛡️",
            colorHex = "#D32F2F"
        ),
        PresetTemplate(
            name = "Optimal Performance",
            description = "Balanced settings optimized for speed and battery life",
            category = PresetCategory.PERFORMANCE,
            difficulty = PresetDifficulty.INTERMEDIATE,
            useCase = "For users who want good protection without sacrificing performance",
            icon = "⚡",
            colorHex = "#2E7D32"
        ),
        PresetTemplate(
            name = "Battery Saver",
            description = "Minimum resource usage while maintaining essential protection",
            category = PresetCategory.PERFORMANCE,
            difficulty = PresetDifficulty.BEGINNER,
            useCase = "For users with limited battery or processing power",
            icon = "🔋",
            colorHex = "#F57C00"
        ),
        PresetTemplate(
            name = "Gaming Mode",
            description = "Reduced detection frequency for gaming performance",
            category = PresetCategory.BALANCED,
            difficulty = PresetDifficulty.INTERMEDIATE,
            useCase = "For gamers who want protection without performance impact",
            icon = "🎮",
            colorHex = "#1976D2"
        ),
        PresetTemplate(
            name = "Islamic Focus",
            description = "Enhanced Islamic guidance with comprehensive Quranic support",
            category = PresetCategory.BALANCED,
            difficulty = PresetDifficulty.INTERMEDIATE,
            useCase = "For users seeking stronger spiritual guidance integration",
            icon = "🕌",
            colorHex = "#6A1B9A"
        )
    )

    /**
     * Create Maximum Protection preset
     */
    fun createMaximumProtectionPreset(): PresetData {
        val settings = AppSettings(
            // Maximum detection settings
            enableFaceDetection = true,
            enableNSFWDetection = true,
            blurFemaleFaces = true,
            blurMaleFaces = true,
            detectionSensitivity = 0.9f,
            blurIntensity = BlurIntensity.MAXIMUM,
            blurStyle = BlurStyle.COMBINED,
            expandBlurArea = 80,

            // Performance optimized for accuracy
            processingSpeed = ProcessingSpeed.BALANCED,
            enableRealTimeProcessing = true,
            enableFullScreenBlurForNSFW = true,
            enableGPUAcceleration = true,
            maxProcessingTimeMs = 100L,
            frameSkipThreshold = 1,
            imageDownscaleRatio = 0.7f,

            // Strict confidence thresholds
            genderConfidenceThreshold = 0.3f,
            nsfwConfidenceThreshold = 0.4f,

            // Enhanced Islamic guidance
            enableQuranicGuidance = true,
            preferredLanguage = Language.ENGLISH,
            verseDisplayDuration = 20,
            enableArabicText = true,
            customReflectionTime = 30,
            mandatoryReflectionTime = 20,

            // Comprehensive logging
            enableDetailedLogging = true,
            logLevel = LogLevel.DEBUG,
            enableLogCategories = setOf(
                LogCategory.DETECTION,
                LogCategory.BLOCKING,
                LogCategory.UI,
                LogCategory.ACCESSIBILITY,
                LogCategory.PERFORMANCE,
                LogCategory.NETWORK,
                LogCategory.DATABASE
            ),
            enablePerformanceLogging = true,
            enableErrorReporting = true,
            enableUserActionLogging = true,

            // Advanced features
            enableFallbackDetection = true,
            enablePerformanceMonitoring = true,
            ultraFastModeEnabled = false,
            fullScreenWarningEnabled = true
        )

        return PresetData(
            name = "Maximum Protection",
            description = "Ultimate privacy with maximum content blocking and detection sensitivity",
            settings = settings,
            metadata = PresetMetadata(
                category = PresetCategory.PROTECTION,
                difficulty = PresetDifficulty.ADVANCED,
                useCase = "For users who prioritize maximum privacy and protection",
                isBuiltIn = true
            )
        )
    }

    /**
     * Create Optimal Performance preset
     */
    fun createOptimalPerformancePreset(): PresetData {
        val settings = AppSettings(
            // Balanced detection settings
            enableFaceDetection = true,
            enableNSFWDetection = true,
            blurFemaleFaces = true,
            blurMaleFaces = false,
            detectionSensitivity = 0.7f,
            blurIntensity = BlurIntensity.STRONG,
            blurStyle = BlurStyle.ARTISTIC,
            expandBlurArea = 50,

            // Performance optimized
            processingSpeed = ProcessingSpeed.BALANCED,
            enableRealTimeProcessing = true,
            enableFullScreenBlurForNSFW = true,
            enableGPUAcceleration = true,
            maxProcessingTimeMs = 75L,
            frameSkipThreshold = 2,
            imageDownscaleRatio = 0.6f,

            // Balanced confidence thresholds
            genderConfidenceThreshold = 0.4f,
            nsfwConfidenceThreshold = 0.5f,

            // Standard Islamic guidance
            enableQuranicGuidance = true,
            preferredLanguage = Language.ENGLISH,
            verseDisplayDuration = 15,
            enableArabicText = true,
            customReflectionTime = 20,
            mandatoryReflectionTime = 15,

            // Essential logging only
            enableDetailedLogging = true,
            logLevel = LogLevel.INFO,
            enableLogCategories = setOf(
                LogCategory.DETECTION,
                LogCategory.BLOCKING,
                LogCategory.PERFORMANCE,
                LogCategory.UI
            ),
            enablePerformanceLogging = true,
            enableErrorReporting = true,
            enableUserActionLogging = false,

            // Balanced advanced features
            enableFallbackDetection = true,
            enablePerformanceMonitoring = true,
            ultraFastModeEnabled = false,
            fullScreenWarningEnabled = true
        )

        return PresetData(
            name = "Optimal Performance",
            description = "Balanced settings optimized for speed and battery life",
            settings = settings,
            metadata = PresetMetadata(
                category = PresetCategory.PERFORMANCE,
                difficulty = PresetDifficulty.INTERMEDIATE,
                useCase = "For users who want good protection without sacrificing performance",
                isBuiltIn = true
            )
        )
    }

    /**
     * Validate preset data structure and values
     */
    fun validatePresetData(presetJson: String): ValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Basic JSON validation
            if (presetJson.isBlank()) {
                errors.add("Preset data is empty")
                return ValidationResult(false, errors)
            }

            // TODO: Add comprehensive validation logic
            // For now, return valid result
            ValidationResult(true, errors, warnings)
        } catch (e: Exception) {
            ValidationResult(false, listOf("Invalid JSON format: ${e.message}"))
        }
    }

    /**
     * Calculate settings difference between current and imported settings
     */
    fun calculateSettingsDiff(current: AppSettings, imported: AppSettings): SettingsDiff {
        val added = mutableMapOf<String, Any>()
        val modified = mutableMapOf<String, Pair<Any, Any>>()
        val removed = mutableSetOf<String>()

        // Compare all relevant properties
        if (current.enableFaceDetection != imported.enableFaceDetection) {
            modified["enableFaceDetection"] = Pair(current.enableFaceDetection, imported.enableFaceDetection)
        }

        if (current.enableNSFWDetection != imported.enableNSFWDetection) {
            modified["enableNSFWDetection"] = Pair(current.enableNSFWDetection, imported.enableNSFWDetection)
        }

        if (current.detectionSensitivity != imported.detectionSensitivity) {
            modified["detectionSensitivity"] = Pair(current.detectionSensitivity, imported.detectionSensitivity)
        }

        if (current.blurIntensity != imported.blurIntensity) {
            modified["blurIntensity"] = Pair(current.blurIntensity, imported.blurIntensity)
        }

        // Add more comparisons as needed...

        return SettingsDiff(added, modified, removed)
    }

    /**
     * Get settings organized by category
     */
    fun getSettingsCategories(): Map<SettingsCategory, List<SettingItem>> {
        return mapOf(
            SettingsCategory.ESSENTIAL to listOf(
                SettingItem(
                    key = "enableFaceDetection",
                    displayName = "Face Detection",
                    description = "Detect and blur faces in images",
                    category = SettingsCategory.ESSENTIAL,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "enableNSFWDetection",
                    displayName = "NSFW Content Detection",
                    description = "Detect and blur inappropriate content",
                    category = SettingsCategory.ESSENTIAL,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "blurIntensity",
                    displayName = "Blur Intensity",
                    description = "How strong the blur effect should be",
                    category = SettingsCategory.ESSENTIAL,
                    valueType = SettingValueType.ENUM_SINGLE,
                    options = BlurIntensity.values().map { it.name to it.description }
                )
            ),

            SettingsCategory.DETECTION to listOf(
                SettingItem(
                    key = "detectionSensitivity",
                    displayName = "Detection Sensitivity",
                    description = "Higher values detect more content but may blur normal images",
                    category = SettingsCategory.DETECTION,
                    valueType = SettingValueType.FLOAT_SLIDER,
                    minValue = 0.3f,
                    maxValue = 0.9f
                ),
                SettingItem(
                    key = "genderConfidenceThreshold",
                    displayName = "Gender Confidence Threshold",
                    description = "Lower values detect more faces but may have false positives",
                    category = SettingsCategory.DETECTION,
                    valueType = SettingValueType.FLOAT_SLIDER,
                    minValue = 0.3f,
                    maxValue = 0.8f
                )
            ),

            SettingsCategory.PERFORMANCE to listOf(
                SettingItem(
                    key = "enableGPUAcceleration",
                    displayName = "GPU Acceleration",
                    description = "Use GPU for faster detection (recommended)",
                    category = SettingsCategory.PERFORMANCE,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "processingSpeed",
                    displayName = "Processing Speed",
                    description = "Balance between speed and accuracy",
                    category = SettingsCategory.PERFORMANCE,
                    valueType = SettingValueType.ENUM_SINGLE,
                    options = ProcessingSpeed.values().map { it.name to it.description }
                )
            ),

            SettingsCategory.ISLAMIC to listOf(
                SettingItem(
                    key = "enableQuranicGuidance",
                    displayName = "Quranic Guidance",
                    description = "Show Quranic verses when blocking content",
                    category = SettingsCategory.ISLAMIC,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "preferredLanguage",
                    displayName = "Preferred Language",
                    description = "Language for Islamic guidance and verses",
                    category = SettingsCategory.ISLAMIC,
                    valueType = SettingValueType.ENUM_SINGLE,
                    options = Language.values().map { it.name to "Language for Islamic guidance" }
                )
            ),

            SettingsCategory.AI to listOf(
                SettingItem(
                    key = "enableLLMDecisionMaking",
                    displayName = "LLM Decision Making",
                    description = "Use AI for faster, smarter content actions",
                    category = SettingsCategory.AI,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "llmTimeoutMs",
                    displayName = "AI Response Timeout",
                    description = "Maximum time to wait for AI response",
                    category = SettingsCategory.AI,
                    valueType = SettingValueType.INT_SLIDER,
                    minValue = 1000,
                    maxValue = 10000
                )
            ),

            SettingsCategory.DEVELOPER to listOf(
                SettingItem(
                    key = "enableDetailedLogging",
                    displayName = "Detailed Logging",
                    description = "Enable detailed logging for troubleshooting",
                    category = SettingsCategory.DEVELOPER,
                    valueType = SettingValueType.BOOLEAN
                ),
                SettingItem(
                    key = "logLevel",
                    displayName = "Log Level",
                    description = "Minimum log level to record",
                    category = SettingsCategory.DEVELOPER,
                    valueType = SettingValueType.ENUM_SINGLE,
                    options = LogLevel.values().map { it.name to it.description }
                )
            )
        )
    }

    /**
     * Search settings by query
     */
    fun searchSettings(query: String): List<SettingItem> {
        if (query.isBlank()) return emptyList()

        val allSettings = getSettingsCategories().values.flatten()
        return allSettings.filter { setting ->
            setting.displayName.contains(query, ignoreCase = true) ||
            setting.description.contains(query, ignoreCase = true) ||
            setting.category.displayName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Export preset data to JSON string
     */
    fun exportPresetToJson(preset: PresetData): String {
        return JSONObject().apply {
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
        }.toString(2)
    }
}
