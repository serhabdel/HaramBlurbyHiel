package com.hieltech.haramblur.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hieltech.haramblur.data.database.entities.AppUsageStatsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for app usage statistics operations
 */
@Dao
interface AppUsageStatsDao {

    /**
     * Get today's usage stats for a specific app
     */
    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getUsageStatsForApp(packageName: String, date: Long): AppUsageStatsEntity?

    /**
     * Get today's usage stats for a specific app as Flow
     */
    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName AND date = :date LIMIT 1")
    fun getUsageStatsForAppFlow(packageName: String, date: Long): Flow<AppUsageStatsEntity?>

    /**
     * Get all usage stats for today
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date ORDER BY totalMinutes DESC")
    suspend fun getTodayUsageStats(date: Long): List<AppUsageStatsEntity>

    /**
     * Get all usage stats for today as Flow
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date ORDER BY totalMinutes DESC")
    fun getTodayUsageStatsFlow(date: Long): Flow<List<AppUsageStatsEntity>>

    /**
     * Get apps that have exceeded their time limits today
     */
    @Query("""SELECT * FROM app_usage_stats
             WHERE date = :date
             AND timeLimitMinutes IS NOT NULL
             AND totalMinutes >= timeLimitMinutes
             ORDER BY totalMinutes DESC""")
    suspend fun getAppsExceedingLimits(date: Long): List<AppUsageStatsEntity>

    /**
     * Get apps that have exceeded their time limits today as Flow
     */
    @Query("""SELECT * FROM app_usage_stats
             WHERE date = :date
             AND timeLimitMinutes IS NOT NULL
             AND totalMinutes >= timeLimitMinutes
             ORDER BY totalMinutes DESC""")
    fun getAppsExceedingLimitsFlow(date: Long): Flow<List<AppUsageStatsEntity>>

    /**
     * Get usage stats for a date range
     */
    @Query("SELECT * FROM app_usage_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, totalMinutes DESC")
    suspend fun getUsageStatsForDateRange(startDate: Long, endDate: Long): List<AppUsageStatsEntity>

    /**
     * Get usage stats for a specific app over multiple days
     */
    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getAppUsageHistory(packageName: String, startDate: Long, endDate: Long): List<AppUsageStatsEntity>

    /**
     * Insert or update usage stats
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsageStats(stats: AppUsageStatsEntity)

    /**
     * Insert multiple usage stats
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsageStats(stats: List<AppUsageStatsEntity>)

    /**
     * Update usage stats
     */
    @Update
    suspend fun updateUsageStats(stats: AppUsageStatsEntity)

    /**
     * Increment usage time for a specific app
     */
    @Query("""UPDATE app_usage_stats
             SET totalMinutes = totalMinutes + :incrementMinutes,
                 lastUsed = :currentTime,
                 updatedAt = :currentTime
             WHERE packageName = :packageName AND date = :date""")
    suspend fun incrementUsageTime(
        packageName: String,
        date: Long,
        incrementMinutes: Int,
        currentTime: Long
    )

    /**
     * Mark limit as exceeded for an app
     */
    @Query("""UPDATE app_usage_stats
             SET limitExceededTime = :exceededTime,
                 updatedAt = :currentTime
             WHERE packageName = :packageName AND date = :date
             AND limitExceededTime IS NULL""")
    suspend fun markLimitExceeded(
        packageName: String,
        date: Long,
        exceededTime: Long,
        currentTime: Long
    )

    /**
     * Update notification count and time
     */
    @Query("""UPDATE app_usage_stats
             SET notificationCount = notificationCount + 1,
                 lastNotificationTime = :notificationTime,
                 updatedAt = :currentTime
             WHERE packageName = :packageName AND date = :date""")
    suspend fun incrementNotificationCount(
        packageName: String,
        date: Long,
        notificationTime: Long,
        currentTime: Long
    )

    /**
     * Reset all usage stats for a new day
     */
    @Query("DELETE FROM app_usage_stats WHERE date < :cutoffDate")
    suspend fun deleteOldUsageStats(cutoffDate: Long)

    /**
     * Get total usage time for all apps today
     */
    @Query("SELECT COALESCE(SUM(totalMinutes), 0) FROM app_usage_stats WHERE date = :date")
    suspend fun getTotalUsageTimeToday(date: Long): Int

    /**
     * Get count of apps with usage today
     */
    @Query("SELECT COUNT(*) FROM app_usage_stats WHERE date = :date AND totalMinutes > 0")
    suspend fun getActiveAppsCountToday(date: Long): Int

    /**
     * Get apps approaching their time limits (within 10 minutes)
     */
    @Query("""SELECT * FROM app_usage_stats
             WHERE date = :date
             AND timeLimitMinutes IS NOT NULL
             AND totalMinutes >= (timeLimitMinutes - 10)
             AND totalMinutes < timeLimitMinutes
             ORDER BY (timeLimitMinutes - totalMinutes) ASC""")
    suspend fun getAppsApproachingLimits(date: Long): List<AppUsageStatsEntity>

    /**
     * Get usage stats for a specific category
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date AND appCategory = :category ORDER BY totalMinutes DESC")
    suspend fun getUsageStatsByCategory(date: Long, category: String): List<AppUsageStatsEntity>

    /**
     * Get top N most used apps today
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date ORDER BY totalMinutes DESC LIMIT :limit")
    suspend fun getTopUsedAppsToday(date: Long, limit: Int): List<AppUsageStatsEntity>

    /**
     * Get apps with no time limits set
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date AND timeLimitMinutes IS NULL ORDER BY totalMinutes DESC")
    suspend fun getAppsWithoutLimits(date: Long): List<AppUsageStatsEntity>

    /**
     * Get apps that have never exceeded their limits
     */
    @Query("""SELECT * FROM app_usage_stats
             WHERE date = :date
             AND timeLimitMinutes IS NOT NULL
             AND limitExceededTime IS NULL
             ORDER BY totalMinutes DESC""")
    suspend fun getAppsWithinLimits(date: Long): List<AppUsageStatsEntity>

    /**
     * Get average usage time for an app over the last N days
     */
    @Query("""SELECT COALESCE(AVG(totalMinutes), 0) FROM app_usage_stats
             WHERE packageName = :packageName
             AND date >= :startDate
             AND date <= :endDate""")
    suspend fun getAverageUsageForApp(packageName: String, startDate: Long, endDate: Long): Float

    /**
     * Get total notifications sent today
     */
    @Query("SELECT COALESCE(SUM(notificationCount), 0) FROM app_usage_stats WHERE date = :date")
    suspend fun getTotalNotificationsToday(date: Long): Int

    /**
     * Get apps with the most notifications today
     */
    @Query("SELECT * FROM app_usage_stats WHERE date = :date ORDER BY notificationCount DESC LIMIT :limit")
    suspend fun getMostNotifiedAppsToday(date: Long, limit: Int): List<AppUsageStatsEntity>

    /**
     * Transaction method to create or update usage stats with proper defaults
     */
    @Transaction
    suspend fun createOrUpdateUsageStats(
        packageName: String,
        date: Long,
        incrementMinutes: Int,
        appCategory: String?,
        timeLimitMinutes: Int?,
        currentTime: Long
    ) {
        val existing = getUsageStatsForApp(packageName, date)
        if (existing != null) {
            incrementUsageTime(packageName, date, incrementMinutes, currentTime)
        } else {
            val newStats = AppUsageStatsEntity(
                packageName = packageName,
                date = date,
                totalMinutes = incrementMinutes,
                lastUsed = currentTime,
                appCategory = appCategory,
                timeLimitMinutes = timeLimitMinutes,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            insertOrUpdateUsageStats(newStats)
        }
    }

    /**
     * Bulk update time limits for multiple apps
     */
    @Transaction
    suspend fun updateTimeLimits(updates: Map<String, Int>, date: Long, currentTime: Long) {
        updates.forEach { (packageName, newLimit) ->
            // Update existing records
            getUsageStatsForApp(packageName, date)?.let { existing ->
                val updated = existing.copy(
                    timeLimitMinutes = newLimit,
                    updatedAt = currentTime
                )
                updateUsageStats(updated)
            } ?: run {
                // Create new record if none exists
                val newStats = AppUsageStatsEntity(
                    packageName = packageName,
                    date = date,
                    timeLimitMinutes = newLimit,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                insertOrUpdateUsageStats(newStats)
            }
        }
    }

    /**
     * Clear all usage statistics (for testing or reset)
     */
    @Query("DELETE FROM app_usage_stats")
    suspend fun clearAllUsageStats()

    /**
     * Get usage statistics summary for a date range
     */
    @Query("""
        SELECT
            COUNT(DISTINCT packageName) as totalApps,
            COALESCE(SUM(totalMinutes), 0) as totalMinutes,
            COALESCE(AVG(totalMinutes), 0) as avgMinutesPerApp,
            COALESCE(MAX(totalMinutes), 0) as maxMinutes
        FROM app_usage_stats
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getUsageSummary(startDate: Long, endDate: Long): UsageSummary

    /**
     * Get count of apps that exceeded limits in date range
     */
    @Query("""
        SELECT COUNT(DISTINCT packageName) FROM app_usage_stats
        WHERE date BETWEEN :startDate AND :endDate
        AND limitExceededTime IS NOT NULL
    """)
    suspend fun getAppsExceedingLimitsCount(startDate: Long, endDate: Long): Int

    /**
     * Get total notifications sent in date range
     */
    @Query("""
        SELECT COALESCE(SUM(notificationCount), 0) FROM app_usage_stats
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalNotificationsInRange(startDate: Long, endDate: Long): Int
}

/**
 * Data class for usage summary results
 */
data class UsageSummary(
    val totalApps: Int,
    val totalMinutes: Int,
    val avgMinutesPerApp: Float,
    val maxMinutes: Int
)
