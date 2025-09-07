package com.hieltech.haramblur.data.models

/**
 * Enum representing status indicators for settings
 */
enum class StatusIndicator {
    ENABLED,        // Setting is enabled/active
    DISABLED,       // Setting is disabled/inactive
    WARNING,        // Setting has warnings
    ERROR,          // Setting has errors
    PENDING         // Setting change is pending
}
