package com.hieltech.haramblur.data.models

/**
 * Represents a quick setting that can be toggled or adjusted
 */
data class QuickSetting(
    val id: String,
    val displayName: String, // Display name as string
    val currentValue: Any, // Can be Boolean, Float, Int, etc.
    val settingType: SettingType,
    val iconRes: Int,
    val statusIndicator: StatusIndicator,
    val description: String
) {
    /**
     * Check if this setting can be toggled (for boolean settings)
     */
    fun canToggle(): Boolean {
        return settingType == SettingType.TOGGLE && currentValue is Boolean
    }

    /**
     * Check if this setting can be adjusted (for slider settings)
     */
    fun canAdjust(): Boolean {
        return settingType == SettingType.SLIDER && currentValue is Number
    }

    /**
     * Get the current value as a boolean (safe cast)
     */
    fun getBooleanValue(): Boolean {
        return currentValue as? Boolean ?: false
    }

    /**
     * Get the current value as a float (safe cast)
     */
    fun getFloatValue(): Float {
        return when (currentValue) {
            is Float -> currentValue
            is Double -> currentValue.toFloat()
            is Int -> currentValue.toFloat()
            else -> 0.0f
        }
    }
}
