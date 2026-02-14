package com.hieltech.haramblur.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.HadithRepository
import com.hieltech.haramblur.data.HadithSingleResult
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.utils.AppConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background worker that sends a daily hadith notification.
 *
 * Runs once every 24 hours, fetches the hadith of the day from the API,
 * and displays a notification with the Arabic text and English translation.
 */
@HiltWorker
class HadithNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HadithNotificationWorker"
        private const val HADITH_NOTIFICATION_ID = 3001
        private const val WORK_NAME = "daily_hadith_notification"

        fun scheduleDaily(context: Context) {
            val settings = context.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
            val enabled = settings.getBoolean("enable_hadith_notifications", false)
            val timeStr = settings.getString("hadith_notification_time", "08:00") ?: "08:00"

            if (!enabled) {
                cancel(context)
                return
            }

            // Calculate initial delay to target time
            val parts = timeStr.split(":")
            val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<HadithNotificationWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            Log.i(TAG, "Daily hadith notification scheduled for $targetHour:${"%02d".format(targetMinute)}")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Daily hadith notification cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running daily hadith notification worker")
        try {
            val settings = settingsRepository.settings.value
            if (!settings.enableHadithNotifications) {
                Log.d(TAG, "Hadith notifications disabled, skipping")
                return Result.success()
            }

            // Get API key
            val apiKey = try {
                val field = Class.forName("com.hieltech.haramblur.BuildConfig")
                    .getDeclaredField("HADITH_API_KEY")
                field.get(null) as? String ?: ""
            } catch (_: Exception) { "" }

            if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
                Log.w(TAG, "No API key configured, skipping hadith notification")
                return Result.success()
            }

            val book = settings.preferredHadithBook ?: "sahih-bukhari"
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

            when (val result = hadithRepository.getHadithOfDay(apiKey, book, dayOfYear)) {
                is HadithSingleResult.Success -> {
                    showNotification(result.hadith)
                }
                is HadithSingleResult.Error -> {
                    Log.e(TAG, "Failed to fetch hadith of the day: ${result.message}")
                    return Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in hadith notification worker", e)
            return Result.retry()
        }
        return Result.success()
    }

    private fun showNotification(hadith: com.hieltech.haramblur.data.api.Hadith) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, HADITH_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = applicationContext.getString(R.string.hadith_notification_title)
        val bodyText = hadith.englishText.take(200).let {
            if (hadith.englishText.length > 200) "$it…" else it
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            AppConstants.NotificationChannels.HADITH_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(HADITH_NOTIFICATION_ID, notification)
        Log.d(TAG, "Hadith notification shown: #${hadith.hadithNumber}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NotificationChannels.HADITH_CHANNEL_ID,
                applicationContext.getString(R.string.hadith_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.hadith_notification_channel_desc)
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
