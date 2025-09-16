package com.hieltech.haramblur.accessibility

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Broadcast receiver triggered by the watchdog alarm. It ensures the accessibility service
 * is running and attempts to restart it if necessary.
 */
class AccessibilityServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AccessibilityServiceWatchdog.ACTION_CHECK_SERVICE) {
            Log.d(TAG, "Ignoring unrelated action: ${intent?.action}")
            return
        }

        val serviceRunning = HaramBlurAccessibilityService.isServiceRunning()
        Log.d(TAG, "Watchdog triggered. Service running: $serviceRunning")
        if (serviceRunning) {
            AccessibilityServiceWatchdog.cancel(context, "service_already_running")
            return
        }

        if (!isAccessibilityServiceEnabled(context)) {
            Log.w(TAG, "Accessibility service disabled by user; watchdog will not restart")
            AccessibilityServiceWatchdog.cancel(context, "service_disabled")
            return
        }

        runCatching {
            val serviceIntent = Intent(context, HaramBlurAccessibilityService::class.java).apply {
                putExtra(EXTRA_WATCHDOG_RESTART, true)
            }
            context.startService(serviceIntent)
            Log.w(TAG, "Restart intent dispatched to HaramBlurAccessibilityService")
        }.onFailure { error ->
            Log.e(TAG, "Failed to dispatch restart intent", error)
            AccessibilityServiceWatchdog.scheduleRetry(context, "restart_failure")
            return
        }

        AccessibilityServiceWatchdog.schedule(context, "verify_restart", delayMs = VERIFY_DELAY_MS)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            ) == 1

            if (!accessibilityEnabled) {
                Log.w(TAG, "System accessibility disabled")
                return false
            }

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedComponent = ComponentName(context, HaramBlurAccessibilityService::class.java)
            val flattenedComponent = expectedComponent.flattenToString()

            enabledServices.split(':').any { it.equals(flattenedComponent, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status", e)
            false
        }
    }

    companion object {
        private const val TAG = "AccessibilityRestartRcvr"
        private const val VERIFY_DELAY_MS = 10_000L
        const val EXTRA_WATCHDOG_RESTART = "extra_watchdog_restart"
    }
}
