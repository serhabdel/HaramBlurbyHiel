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
import com.hieltech.haramblur.data.prayer.PrayerName
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
    private val settingsRepository: SettingsRepository,
    private val prayerTimeNotificationManager: PrayerTimeNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "prayer_notifications"
        private const val CHANNEL_NAME = "Prayer Times"
        private const val NOTIFICATION_ID = 1001

        fun schedulePrayerNotifications(context: Context) {
            // Get settings to check if prayer notifications are enabled
            val prefs = context.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
            val enablePrayerNotifications = prefs.getBoolean("enable_prayer_notifications", true)
            val enablePrayerTimes = prefs.getBoolean("enable_prayer_times", true)
            val enableLocalCalculations = prefs.getBoolean("enable_local_calculations", false)
            
            if (enablePrayerNotifications && enablePrayerTimes) {
                // Adjust network constraints based on local calculation settings
                val networkConstraint = if (enableLocalCalculations) {
                    // If local calculations are enabled, we don't require network connectivity
                    NetworkType.NOT_REQUIRED
                } else {
                    // If local calculations are disabled, we require network connectivity
                    NetworkType.CONNECTED
                }
                
                val workRequest = PeriodicWorkRequestBuilder<PrayerNotificationWorker>(
                    1, TimeUnit.HOURS // Check every hour
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(networkConstraint)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "prayer_notifications",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
            } else {
                // Cancel prayer notifications if disabled
                cancelPrayerNotifications(context)
            }
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

            // Log which calculation method is being used
            val calculationMethod = if (settings.enableLocalCalculations) {
                if (settings.preferLocalOverApi) {
                    "Local calculation (preferred)"
                } else {
                    "API with local fallback"
                }
            } else {
                "API only"
            }
            println("PrayerNotificationWorker using calculation method: $calculationMethod")

            // Get next prayer information
            val nextPrayer = prayerTimesRepository.getNextPrayer()

            nextPrayer.onSuccess { prayerInfo ->
                if (prayerInfo != null) {
                    // Check if we should send notification
                    val advanceTimeMs = settings.prayerNotificationAdvanceTime * 60 * 1000L
                    val timeUntilMs = calculateTimeUntil(prayerInfo.timestamp)

                    if (timeUntilMs <= advanceTimeMs && timeUntilMs > 0) {
                        // Convert prayer name to PrayerName enum
                        val prayerName = try {
                            PrayerName.valueOf(prayerInfo.name.uppercase())
                        } catch (e: IllegalArgumentException) {
                            // Default to Fajr if conversion fails
                            PrayerName.FAJR
                        }
                        
                        // Log that we're sending a notification
                        println("Sending prayer notification for $prayerName at ${prayerInfo.time} (in $timeUntilMs ms)")
                        
                        // Use PrayerTimeNotificationManager to send the notification
                        prayerTimeNotificationManager.sendPrayerTimeNotification(prayerName, prayerInfo.time)
                    }
                }
            }.onFailure { error ->
                // Log error but don't fail the work
                println("Error getting next prayer: ${error.message}")
                
                // If both API and local calculations failed, log a more detailed error
                if (settings.enableLocalCalculations) {
                    println("Both API and local calculation methods failed for prayer times")
                } else {
                    println("API failed for prayer times and local calculations are disabled")
                }
            }

            Result.success()
        } catch (e: Exception) {
            println("Error in PrayerNotificationWorker: ${e.message}")
            Result.failure()
        }
    }


    private fun calculateTimeUntil(prayerTimestamp: Long): Long {
        return prayerTimestamp - System.currentTimeMillis()
    }
}