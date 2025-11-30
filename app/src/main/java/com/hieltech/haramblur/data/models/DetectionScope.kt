package com.hieltech.haramblur.data.models

/**
 * Represents the scope of content detection - which apps should be monitored
 */
data class DetectionScope(
    val mode: DetectionMode = DetectionMode.SPECIFIC_CATEGORIES,
    val monitoredCategories: Set<AppCategory> = setOf(
        AppCategory.SOCIAL_MEDIA,    // Instagram, Facebook, TikTok, LinkedIn, Twitter, Snapchat
        AppCategory.BROWSERS,        // Chrome, Firefox, Edge, Opera, etc.
        AppCategory.MESSAGING,       // WhatsApp, Telegram, Discord, Messenger
        AppCategory.ENTERTAINMENT,   // YouTube, Netflix, Twitch
        AppCategory.DATING           // Tinder, Bumble, etc.
    ),
    val customIncludedApps: Set<String> = emptySet(),
    val excludedApps: Set<String> = emptySet()
) {
    /**
     * Check if a specific app package should be monitored
     */
    fun shouldMonitorApp(packageName: String): Boolean {
        // Always exclude if explicitly excluded
        if (excludedApps.contains(packageName)) {
            return false
        }

        return when (mode) {
            DetectionMode.ALL_APPS -> true
            DetectionMode.SPECIFIC_CATEGORIES -> {
                // Check if app is in any monitored category
                val isInMonitoredCategory = monitoredCategories.any { category ->
                    category.defaultApps.contains(packageName)
                }
                // Also include custom apps
                isInMonitoredCategory || customIncludedApps.contains(packageName)
            }
            DetectionMode.DISABLED -> false
        }
    }

    /**
     * Get all package names that should be monitored
     */
    fun getMonitoredPackageNames(): Set<String> {
        return when (mode) {
            DetectionMode.ALL_APPS -> emptySet() // Special case - monitor everything
            DetectionMode.SPECIFIC_CATEGORIES -> {
                val categoryApps = monitoredCategories.flatMap { it.defaultApps }.toSet()
                (categoryApps + customIncludedApps) - excludedApps
            }
            DetectionMode.DISABLED -> emptySet()
        }
    }

    /**
     * Check if detection is completely disabled
     */
    fun isDetectionDisabled(): Boolean {
        return mode == DetectionMode.DISABLED
    }

    /**
     * Check if monitoring all apps (no filtering)
     */
    fun isMonitoringAllApps(): Boolean {
        return mode == DetectionMode.ALL_APPS
    }
}

/**
 * Detection mode options
 */
enum class DetectionMode(val displayName: String, val description: String) {
    ALL_APPS(
        "Monitor All Apps",
        "Content detection runs on all installed apps"
    ),
    SPECIFIC_CATEGORIES(
        "Monitor Specific Categories",
        "Content detection runs only on selected app categories"
    ),
    DISABLED(
        "Detection Disabled",
        "Content detection is completely disabled"
    )
}
