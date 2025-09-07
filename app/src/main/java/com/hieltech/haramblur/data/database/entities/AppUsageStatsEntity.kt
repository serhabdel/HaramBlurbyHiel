package com.hieltech.haramblur.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Room entity for storing daily app usage statistics
 */
@Entity(
    tableName = "app_usage_stats",
    indices = [
        Index(value = ["packageName", "date"], unique = true),
        Index(value = ["date"]),
        Index(value = ["packageName"])
    ]
)
data class AppUsageStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: Long, // LocalDate as epoch day
    val totalMinutes: Int = 0,
    val lastUsed: Long = System.currentTimeMillis(), // LocalDateTime as epoch millis
    val limitExceededTime: Long? = null, // LocalDateTime as epoch millis when limit was first exceeded
    val notificationCount: Int = 0,
    val lastNotificationTime: Long? = null, // LocalDateTime as epoch millis of last notification
    val appCategory: String? = null, // AppCategory name for faster queries
    val timeLimitMinutes: Int? = null, // Cached time limit for this app
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if the time limit has been exceeded
     */
    fun hasExceededLimit(): Boolean {
        return timeLimitMinutes?.let { limit -> totalMinutes >= limit } ?: false
    }

    /**
     * Get remaining time in minutes
     */
    fun getRemainingMinutes(): Int? {
        return timeLimitMinutes?.let { limit -> (limit - totalMinutes).coerceAtLeast(0) }
    }

    /**
     * Check if limit was exceeded today
     */
    fun wasLimitExceededToday(): Boolean {
        return limitExceededTime != null
    }

    /**
     * Get usage percentage (0.0 to 1.0+)
     */
    fun getUsagePercentage(): Float {
        return timeLimitMinutes?.let { limit ->
            if (limit > 0) totalMinutes.toFloat() / limit else 0f
        } ?: 0f
    }

    /**
     * Get formatted usage string (e.g., "45/60 min")
     */
    fun getUsageString(): String {
        return timeLimitMinutes?.let { limit ->
            "${totalMinutes}/${limit} min"
        } ?: "${totalMinutes} min"
    }

    /**
     * Check if notification should be shown based on time since last notification
     */
    fun shouldShowNotification(notificationFrequencyMinutes: Int): Boolean {
        if (!hasExceededLimit()) return false

        val lastNotification = lastNotificationTime ?: return true
        val timeSinceLastNotification = System.currentTimeMillis() - lastNotification
        val notificationFrequencyMillis = notificationFrequencyMinutes * 60 * 1000L

        return timeSinceLastNotification >= notificationFrequencyMillis
    }

    /**
     * Get LocalDate representation of the date field
     */
    fun getLocalDate(): LocalDate {
        return LocalDate.ofEpochDay(date)
    }

    /**
     * Get LocalDateTime representation of the lastUsed field
     */
    fun getLastUsedDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastUsed),
            ZoneId.systemDefault()
        )
    }

    /**
     * Get LocalDateTime representation of the limitExceededTime field
     */
    fun getLimitExceededDateTime(): LocalDateTime? {
        return limitExceededTime?.let { time ->
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(time),
                ZoneId.systemDefault()
            )
        }
    }

    /**
     * Create a copy with updated usage time
     */
    fun withUpdatedUsage(newTotalMinutes: Int): AppUsageStatsEntity {
        return copy(
            totalMinutes = newTotalMinutes,
            lastUsed = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Create a copy with limit exceeded marked
     */
    fun withLimitExceeded(): AppUsageStatsEntity {
        val currentTime = System.currentTimeMillis()
        return copy(
            limitExceededTime = limitExceededTime ?: currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Create a copy with notification recorded
     */
    fun withNotificationRecorded(): AppUsageStatsEntity {
        return copy(
            notificationCount = notificationCount + 1,
            lastNotificationTime = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Check if the usage stats are for today
     */
    fun isForToday(): Boolean {
        return date == LocalDate.now().toEpochDay()
    }

    /**
     * Get time until limit is reached in minutes
     */
    fun getMinutesUntilLimit(): Int? {
        return timeLimitMinutes?.let { limit ->
            (limit - totalMinutes).coerceAtLeast(0)
        }
    }

    /**
     * Check if app is approaching its limit (within 10 minutes)
     */
    fun isApproachingLimit(): Boolean {
        return getMinutesUntilLimit()?.let { minutesLeft ->
            minutesLeft <= 10 && minutesLeft > 0
        } ?: false
    }
}
