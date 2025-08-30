package com.hieltech.haramblur.detection

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.hieltech.haramblur.data.LogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hieltech.haramblur.data.LogRepository.LogCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Callback interface for handling blocked app launches
 */
interface BlockedAppLaunchCallback {
    suspend fun onBlockedAppLaunched(packageName: String): Boolean
}

/**
 * Real-time app monitoring service using UsageStatsManager.
 *
 * This service monitors foreground app changes to detect blocked app launches
 * and trigger enhanced blocking mechanisms when available.
 */
@Singleton
class ForegroundAppMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val systemCapabilities: SystemCapabilities,
    private val logRepository: LogRepository,
    private val blockedAppLaunchCallback: BlockedAppLaunchCallback
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    private val _isMonitoringFlow = MutableStateFlow(false)
    val isMonitoringFlow: StateFlow<Boolean> = _isMonitoringFlow.asStateFlow()

    private var lastForegroundApp: String? = null
    private var lastEventTime: Long = 0

    companion object {
        private const val MONITORING_INTERVAL_MS = 1000L // 1 second base interval
        private const val MONITORING_INTERVAL_IDLE_MS = 5000L // 5 seconds when device is idle
        private const val EVENT_TIME_WINDOW_MS = 2000L // 2 seconds
        private const val MIN_EVENT_INTERVAL_MS = 500L // Prevent duplicate events
    }

    /**
     * Start monitoring foreground apps
     */
    fun startMonitoring() {
        if (!systemCapabilities.isUsageStatsPermissionGranted()) {
            scope.launch { logRepository.logInfo(
                tag = "AppBlockingManager", message = "Cannot start monitoring - Usage Stats permission not granted",
                category = LogCategory.ACCESSIBILITY,
                userAction = "MONITORING_START_FAILED"
            ) }
            return
        }

        if (monitoringJob?.isActive == true) {
            return // Already monitoring
        }

        monitoringJob = scope.launch {
            _isMonitoringFlow.value = true

            logRepository.logInfo(
                tag = "AppBlockingManager", message = "Foreground app monitoring started",
                category = LogCategory.ACCESSIBILITY,
                userAction = "MONITORING_STARTED"
            )

            while (isActive) {
                try {
                    checkForegroundApp()
                    val interval = getAdaptiveInterval()
                    delay(interval)
                } catch (e: Exception) {
                    logRepository.logInfo(
                        tag = "AppBlockingManager",
                        message = "Error during app monitoring: ${e.message}",
                        category = LogCategory.ACCESSIBILITY,
                        userAction = "MONITORING_ERROR"
                    )
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Stop monitoring foreground apps
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoringFlow.value = false

        scope.launch {
            logRepository.logInfo(
                tag = "AppBlockingManager", message = "Foreground app monitoring stopped",
                category = LogCategory.ACCESSIBILITY,
                userAction = "MONITORING_STOPPED"
            )
        }
    }

    /**
     * Check current foreground app and handle blocking
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private suspend fun checkForegroundApp() {
        if (!isDeviceActive()) {
            return // Skip monitoring when device is inactive
        }

        try {
            val currentTime = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(currentTime - EVENT_TIME_WINDOW_MS, currentTime)

            var foregroundApp: String? = null
            var eventTime: Long = 0

            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    foregroundApp = event.packageName
                    eventTime = event.timeStamp
                    break
                }
            }

            // Check if this is a new app launch
            if (foregroundApp != null &&
                foregroundApp != lastForegroundApp &&
                currentTime - lastEventTime > MIN_EVENT_INTERVAL_MS) {

                // Always log app launches for debugging
                scope.launch {
                    logRepository.logInfo(
                        tag = "ForegroundAppMonitor",
                        message = "App launch detected: $foregroundApp",
                        category = LogCategory.ACCESSIBILITY,
                        userAction = "APP_LAUNCH_DETECTED"
                    )
                }

                handleAppLaunch(foregroundApp, eventTime)
                lastForegroundApp = foregroundApp
                lastEventTime = currentTime
            }

        } catch (e: SecurityException) {
            // Usage Stats permission revoked
            scope.launch {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Usage Stats permission revoked during monitoring: ${e.message}",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = "MONITORING_PERMISSION_REVOKED"
                )
            }
            stopMonitoring()
        }
    }

    /**
     * Handle detected app launch with aggressive blocking
     */
    private suspend fun handleAppLaunch(packageName: String, eventTime: Long) {
        try {
            // Notify callback about the app launch - it will handle blocking logic
            val blockEnforced = blockedAppLaunchCallback.onBlockedAppLaunched(packageName)

            if (blockEnforced) {
                // If blocking was successful, set up additional monitoring for this app
                // to ensure it stays blocked if it tries to relaunch
                setupAggressiveMonitoring(packageName)
            }

            scope.launch {
                logRepository.logInfo(
                    tag = "ForegroundAppMonitor",
                    message = "App launch detected: $packageName (block enforced: $blockEnforced)",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = if (blockEnforced) "BLOCKED_APP_LAUNCH_HANDLED" else "NON_BLOCKED_APP_LAUNCH"
                )
            }

        } catch (e: Exception) {
            scope.launch {
                logRepository.logInfo(
                    tag = "ForegroundAppMonitor",
                    message = "Error handling app launch for $packageName: ${e.message}",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = "APP_LAUNCH_HANDLING_ERROR"
                )
            }
        }
    }

    /**
     * Setup aggressive monitoring for persistently blocked apps
     */
    private fun setupAggressiveMonitoring(packageName: String) {
        // Add this package to a watchlist for extra monitoring
        // This ensures that if the app tries to relaunch, we catch it immediately
        scope.launch {
            try {
                // Monitor for a short period to catch any relaunch attempts
                val monitorDuration = 10000L // 10 seconds of aggressive monitoring
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < monitorDuration && isActive) {
                    // Check if the app is still running and block it if needed
                    if (isAppRunning(packageName)) {
                        blockedAppLaunchCallback.onBlockedAppLaunched(packageName)
                        logRepository.logInfo(
                            tag = "ForegroundAppMonitor",
                            message = "Re-blocking persistent app: $packageName",
                            category = LogCategory.ACCESSIBILITY,
                            userAction = "PERSISTENT_APP_REBLOCKED"
                        )
                    }
                    delay(1000L) // Check every second
                }
            } catch (e: Exception) {
                logRepository.logInfo(
                    tag = "ForegroundAppMonitor",
                    message = "Error in aggressive monitoring for $packageName: ${e.message}",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = "AGGRESSIVE_MONITORING_ERROR"
                )
            }
        }
    }

    /**
     * Check if an app is currently running
     */
    private fun isAppRunning(packageName: String): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = activityManager.runningAppProcesses ?: return false

            for (process in runningProcesses) {
                if (process.processName == packageName || process.processName.startsWith("$packageName:")) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
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
     * Get adaptive monitoring interval based on device state
     */
    private fun getAdaptiveInterval(): Long {
        return if (isDeviceActive()) {
            MONITORING_INTERVAL_MS
        } else {
            MONITORING_INTERVAL_IDLE_MS
        }
    }

    /**
     * Set monitoring exclusions (for pause-in-apps feature)
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }
}
