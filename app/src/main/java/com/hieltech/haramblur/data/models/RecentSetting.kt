package com.hieltech.haramblur.data.models

import java.time.LocalDateTime

/**
 * Represents a recently changed setting
 */
data class RecentSetting(
    val id: String,
    val settingName: String,
    val category: String,
    val previousValue: String,
    val newValue: String,
    val changeTime: LocalDateTime = LocalDateTime.now(),
    val userActionType: UserActionType = UserActionType.SETTING_CHANGE
) {
    /**
     * Get a human-readable change description
     */
    fun getChangeDescription(): String {
        return when (userActionType) {
            UserActionType.SETTING_CHANGE -> "$settingName changed from $previousValue to $newValue"
            UserActionType.RESET -> "$settingName reset to default"
            UserActionType.TOGGLE -> "$settingName ${if (newValue == "true") "enabled" else "disabled"}"
            UserActionType.EXPORT -> "$settingName exported"
            UserActionType.IMPORT -> "$settingName imported"
        }
    }

    /**
     * Check if this change was recent (within last hour)
     */
    fun isRecent(): Boolean {
        return changeTime.isAfter(LocalDateTime.now().minusHours(1))
    }
}
