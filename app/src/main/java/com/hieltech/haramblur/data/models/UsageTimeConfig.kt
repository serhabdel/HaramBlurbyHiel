package com.hieltech.haramblur.data.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Configuration for app usage time tracking and notifications
 */
data class UsageTimeConfig(
    val enabled: Boolean = true,
    val defaultLimits: Map<AppCategory, Int> = mapOf(
        AppCategory.SOCIAL_MEDIA to 60, // 1 hour
        AppCategory.ENTERTAINMENT to 60, // 1 hour
        AppCategory.MESSAGING to 120, // 2 hours
        AppCategory.DATING to 30, // 30 minutes
        AppCategory.BROWSERS to 180 // 3 hours (more lenient for productivity)
    ),
    val customAppLimits: Map<String, Int> = emptyMap(), // Package name to minutes
    val notificationFrequencyMinutes: Int = 30,
    val enableDailyReset: Boolean = true
) {
    /**
     * Get the time limit for a specific app in minutes
     */
    fun getTimeLimitForApp(packageName: String, appCategory: AppCategory?): Int? {
        if (!enabled) return null

        // Check custom limits first
        customAppLimits[packageName]?.let { return it }

        // Fall back to category default
        return appCategory?.let { defaultLimits[it] }
    }

    /**
     * Check if an app has a time limit configured
     */
    fun hasTimeLimitForApp(packageName: String, appCategory: AppCategory?): Boolean {
        return getTimeLimitForApp(packageName, appCategory) != null
    }

    /**
     * Get all apps that have time limits configured
     */
    fun getAppsWithTimeLimits(): Set<String> {
        if (!enabled) return emptySet()

        val categoryApps = defaultLimits.keys.flatMap { category ->
            category.defaultApps
        }.toSet()

        return categoryApps + customAppLimits.keys
    }

    /**
     * Check if usage should reset (daily at midnight)
     */
    fun shouldResetUsage(lastResetDate: LocalDate): Boolean {
        return enableDailyReset && lastResetDate.isBefore(LocalDate.now())
    }

    /**
     * Get the next notification time after limit exceeded
     */
    fun getNextNotificationTime(limitExceededTime: LocalDateTime): LocalDateTime {
        return limitExceededTime.plusMinutes(notificationFrequencyMinutes.toLong())
    }
}

/**
 * Represents daily usage statistics for an app
 */
data class AppUsageStats(
    val packageName: String,
    val date: LocalDate,
    val totalMinutes: Int,
    val lastUsed: LocalDateTime,
    val limitExceededTime: LocalDateTime? = null,
    val notificationCount: Int = 0
) {
    /**
     * Check if the time limit has been exceeded
     */
    fun hasExceededLimit(timeLimit: Int): Boolean {
        return totalMinutes >= timeLimit
    }

    /**
     * Check if it's time for another notification
     */
    fun shouldShowNotification(config: UsageTimeConfig, currentTime: LocalDateTime): Boolean {
        val exceededTime = limitExceededTime ?: return false
        val nextNotificationTime = config.getNextNotificationTime(exceededTime)
        return currentTime.isAfter(nextNotificationTime)
    }
}
