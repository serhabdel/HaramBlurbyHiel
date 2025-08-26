package com.hieltech.haramblur.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

/**
 * Entity representing a log entry in the database
 */
@Entity(
    tableName = "logs",
    indices = [
        Index("timestamp"),
        Index("level"),
        Index("category"),
        Index("tag")
    ]
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Timestamp when the log was created (milliseconds since epoch)
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * Log tag (usually class name or component)
     */
    @ColumnInfo(name = "tag")
    val tag: String,

    /**
     * Log message
     */
    @ColumnInfo(name = "message")
    val message: String,

    /**
     * Log level: DEBUG, INFO, WARN, ERROR
     */
    @ColumnInfo(name = "level", defaultValue = "DEBUG")
    val level: String = "DEBUG",

    /**
     * Log category: GENERAL, DETECTION, BLOCKING, UI, NETWORK, etc.
     */
    @ColumnInfo(name = "category", defaultValue = "GENERAL")
    val category: String = "GENERAL",

    /**
     * Optional stack trace for error logs
     */
    @ColumnInfo(name = "stack_trace")
    val stackTrace: String? = null,

    /**
     * Optional user action that triggered this log
     */
    @ColumnInfo(name = "user_action")
    val userAction: String? = null,

    /**
     * Device information (model, Android version, etc.)
     */
    @ColumnInfo(name = "device_info")
    val deviceInfo: String? = null,

    /**
     * App version when log was created
     */
    @ColumnInfo(name = "app_version")
    val appVersion: String? = null,

    /**
     * Session ID for tracking related logs
     */
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,

    /**
     * When this log entry was created in the database
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    enum class LogCategory {
        GENERAL, DETECTION, BLOCKING, UI, NETWORK, DATABASE, ACCESSIBILITY, PERFORMANCE
    }

    companion object {
        fun create(
            tag: String,
            message: String,
            level: LogLevel = LogLevel.DEBUG,
            category: LogCategory = LogCategory.GENERAL,
            stackTrace: String? = null,
            userAction: String? = null,
            deviceInfo: String? = null,
            appVersion: String? = null,
            sessionId: String? = null
        ) = LogEntity(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level.name,
            category = category.name,
            stackTrace = stackTrace,
            userAction = userAction,
            deviceInfo = deviceInfo,
            appVersion = appVersion,
            sessionId = sessionId
        )
    }
}
