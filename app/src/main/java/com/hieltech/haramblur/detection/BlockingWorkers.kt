package com.hieltech.haramblur.detection

import android.content.Context
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker for handling scheduled blocking actions
 */
class BlockWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleManager: ScheduleManager,
    private val appBlockingManager: AppBlockingManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val scheduleId = inputData.getLong("schedule_id", -1)
            if (scheduleId == -1L) {
                return@withContext Result.failure()
            }

            // Process the scheduled blocking
            val processed = scheduleManager.processDueSchedules()
            if (processed > 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Worker for handling scheduled unblocking actions
 */
class UnblockWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleManager: ScheduleManager,
    private val appBlockingManager: AppBlockingManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val scheduleId = inputData.getLong("schedule_id", -1)
            if (scheduleId == -1L) {
                return@withContext Result.failure()
            }

            // Process the scheduled unblocking
            val processed = scheduleManager.processDueSchedules()
            if (processed > 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Worker for periodic schedule checking and processing
 */
class ScheduleCheckWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleManager: ScheduleManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Process any due schedules
            val processed = scheduleManager.processDueSchedules()

            // Reschedule for next check
            scheduleManager.scheduleWorkManagerTasks()

            Result.success(workDataOf("processed_schedules" to processed))
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
