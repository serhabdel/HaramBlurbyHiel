package com.hieltech.haramblur.services

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.PrayerTimesRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.prayer.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Prayer notification scheduler that runs inside the AccessibilityService's coroutine scope.
 * Mirrors DhikrManager's architecture: a foreground-service-hosted coroutine loop that checks
 * every 60 seconds, eliminating WorkManager's 15-minute minimum interval and Doze-mode delays.
 *
 * WorkManager (PrayerNotificationWorker) is kept as a secondary fallback.
 */
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimeNotificationManager: PrayerTimeNotificationManager
) {

    companion object {
        private const val TAG = "PrayerNotifScheduler"
        private const val CHECK_INTERVAL_MS = 60_000L // 60 seconds

        // Tolerance: ±90 seconds to comfortably cover the 60-second loop
        private const val TIME_WINDOW_TOLERANCE_MS = 90 * 1000L
    }

    private var context: Context? = null
    private var schedulerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    fun initialize(context: Context) {
        this.context = context
        Log.d(TAG, "PrayerNotificationScheduler initialized")
    }

    fun startScheduler() {
        schedulerJob?.cancel()
        schedulerJob = serviceScope.launch {
            Log.d(TAG, "Starting prayer notification scheduler loop...")
            while (true) {
                try {
                    checkPrayerNotifications()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in prayer scheduler loop", e)
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
        Log.d(TAG, "Prayer notification scheduler started")
    }

    fun stopScheduler() {
        schedulerJob?.cancel()
        Log.d(TAG, "Prayer notification scheduler stopped")
    }

    fun cleanup() {
        schedulerJob?.cancel()
        context = null
        Log.d(TAG, "PrayerNotificationScheduler cleaned up")
    }

    private suspend fun checkPrayerNotifications() {
        val settings = settingsRepository.settings.value

        if (!settings.enablePrayerNotifications || !settings.enablePrayerTimes) {
            return
        }

        val prayerTimesResult = prayerTimesRepository.getPrayerTimes()

        prayerTimesResult.onSuccess { prayerData ->
            if (prayerData == null) return@onSuccess

            val currentTime = System.currentTimeMillis()
            val language = settings.preferredLanguage.name

            val prayers = mapOf(
                "Fajr" to prayerData.timings.Fajr,
                "Dhuhr" to prayerData.timings.Dhuhr,
                "Asr" to prayerData.timings.Asr,
                "Maghrib" to prayerData.timings.Maghrib,
                "Isha" to prayerData.timings.Isha
            )

            prayers.forEach { (prayerNameStr, prayerTimeStr) ->
                try {
                    val prayerTimestamp = parsePrayerTime(prayerTimeStr)
                    if (prayerTimestamp == 0L) return@forEach
                    val diff = prayerTimestamp - currentTime

                    val prayerName = try {
                        PrayerName.valueOf(prayerNameStr.uppercase())
                    } catch (_: IllegalArgumentException) { return@forEach }

                    // At prayer time
                    if (isAtPrayerTime(diff)) {
                        if (!prayerTimeNotificationManager.hasNotificationBeenSent(prayerNameStr, "attime")) {
                            Log.i(TAG, "🔔 Prayer time: $prayerNameStr at $prayerTimeStr")
                            prayerTimeNotificationManager.sendPrayerTimeNotification(prayerName, prayerTimeStr)
                            prayerTimeNotificationManager.markNotificationAsSent(prayerNameStr, "attime")
                        }
                    }
                    // 10 min before
                    else if (isWithinWindow(diff, 10, before = true)) {
                        if (!prayerTimeNotificationManager.hasNotificationBeenSent(prayerNameStr, "10min")) {
                            Log.i(TAG, "🔔 10-min advance: $prayerNameStr")
                            prayerTimeNotificationManager.sendAdvanceNotification(prayerName, 10, language)
                            prayerTimeNotificationManager.markNotificationAsSent(prayerNameStr, "10min")
                        }
                    }
                    // 5 min before
                    else if (isWithinWindow(diff, 5, before = true)) {
                        if (!prayerTimeNotificationManager.hasNotificationBeenSent(prayerNameStr, "5min")) {
                            Log.i(TAG, "🔔 5-min advance: $prayerNameStr")
                            prayerTimeNotificationManager.sendAdvanceNotification(prayerName, 5, language)
                            prayerTimeNotificationManager.markNotificationAsSent(prayerNameStr, "5min")
                        }
                    }
                    // 5 min after (post-prayer dhikr)
                    else if (isWithinWindow(diff, 5, before = false)) {
                        if (!prayerTimeNotificationManager.hasNotificationBeenSent(prayerNameStr, "after5min")) {
                            Log.i(TAG, "🔔 Post-prayer: $prayerNameStr")
                            prayerTimeNotificationManager.sendPostPrayerNotification(prayerName)
                            prayerTimeNotificationManager.markNotificationAsSent(prayerNameStr, "after5min")
                        }
                    }
                    // 10 min after (late reminder)
                    else if (isWithinWindow(diff, 10, before = false)) {
                        if (!prayerTimeNotificationManager.hasNotificationBeenSent(prayerNameStr, "late")) {
                            Log.i(TAG, "🔔 Late reminder: $prayerNameStr")
                            prayerTimeNotificationManager.sendPrayerReminderNotification(prayerName)
                            prayerTimeNotificationManager.markNotificationAsSent(prayerNameStr, "late")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing $prayerNameStr", e)
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Error fetching prayer times: ${error.message}")
        }

        // Check pending follow-ups
        try {
            prayerTimeNotificationManager.checkAndSendPendingFollowUps()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking follow-ups", e)
        }
    }

    private fun parsePrayerTime(timeString: String): Long {
        try {
            // Strip timezone suffix from API format, e.g. "05:30 (EET)" -> "05:30"
            val cleanTime = timeString.replace(Regex("\\s*\\(.*\\)$"), "").trim()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.parse(cleanTime) ?: return 0L

            val calendar = Calendar.getInstance()
            calendar.time = time

            val now = Calendar.getInstance()
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

            return calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing prayer time '$timeString'", e)
            return 0L
        }
    }

    private fun isAtPrayerTime(diffMs: Long): Boolean {
        return abs(diffMs) <= TIME_WINDOW_TOLERANCE_MS
    }

    private fun isWithinWindow(diffMs: Long, targetMinutes: Int, before: Boolean): Boolean {
        val targetMs = targetMinutes * 60 * 1000L
        return if (before) {
            diffMs > 0 && abs(diffMs - targetMs) <= TIME_WINDOW_TOLERANCE_MS
        } else {
            diffMs < 0 && abs(abs(diffMs) - targetMs) <= TIME_WINDOW_TOLERANCE_MS
        }
    }
}
