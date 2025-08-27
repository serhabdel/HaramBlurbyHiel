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
 * Real-time app monitoring service using UsageStatsManager.
 *
 * This service monitors foreground app changes to detect blocked app launches
 * and trigger enhanced blocking mechanisms when available.
 */
@Singleton
class ForegroundAppMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val appBlockingManager: AppBlockingManager,
    private val systemCapabilities: SystemCapabilities,
    private val logRepository: LogRepository
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
     * Handle detected app launch
     */
    private suspend fun handleAppLaunch(packageName: String, eventTime: Long) {
        try {
            // Check if app is blocked
            val isBlocked = appBlockingManager.isAppBlocked(packageName)
            if (!isBlocked) {
                return // Not a blocked app
            }

            // Check schedule-based blocking
            // If app has schedules, check if currently blocked by schedule
            // If app has no schedules, it's always blocked when added to blocked list
            val hasSchedules = appBlockingManager.hasSchedules(packageName)
            val shouldBlock = if (hasSchedules) {
                appBlockingManager.isCurrentlyBlockedBySchedule(packageName)
            } else {
                true // No schedules means always blocked
            }

            if (!shouldBlock) {
                scope.launch {
                    logRepository.logInfo(
                        tag = "AppBlockingManager",
                        message = "App launch not blocked due to schedule: $packageName",
                        category = LogCategory.ACCESSIBILITY,
                        userAction = "APP_LAUNCH_NOT_BLOCKED"
                    )
                }
                return // Not currently blocked by schedule or no schedules but shouldn't block
            }

            // Get recommended blocking method
            val blockingMethod = systemCapabilities.getRecommendedBlockingMethod(packageName)

            scope.launch {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Blocked app launch detected: $packageName (method: ${blockingMethod.name})",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = "BLOCKED_APP_LAUNCH_DETECTED"
                )
            }

            // Trigger blocking
            appBlockingManager.enforceBlock(packageName)
            appBlockingManager.incrementBlockCount(packageName)

        } catch (e: Exception) {
            scope.launch {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Error handling app launch for $packageName: ${e.message}",
                    category = LogCategory.ACCESSIBILITY,
                    userAction = "APP_LAUNCH_HANDLING_ERROR"
                )
            }
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
