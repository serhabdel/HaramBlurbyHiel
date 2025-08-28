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

            // Note: Cleanup is now scheduled separately to reduce per-insert overhead

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
     * Scheduled cleanup method - call this periodically (e.g., once per day)
     * Cleans up old logs to prevent database bloat
     */
    suspend fun scheduledCleanup(retainDays: Int = 7) {
        try {
            val cutoffTimestamp = System.currentTimeMillis() - (retainDays * 24 * 60 * 60 * 1000L)
            val deletedCount = logDao.cleanupOldLogs(cutoffTimestamp)
            if (deletedCount > 0) {
                logInfo("LogRepository", "Scheduled cleanup: deleted $deletedCount old log entries (older than $retainDays days)")
            }
        } catch (e: Exception) {
            logError("LogRepository", "Failed to perform scheduled cleanup", e)
        }
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

    // Detection Analytics Methods

    /**
     * Get detection events since a specific timestamp
     */
    fun getDetectionEventsSince(timestamp: Long): Flow<List<LogEntity>> {
        return logDao.getLogsInRange(timestamp, System.currentTimeMillis())
            .map { logs -> logs.filter { it.category == LogCategory.DETECTION.name } }
    }

    /**
     * Get daily detection summary for a specific date
     */
    fun getDailyDetectionSummary(date: Long): Flow<DetectionSummary> {
        val startOfDay = getStartOfDay(date)
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1

        return logDao.getLogsInRange(startOfDay, endOfDay)
            .map { logs ->
                val detectionLogs = logs.filter { it.category == LogCategory.DETECTION.name }
                processDetectionLogs(detectionLogs)
            }
    }

    /**
     * Get weekly detection summary starting from a specific date
     */
    fun getWeeklyDetectionSummary(weekStart: Long): Flow<DetectionSummary> {
        val startOfWeek = getStartOfWeek(weekStart)
        val endOfWeek = startOfWeek + (7 * 24 * 60 * 60 * 1000L) - 1

        return logDao.getLogsInRange(startOfWeek, endOfWeek)
            .map { logs ->
                val detectionLogs = logs.filter { it.category == LogCategory.DETECTION.name }
                processDetectionLogs(detectionLogs)
            }
    }

    /**
     * Get detection timeline data for chart visualization
     */
    fun getDetectionTimeline(hours: Int): Flow<List<TimelinePoint>> {
        val sinceTimestamp = System.currentTimeMillis() - (hours.toLong() * 60 * 60 * 1000)

        return logDao.getLogsSince(sinceTimestamp)
            .map { logs ->
                val detectionLogs = logs.filter { it.category == LogCategory.DETECTION.name }
                groupLogsByHour(detectionLogs)
            }
    }

    /**
     * Get blocked content timeline data for chart visualization
     * Filters events where content was actually blocked (should_blur=true or action contains blur/redirect)
     */
    fun getBlockedTimeline(hours: Int): Flow<List<TimelinePoint>> {
        val sinceTimestamp = System.currentTimeMillis() - (hours.toLong() * 60 * 60 * 1000)

        return logDao.getLogsSince(sinceTimestamp)
            .map { logs ->
                val blockedLogs = logs.filter { log ->
                    log.category == LogCategory.DETECTION.name &&
                    isBlockedEvent(log.message)
                }
                groupLogsByHour(blockedLogs)
            }
    }

    /**
     * Check if a log message represents a blocked content event
     */
    private fun isBlockedEvent(message: String): Boolean {
        // Check for should_blur=true
        if (message.contains("should_blur:true")) {
            return true
        }

        // Check for blur/redirect actions
        val actionsToCheck = listOf("blur", "redirect", "scroll_away", "navigate_back", "auto_close_app")
        return actionsToCheck.any { action ->
            message.contains("action:$action", ignoreCase = true)
        }
    }

    // Helper data classes for analytics

    /**
     * Detection summary data class
     */
    data class DetectionSummary(
        val totalDetections: Int = 0,
        val averageProcessingTime: Double = 0.0,
        val faceDetections: Int = 0,
        val nsfwDetections: Int = 0,
        val violationRate: Float = 0f,
        val performanceScore: Float = 100f,
        val successfulDetections: Int = 0,
        val failedDetections: Int = 0,
        val uniqueProcessingModes: Set<String> = emptySet(),
        val averageNsfwConfidence: Float = 0.0f,
        val maxProcessingTime: Long = 0L,
        val minProcessingTime: Long = Long.MAX_VALUE
    ) {
        companion object {
            fun empty() = DetectionSummary()
        }
    }

    /**
     * Timeline point for chart visualization
     */
    data class TimelinePoint(
        val timestamp: Long,
        val detectionCount: Int,
        val averageProcessingTime: Double,
        val successRate: Float
    )

    // Private helper methods

    private fun getStartOfDay(date: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfWeek(date: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun processDetectionLogs(logs: List<LogEntity>): DetectionSummary {
        if (logs.isEmpty()) return DetectionSummary.empty()

        var totalDetections = 0
        var totalProcessingTime = 0.0
        var faceDetections = 0
        var nsfwDetections = 0
        var successfulDetections = 0
        var failedDetections = 0
        val processingModes = mutableSetOf<String>()
        var totalNsfwConfidence = 0.0f
        var nsfwConfidenceCount = 0
        var maxProcessingTime = 0L
        var minProcessingTime = Long.MAX_VALUE

        logs.forEach { log ->
            totalDetections++
            val metrics = parseDetectionMetrics(log.message)

            totalProcessingTime += metrics.processingTime
            faceDetections += metrics.faceCount
            if (metrics.isNsfw) {
                nsfwDetections++
                totalNsfwConfidence += metrics.nsfwConfidence
                nsfwConfidenceCount++
            }
            if (metrics.success) successfulDetections++ else failedDetections++
            processingModes.add(metrics.performanceMode)

            maxProcessingTime = maxOf(maxProcessingTime, metrics.processingTime.toLong())
            minProcessingTime = minOf(minProcessingTime, metrics.processingTime.toLong())
        }

        val averageProcessingTime = if (totalDetections > 0) totalProcessingTime / totalDetections else 0.0
        val violationRate = if (totalDetections > 0) failedDetections.toFloat() / totalDetections else 0f
        val performanceScore = (100f - violationRate * 100f).coerceIn(0f, 100f)
        val averageNsfwConfidence = if (nsfwConfidenceCount > 0) totalNsfwConfidence / nsfwConfidenceCount else 0.0f

        return DetectionSummary(
            totalDetections = totalDetections,
            averageProcessingTime = averageProcessingTime,
            faceDetections = faceDetections,
            nsfwDetections = nsfwDetections,
            violationRate = violationRate,
            performanceScore = performanceScore,
            successfulDetections = successfulDetections,
            failedDetections = failedDetections,
            uniqueProcessingModes = processingModes,
            averageNsfwConfidence = averageNsfwConfidence,
            maxProcessingTime = maxProcessingTime,
            minProcessingTime = if (minProcessingTime == Long.MAX_VALUE) 0L else minProcessingTime
        )
    }

    private fun groupLogsByHour(logs: List<LogEntity>): List<TimelinePoint> {
        val hourlyData = logs.groupBy { log ->
            val timestamp = log.timestamp
            timestamp / (60 * 60 * 1000) // Group by hour
        }

        return hourlyData.map { (hourTimestamp, hourLogs) ->
            val detectionCount = hourLogs.size
            val metricsList = hourLogs.map { parseDetectionMetrics(it.message) }
            val averageProcessingTime = metricsList.map { it.processingTime }.average()
            val successRate = metricsList.count { it.success }.toFloat() / detectionCount

            TimelinePoint(
                timestamp = hourTimestamp * 60 * 60 * 1000,
                detectionCount = detectionCount,
                averageProcessingTime = averageProcessingTime,
                successRate = successRate
            )
        }.sortedBy { it.timestamp }
    }

    private fun parseDetectionMetrics(message: String): DetectionMetrics {
        // Parse structured detection log message
        // Format: DETECTION|faces:X|nsfw:true|false|nsfw_confidence:X.X|processing_time:Xms|...

        val parts = message.split("|")
        var faceCount = 0
        var isNsfw = false
        var nsfwConfidence = 0.0f
        var processingTime = 0.0
        var success = true
        var performanceMode = "unknown"

        parts.forEach { part ->
            when {
                part.startsWith("faces:") -> faceCount = part.substringAfter(":").toIntOrNull() ?: 0
                part.startsWith("nsfw:") -> isNsfw = part.substringAfter(":").toBoolean()
                part.startsWith("nsfw_confidence:") -> nsfwConfidence = part.substringAfter(":").toFloatOrNull() ?: 0.0f
                part.startsWith("processing_time:") -> processingTime = part.substringAfter(":").removeSuffix("ms").toDoubleOrNull() ?: 0.0
                part.startsWith("success:") -> success = part.substringAfter(":").toBoolean()
                part.startsWith("performance_mode:") -> performanceMode = part.substringAfter(":")
                part.startsWith("error:") -> success = false
            }
        }

        return DetectionMetrics(
            faceCount = faceCount,
            isNsfw = isNsfw,
            nsfwConfidence = nsfwConfidence,
            processingTime = processingTime,
            success = success,
            performanceMode = performanceMode
        )
    }

    private data class DetectionMetrics(
        val faceCount: Int,
        val isNsfw: Boolean,
        val nsfwConfidence: Float,
        val processingTime: Double,
        val success: Boolean,
        val performanceMode: String
    )

    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}
