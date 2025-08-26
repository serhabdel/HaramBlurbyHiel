package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blocking schedules
 */
@Dao
interface BlockingScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BlockingScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<BlockingScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: BlockingScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: BlockingScheduleEntity)

    @Query("SELECT * FROM blocking_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): BlockingScheduleEntity?

    @Query("SELECT * FROM blocking_schedules WHERE is_active = 1 ORDER BY created_at DESC")
    suspend fun getAllActiveSchedules(): List<BlockingScheduleEntity>

    @Query("SELECT * FROM blocking_schedules WHERE app_package_name = :packageName AND is_active = 1")
    suspend fun getSchedulesForApp(packageName: String): List<BlockingScheduleEntity>

    @Query("SELECT * FROM blocking_schedules WHERE site_domain = :domain AND is_active = 1")
    suspend fun getSchedulesForSite(domain: String): List<BlockingScheduleEntity>

    @Query("SELECT * FROM blocking_schedules WHERE schedule_type = :type AND is_active = 1")
    suspend fun getSchedulesByType(type: String): List<BlockingScheduleEntity>

    @Query("SELECT * FROM blocking_schedules WHERE next_scheduled_at IS NOT NULL AND next_scheduled_at <= :currentTime AND is_active = 1 ORDER BY next_scheduled_at ASC")
    suspend fun getDueSchedules(currentTime: Long): List<BlockingScheduleEntity>

    @Query("SELECT * FROM blocking_schedules WHERE days_of_week LIKE '%' || :dayOfWeek || '%' AND is_active = 1")
    suspend fun getSchedulesForDay(dayOfWeek: Int): List<BlockingScheduleEntity>

    @Query("UPDATE blocking_schedules SET is_active = :isActive, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, isActive: Boolean, updatedAt: Long)

    @Query("UPDATE blocking_schedules SET last_applied_at = :appliedAt, next_scheduled_at = :nextScheduled WHERE id = :id")
    suspend fun updateScheduleExecution(id: Long, appliedAt: Long, nextScheduled: Long?)

    @Query("DELETE FROM blocking_schedules WHERE app_package_name = :packageName")
    suspend fun deleteSchedulesForApp(packageName: String)

    @Query("DELETE FROM blocking_schedules WHERE site_domain = :domain")
    suspend fun deleteSchedulesForSite(domain: String)

    @Query("SELECT COUNT(*) FROM blocking_schedules WHERE is_active = 1")
    fun getActiveSchedulesCount(): Flow<Int>

    // Flow versions for reactive UI updates
    @Query("SELECT * FROM blocking_schedules WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveSchedulesFlow(): Flow<List<BlockingScheduleEntity>>

    @Query("SELECT * FROM blocking_schedules WHERE app_package_name = :packageName AND is_active = 1 ORDER BY created_at DESC")
    fun getAppSchedulesFlow(packageName: String): Flow<List<BlockingScheduleEntity>>

    // Get schedules that are currently active (within time range)
    @Query("""
        SELECT * FROM blocking_schedules
        WHERE is_active = 1
        AND schedule_type = 'time_range'
        AND start_hour IS NOT NULL
        AND end_hour IS NOT NULL
        AND (
            (start_hour < end_hour AND :currentHour >= start_hour AND :currentHour <= end_hour) OR
            (start_hour > end_hour AND (:currentHour >= start_hour OR :currentHour <= end_hour))
        )
    """)
    suspend fun getCurrentlyActiveTimeRangeSchedules(currentHour: Int): List<BlockingScheduleEntity>
}
