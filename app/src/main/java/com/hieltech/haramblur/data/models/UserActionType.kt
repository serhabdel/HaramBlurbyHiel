package com.hieltech.haramblur.data.models

/**
 * Enum representing different types of user actions on settings
 */
enum class UserActionType {
    SETTING_CHANGE,     // General setting value change
    TOGGLE,            // Boolean toggle action
    RESET,             // Reset to default values
    EXPORT,            // Export settings/data
    IMPORT             // Import settings/data
}
