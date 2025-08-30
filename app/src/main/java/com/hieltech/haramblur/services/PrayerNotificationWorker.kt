package com.hieltech.haramblur.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.PrayerTimesRepository
import com.hieltech.haramblur.data.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker for scheduling and sending prayer time notifications
 */
@HiltWorker
class PrayerNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "prayer_notifications"
        private const val CHANNEL_NAME = "Prayer Times"
        private const val NOTIFICATION_ID = 1001

        fun schedulePrayerNotifications(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<PrayerNotificationWorker>(
                1, TimeUnit.HOURS // Check every hour
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "prayer_notifications",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelPrayerNotifications(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("prayer_notifications")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.settings.value

            // Check if prayer notifications are enabled
            if (!settings.enablePrayerNotifications || !settings.enablePrayerTimes) {
                return Result.success()
            }

            // Get next prayer information
            val nextPrayer = prayerTimesRepository.getNextPrayer()

            nextPrayer.onSuccess { prayerInfo ->
                if (prayerInfo != null) {
                    // Check if we should send notification
                    val advanceTimeMs = settings.prayerNotificationAdvanceTime * 60 * 1000L
                    val timeUntilMs = calculateTimeUntil(prayerInfo.timestamp)

                    if (timeUntilMs <= advanceTimeMs && timeUntilMs > 0) {
                        sendPrayerNotification(prayerInfo)
                    }
                }
            }.onFailure { error ->
                // Log error but don't fail the work
                println("Error getting next prayer: ${error.message}")
            }

            Result.success()
        } catch (e: Exception) {
            println("Error in PrayerNotificationWorker: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun sendPrayerNotification(prayerInfo: com.hieltech.haramblur.data.prayer.NextPrayerInfo) {
        createNotificationChannel()

        val settings = settingsRepository.settings.value
        val advanceMinutes = settings.prayerNotificationAdvanceTime

        val title = "${prayerInfo.name} Prayer"
        val message = "${prayerInfo.name} prayer is at ${prayerInfo.time} (${prayerInfo.timeUntil} remaining)"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Islamic prayer times"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun calculateTimeUntil(prayerTimestamp: Long): Long {
        return prayerTimestamp - System.currentTimeMillis()
    }
}