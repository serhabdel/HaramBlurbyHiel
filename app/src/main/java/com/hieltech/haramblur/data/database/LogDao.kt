package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for log entries
 */
@Dao
interface LogDao {

    /**
     * Insert a new log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    /**
     * Insert multiple log entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>): List<Long>

    /**
     * Get all log entries ordered by timestamp descending
     */
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    /**
     * Get log entries by level
     */
    @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<LogEntity>>

    /**
     * Get log entries by category
     */
    @Query("SELECT * FROM logs WHERE category = :category ORDER BY timestamp DESC")
    fun getLogsByCategory(category: String): Flow<List<LogEntity>>

    /**
     * Get log entries by tag
     */
    @Query("SELECT * FROM logs WHERE tag = :tag ORDER BY timestamp DESC")
    fun getLogsByTag(tag: String): Flow<List<LogEntity>>

    /**
     * Get log entries since a specific timestamp
     */
    @Query("SELECT * FROM logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<LogEntity>>

    /**
     * Get log entries within a time range
     */
    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC")
    fun getLogsInRange(startTimestamp: Long, endTimestamp: Long): Flow<List<LogEntity>>

    /**
     * Get the most recent N log entries
     */
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<LogEntity>>

    /**
     * Get log entries with specific levels (for filtering)
     */
    @Query("SELECT * FROM logs WHERE level IN (:levels) ORDER BY timestamp DESC")
    fun getLogsWithLevels(levels: List<String>): Flow<List<LogEntity>>

    /**
     * Get log entries with specific levels and limited count
     */
    @Query("SELECT * FROM logs WHERE level IN (:levels) ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsWithLevels(limit: Int, levels: List<String>): Flow<List<LogEntity>>

    /**
     * Delete logs older than the specified timestamp
     */
    @Query("DELETE FROM logs WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteLogsOlderThan(olderThanTimestamp: Long): Int

    /**
     * Delete logs by level
     */
    @Query("DELETE FROM logs WHERE level = :level")
    suspend fun deleteLogsByLevel(level: String): Int

    /**
     * Delete all logs
     */
    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs(): Int

    /**
     * Get log count
     */
    @Query("SELECT COUNT(*) FROM logs")
    fun getLogCount(): Flow<Int>

    /**
     * Get log count by level
     */
    @Query("SELECT COUNT(*) FROM logs WHERE level = :level")
    fun getLogCountByLevel(level: String): Flow<Int>

    /**
     * Get log statistics
     */
    @Query("""
        SELECT
            COUNT(*) as totalLogs,
            COUNT(CASE WHEN level = 'ERROR' THEN 1 END) as errorCount,
            COUNT(CASE WHEN level = 'WARN' THEN 1 END) as warnCount,
            COUNT(CASE WHEN level = 'INFO' THEN 1 END) as infoCount,
            COUNT(CASE WHEN level = 'DEBUG' THEN 1 END) as debugCount,
            MIN(timestamp) as oldestLog,
            MAX(timestamp) as newestLog
        FROM logs
    """)
    fun getLogStatistics(): Flow<LogStatistics>

    /**
     * Search logs by message content
     */
    @Query("SELECT * FROM logs WHERE message LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    fun searchLogs(searchTerm: String): Flow<List<LogEntity>>

    /**
     * Get logs for export (formatted for sharing)
     */
    @Query("""
        SELECT
            id,
            datetime(timestamp/1000, 'unixepoch', 'localtime') as formatted_timestamp,
            tag,
            message,
            level,
            category,
            stack_trace,
            user_action,
            device_info,
            app_version,
            session_id
        FROM logs
        WHERE timestamp >= :sinceTimestamp
        ORDER BY timestamp ASC
    """)
    fun getLogsForExport(sinceTimestamp: Long = 0): Flow<List<LogExportData>>

    /**
     * Get distinct tags for filtering
     */
    @Query("SELECT DISTINCT tag FROM logs ORDER BY tag")
    fun getDistinctTags(): Flow<List<String>>

    /**
     * Get distinct categories for filtering
     */
    @Query("SELECT DISTINCT category FROM logs ORDER BY category")
    fun getDistinctCategories(): Flow<List<String>>

    /**
     * Clean up old logs (keep only last N days)
     */
    @Query("DELETE FROM logs WHERE timestamp < :cutoffTimestamp")
    suspend fun cleanupOldLogs(cutoffTimestamp: Long): Int
}

data class LogStatistics(
    val totalLogs: Int,
    val errorCount: Int,
    val warnCount: Int,
    val infoCount: Int,
    val debugCount: Int,
    val oldestLog: Long?,
    val newestLog: Long?
)

data class LogExportData(
    val id: Long,
    val formatted_timestamp: String,
    val tag: String,
    val message: String,
    val level: String,
    val category: String,
    val stack_trace: String?,
    val user_action: String?,
    val device_info: String?,
    val app_version: String?,
    val session_id: String?
)
