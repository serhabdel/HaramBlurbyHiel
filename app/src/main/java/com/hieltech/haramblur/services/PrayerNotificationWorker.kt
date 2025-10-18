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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Background worker for scheduling and sending prayer time notifications
 * 
 * This worker runs every 5 minutes to:
 * - Check all prayer times for advance notifications (10 min and 5 min before)
 * - Check for late reminders (10 min after prayer time)
 * - Check for pending follow-up reminders stored in SharedPreferences
 * 
 * Follow-up reminders are scheduled when users respond "No" to prayer reminders
 * or when they commit to praying ("Will Pray Now"). These follow-ups are stored
 * in SharedPreferences and persist across app restarts, ensuring reliable delivery.
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
        
        // Time window constants
        private const val ADVANCE_10_MIN_MS = 10 * 60 * 1000L
        private const val ADVANCE_5_MIN_MS = 5 * 60 * 1000L
        private const val LATE_10_MIN_MS = 10 * 60 * 1000L
        private const val TIME_WINDOW_TOLERANCE_MS = 1 * 60 * 1000L // 1 minute tolerance

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
                    5, TimeUnit.MINUTES // Check every 5 minutes
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

            // Get all prayer times for today
            val prayerTimesResult = prayerTimesRepository.getPrayerTimes()

            prayerTimesResult.onSuccess { prayerData ->
                if (prayerData != null) {
                    val currentTime = System.currentTimeMillis()
                    val language = settings.preferredLanguage.name
                    
                    // Extract all five prayer times
                    val prayers = mapOf(
                        "Fajr" to prayerData.timings.Fajr,
                        "Dhuhr" to prayerData.timings.Dhuhr,
                        "Asr" to prayerData.timings.Asr,
                        "Maghrib" to prayerData.timings.Maghrib,
                        "Isha" to prayerData.timings.Isha
                    )
                    
                    // Check each prayer against the three time windows
                    prayers.forEach { (prayerNameStr, prayerTimeStr) ->
                        try {
                            val prayerTimestamp = parsePrayerTime(prayerTimeStr)
                            val timeDifferenceMs = prayerTimestamp - currentTime
                            
                            // Convert prayer name to PrayerName enum
                            val prayerName = try {
                                PrayerName.valueOf(prayerNameStr.uppercase())
                            } catch (e: IllegalArgumentException) {
                                println("Invalid prayer name: $prayerNameStr")
                                return@forEach
                            }
                            
                            // Check 10 minutes before window
                            if (isWithinTimeWindow(timeDifferenceMs, 10, isBeforeWindow = true)) {
                                if (!hasNotificationBeenSent(prayerNameStr, "10min")) {
                                    println("Sending 10-minute advance notification for $prayerNameStr at $prayerTimeStr")
                                    prayerTimeNotificationManager.sendAdvanceNotification(prayerName, 10, language)
                                    markNotificationAsSent(prayerNameStr, "10min")
                                } else {
                                    println("Skipping 10-minute notification for $prayerNameStr - already sent today")
                                }
                            }
                            // Check 5 minutes before window
                            else if (isWithinTimeWindow(timeDifferenceMs, 5, isBeforeWindow = true)) {
                                if (!hasNotificationBeenSent(prayerNameStr, "5min")) {
                                    println("Sending 5-minute advance notification for $prayerNameStr at $prayerTimeStr")
                                    prayerTimeNotificationManager.sendAdvanceNotification(prayerName, 5, language)
                                    markNotificationAsSent(prayerNameStr, "5min")
                                } else {
                                    println("Skipping 5-minute notification for $prayerNameStr - already sent today")
                                }
                            }
                            // Check 10 minutes after window
                            else if (isWithinTimeWindow(timeDifferenceMs, 10, isBeforeWindow = false)) {
                                if (!hasNotificationBeenSent(prayerNameStr, "late")) {
                                    println("Sending late reminder notification for $prayerNameStr (10 minutes after)")
                                    prayerTimeNotificationManager.sendPrayerReminderNotification(prayerName)
                                    markNotificationAsSent(prayerNameStr, "late")
                                } else {
                                    println("Skipping late notification for $prayerNameStr - already sent today")
                                }
                            }
                        } catch (e: Exception) {
                            println("Error processing prayer $prayerNameStr: ${e.message}")
                        }
                    }
                }
            }.onFailure { error ->
                // Log error but don't fail the work
                println("Error getting prayer times: ${error.message}")
                
                // If both API and local calculations failed, log a more detailed error
                if (settings.enableLocalCalculations) {
                    println("Both API and local calculation methods failed for prayer times")
                } else {
                    println("API failed for prayer times and local calculations are disabled")
                }
            }
            
            // Check for pending follow-up reminders
            try {
                println("Checking for pending follow-up reminders")
                prayerTimeNotificationManager.checkAndSendPendingFollowUps()
                println("Completed follow-up reminder check")
            } catch (e: Exception) {
                println("Error checking pending follow-ups: ${e.message}")
                e.printStackTrace()
            }

            Result.success()
        } catch (e: Exception) {
            println("Error in PrayerNotificationWorker: ${e.message}")
            e.printStackTrace()
            Result.failure()
        }
    }

    
    /**
     * Parse prayer time string (HH:mm format) to timestamp for today
     */
    private fun parsePrayerTime(timeString: String): Long {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.parse(timeString)

            val calendar = Calendar.getInstance()
            calendar.time = time ?: Date()

            // Set current date but with prayer time
            val now = Calendar.getInstance()
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

            return calendar.timeInMillis
        } catch (e: Exception) {
            println("Error parsing prayer time '$timeString': ${e.message}")
            return 0L
        }
    }
    
    /**
     * Get current date key for tracking notifications
     */
    private fun getCurrentDateKey(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * Check if a notification has been sent today for a specific prayer and type
     */
    private fun hasNotificationBeenSent(prayerName: String, notificationType: String): Boolean {
        val prefs = applicationContext.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
        val dateKey = getCurrentDateKey()
        val key = "prayer_notif_${prayerName}_${notificationType}_$dateKey"
        val storedDate = prefs.getString(key, null)
        return storedDate == dateKey
    }
    
    /**
     * Mark a notification as sent for today
     */
    private fun markNotificationAsSent(prayerName: String, notificationType: String) {
        val prefs = applicationContext.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
        val dateKey = getCurrentDateKey()
        val key = "prayer_notif_${prayerName}_${notificationType}_$dateKey"
        prefs.edit().putString(key, dateKey).apply()
    }
    
    /**
     * Check if time difference falls within a target window
     * @param timeDifferenceMs Time difference in milliseconds (positive = future, negative = past)
     * @param targetMinutes Target time window in minutes
     * @param isBeforeWindow True for "before" notifications, false for "after" notifications
     */
    private fun isWithinTimeWindow(
        timeDifferenceMs: Long,
        targetMinutes: Int,
        isBeforeWindow: Boolean
    ): Boolean {
        val targetMs = targetMinutes * 60 * 1000L
        
        return if (isBeforeWindow) {
            // For "before" windows: time difference should be positive and within tolerance
            timeDifferenceMs > 0 && 
            abs(timeDifferenceMs - targetMs) <= TIME_WINDOW_TOLERANCE_MS
        } else {
            // For "after" windows: time difference should be negative and within tolerance
            timeDifferenceMs < 0 && 
            abs(abs(timeDifferenceMs) - targetMs) <= TIME_WINDOW_TOLERANCE_MS
        }
    }
}