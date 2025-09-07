package com.hieltech.haramblur.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for UsageStatsManager integration and usage time calculations
 */
@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager
) {

    companion object {
        private const val TAG = "UsageStatsHelper"
    }

    /**
     * Get daily usage time for a specific app in minutes
     */
    fun getDailyUsageTimeMinutes(packageName: String, date: LocalDate = LocalDate.now()): Int {
        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endOfDay
            )

            val appUsage = usageStats.find { it.packageName == packageName }
            val totalTimeInForeground = appUsage?.totalTimeInForeground ?: 0L

            // Convert milliseconds to minutes
            TimeUnit.MILLISECONDS.toMinutes(totalTimeInForeground).toInt()
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for package: $packageName")
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily usage time for $packageName", e)
            0
        }
    }

    /**
     * Get usage time for a specific time period in minutes
     */
    fun getUsageTimeForPeriod(
        packageName: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int {
        return try {
            val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startMillis,
                endMillis
            )

            val appUsage = usageStats.find { it.packageName == packageName }
            val totalTimeInForeground = appUsage?.totalTimeInForeground ?: 0L

            TimeUnit.MILLISECONDS.toMinutes(totalTimeInForeground).toInt()
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for period query")
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage time for period", e)
            0
        }
    }

    /**
     * Get all apps with usage today
     */
    fun getAppsWithUsageToday(date: LocalDate = LocalDate.now()): Map<String, Int> {
        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endOfDay
            )

            usageStats
                .filter { it.totalTimeInForeground > 0 }
                .associate {
                    it.packageName to TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground).toInt()
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for daily query")
            emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting apps with usage today", e)
            emptyMap()
        }
    }

    /**
     * Get incremental usage time since last check
     */
    fun getIncrementalUsageTime(
        packageName: String,
        lastCheckTime: LocalDateTime,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): Int {
        return getUsageTimeForPeriod(packageName, lastCheckTime, currentTime)
    }

    /**
     * Check if usage stats permission is granted
     */
    fun isUsageStatsPermissionGranted(): Boolean {
        return try {
            val currentTime = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - TimeUnit.DAYS.toMillis(1),
                currentTime
            )
            usageStats.isNotEmpty()
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
    }

    /**
     * Get most used apps today
     */
    fun getMostUsedAppsToday(limit: Int = 10, date: LocalDate = LocalDate.now()): List<Pair<String, Int>> {
        return getAppsWithUsageToday(date)
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Get total screen time for today in minutes
     */
    fun getTotalScreenTimeToday(date: LocalDate = LocalDate.now()): Int {
        return getAppsWithUsageToday(date).values.sum()
    }

    /**
     * Check if an app was used in the last N minutes
     */
    fun wasAppUsedRecently(packageName: String, minutesAgo: Int): Boolean {
        val currentTime = LocalDateTime.now()
        val checkTime = currentTime.minusMinutes(minutesAgo.toLong())
        return getUsageTimeForPeriod(packageName, checkTime, currentTime) > 0
    }

    /**
     * Get usage statistics for multiple apps at once
     */
    fun getMultipleAppsUsageToday(
        packageNames: Set<String>,
        date: LocalDate = LocalDate.now()
    ): Map<String, Int> {
        if (packageNames.isEmpty()) return emptyMap()

        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endOfDay
            )

            packageNames.associateWith { packageName ->
                val appUsage = usageStats.find { it.packageName == packageName }
                val totalTimeInForeground = appUsage?.totalTimeInForeground ?: 0L
                TimeUnit.MILLISECONDS.toMinutes(totalTimeInForeground).toInt()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for multiple apps query")
            emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting multiple apps usage", e)
            emptyMap()
        }
    }

    /**
     * Get detailed usage stats for an app including launch count and last used time
     */
    fun getDetailedAppUsage(
        packageName: String,
        date: LocalDate = LocalDate.now()
    ): DetailedAppUsage? {
        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endOfDay
            )

            val appUsage = usageStats.find { it.packageName == packageName }
            appUsage?.let {
                DetailedAppUsage(
                    packageName = it.packageName,
                    totalTimeInForeground = TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground).toInt(),
                    firstTimeStamp = it.firstTimeStamp,
                    lastTimeStamp = it.lastTimeStamp,
                    lastTimeUsed = it.lastTimeUsed,
                    totalTimeVisible = TimeUnit.MILLISECONDS.toMinutes(it.totalTimeVisible).toInt(),
                    totalTimeInForegroundToday = TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground).toInt()
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for detailed query")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detailed app usage for $packageName", e)
            null
        }
    }

    /**
     * Get usage time for the current hour
     */
    fun getCurrentHourUsage(packageName: String): Int {
        val now = LocalDateTime.now()
        val startOfHour = now.withMinute(0).withSecond(0).withNano(0)
        return getUsageTimeForPeriod(packageName, startOfHour, now)
    }

    /**
     * Get usage time for the last N hours
     */
    fun getUsageLastNHours(packageName: String, hours: Int): Int {
        val now = LocalDateTime.now()
        val startTime = now.minusHours(hours.toLong())
        return getUsageTimeForPeriod(packageName, startTime, now)
    }

    /**
     * Get usage time by hour for the last 24 hours
     */
    fun getHourlyUsageLast24Hours(packageName: String): Map<Int, Int> {
        val now = LocalDateTime.now()
        val result = mutableMapOf<Int, Int>()

        for (i in 0..23) {
            val hourStart = now.minusHours(i.toLong()).withMinute(0).withSecond(0).withNano(0)
            val hourEnd = hourStart.plusHours(1)
            val usage = getUsageTimeForPeriod(packageName, hourStart, hourEnd)
            result[i] = usage
        }

        return result
    }

    /**
     * Check if device has been used in the last N minutes
     */
    fun hasDeviceBeenUsedRecently(minutesAgo: Int): Boolean {
        return try {
            val currentTime = System.currentTimeMillis()
            val checkTime = currentTime - TimeUnit.MINUTES.toMillis(minutesAgo.toLong())

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                checkTime,
                currentTime
            )

            usageStats.any { it.totalTimeInForeground > 0 }
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device usage", e)
            false
        }
    }

    /**
     * Get the most recently used app
     */
    fun getMostRecentlyUsedApp(): String? {
        return try {
            val currentTime = System.currentTimeMillis()
            val oneHourAgo = currentTime - TimeUnit.HOURS.toMillis(1)

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                oneHourAgo,
                currentTime
            )

            usageStats
                .filter { it.totalTimeInForeground > 0 }
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for recent app query")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting most recently used app", e)
            null
        }
    }

    /**
     * Get usage time distribution across different time periods
     */
    fun getUsageTimeDistribution(packageName: String, date: LocalDate = LocalDate.now()): UsageTimeDistribution? {
        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startOfDay,
                endOfDay
            )

            val appStats = usageStats.filter { it.packageName == packageName }
            if (appStats.isEmpty()) return null

            val morningUsage = appStats
                .filter { getHourOfDay(it.firstTimeStamp) in 6..11 }
                .sumOf { it.totalTimeInForeground }

            val afternoonUsage = appStats
                .filter { getHourOfDay(it.firstTimeStamp) in 12..17 }
                .sumOf { it.totalTimeInForeground }

            val eveningUsage = appStats
                .filter { getHourOfDay(it.firstTimeStamp) in 18..23 }
                .sumOf { it.totalTimeInForeground }

            val nightUsage = appStats
                .filter { getHourOfDay(it.firstTimeStamp) in 0..5 }
                .sumOf { it.totalTimeInForeground }

            UsageTimeDistribution(
                morningMinutes = TimeUnit.MILLISECONDS.toMinutes(morningUsage).toInt(),
                afternoonMinutes = TimeUnit.MILLISECONDS.toMinutes(afternoonUsage).toInt(),
                eveningMinutes = TimeUnit.MILLISECONDS.toMinutes(eveningUsage).toInt(),
                nightMinutes = TimeUnit.MILLISECONDS.toMinutes(nightUsage).toInt()
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission not granted for distribution query")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage time distribution for $packageName", e)
            null
        }
    }

    private fun getHourOfDay(timestamp: Long): Int {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        ).hour
    }
}

/**
 * Detailed usage information for an app
 */
data class DetailedAppUsage(
    val packageName: String,
    val totalTimeInForeground: Int, // minutes
    val firstTimeStamp: Long,
    val lastTimeStamp: Long,
    val lastTimeUsed: Long,
    val totalTimeVisible: Int, // minutes
    val totalTimeInForegroundToday: Int // minutes
)

/**
 * Usage time distribution across different periods of the day
 */
data class UsageTimeDistribution(
    val morningMinutes: Int, // 6 AM - 12 PM
    val afternoonMinutes: Int, // 12 PM - 6 PM
    val eveningMinutes: Int, // 6 PM - 12 AM
    val nightMinutes: Int // 12 AM - 6 AM
)
