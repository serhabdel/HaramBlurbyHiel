package com.hieltech.haramblur.detection

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.hieltech.haramblur.data.AppFilteringManager
import com.hieltech.haramblur.data.AppUsageTracker
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.services.UsageTimeNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles integration between foreground app monitoring and usage time tracking
 */
@Singleton
class UsageTrackingIntegrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageTracker: AppUsageTracker,
    private val usageTimeNotificationManager: UsageTimeNotificationManager,
    private val appFilteringManager: AppFilteringManager,
    private val settingsRepository: SettingsRepository,
    private val appNameResolver: AppNameResolver
) {

    companion object {
        private const val TAG = "UsageTrackingIntegrator"
        private const val PERIODIC_CHECK_INTERVAL_MS = 300000L // 5 minutes
    }

    private var periodicCheckJob: Job? = null
    private var resetMonitoringJob: Job? = null
    private var isEnabled = false

    /**
     * Start usage tracking integration
     */
    fun startIntegration(scope: CoroutineScope) {
        stopIntegration()

        // Monitor settings changes
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val config = settingsRepository.getUsageTimeConfig()
                val newEnabled = config.enabled

                if (newEnabled != isEnabled) {
                    isEnabled = newEnabled
                    Log.i(TAG, "Usage tracking integration ${if (newEnabled) "enabled" else "disabled"}")
                }
            }
        }

        // Start periodic usage checking
        periodicCheckJob = scope.launch {
            while (true) {
                try {
                    if (isEnabled) {
                        performPeriodicUsageCheck()
                    }
                    delay(PERIODIC_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during periodic usage check", e)
                    delay(PERIODIC_CHECK_INTERVAL_MS)
                }
            }
        }

        // Monitor for daily resets
        resetMonitoringJob = scope.launch {
            appUsageTracker.usageStatsFlow.collectLatest { stats ->
                if (stats.isEmpty()) {
                    handleDailyReset()
                }
            }
        }

        Log.i(TAG, "Usage tracking integration started")
    }

    /**
     * Stop usage tracking integration
     */
    fun stopIntegration() {
        periodicCheckJob?.cancel()
        resetMonitoringJob?.cancel()
        periodicCheckJob = null
        resetMonitoringJob = null
        Log.i(TAG, "Usage tracking integration stopped")
    }

    /**
     * Track app usage when an app is launched or used
     */
    suspend fun trackAppUsage(packageName: String, eventTime: Long) {
        if (!isEnabled) return

        try {
            // Check if app should be tracked
            if (!appFilteringManager.shouldMonitorAppSync(packageName)) {
                Log.d(TAG, "Skipping usage tracking for $packageName - not monitored")
                return
            }

            // Get app category for context
            val appCategory = appFilteringManager.getAppCategory(packageName)
            Log.d(TAG, "Tracking usage for $packageName (category: ${appCategory?.displayName ?: "Unknown"})")

            // Refresh usage stats to get latest data
            appUsageTracker.refreshUsageStats()

            Log.d(TAG, "Usage tracking completed for $packageName")

        } catch (e: Exception) {
            Log.e(TAG, "Error tracking usage for $packageName", e)
        }
    }

    /**
     * Check and handle time limit violations for a specific app
     */
    suspend fun checkAndHandleTimeLimits(packageName: String) {
        if (!isEnabled) return

        try {
            // Check if app has exceeded time limit
            if (!appUsageTracker.hasExceededTimeLimit(packageName)) {
                return
            }

            // Check if notification should be shown
            if (!appUsageTracker.shouldShowNotification(packageName)) {
                Log.d(TAG, "Time limit exceeded for $packageName but notification not due")
                return
            }

            // Get usage stats
            val stats = appUsageTracker.getAppUsageStats(packageName)
            if (stats == null) {
                Log.w(TAG, "No usage stats found for $packageName despite limit exceeded")
                return
            }

            val timeLimit = stats.timeLimitMinutes ?: return
            val appCategory = appFilteringManager.getAppCategory(packageName)
            val appName = appNameResolver.getDisplayName(packageName, appCategory)

            // Show notification
            usageTimeNotificationManager.showTimeLimitExceededNotification(
                packageName = packageName,
                appName = appName,
                timeUsed = stats.totalMinutes,
                timeLimit = timeLimit
            )

            Log.i(TAG, "Time limit notification shown for $appName (${stats.totalMinutes}m/${timeLimit}m)")

        } catch (e: Exception) {
            Log.e(TAG, "Error checking time limits for $packageName", e)
        }
    }

    /**
     * Perform periodic usage check for all monitored apps
     */
    private suspend fun performPeriodicUsageCheck() {
        try {
            // Only perform checks when device is active to avoid unnecessary processing
            if (!isDeviceActive()) {
                Log.d(TAG, "Skipping periodic usage check - device is idle")
                return
            }

            Log.d(TAG, "Performing periodic usage check")

            // Refresh usage stats
            appUsageTracker.refreshUsageStats()

            // Check all apps exceeding limits
            val exceedingApps = appUsageTracker.getAppsExceedingLimits()

            for (packageName in exceedingApps) {
                checkAndHandleTimeLimits(packageName)
            }

            if (exceedingApps.isNotEmpty()) {
                Log.d(TAG, "Periodic check completed: ${exceedingApps.size} apps exceeding limits")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during periodic usage check", e)
        }
    }

    /**
     * Check if device is active (not in deep sleep or power save mode)
     */
    private fun isDeviceActive(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            // Check if screen is interactive
            if (!powerManager.isInteractive) {
                return false
            }

            // Check for Doze mode (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (powerManager.isDeviceIdleMode) {
                    return false
                }
            }

            // Check for Power Save mode (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (powerManager.isPowerSaveMode) {
                    return false
                }
            }

            true
        } catch (e: Exception) {
            true // Default to active if we can't check
        }
    }

    /**
     * Handle daily reset events
     */
    private fun handleDailyReset() {
        try {
            Log.i(TAG, "Daily usage reset detected, clearing notification states")
            // Cancel all notifications to prevent stale notifications after midnight
            usageTimeNotificationManager.cancelAllNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling daily reset", e)
        }
    }


    /**
     * Check if usage tracking is currently enabled
     */
    fun isUsageTrackingEnabled(): Boolean {
        return isEnabled
    }

    /**
     * Force refresh of usage statistics
     */
    suspend fun forceRefreshUsageStats() {
        if (isEnabled) {
            try {
                appUsageTracker.refreshUsageStats()
                Log.d(TAG, "Forced refresh of usage statistics completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during forced refresh", e)
            }
        }
    }
}
