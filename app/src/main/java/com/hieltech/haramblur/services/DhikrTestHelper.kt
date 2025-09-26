package com.hieltech.haramblur.services

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.DhikrRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for testing and debugging dhikr notifications
 */
@Singleton
class DhikrTestHelper @Inject constructor(
    private val dhikrManager: DhikrManager,
    private val dhikrRepository: DhikrRepository
) {
    
    companion object {
        private const val TAG = "DhikrTestHelper"
    }
    
    /**
     * Get comprehensive dhikr system status for debugging
     */
    fun getDhikrSystemStatus(): String {
        return try {
            val settings = dhikrRepository.dhikrSettings.value
            val status = dhikrManager.getSystemStatus()
            val timeUntil = dhikrRepository.getTimeUntilNextDhikr()
            val currentTime = dhikrRepository.getCurrentTimeType()
            val shouldShow = dhikrRepository.shouldShowDhikr()
            
            buildString {
                appendLine("🕌 Dhikr System Status:")
                appendLine("   Enabled: ${settings.enabled}")
                appendLine("   Current Time Window: $currentTime")
                appendLine("   Morning Enabled: ${settings.morningEnabled} (${settings.morningStartTime}:00 - ${settings.morningEndTime}:00)")
                appendLine("   Evening Enabled: ${settings.eveningEnabled} (${settings.eveningStartTime}:00 - ${settings.eveningEndTime}:00)")
                appendLine("   Anytime Enabled: ${settings.anytimeEnabled}")
                appendLine("   Interval: ${settings.intervalMinutes} minutes")
                appendLine("   Display Duration: ${settings.displayDurationSeconds} seconds")
                appendLine("   Sleep Hours: ${formatSleepTime(settings.sleepStartMinutes)} - ${formatSleepTime(settings.sleepEndMinutes)}")
                appendLine("")
                appendLine("   Should Show Now: $shouldShow")
                appendLine("   Time Until Next: ${if (timeUntil > 0) "${timeUntil / 1000 / 60} minutes" else "Ready now"}")
                appendLine("   Daily Count: ${dhikrRepository.getDailyDhikrCount()}")
                appendLine("   Last Shown: ${formatLastShownTime()}")
                appendLine("")
                appendLine("   Notification Permission: ${status.notificationPermissionGranted}")
                appendLine("   Accessibility Service: ${status.accessibilityServiceRunning}")
                appendLine("   Can Show Dhikr: ${status.canShowDhikr}")
                appendLine("")
                appendLine("   Status: ${status.statusDescription}")
            }
        } catch (e: Exception) {
            "Error getting dhikr status: ${e.message}"
        }
    }
    
    /**
     * Force show a test dhikr notification immediately
     */
    fun testDhikrNotification(): String {
        return try {
            Log.i(TAG, "Testing dhikr notification...")
            dhikrManager.testDhikrNotificationNow()
            "✅ Test dhikr notification triggered. Check notification panel."
        } catch (e: Exception) {
            val error = "❌ Failed to test dhikr notification: ${e.message}"
            Log.e(TAG, error, e)
            error
        }
    }
    
    /**
     * Reset dhikr timing to allow immediate testing
     */
    fun resetDhikrTiming(): String {
        return try {
            // Clear the last shown time to allow immediate dhikr
            val prefs = dhikrRepository.getLastDhikrShownTime()
            Log.i(TAG, "Resetting dhikr timing (last shown was: ${formatTime(prefs)})")
            
            // We can't directly access the SharedPreferences from here, so we'll use forceShowDhikrNow
            dhikrManager.forceShowDhikrNow()
            "✅ Dhikr timing reset and test notification shown"
        } catch (e: Exception) {
            val error = "❌ Failed to reset dhikr timing: ${e.message}"
            Log.e(TAG, error, e)
            error
        }
    }
    
    private fun formatSleepTime(minutes: Int): String {
        val hour = minutes / 60
        val min = minutes % 60
        return String.format("%02d:%02d", hour, min)
    }
    
    private fun formatLastShownTime(): String {
        val lastShown = dhikrRepository.getLastDhikrShownTime()
        return if (lastShown == 0L) {
            "Never"
        } else {
            val minutesAgo = (System.currentTimeMillis() - lastShown) / 1000 / 60
            "${minutesAgo} minutes ago"
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        return if (timestamp == 0L) {
            "Never"
        } else {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        }
    }
}