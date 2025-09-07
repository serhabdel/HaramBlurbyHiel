package com.hieltech.haramblur.data.models

/**
 * Enum representing different categories of health issues
 */
enum class IssueCategory {
    SERVICE,        // Related to app services (accessibility, etc.)
    PERMISSION,     // Related to app permissions
    PERFORMANCE,    // Related to app performance
    BATTERY,        // Related to battery optimization
    STORAGE,        // Related to storage space
    NETWORK,        // Related to network connectivity
    OTHER           // Other miscellaneous issues
}
