package com.hieltech.haramblur.accessibility

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Utility object that manages watchdog alarms for the accessibility service.
 * It ensures the service is restarted if the system terminates it unexpectedly.
 */
object AccessibilityServiceWatchdog {

    private const val TAG = "AccessibilityWatchdog"
    private const val WATCHDOG_REQUEST_CODE = 1001
    private const val DEFAULT_DELAY_MS = 5_000L
    private const val RETRY_DELAY_MS = 15_000L

    const val ACTION_CHECK_SERVICE = "com.hieltech.haramblur.action.CHECK_ACCESSIBILITY_SERVICE"

    /**
     * Schedule a watchdog alarm that will trigger a restart check for the accessibility service.
     *
     * @param context Context used to obtain [AlarmManager]
     * @param reason Descriptive reason used for logging
     * @param delayMs Delay in milliseconds before the watchdog fires
     */
    fun schedule(context: Context, reason: String, delayMs: Long = DEFAULT_DELAY_MS) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available; cannot schedule watchdog ($reason)")
            return
        }

        val pendingIntent = getPendingIntent(context, createIfMissing = true) ?: return
        val triggerAtMillis = System.currentTimeMillis() + delayMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.w(TAG, "Watchdog scheduled in ${delayMs}ms (reason: $reason)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule watchdog alarm", e)
        }
    }

    /**
     * Cancel any pending watchdog alarms.
     */
    fun cancel(context: Context, reason: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available; cannot cancel watchdog${reason?.let { " ($it)" } ?: ""}")
            return
        }

        val pendingIntent = getPendingIntent(context, createIfMissing = false)
        if (pendingIntent == null) {
            Log.d(TAG, "No watchdog alarm to cancel${reason?.let { " ($it)" } ?: ""}")
            return
        }

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Watchdog alarm canceled${reason?.let { " ($it)" } ?: ""}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel watchdog alarm", e)
        }
    }

    /**
     * Schedule a retry after a failed restart attempt.
     */
    fun scheduleRetry(context: Context, reason: String = "retry") {
        schedule(context, reason, RETRY_DELAY_MS)
    }

    private fun getPendingIntent(context: Context, createIfMissing: Boolean): PendingIntent? {
        val intent = Intent(context, AccessibilityServiceRestartReceiver::class.java).apply {
            action = ACTION_CHECK_SERVICE
        }

        val flags = if (createIfMissing) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(context, WATCHDOG_REQUEST_CODE, intent, flags)
    }
}
