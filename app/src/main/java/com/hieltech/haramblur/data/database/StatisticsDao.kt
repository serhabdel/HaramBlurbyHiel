package com.hieltech.haramblur.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hieltech.haramblur.data.database.entities.StatisticsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for statistics operations
 */
@Dao
interface StatisticsDao {

    /**
     * Get today's statistics as Flow
     */
    @Query("SELECT * FROM daily_statistics WHERE date = :date LIMIT 1")
    fun getTodayStatisticsFlow(date: LocalDate): Flow<StatisticsEntity?>

    /**
     * Get today's statistics (suspend function)
     */
    @Query("SELECT * FROM daily_statistics WHERE date = :date LIMIT 1")
    suspend fun getTodayStatistics(date: LocalDate): StatisticsEntity?

    /**
     * Get recent statistics for the last N days
     */
    @Query("SELECT * FROM daily_statistics WHERE date >= :startDate ORDER BY date DESC")
    fun getRecentStatisticsFlow(startDate: LocalDate): Flow<List<StatisticsEntity>>

    /**
     * Get recent statistics for the last N days (suspend function)
     */
    @Query("SELECT * FROM daily_statistics WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getRecentStatistics(startDate: LocalDate): List<StatisticsEntity>

    /**
     * Get recent statistics for the last N days (convenience method)
     */
    suspend fun getRecentStatistics(days: Int): List<StatisticsEntity> {
        val startDate = LocalDate.now().minusDays(days.toLong())
        return getRecentStatistics(startDate)
    }

    /**
     * Get statistics for a specific date
     */
    @Query("SELECT * FROM daily_statistics WHERE date = :date LIMIT 1")
    suspend fun getStatisticsForDate(date: LocalDate): StatisticsEntity?

    /**
     * Get statistics for a date range
     */
    @Query("SELECT * FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    suspend fun getStatisticsForDateRange(startDate: LocalDate, endDate: LocalDate): List<StatisticsEntity>

    /**
     * Insert statistics (replace on conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(statistics: StatisticsEntity)

    /**
     * Update statistics
     */
    @Update
    suspend fun updateStatistics(statistics: StatisticsEntity)

    /**
     * Increment detection counts for a specific date
     */
    @Query("""
        UPDATE daily_statistics
        SET facesDetected = facesDetected + :faceIncrement,
            sitesBlocked = sitesBlocked + :siteIncrement,
            lastUpdated = :timestamp
        WHERE date = :date
    """)
    suspend fun incrementDetections(
        date: LocalDate,
        faceIncrement: Int,
        siteIncrement: Int,
        timestamp: Long
    )

    /**
     * Increment protection time for a specific date
     */
    @Query("""
        UPDATE daily_statistics
        SET protectionActiveTime = protectionActiveTime + :timeIncrement,
            lastUpdated = :timestamp
        WHERE date = :date
    """)
    suspend fun incrementProtectionTime(
        date: LocalDate,
        timeIncrement: Long,
        timestamp: Long
    )

    /**
     * Get performance trends data
     */
    @Query("""
        SELECT date, averageProcessingTime, batteryUsage, memoryPeakUsage, errorsEncountered
        FROM daily_statistics
        WHERE date >= :startDate
        ORDER BY date
    """)
    suspend fun getPerformanceTrends(startDate: LocalDate): List<PerformanceTrend>

    /**
     * Delete old statistics
     */
    @Query("DELETE FROM daily_statistics WHERE date < :cutoffDate")
    suspend fun deleteOldStatistics(cutoffDate: LocalDate)

    /**
     * Get total statistics count
     */
    @Query("SELECT COUNT(*) FROM daily_statistics")
    suspend fun getTotalCount(): Int
}

/**
 * Data class for performance trend queries
 */
data class PerformanceTrend(
    val date: LocalDate,
    val averageProcessingTime: Long,
    val batteryUsage: Float,
    val memoryPeakUsage: Float,
    val errorsEncountered: Int
)
