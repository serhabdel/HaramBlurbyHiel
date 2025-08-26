package com.hieltech.haramblur.detection

import android.content.Context
import androidx.work.*
import com.hieltech.haramblur.data.database.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing blocking schedules
 */
interface ScheduleManager {
    suspend fun createDurationSchedule(
        appPackageName: String? = null,
        siteDomain: String? = null,
        durationMinutes: Int,
        scheduleName: String? = null
    ): Long?

    suspend fun createTimeRangeSchedule(
        appPackageName: String? = null,
        siteDomain: String? = null,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: List<Int>? = null,
        scheduleName: String? = null
    ): Long?

    suspend fun removeSchedule(scheduleId: Long): Boolean
    suspend fun updateScheduleStatus(scheduleId: Long, isActive: Boolean): Boolean
    suspend fun getDueSchedules(): List<BlockingScheduleEntity>
    suspend fun processDueSchedules(): Int
    suspend fun getActiveSchedules(): List<BlockingScheduleEntity>
    suspend fun getSchedulesForTarget(appPackageName: String? = null, siteDomain: String? = null): List<BlockingScheduleEntity>
    suspend fun isCurrentlyBlocked(appPackageName: String? = null, siteDomain: String? = null): Boolean
    suspend fun getNextScheduledAction(appPackageName: String? = null, siteDomain: String? = null): Long?
    suspend fun scheduleWorkManagerTasks()
    suspend fun cancelAllScheduledWork()

    // Flow for real-time updates
    fun getScheduleUpdates(): Flow<List<BlockingScheduleEntity>>
}

/**
 * Implementation of schedule manager with WorkManager integration
 */
@Singleton
class ScheduleManagerImpl @Inject constructor(
    private val database: SiteBlockingDatabase,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager?
) : ScheduleManager {

    private val scheduleDao = database.blockingScheduleDao()

    companion object {
        private const val TAG = "ScheduleManager"
        private const val BLOCK_WORK_TAG = "app_blocking_work"
        private const val UNBLOCK_WORK_TAG = "app_unblocking_work"
        private const val SCHEDULE_CHECK_TAG = "schedule_check_work"
    }

    override suspend fun createDurationSchedule(
        appPackageName: String?,
        siteDomain: String?,
        durationMinutes: Int,
        scheduleName: String?
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val endTime = currentTime + (durationMinutes * 60 * 1000)

            val schedule = BlockingScheduleEntity(
                appPackageName = appPackageName,
                siteDomain = siteDomain,
                scheduleType = "duration",
                durationMinutes = durationMinutes,
                isActive = true,
                scheduleName = scheduleName ?: "Block for $durationMinutes minutes",
                createdAt = currentTime,
                updatedAt = currentTime,
                nextScheduledAt = endTime
            )

            val scheduleId = scheduleDao.insertSchedule(schedule)

            // Schedule WorkManager task for unblocking
            scheduleUnblockWork(scheduleId, durationMinutes.toLong())

            return@withContext scheduleId
        } catch (e: Exception) {
            return@withContext null
        }
    }

    override suspend fun createTimeRangeSchedule(
        appPackageName: String?,
        siteDomain: String?,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: List<Int>?,
        scheduleName: String?
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val nextStartTime = calculateNextStartTime(startHour, startMinute, daysOfWeek)

            val schedule = BlockingScheduleEntity(
                appPackageName = appPackageName,
                siteDomain = siteDomain,
                scheduleType = "time_range",
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                daysOfWeek = daysOfWeek?.joinToString(","),
                isActive = true,
                scheduleName = scheduleName ?: createTimeRangeScheduleName(startHour, startMinute, endHour, endMinute, daysOfWeek),
                createdAt = currentTime,
                updatedAt = currentTime,
                nextScheduledAt = nextStartTime
            )

            val scheduleId = scheduleDao.insertSchedule(schedule)

            // Schedule WorkManager tasks
            if (nextStartTime != null) {
                val delay = nextStartTime - currentTime
                scheduleBlockWork(scheduleId, delay)
            }

            return@withContext scheduleId
        } catch (e: Exception) {
            return@withContext null
        }
    }

    override suspend fun removeSchedule(scheduleId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val schedule = scheduleDao.getScheduleById(scheduleId) ?: return@withContext false

            // Cancel associated WorkManager tasks
            cancelScheduleWork(scheduleId)

            // Remove from database
            scheduleDao.deleteSchedule(schedule)

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun updateScheduleStatus(scheduleId: Long, isActive: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            scheduleDao.updateScheduleStatus(scheduleId, isActive, System.currentTimeMillis())

            if (!isActive) {
                // Cancel associated WorkManager tasks
                cancelScheduleWork(scheduleId)
            }

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun getDueSchedules(): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        scheduleDao.getDueSchedules(currentTime)
    }

    override suspend fun processDueSchedules(): Int = withContext(Dispatchers.IO) {
        try {
            val dueSchedules = getDueSchedules()
            var processedCount = 0

            for (schedule in dueSchedules) {
                when (schedule.scheduleType) {
                    "duration" -> {
                        // Duration-based schedule is ending - unblock
                        unblockTarget(schedule)
                        scheduleDao.deleteSchedule(schedule)
                        processedCount++
                    }
                    "time_range" -> {
                        // Time range schedule - handle block/unblock logic
                        val currentTime = System.currentTimeMillis()
                        val calendar = Calendar.getInstance()

                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)

                        val startHour = schedule.startHour ?: continue
                        val startMinute = schedule.startMinute ?: continue
                        val endHour = schedule.endHour ?: continue
                        val endMinute = schedule.endMinute ?: continue

                        // Determine if we should block or unblock
                        val isInBlockedTime = if (startHour < endHour) {
                            // Same day blocking
                            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) &&
                            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                        } else {
                            // Overnight blocking
                            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) ||
                            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                        }

                        if (isInBlockedTime) {
                            // Should be blocked - apply blocking
                            blockTarget(schedule)
                        } else {
                            // Should be unblocked - remove blocking
                            unblockTarget(schedule)
                        }

                        // Update next scheduled time
                        val nextScheduledTime = calculateNextTimeRangeAction(schedule)
                        scheduleDao.updateScheduleExecution(schedule.id, currentTime, nextScheduledTime)

                        processedCount++
                    }
                }
            }

            return@withContext processedCount
        } catch (e: Exception) {
            return@withContext 0
        }
    }

    override suspend fun getActiveSchedules(): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        scheduleDao.getAllActiveSchedules()
    }

    override suspend fun getSchedulesForTarget(appPackageName: String?, siteDomain: String?): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        if (appPackageName != null) {
            scheduleDao.getSchedulesForApp(appPackageName)
        } else if (siteDomain != null) {
            scheduleDao.getSchedulesForSite(siteDomain)
        } else {
            emptyList()
        }
    }

    override suspend fun isCurrentlyBlocked(appPackageName: String?, siteDomain: String?): Boolean = withContext(Dispatchers.IO) {
        val schedules = getSchedulesForTarget(appPackageName, siteDomain)
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        for (schedule in schedules) {
            if (!schedule.isActive) continue

            when (schedule.scheduleType) {
                "duration" -> {
                    val nextScheduled = schedule.nextScheduledAt ?: continue
                    if (currentTime < nextScheduled) {
                        return@withContext true
                    }
                }
                "time_range" -> {
                    val startHour = schedule.startHour ?: continue
                    val startMinute = schedule.startMinute ?: continue
                    val endHour = schedule.endHour ?: continue
                    val endMinute = schedule.endMinute ?: continue

                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = calendar.get(Calendar.MINUTE)

                    // Check if current time is within the blocked range
                    val isInBlockedTime = if (startHour < endHour) {
                        // Same day blocking
                        (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) &&
                        (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                    } else {
                        // Overnight blocking
                        (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) ||
                        (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                    }

                    if (isInBlockedTime) {
                        // Check if it's the right day of the week
                        val daysOfWeek = schedule.daysOfWeek
                        if (daysOfWeek.isNullOrBlank()) {
                            return@withContext true // Every day
                        } else {
                            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                            val blockedDays = daysOfWeek.split(",").map { it.toInt() }
                            if (blockedDays.contains(currentDay)) {
                                return@withContext true
                            }
                        }
                    }
                }
            }
        }

        return@withContext false
    }

    override suspend fun getNextScheduledAction(appPackageName: String?, siteDomain: String?): Long? = withContext(Dispatchers.IO) {
        val schedules = getSchedulesForTarget(appPackageName, siteDomain)
        return@withContext schedules
            .filter { it.isActive && it.nextScheduledAt != null }
            .minOfOrNull { it.nextScheduledAt!! }
    }

    override suspend fun scheduleWorkManagerTasks() = withContext(Dispatchers.IO) {
        try {
            workManager ?: return@withContext // WorkManager not available

            val schedules = getActiveSchedules()

            for (schedule in schedules) {
                val nextScheduledTime = schedule.nextScheduledAt ?: continue
                val currentTime = System.currentTimeMillis()
                val delay = nextScheduledTime - currentTime

                if (delay > 0) {
                    when (schedule.scheduleType) {
                        "duration" -> scheduleUnblockWork(schedule.id, delay / (1000 * 60)) // Convert to minutes
                        "time_range" -> scheduleBlockWork(schedule.id, delay)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle scheduling errors
        }
    }

    override suspend fun cancelAllScheduledWork() {
        withContext(Dispatchers.IO) {
            workManager?.cancelAllWorkByTag(BLOCK_WORK_TAG)
            workManager?.cancelAllWorkByTag(UNBLOCK_WORK_TAG)
            workManager?.cancelAllWorkByTag(SCHEDULE_CHECK_TAG)
        }
    }

    override fun getScheduleUpdates(): Flow<List<BlockingScheduleEntity>> = flow {
        while (currentCoroutineContext().isActive) {
            val schedules = getActiveSchedules()
            emit(schedules)
            delay(5000) // Update every 5 seconds
        }
    }

    // Private helper methods

    private suspend fun blockTarget(schedule: BlockingScheduleEntity) {
        // This will be implemented when we integrate with the blocking managers
        // For now, just log the action
        android.util.Log.d(TAG, "Blocking target: app=${schedule.appPackageName}, site=${schedule.siteDomain}")
    }

    private suspend fun unblockTarget(schedule: BlockingScheduleEntity) {
        // This will be implemented when we integrate with the blocking managers
        // For now, just log the action
        android.util.Log.d(TAG, "Unblocking target: app=${schedule.appPackageName}, site=${schedule.siteDomain}")
    }

    private fun calculateNextStartTime(startHour: Int, startMinute: Int, daysOfWeek: List<Int>?): Long {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Set to today's start time
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var scheduledTime = calendar.timeInMillis

        // If the time has already passed today, move to next occurrence
        if (scheduledTime <= currentTime) {
            if (daysOfWeek.isNullOrEmpty()) {
                // Daily schedule - move to tomorrow
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                scheduledTime = calendar.timeInMillis
            } else {
                // Weekly schedule - find next day of week
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                val nextDay = daysOfWeek.firstOrNull { it > currentDay } ?: daysOfWeek.first()

                if (nextDay > currentDay) {
                    // Later this week
                    calendar.add(Calendar.DAY_OF_MONTH, nextDay - currentDay)
                } else {
                    // Next week
                    calendar.add(Calendar.DAY_OF_MONTH, 7 - currentDay + nextDay)
                }
                scheduledTime = calendar.timeInMillis
            }
        }

        return scheduledTime
    }

    private fun calculateNextTimeRangeAction(schedule: BlockingScheduleEntity): Long? {
        val startHour = schedule.startHour ?: return null
        val startMinute = schedule.startMinute ?: return null
        val endHour = schedule.endHour ?: return null
        val endMinute = schedule.endMinute ?: return null

        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Determine next action time based on current state
        val isCurrentlyInRange = if (startHour < endHour) {
            // Same day blocking
            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) &&
            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
        } else {
            // Overnight blocking
            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) ||
            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
        }

        // Set calendar to next action time
        if (isCurrentlyInRange) {
            // Currently blocked, next action is unblock at end time
            calendar.set(Calendar.HOUR_OF_DAY, endHour)
            calendar.set(Calendar.MINUTE, endMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // If end time has passed today, move to tomorrow
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            // Currently unblocked, next action is block at start time
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, startMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // If start time has passed today, move to tomorrow
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    private fun createTimeRangeScheduleName(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: List<Int>?
    ): String {
        val startTime = String.format("%02d:%02d", startHour, startMinute)
        val endTime = String.format("%02d:%02d", endHour, endMinute)

        val daysText = if (daysOfWeek.isNullOrEmpty()) {
            "Daily"
        } else {
            val dayNames = daysOfWeek.map { dayOfWeek ->
                when (dayOfWeek) {
                    Calendar.SUNDAY -> "Sun"
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    else -> "?"
                }
            }.joinToString(",")
            dayNames
        }

        return "$daysText $startTime-$endTime"
    }

    private fun scheduleBlockWork(scheduleId: Long, delayMillis: Long) {
        workManager ?: return // WorkManager not available

        val blockWork = OneTimeWorkRequestBuilder<BlockWork>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(BLOCK_WORK_TAG)
            .setInputData(workDataOf("schedule_id" to scheduleId))
            .build()

        workManager.enqueueUniqueWork(
            "block_work_$scheduleId",
            ExistingWorkPolicy.REPLACE,
            blockWork
        )
    }

    private fun scheduleUnblockWork(scheduleId: Long, delayMinutes: Long) {
        workManager ?: return // WorkManager not available

        val unblockWork = OneTimeWorkRequestBuilder<UnblockWork>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(UNBLOCK_WORK_TAG)
            .setInputData(workDataOf("schedule_id" to scheduleId))
            .build()

        workManager.enqueueUniqueWork(
            "unblock_work_$scheduleId",
            ExistingWorkPolicy.REPLACE,
            unblockWork
        )
    }

    private fun cancelScheduleWork(scheduleId: Long) {
        workManager?.cancelAllWorkByTag("${BLOCK_WORK_TAG}_$scheduleId")
        workManager?.cancelAllWorkByTag("${UNBLOCK_WORK_TAG}_$scheduleId")
    }
}
