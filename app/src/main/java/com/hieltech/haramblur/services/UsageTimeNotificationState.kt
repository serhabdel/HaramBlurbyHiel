package com.hieltech.haramblur.services

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tracks notification state for usage time limit notifications
 */
data class UsageTimeNotificationState(
    val packageName: String,
    val firstNotificationTime: LocalDateTime? = null,
    val lastNotificationTime: LocalDateTime? = null,
    val notificationCount: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val limitExceededTime: LocalDateTime? = null,
    val lastVerseShown: String? = null // Track last verse to avoid repetition
) {
    /**
     * Check if this is the first notification for the app today
     */
    fun isFirstNotification(): Boolean {
        return firstNotificationTime == null || date.isBefore(LocalDate.now())
    }

    /**
     * Check if enough time has passed for the next notification (30 minutes)
     */
    fun shouldShowNextNotification(frequencyMinutes: Int = 30): Boolean {
        val lastTime = lastNotificationTime ?: return true
        val nextAllowedTime = lastTime.plusMinutes(frequencyMinutes.toLong())
        return LocalDateTime.now().isAfter(nextAllowedTime)
    }

    /**
     * Check if daily notification limit has been reached
     */
    fun hasReachedDailyLimit(maxNotifications: Int = 5): Boolean {
        return notificationCount >= maxNotifications
    }

    /**
     * Create updated state after showing notification
     */
    fun afterNotificationShown(verseId: String): UsageTimeNotificationState {
        val now = LocalDateTime.now()
        return copy(
            firstNotificationTime = firstNotificationTime ?: now,
            lastNotificationTime = now,
            notificationCount = notificationCount + 1,
            lastVerseShown = verseId,
            date = LocalDate.now()
        )
    }

    /**
     * Reset state for new day
     */
    fun resetForNewDay(): UsageTimeNotificationState {
        return copy(
            firstNotificationTime = null,
            lastNotificationTime = null,
            notificationCount = 0,
            date = LocalDate.now(),
            lastVerseShown = null
        )
    }

    /**
     * Get time until next notification is allowed
     */
    fun getTimeUntilNextNotification(frequencyMinutes: Int = 30): Long? {
        val lastTime = lastNotificationTime ?: return null
        val nextAllowedTime = lastTime.plusMinutes(frequencyMinutes.toLong())
        val now = LocalDateTime.now()

        return if (now.isBefore(nextAllowedTime)) {
            java.time.Duration.between(now, nextAllowedTime).toMinutes()
        } else {
            0L
        }
    }

    /**
     * Get time since limit was first exceeded
     */
    fun getTimeSinceLimitExceeded(): Long? {
        val exceededTime = limitExceededTime ?: return null
        return java.time.Duration.between(exceededTime, LocalDateTime.now()).toMinutes()
    }

    /**
     * Check if this is a repeated offense (multiple notifications in same day)
     */
    fun isRepeatedOffense(): Boolean {
        return notificationCount > 1
    }

    /**
     * Get notification severity level based on usage pattern
     */
    fun getSeverityLevel(): NotificationSeverity {
        return when {
            notificationCount >= 4 -> NotificationSeverity.CRITICAL
            notificationCount >= 2 -> NotificationSeverity.HIGH
            isRepeatedOffense() -> NotificationSeverity.MEDIUM
            else -> NotificationSeverity.LOW
        }
    }

    /**
     * Check if notification should be more insistent based on pattern
     */
    fun shouldBeMoreInsistent(): Boolean {
        return notificationCount > 2 || getTimeSinceLimitExceeded()?.let { it > 60 } == true
    }
}

/**
 * Notification severity levels for different usage patterns
 */
enum class NotificationSeverity {
    LOW,      // First offense, minimal disruption
    MEDIUM,   // Second notification, moderate guidance
    HIGH,     // Multiple notifications, strong guidance
    CRITICAL  // Maximum notifications, intensive guidance
}

/**
 * Notification action types for usage time notifications
 */
enum class UsageTimeNotificationAction {
    REFLECT_AND_CONTINUE,
    CLOSE_APP,
    OPEN_SETTINGS,
    DISMISS,
    SHOW_GUIDANCE
}

/**
 * Result of notification interaction
 */
data class UsageTimeNotificationResult(
    val action: UsageTimeNotificationAction,
    val packageName: String,
    val reflectionCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if action was positive (user engaged with guidance)
     */
    fun isPositiveAction(): Boolean {
        return when (action) {
            UsageTimeNotificationAction.REFLECT_AND_CONTINUE -> reflectionCompleted
            UsageTimeNotificationAction.CLOSE_APP -> true
            UsageTimeNotificationAction.SHOW_GUIDANCE -> true
            UsageTimeNotificationAction.OPEN_SETTINGS -> true
            UsageTimeNotificationAction.DISMISS -> false
        }
    }

    /**
     * Get action description for logging
     */
    fun getActionDescription(): String {
        return when (action) {
            UsageTimeNotificationAction.REFLECT_AND_CONTINUE ->
                if (reflectionCompleted) "Completed reflection and continued"
                else "Started reflection process"
            UsageTimeNotificationAction.CLOSE_APP -> "Chose to close app"
            UsageTimeNotificationAction.OPEN_SETTINGS -> "Opened settings to adjust limits"
            UsageTimeNotificationAction.DISMISS -> "Dismissed notification"
            UsageTimeNotificationAction.SHOW_GUIDANCE -> "Requested additional Islamic guidance"
        }
    }
}

/**
 * Usage notification preferences and settings
 */
data class UsageNotificationPreferences(
    val enableQuranicVerses: Boolean = true,
    val enableDuaIntegration: Boolean = true,
    val enableReflectionTimer: Boolean = true,
    val reflectionDurationSeconds: Int = 15,
    val maxNotificationsPerDay: Int = 5,
    val notificationFrequencyMinutes: Int = 30,
    val enableStatusNotifications: Boolean = true,
    val enableTimeSpecificGuidance: Boolean = true,
    val preferredNotificationStyle: NotificationStyle = NotificationStyle.DETAILED
) {
    /**
     * Check if preferences allow showing Quranic content
     */
    fun allowsQuranicContent(): Boolean {
        return enableQuranicVerses || enableDuaIntegration
    }

    /**
     * Get reflection duration in milliseconds
     */
    fun getReflectionDurationMillis(): Long {
        return reflectionDurationSeconds * 1000L
    }
}

/**
 * Notification style preferences
 */
enum class NotificationStyle {
    MINIMAL,    // Basic notification with essential info
    STANDARD,   // Standard notification with guidance
    DETAILED,   // Detailed notification with Quranic verses and actions
    COMPREHENSIVE // Full notification with all Islamic content
}

/**
 * Daily notification summary for analytics
 */
data class DailyNotificationSummary(
    val date: LocalDate = LocalDate.now(),
    val totalNotifications: Int = 0,
    val notificationsByApp: Map<String, Int> = emptyMap(),
    val positiveActions: Int = 0,
    val dismissals: Int = 0,
    val mostUsedApp: String? = null,
    val mostFrequentCategory: String? = null
) {
    /**
     * Calculate positive action rate
     */
    fun getPositiveActionRate(): Float {
        return if (totalNotifications > 0) {
            positiveActions.toFloat() / totalNotifications.toFloat()
        } else {
            0f
        }
    }

    /**
     * Get engagement level based on actions
     */
    fun getEngagementLevel(): EngagementLevel {
        val rate = getPositiveActionRate()
        return when {
            rate >= 0.8f -> EngagementLevel.HIGH
            rate >= 0.5f -> EngagementLevel.MEDIUM
            rate >= 0.2f -> EngagementLevel.LOW
            else -> EngagementLevel.VERY_LOW
        }
    }

    /**
     * Check if day was productive in terms of notifications
     */
    fun wasProductiveDay(): Boolean {
        return totalNotifications > 0 && getPositiveActionRate() > 0.3f
    }
}

/**
 * User engagement levels
 */
enum class EngagementLevel {
    VERY_LOW,  // < 20% positive actions
    LOW,       // 20-49% positive actions
    MEDIUM,    // 50-79% positive actions
    HIGH       // >= 80% positive actions
}
