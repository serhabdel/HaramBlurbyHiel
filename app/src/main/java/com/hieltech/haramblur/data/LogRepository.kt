package com.hieltech.haramblur.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.hieltech.haramblur.data.database.LogDao
import com.hieltech.haramblur.data.database.LogEntity
import com.hieltech.haramblur.data.database.LogStatistics
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing application logs
 */
@Singleton
class LogRepository @Inject constructor(
    private val context: Context,
    private val database: SiteBlockingDatabase
) {
    private val logDao: LogDao = database.logDao()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val exportDateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    // Device and app info for logs
    private val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    private val appVersion = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${packageInfo.versionName} (${packageInfo.versionCode})"
    } catch (e: Exception) {
        "Unknown"
    }

    /**
     * Log levels for filtering
     */
    enum class LogLevel(val priority: Int) {
        DEBUG(0), INFO(1), WARN(2), ERROR(3)
    }

    /**
     * Log categories for organization
     */
    enum class LogCategory {
        GENERAL, DETECTION, BLOCKING, UI, NETWORK, DATABASE, ACCESSIBILITY, PERFORMANCE
    }

    /**
     * Add a log entry
     */
    suspend fun log(
        tag: String,
        message: String,
        level: LogLevel = LogLevel.DEBUG,
        category: LogCategory = LogCategory.GENERAL,
        stackTrace: String? = null,
        userAction: String? = null,
        sessionId: String? = null
    ) {
        try {
            val logEntry = LogEntity.create(
                tag = tag,
                message = message,
                level = LogEntity.LogLevel.valueOf(level.name),
                category = LogEntity.LogCategory.valueOf(category.name),
                stackTrace = stackTrace,
                userAction = userAction,
                deviceInfo = deviceInfo,
                appVersion = appVersion,
                sessionId = sessionId
            )

            logDao.insertLog(logEntry)

            // Also log to Android Log
            when (level) {
                LogLevel.ERROR -> Log.e(tag, message)
                LogLevel.WARN -> Log.w(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.DEBUG -> Log.d(tag, message)
            }

            // Clean up old logs (keep last 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            logDao.cleanupOldLogs(sevenDaysAgo)

        } catch (e: Exception) {
            // Fallback to Android logging if database fails
            Log.e("LogRepository", "Failed to save log to database", e)
            Log.e(tag, message)
        }
    }

    /**
     * Log an error with stack trace
     */
    suspend fun logError(
        tag: String,
        message: String,
        exception: Exception? = null,
        category: LogCategory = LogCategory.GENERAL,
        userAction: String? = null
    ) {
        val stackTrace = exception?.stackTraceToString()
        log(tag, message, LogLevel.ERROR, category, stackTrace, userAction)
    }

    /**
     * Log a warning
     */
    suspend fun logWarning(
        tag: String,
        message: String,
        category: LogCategory = LogCategory.GENERAL,
        userAction: String? = null
    ) {
        log(tag, message, LogLevel.WARN, category, userAction = userAction)
    }

    /**
     * Log an info message
     */
    suspend fun logInfo(
        tag: String,
        message: String,
        category: LogCategory = LogCategory.GENERAL,
        userAction: String? = null
    ) {
        log(tag, message, LogLevel.INFO, category, userAction = userAction)
    }

    /**
     * Log a debug message
     */
    suspend fun logDebug(
        tag: String,
        message: String,
        category: LogCategory = LogCategory.GENERAL,
        userAction: String? = null
    ) {
        log(tag, message, LogLevel.DEBUG, category, userAction = userAction)
    }

    /**
     * Get all logs
     */
    fun getAllLogs(): Flow<List<LogEntity>> = logDao.getAllLogs()

    /**
     * Get logs by level
     */
    fun getLogsByLevel(level: String): Flow<List<LogEntity>> = logDao.getLogsByLevel(level)

    /**
     * Get logs by category
     */
    fun getLogsByCategory(category: String): Flow<List<LogEntity>> = logDao.getLogsByCategory(category)

    /**
     * Get logs by tag
     */
    fun getLogsByTag(tag: String): Flow<List<LogEntity>> = logDao.getLogsByTag(tag)

    /**
     * Get recent logs (last N entries)
     */
    fun getRecentLogs(limit: Int = 100): Flow<List<LogEntity>> = logDao.getRecentLogs(limit)

    /**
     * Get logs with specific levels
     */
    fun getLogsWithLevels(levels: List<String>, limit: Int = 100): Flow<List<LogEntity>> =
        logDao.getLogsWithLevels(limit, levels)

    /**
     * Get logs since timestamp
     */
    fun getLogsSince(sinceTimestamp: Long): Flow<List<LogEntity>> = logDao.getLogsSince(sinceTimestamp)

    /**
     * Search logs by content
     */
    fun searchLogs(searchTerm: String): Flow<List<LogEntity>> = logDao.searchLogs(searchTerm)

    /**
     * Get log statistics
     */
    fun getLogStatistics(): Flow<LogStatistics> = logDao.getLogStatistics()

    /**
     * Get distinct tags
     */
    fun getDistinctTags(): Flow<List<String>> = logDao.getDistinctTags()

    /**
     * Get distinct categories
     */
    fun getDistinctCategories(): Flow<List<String>> = logDao.getDistinctCategories()

    /**
     * Export logs to file
     */
    suspend fun exportLogsToFile(
        levels: List<String> = listOf("DEBUG", "INFO", "WARN", "ERROR"),
        sinceTimestamp: Long = 0
    ): File? {
        return try {
            val logs = logDao.getLogsForExport(sinceTimestamp)

            val exportDir = File(context.getExternalFilesDir(null), "logs")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val fileName = "haramblur_logs_${exportDateFormatter.format(Date())}.txt"
            val exportFile = File(exportDir, fileName)

            FileWriter(exportFile).use { writer ->
                writer.write("HaramBlur Log Export\n")
                writer.write("Generated: ${dateFormatter.format(Date())}\n")
                writer.write("Device: $deviceInfo\n")
                writer.write("App Version: $appVersion\n")
                writer.write("Export Period: ${if (sinceTimestamp > 0) "Since ${dateFormatter.format(Date(sinceTimestamp))}" else "All logs"}\n")
                writer.write("Levels: ${levels.joinToString(", ")}\n")
                writer.write("=" * 80 + "\n\n")

                logs.collect { logEntries ->
                    logEntries.forEach { log ->
                        writer.write("[${log.formatted_timestamp}] ${log.level}/${log.tag}: ${log.message}\n")
                        if (!log.category.isNullOrBlank() && log.category != "GENERAL") {
                            writer.write("Category: ${log.category}\n")
                        }
                        if (!log.user_action.isNullOrBlank()) {
                            writer.write("User Action: ${log.user_action}\n")
                        }
                        if (!log.stack_trace.isNullOrBlank()) {
                            writer.write("Stack Trace:\n${log.stack_trace}\n")
                        }
                        writer.write("\n")
                    }
                }
            }

            logInfo("LogRepository", "Logs exported to ${exportFile.absolutePath}")
            exportFile

        } catch (e: Exception) {
            logError("LogRepository", "Failed to export logs", e)
            null
        }
    }

    /**
     * Export logs as formatted text string
     */
    suspend fun exportLogsAsText(
        levels: List<String> = listOf("DEBUG", "INFO", "WARN", "ERROR"),
        sinceTimestamp: Long = 0
    ): String {
        return try {
            val logs = logDao.getLogsForExport(sinceTimestamp)
            val stringBuilder = StringBuilder()

            stringBuilder.append("HaramBlur Log Export\n")
            stringBuilder.append("Generated: ${dateFormatter.format(Date())}\n")
            stringBuilder.append("Device: $deviceInfo\n")
            stringBuilder.append("App Version: $appVersion\n")
            stringBuilder.append("Export Period: ${if (sinceTimestamp > 0) "Since ${dateFormatter.format(Date(sinceTimestamp))}" else "All logs"}\n")
            stringBuilder.append("Levels: ${levels.joinToString(", ")}\n")
            stringBuilder.append("=" * 80 + "\n\n")

            logs.collect { logEntries ->
                logEntries.forEach { log ->
                    stringBuilder.append("[${log.formatted_timestamp}] ${log.level}/${log.tag}: ${log.message}\n")
                    if (!log.category.isNullOrBlank() && log.category != "GENERAL") {
                        stringBuilder.append("Category: ${log.category}\n")
                    }
                    if (!log.user_action.isNullOrBlank()) {
                        stringBuilder.append("User Action: ${log.user_action}\n")
                    }
                    if (!log.stack_trace.isNullOrBlank()) {
                        stringBuilder.append("Stack Trace:\n${log.stack_trace}\n")
                    }
                    stringBuilder.append("\n")
                }
            }

            stringBuilder.toString()

        } catch (e: Exception) {
            logError("LogRepository", "Failed to export logs as text", e)
            "Error exporting logs: ${e.message}"
        }
    }

    /**
     * Clear all logs
     */
    suspend fun clearAllLogs() {
        logInfo("LogRepository", "Clearing all logs")
        logDao.deleteAllLogs()
    }

    /**
     * Clear logs older than specified days
     */
    suspend fun clearOldLogs(daysToKeep: Int = 7) {
        val cutoffTimestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        val deletedCount = logDao.cleanupOldLogs(cutoffTimestamp)
        logInfo("LogRepository", "Cleared $deletedCount old log entries (older than $daysToKeep days)")
    }

    /**
     * Clear logs by level
     */
    suspend fun clearLogsByLevel(level: String) {
        val deletedCount = logDao.deleteLogsByLevel(level)
        logInfo("LogRepository", "Cleared $deletedCount log entries with level $level")
    }

    /**
     * Get log count
     */
    fun getLogCount(): Flow<Int> = logDao.getLogCount()

    /**
     * Generate session ID for tracking related logs
     */
    fun generateSessionId(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }

    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}
