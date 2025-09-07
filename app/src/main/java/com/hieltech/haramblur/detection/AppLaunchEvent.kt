package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.models.AppCategory
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Represents an app launch event with context for usage tracking
 */
data class AppLaunchEvent(
    val packageName: String,
    val eventTime: Long,
    val eventDateTime: LocalDateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(eventTime),
        ZoneId.systemDefault()
    ),
    val appCategory: AppCategory? = null,
    val isBlocked: Boolean = false,
    val isMonitored: Boolean = false,
    val hasTimeLimit: Boolean = false,
    val currentUsageMinutes: Int = 0,
    val timeLimitMinutes: Int? = null,
    val isLimitExceeded: Boolean = false
) {
    /**
     * Check if this app launch should trigger usage tracking
     */
    fun shouldTrackUsage(): Boolean {
        return isMonitored && hasTimeLimit
    }

    /**
     * Check if this app launch should trigger a time limit notification
     */
    fun shouldShowTimeNotification(): Boolean {
        return isLimitExceeded && hasTimeLimit
    }

    /**
     * Get usage percentage (0.0 to 1.0+)
     */
    fun getUsagePercentage(): Float {
        return timeLimitMinutes?.let { limit ->
            if (limit > 0) currentUsageMinutes.toFloat() / limit else 0f
        } ?: 0f
    }

    /**
     * Get remaining time in minutes
     */
    fun getRemainingMinutes(): Int? {
        return timeLimitMinutes?.let { limit ->
            (limit - currentUsageMinutes).coerceAtLeast(0)
        }
    }

    /**
     * Get overage time in minutes (positive if over limit)
     */
    fun getOverageMinutes(): Int {
        return timeLimitMinutes?.let { limit ->
            (currentUsageMinutes - limit).coerceAtLeast(0)
        } ?: 0
    }

    /**
     * Get display name for the app
     */
    fun getDisplayName(): String {
        return when {
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("facebook") -> "Facebook"
            packageName.contains("tiktok") || packageName.contains("musically") -> "TikTok"
            packageName.contains("twitter") -> "Twitter"
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("chrome") -> "Chrome"
            packageName.contains("firefox") -> "Firefox"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("netflix") -> "Netflix"
            packageName.contains("tinder") -> "Tinder"
            packageName.contains("bumble") -> "Bumble"
            appCategory != null -> appCategory.displayName
            else -> packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }
    }

    /**
     * Get priority level for notifications (higher = more urgent)
     */
    fun getNotificationPriority(): Int {
        return when {
            isLimitExceeded && getOverageMinutes() > 60 -> 3 // High priority for 1+ hour overage
            isLimitExceeded && getOverageMinutes() > 30 -> 2 // Medium priority for 30+ min overage
            isLimitExceeded -> 1 // Low priority for just exceeded
            getUsagePercentage() > 0.8f -> 1 // Warning for approaching limit
            else -> 0 // No notification needed
        }
    }

    /**
     * Check if this is a high-risk app category
     */
    fun isHighRiskCategory(): Boolean {
        return appCategory in setOf(
            AppCategory.SOCIAL_MEDIA,
            AppCategory.DATING,
            AppCategory.ENTERTAINMENT
        )
    }

    /**
     * Get contextual message for the app launch
     */
    fun getContextualMessage(): String {
        return when {
            isLimitExceeded -> "Time limit exceeded by ${getOverageMinutes()} minutes"
            getUsagePercentage() > 0.8f -> "Approaching time limit (${getRemainingMinutes()}m remaining)"
            hasTimeLimit -> "${getRemainingMinutes()}m remaining today"
            isMonitored -> "App is being monitored"
            isBlocked -> "App is blocked"
            else -> "App launched"
        }
    }
}

/**
 * Builder class for creating AppLaunchEvent instances
 */
class AppLaunchEventBuilder {
    private var packageName: String = ""
    private var eventTime: Long = System.currentTimeMillis()
    private var appCategory: AppCategory? = null
    private var isBlocked: Boolean = false
    private var isMonitored: Boolean = false
    private var hasTimeLimit: Boolean = false
    private var currentUsageMinutes: Int = 0
    private var timeLimitMinutes: Int? = null
    private var isLimitExceeded: Boolean = false

    fun packageName(packageName: String) = apply { this.packageName = packageName }
    fun eventTime(eventTime: Long) = apply { this.eventTime = eventTime }
    fun appCategory(appCategory: AppCategory?) = apply { this.appCategory = appCategory }
    fun isBlocked(isBlocked: Boolean) = apply { this.isBlocked = isBlocked }
    fun isMonitored(isMonitored: Boolean) = apply { this.isMonitored = isMonitored }
    fun hasTimeLimit(hasTimeLimit: Boolean) = apply { this.hasTimeLimit = hasTimeLimit }
    fun currentUsageMinutes(currentUsageMinutes: Int) = apply { this.currentUsageMinutes = currentUsageMinutes }
    fun timeLimitMinutes(timeLimitMinutes: Int?) = apply { this.timeLimitMinutes = timeLimitMinutes }
    fun isLimitExceeded(isLimitExceeded: Boolean) = apply { this.isLimitExceeded = isLimitExceeded }

    fun build(): AppLaunchEvent {
        return AppLaunchEvent(
            packageName = packageName,
            eventTime = eventTime,
            appCategory = appCategory,
            isBlocked = isBlocked,
            isMonitored = isMonitored,
            hasTimeLimit = hasTimeLimit,
            currentUsageMinutes = currentUsageMinutes,
            timeLimitMinutes = timeLimitMinutes,
            isLimitExceeded = isLimitExceeded
        )
    }
}
