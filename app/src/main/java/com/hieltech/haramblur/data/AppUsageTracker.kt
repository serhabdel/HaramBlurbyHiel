package com.hieltech.haramblur.data

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.database.AppUsageStatsDao
import com.hieltech.haramblur.data.database.entities.AppUsageStatsEntity
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.models.UsageTimeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

    /**
     * Comprehensive app usage tracking service that manages daily usage time limits,
     * notifications, and statistics. Integrates with UsageStatsManager for accurate
     * usage data and provides reactive updates through StateFlow.
     *
     * Usage Strategy:
     * - Uses OS-provided total usage time directly (via UsageStatsHelper) rather than
     *   incremental updates to avoid cumulative errors and ensure accuracy
     * - Updates database with latest totals every tracking interval
     * - DAO increment methods are available but unused in favor of this approach
     *
     * Lifecycle Management:
     * - Call cleanup() when the service is no longer needed (e.g., on app shutdown)
     * - The service uses CoroutineScope with SupervisorJob for proper cancellation
     * - Jobs are automatically cancelled when scope is cancelled
     */
@Singleton
class AppUsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val appUsageStatsDao: AppUsageStatsDao
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val TAG = "AppUsageTracker"
        private const val TRACKING_INTERVAL_MS = 60000L // 1 minute
        private const val RESET_CHECK_INTERVAL_MS = 3600000L // 1 hour
    }

    private val _usageStatsFlow = MutableStateFlow<Map<String, AppUsageStatsEntity>>(emptyMap())
    val usageStatsFlow: StateFlow<Map<String, AppUsageStatsEntity>> = _usageStatsFlow.asStateFlow()

    private val _appsExceedingLimitsFlow = MutableStateFlow<Set<String>>(emptySet())
    val appsExceedingLimitsFlow: StateFlow<Set<String>> = _appsExceedingLimitsFlow.asStateFlow()

    private var trackingJob: Job? = null
    private var resetCheckJob: Job? = null
    private var isTrackingEnabled: Boolean = false

    init {
        // Start monitoring settings changes
        scope.launch {
            settingsRepository.settings.collect { settings ->
                val config = settingsRepository.getUsageTimeConfig()
                val newEnabled = config.enabled

                if (newEnabled != isTrackingEnabled) {
                    isTrackingEnabled = newEnabled
                    if (newEnabled) {
                        startTracking()
                    } else {
                        stopTracking()
                    }
                }
            }
        }
    }

    /**
     * Start usage tracking
     */
    fun startTracking() {
        if (!isTrackingEnabled) return

        // Check if usage stats permission is granted
        if (!usageStatsHelper.isUsageStatsPermissionGranted()) {
            Log.w(TAG, "Cannot start tracking - Usage Stats permission not granted")
            return
        }

        stopTracking() // Clean up any existing jobs

        trackingJob = scope.launch {
            Log.i(TAG, "Starting app usage tracking")

            while (isActive) {
                try {
                    updateUsageStats()
                    delay(TRACKING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during usage tracking", e)
                    delay(TRACKING_INTERVAL_MS)
                }
            }
        }

        resetCheckJob = scope.launch {
            while (isActive) {
                try {
                    checkAndHandleDailyReset()
                    delay(RESET_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during reset check", e)
                    delay(RESET_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Stop usage tracking
     */
    fun stopTracking() {
        trackingJob?.cancel()
        resetCheckJob?.cancel()
        trackingJob = null
        resetCheckJob = null
        Log.i(TAG, "Stopped app usage tracking")
    }

    /**
     * Update usage statistics for all monitored apps
     */
    private suspend fun updateUsageStats() {
        if (!isTrackingEnabled) return

        val config = settingsRepository.getUsageTimeConfig()
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()

        // Get all apps with usage today
        val appsWithUsage = usageStatsHelper.getAppsWithUsageToday(today)

        // Get monitored apps from settings
        val monitoredCategories = settingsRepository.getCurrentSettings().monitoredAppCategories
        val customMonitoredApps = settingsRepository.getCurrentSettings().customMonitoredApps
        val excludedApps = settingsRepository.getCurrentSettings().excludedApps

        val monitoredApps = getMonitoredApps(monitoredCategories, customMonitoredApps)

        val updatedStats = mutableMapOf<String, AppUsageStatsEntity>()
        val exceedingApps = mutableSetOf<String>()

        for ((packageName, minutesUsed) in appsWithUsage) {
            if ((packageName in monitoredApps || minutesUsed > 0) && packageName !in excludedApps) {
                val appCategory = getAppCategory(packageName)
                val timeLimit = config.getTimeLimitForApp(packageName, appCategory)

                // Get or create usage stats entity
                val existingStats = appUsageStatsDao.getUsageStatsForApp(packageName, todayEpochDay)
                val currentTime = System.currentTimeMillis()

                val stats = if (existingStats != null) {
                    // Update existing stats
                    existingStats.copy(
                        totalMinutes = minutesUsed,
                        lastUsed = currentTime,
                        appCategory = appCategory?.name,
                        timeLimitMinutes = timeLimit,
                        updatedAt = currentTime
                    )
                } else {
                    // Create new stats
                    AppUsageStatsEntity(
                        packageName = packageName,
                        date = todayEpochDay,
                        totalMinutes = minutesUsed,
                        lastUsed = currentTime,
                        appCategory = appCategory?.name,
                        timeLimitMinutes = timeLimit,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                }

                appUsageStatsDao.insertOrUpdateUsageStats(stats)
                updatedStats[packageName] = stats

                // Check if limit exceeded
                if (stats.hasExceededLimit()) {
                    exceedingApps.add(packageName)

                    // Mark as exceeded if not already marked
                    if (existingStats?.limitExceededTime == null) {
                        appUsageStatsDao.markLimitExceeded(
                            packageName,
                            todayEpochDay,
                            currentTime,
                            currentTime
                        )
                        // Refresh stats from DB to get updated limitExceededTime
                        val refreshedStats = appUsageStatsDao.getUsageStatsForApp(packageName, todayEpochDay)
                        if (refreshedStats != null) {
                            updatedStats[packageName] = refreshedStats
                        }
                    }
                }
            }
        }

        // Update flows
        _usageStatsFlow.value = updatedStats
        _appsExceedingLimitsFlow.value = exceedingApps
    }

    /**
     * Check and handle daily reset if needed
     */
    private suspend fun checkAndHandleDailyReset() {
        val config = settingsRepository.getUsageTimeConfig()
        val today = LocalDate.now()
        val lastResetDate = getLastResetDate()

        if (config.shouldResetUsage(lastResetDate)) {
            Log.i(TAG, "Performing daily usage reset for ${lastResetDate.format(dateFormatter)}")
            performDailyReset(today)
            updateLastResetDate(today)
        }
    }

    /**
     * Perform daily reset of usage statistics
     */
    private suspend fun performDailyReset(newDate: LocalDate) {
        try {
            val cutoffDate = newDate.minusDays(7).toEpochDay() // Keep 7 days of history
            appUsageStatsDao.deleteOldUsageStats(cutoffDate)

            // Clear current flows
            _usageStatsFlow.value = emptyMap()
            _appsExceedingLimitsFlow.value = emptySet()

            Log.i(TAG, "Daily usage reset completed, keeping data since ${newDate.minusDays(7).format(dateFormatter)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily reset", e)
        }
    }

    /**
     * Get monitored apps based on categories and custom apps
     */
    private fun getMonitoredApps(categories: Set<AppCategory>, customApps: Set<String>): Set<String> {
        val categoryApps = categories.flatMap { it.defaultApps }.toSet()
        return categoryApps + customApps
    }

    /**
     * Determine app category for a package name
     */
    fun getAppCategory(packageName: String): AppCategory? {
        return AppCategory.values().firstOrNull { category ->
            packageName in category.defaultApps
        }
    }

    /**
     * Public method to get app category for a package name (exposed for UsageTimeNotificationManager)
     */
    fun getAppCategoryFor(packageName: String): AppCategory? {
        return getAppCategory(packageName)
    }

    /**
     * Check if a specific app has exceeded its time limit
     */
    fun hasExceededTimeLimit(packageName: String): Boolean {
        return _appsExceedingLimitsFlow.value.contains(packageName)
    }

    /**
     * Get remaining time for an app in minutes
     */
    fun getTimeRemainingForApp(packageName: String): Int? {
        val stats = _usageStatsFlow.value[packageName]
        return stats?.getRemainingMinutes()
    }

    /**
     * Get all apps currently exceeding their limits
     */
    fun getAppsExceedingLimits(): Set<String> {
        return _appsExceedingLimitsFlow.value
    }

    /**
     * Check if notification should be shown for an app
     */
    fun shouldShowNotification(packageName: String): Boolean {
        if (!isTrackingEnabled) return false

        val config = settingsRepository.getUsageTimeConfig()
        val stats = _usageStatsFlow.value[packageName] ?: return false

        if (!stats.hasExceededLimit()) return false

        val exceededTime = stats.limitExceededTime ?: return false
        val exceededDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(exceededTime),
            ZoneId.systemDefault()
        )

        val nextNotificationTime = config.getNextNotificationTime(exceededDateTime)
        val currentTime = LocalDateTime.now()

        return currentTime.isAfter(nextNotificationTime) &&
               stats.notificationCount < 5 // Limit notifications per day
    }

    /**
     * Record notification shown for an app
     */
    suspend fun recordNotificationShown(packageName: String) {
        val today = LocalDate.now().toEpochDay()
        val currentTime = System.currentTimeMillis()

        appUsageStatsDao.incrementNotificationCount(
            packageName = packageName,
            date = today,
            notificationTime = currentTime,
            currentTime = currentTime
        )

        // Refresh in-memory stats to reflect the notification count update
        val refreshedStats = appUsageStatsDao.getUsageStatsForApp(packageName, today)
        if (refreshedStats != null) {
            val currentStats = _usageStatsFlow.value.toMutableMap()
            currentStats[packageName] = refreshedStats
            _usageStatsFlow.value = currentStats
        }
    }

    /**
     * Record notification dismissed for an app
     */
    suspend fun recordNotificationDismissed(packageName: String) {
        val today = LocalDate.now().toEpochDay()
        val currentTime = System.currentTimeMillis()

        // Log the dismissal (could be used for analytics)
        Log.d(TAG, "Notification dismissed for $packageName at ${java.time.Instant.ofEpochMilli(currentTime)}")

        // Refresh in-memory stats if needed
        val refreshedStats = appUsageStatsDao.getUsageStatsForApp(packageName, today)
        if (refreshedStats != null) {
            val currentStats = _usageStatsFlow.value.toMutableMap()
            currentStats[packageName] = refreshedStats
            _usageStatsFlow.value = currentStats
        }
    }

    /**
     * Get usage statistics for a specific app
     */
    fun getAppUsageStats(packageName: String): AppUsageStatsEntity? {
        return _usageStatsFlow.value[packageName]
    }

    /**
     * Get total usage time for today across all apps
     */
    suspend fun getTotalUsageTimeToday(): Int {
        val today = LocalDate.now().toEpochDay()
        return appUsageStatsDao.getTotalUsageTimeToday(today)
    }

    /**
     * Get count of apps with usage today
     */
    suspend fun getActiveAppsCountToday(): Int {
        val today = LocalDate.now().toEpochDay()
        return appUsageStatsDao.getActiveAppsCountToday(today)
    }

    /**
     * Force refresh of usage statistics
     */
    suspend fun refreshUsageStats() {
        updateUsageStats()
    }

    /**
     * Get the last reset date from settings, defaulting to yesterday if not set
     */
    private fun getLastResetDate(): LocalDate {
        val lastResetEpochDay = settingsRepository.getCurrentSettings().lastUsageResetDate
        return lastResetEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().minusDays(1)
    }

    /**
     * Update the last reset date in settings
     */
    private fun updateLastResetDate(date: LocalDate) {
        val currentSettings = settingsRepository.getCurrentSettings()
        val updatedSettings = currentSettings.copy(lastUsageResetDate = date.toEpochDay())
        settingsRepository.updateSettings(updatedSettings)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopTracking()
        scope.cancel()
    }
}
