package com.hieltech.haramblur.detection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.hieltech.haramblur.data.AppRegistry
import com.hieltech.haramblur.data.database.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for app blocking functionality
 */
interface AppBlockingManager {
    suspend fun blockApp(packageName: String, blockType: String = "simple"): Boolean
    suspend fun unblockApp(packageName: String): Boolean
    suspend fun isAppBlocked(packageName: String): Boolean
    suspend fun getBlockedApps(): List<BlockedAppEntity>
    suspend fun getAppByPackageName(packageName: String): BlockedAppEntity?
    suspend fun addAppToDatabase(packageName: String): BlockedAppEntity?
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getBlockedAppsCount(): Flow<Int>
    suspend fun updateAppBlockType(packageName: String, blockType: String): Boolean
    suspend fun incrementBlockCount(packageName: String)

    // Time-based blocking
    suspend fun blockAppForDuration(packageName: String, durationMinutes: Int): Boolean
    suspend fun blockAppWithSchedule(
        packageName: String,
        scheduleType: String,
        durationMinutes: Int? = null,
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null,
        daysOfWeek: List<Int>? = null
    ): Boolean

    // Schedule management
    suspend fun getAppSchedules(packageName: String): List<BlockingScheduleEntity>
    suspend fun removeSchedule(scheduleId: Long): Boolean
    suspend fun isCurrentlyBlockedBySchedule(packageName: String): Boolean
    suspend fun getNextScheduledBlock(packageName: String): Long?

    // Bulk operations
    suspend fun blockMultipleApps(packageNames: List<String>): Boolean
    suspend fun unblockMultipleApps(packageNames: List<String>): Boolean

    // Popular apps management
    suspend fun initializePopularApps(): Int
    suspend fun getPopularApps(): List<BlockedAppEntity>
    suspend fun getSuggestedAppsToBlock(): List<String>
    suspend fun blockDefaultApps(): Int
    suspend fun getAppsByCategory(category: String): List<BlockedAppEntity>
}

/**
 * Implementation of app blocking manager
 */
@Singleton
class AppBlockingManagerImpl @Inject constructor(
    private val database: SiteBlockingDatabase,
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager
) : AppBlockingManager {

    private val blockedAppDao = database.blockedAppDao()
    private val scheduleDao = database.blockingScheduleDao()

    companion object {
        private const val TAG = "AppBlockingManager"
        private val DEFAULT_BLOCKED_APPS = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.discord"
        )
    }

    override suspend fun blockApp(packageName: String, blockType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val existingApp = blockedAppDao.getAppByPackageName(packageName)

            if (existingApp != null) {
                // Update existing app
                val updatedApp = existingApp.copy(
                    isBlocked = true,
                    blockType = blockType,
                    updatedAt = currentTime,
                    lastBlockedAt = currentTime
                )
                blockedAppDao.updateApp(updatedApp)
            } else {
                // Add new app to database
                val appInfo = getAppInfo(packageName)
                if (appInfo != null) {
                    val blockedApp = BlockedAppEntity(
                        packageName = packageName,
                        appName = appInfo.appName,
                        isBlocked = true,
                        blockType = blockType,
                        iconPath = null, // TODO: Implement icon saving
                        category = appInfo.category,
                        isSystemApp = appInfo.isSystemApp,
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        lastBlockedAt = currentTime
                    )
                    blockedAppDao.insertApp(blockedApp)
                } else {
                    return@withContext false
                }
            }

            // Apply system-level blocking
            applySystemBlocking(packageName)

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun unblockApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            blockedAppDao.updateBlockStatus(packageName, false, System.currentTimeMillis())

            // Remove system-level blocking
            removeSystemBlocking(packageName)

            // Remove any schedules for this app
            scheduleDao.deleteSchedulesForApp(packageName)

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun isAppBlocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = blockedAppDao.getAppByPackageName(packageName)
        return@withContext app?.isBlocked == true
    }

    override suspend fun getBlockedApps(): List<BlockedAppEntity> = withContext(Dispatchers.IO) {
        blockedAppDao.getAllBlockedApps()
    }

    override suspend fun getAppByPackageName(packageName: String): BlockedAppEntity? = withContext(Dispatchers.IO) {
        blockedAppDao.getAppByPackageName(packageName)
    }

    override suspend fun addAppToDatabase(packageName: String): BlockedAppEntity? = withContext(Dispatchers.IO) {
        try {
            val appInfo = getAppInfo(packageName) ?: return@withContext null

            val currentTime = System.currentTimeMillis()
            val blockedApp = BlockedAppEntity(
                packageName = packageName,
                appName = appInfo.appName,
                isBlocked = false, // Don't block by default
                blockType = "simple",
                iconPath = null,
                category = appInfo.category,
                isSystemApp = appInfo.isSystemApp,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            blockedAppDao.insertApp(blockedApp)
            return@withContext blockedApp
        } catch (e: Exception) {
            return@withContext null
        }
    }

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            android.util.Log.d(TAG, "Total installed applications: ${installedApps.size}")
            val appInfos = mutableListOf<AppInfo>()

            for (app in installedApps) {
                // Debug logging for specific apps
                val isTargetApp = app.packageName?.contains("instagram") == true ||
                    app.packageName?.contains("whatsapp") == true ||
                    app.packageName?.contains("chrome") == true

                if (isTargetApp) {
                    android.util.Log.d(TAG, "Checking app: ${app.packageName}, isSystemApp: ${isSystemApp(app.packageName)}, inPopularApps: ${AppRegistry.ALL_POPULAR_APPS.containsKey(app.packageName)}")
                }

                // Debug first 10 apps to see the pattern
                if (appInfos.size < 10) {
                    android.util.Log.d(TAG, "Processing app: ${app.packageName}, isSystemApp: ${isSystemApp(app.packageName)}")
                }

                // TEMPORARILY DISABLE FILTERING TO SEE ALL APPS
                // Skip only core system components that are definitely not user apps
                if (isSystemApp(app.packageName) &&
                    !AppRegistry.ALL_POPULAR_APPS.containsKey(app.packageName) &&
                    !isUserFacingSystemApp(app.packageName) &&
                    !isCommonlyUsedSystemApp(app.packageName) &&
                    (app.packageName?.startsWith("com.android.") == true) &&
                    (app.packageName?.contains("vending") != true)) { // Allow Google Play Store
                    continue
                }

                // Skip the app itself
                if (app.packageName == "com.hieltech.haramblur") {
                    continue
                }

                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val category = getAppCategory(app.packageName)

                // Debug logging for popular apps
                if (app.packageName?.contains("instagram") == true ||
                    app.packageName?.contains("whatsapp") == true ||
                    app.packageName?.contains("chrome") == true) {
                    android.util.Log.d(TAG, "Adding app: $appName (${app.packageName}) - Category: $category")
                }

                appInfos.add(AppInfo(
                    packageName = app.packageName,
                    appName = appName,
                    category = category,
                    isSystemApp = isSystemApp(app.packageName),
                    icon = null // Will be loaded when needed
                ))
            }

            val sortedApps = appInfos.sortedBy { it.appName }
            android.util.Log.d(TAG, "Returning ${sortedApps.size} user-facing apps")
            return@withContext sortedApps
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting installed apps", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getBlockedAppsCount(): Flow<Int> {
        return blockedAppDao.getBlockedAppsCount()
    }

    override suspend fun updateAppBlockType(packageName: String, blockType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val existingApp = blockedAppDao.getAppByPackageName(packageName) ?: return@withContext false

            val updatedApp = existingApp.copy(
                blockType = blockType,
                updatedAt = currentTime
            )
            blockedAppDao.updateApp(updatedApp)

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun incrementBlockCount(packageName: String) = withContext(Dispatchers.IO) {
        blockedAppDao.incrementBlockCount(packageName, System.currentTimeMillis())
    }

    override suspend fun blockAppForDuration(packageName: String, durationMinutes: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // First block the app
            val blocked = blockApp(packageName, "time_based")
            if (!blocked) return@withContext false

            // Create a schedule for the duration
            val schedule = BlockingScheduleEntity(
                appPackageName = packageName,
                scheduleType = "duration",
                durationMinutes = durationMinutes,
                isActive = true,
                scheduleName = "Block for $durationMinutes minutes",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                nextScheduledAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            )

            scheduleDao.insertSchedule(schedule)
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun blockAppWithSchedule(
        packageName: String,
        scheduleType: String,
        durationMinutes: Int?,
        startHour: Int?,
        startMinute: Int?,
        endHour: Int?,
        endMinute: Int?,
        daysOfWeek: List<Int>?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // First block the app
            val blocked = blockApp(packageName, "scheduled")
            if (!blocked) return@withContext false

            // Create schedule
            val currentTime = System.currentTimeMillis()
            val schedule = BlockingScheduleEntity(
                appPackageName = packageName,
                scheduleType = scheduleType,
                durationMinutes = durationMinutes,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                daysOfWeek = daysOfWeek?.joinToString(","),
                isActive = true,
                createdAt = currentTime,
                updatedAt = currentTime,
                nextScheduledAt = calculateNextScheduledTime(scheduleType, startHour, startMinute, daysOfWeek)
            )

            scheduleDao.insertSchedule(schedule)
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun getAppSchedules(packageName: String): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        scheduleDao.getSchedulesForApp(packageName)
    }

    override suspend fun removeSchedule(scheduleId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val schedule = scheduleDao.getScheduleById(scheduleId) ?: return@withContext false
            scheduleDao.deleteSchedule(schedule)
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun isCurrentlyBlockedBySchedule(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val schedules = scheduleDao.getSchedulesForApp(packageName)
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            for (schedule in schedules) {
                if (!schedule.isActive) continue

                when (schedule.scheduleType) {
                    "duration" -> {
                        val nextScheduled = schedule.nextScheduledAt ?: continue
                        if (currentTime < nextScheduled) {
                            return@withContext true
                        }
                    }
                    "time_range" -> {
                        val startHour = schedule.startHour ?: continue
                        val startMinute = schedule.startMinute ?: continue
                        val endHour = schedule.endHour ?: continue
                        val endMinute = schedule.endMinute ?: continue

                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)

                        // Check if current time is within the blocked range
                        val isInBlockedTime = if (startHour < endHour) {
                            // Same day blocking (e.g., 9:00 to 17:00)
                            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) &&
                            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                        } else {
                            // Overnight blocking (e.g., 22:00 to 6:00)
                            (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) ||
                            (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute))
                        }

                        if (isInBlockedTime) {
                            // Check if it's the right day of the week
                            val daysOfWeek = schedule.daysOfWeek
                            if (daysOfWeek.isNullOrBlank()) {
                                return@withContext true // Every day
                            } else {
                                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                                val blockedDays = daysOfWeek.split(",").map { it.toInt() }
                                if (blockedDays.contains(currentDay)) {
                                    return@withContext true
                                }
                            }
                        }
                    }
                }
            }

            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun getNextScheduledBlock(packageName: String): Long? = withContext(Dispatchers.IO) {
        val schedules = scheduleDao.getSchedulesForApp(packageName)
        return@withContext schedules
            .filter { it.isActive && it.nextScheduledAt != null }
            .minOfOrNull { it.nextScheduledAt!! }
    }

    override suspend fun blockMultipleApps(packageNames: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            packageNames.forEach { packageName ->
                blockApp(packageName, "simple")
            }
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun unblockMultipleApps(packageNames: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            packageNames.forEach { packageName ->
                unblockApp(packageName)
            }
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun initializePopularApps(): Int = withContext(Dispatchers.IO) {
        try {
            val installedApps = getInstalledApps()
            val installedPackageNames = installedApps.map { it.packageName }.toSet()

            var addedCount = 0

            // Add popular apps that are installed
            AppRegistry.ALL_POPULAR_APPS.forEach { (packageName, appInfo) ->
                if (installedPackageNames.contains(packageName)) {
                    // Check if app already exists in database
                    val existingApp = blockedAppDao.getAppByPackageName(packageName)
                    if (existingApp == null) {
                        // Add app to database
                        val currentTime = System.currentTimeMillis()
                        val blockedApp = BlockedAppEntity(
                            packageName = packageName,
                            appName = appInfo.name,
                            isBlocked = AppRegistry.shouldBlockByDefault(packageName), // Block by default if in default list
                            blockType = if (AppRegistry.shouldBlockByDefault(packageName)) "simple" else "simple",
                            iconPath = null,
                            category = appInfo.category,
                            isSystemApp = false,
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            lastBlockedAt = if (AppRegistry.shouldBlockByDefault(packageName)) currentTime else null
                        )

                        blockedAppDao.insertApp(blockedApp)
                        addedCount++

                        android.util.Log.d(TAG, "Added popular app: ${appInfo.name} (${packageName})")
                    }
                }
            }

            android.util.Log.d(TAG, "Initialized $addedCount popular apps")
            return@withContext addedCount
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error initializing popular apps", e)
            return@withContext 0
        }
    }

    override suspend fun getPopularApps(): List<BlockedAppEntity> = withContext(Dispatchers.IO) {
        try {
            val allApps = blockedAppDao.getAllApps()
            val popularPackageNames = AppRegistry.ALL_POPULAR_APPS.keys

            return@withContext allApps.filter { popularPackageNames.contains(it.packageName) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting popular apps", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getSuggestedAppsToBlock(): List<String> = withContext(Dispatchers.IO) {
        try {
            val installedApps = getInstalledApps()
            val installedPackageNames = installedApps.map { it.packageName }.toSet()

            return@withContext AppRegistry.SUGGESTED_BLOCKED_APPS.filter { packageName ->
                installedPackageNames.contains(packageName) &&
                blockedAppDao.getAppByPackageName(packageName)?.isBlocked != true
            }.toList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting suggested apps", e)
            return@withContext emptyList()
        }
    }

    override suspend fun blockDefaultApps(): Int = withContext(Dispatchers.IO) {
        try {
            val installedApps = getInstalledApps()
            val installedPackageNames = installedApps.map { it.packageName }.toSet()

            var blockedCount = 0

            AppRegistry.DEFAULT_BLOCKED_APPS.forEach { packageName ->
                if (installedPackageNames.contains(packageName)) {
                    val success = blockApp(packageName, "simple")
                    if (success) {
                        blockedCount++
                        android.util.Log.d(TAG, "Auto-blocked default app: $packageName")
                    }
                }
            }

            android.util.Log.d(TAG, "Auto-blocked $blockedCount default apps")
            return@withContext blockedCount
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error blocking default apps", e)
            return@withContext 0
        }
    }

    override suspend fun getAppsByCategory(category: String): List<BlockedAppEntity> = withContext(Dispatchers.IO) {
        try {
            return@withContext blockedAppDao.getAppsByCategory(category)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting apps by category", e)
            return@withContext emptyList()
        }
    }

    // Private helper methods

    private fun isUserFacingSystemApp(packageName: String): Boolean {
        // Common user-facing system apps that users might want to manage
        val userFacingSystemApps = setOf(
            "com.android.settings",           // Settings
            "com.android.browser",            // Browser
            "com.android.chrome",             // Chrome
            "com.google.android.googlequicksearchbox", // Google Search
            "com.google.android.apps.photos", // Google Photos
            "com.google.android.calendar",    // Calendar
            "com.google.android.contacts",    // Contacts
            "com.google.android.dialer",      // Phone
            "com.google.android.messaging",   // Messages
            "com.android.vending",            // Google Play Store
            "com.google.android.youtube",     // YouTube
            "com.google.android.music",       // Google Play Music
            "com.google.android.apps.maps",   // Maps
            "com.google.android.gm",          // Gmail
            "com.google.android.apps.docs",   // Google Docs
            "com.google.android.keep",        // Google Keep
            "com.google.android.apps.translate", // Translate
            "com.android.camera",             // Camera
            "com.android.gallery",            // Gallery
            "com.android.calculator",         // Calculator
            "com.android.deskclock",          // Clock
            "com.android.music",              // Music Player
            "com.android.email"               // Email
        )

        return userFacingSystemApps.contains(packageName)
    }

    private fun isCommonlyUsedSystemApp(packageName: String): Boolean {
        // Additional commonly used system apps that users might want to manage
        val commonlyUsedSystemApps = setOf(
            // Browsers
            "com.android.browser",
            "com.android.chrome",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.duckduckgo.mobile.android",
            "com.microsoft.emmx",

            // Communication
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.google.android.gms", // Google Services (often appears as user-facing)
            "com.google.android.apps.tachyon", // Google Duo

            // Google Apps
            "com.google.android.googlequicksearchbox", // Google Search
            "com.google.android.apps.photos", // Google Photos
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.keep", // Google Keep
            "com.google.android.calendar", // Calendar
            "com.google.android.contacts", // Contacts
            "com.google.android.dialer", // Phone
            "com.google.android.apps.youtube.music", // YouTube Music
            "app.revanced.android.youtube", // YouTube (ReVanced)
            "app.revanced.android.apps.youtube.music", // YouTube Music (ReVanced)

            // Social & Entertainment
            "com.google.android.youtube", // YouTube
            "com.google.android.apps.youtube.kids", // YouTube Kids
            "com.google.android.videos", // Google Play Movies
            "com.google.android.music", // Google Play Music
            "com.google.android.apps.books", // Google Play Books

            // Productivity
            "com.google.android.apps.translate", // Translate
            "com.google.android.apps.maps", // Maps
            "com.google.android.gm", // Gmail
            "com.google.android.apps.drive", // Google Drive
            "com.google.android.apps.authenticator2", // Google Authenticator

            // Utilities
            "com.android.vending", // Google Play Store
            "com.android.settings", // Settings
            "com.android.systemui", // System UI (might be useful for blocking)
            "com.google.android.packageinstaller", // Package Installer
            "com.google.android.feedback", // Feedback
            "com.google.android.apps.nexuslauncher", // Nexus Launcher
            "com.google.android.launcher", // Google Launcher

            // Samsung specific
            "com.samsung.android.messaging", // Samsung Messages
            "com.samsung.android.email.provider", // Samsung Email
            "com.samsung.android.calendar", // Samsung Calendar
            "com.samsung.android.contacts", // Samsung Contacts
            "com.samsung.android.dialer", // Samsung Phone
            "com.samsung.android.browser", // Samsung Internet
            "com.sec.android.app.camera", // Samsung Camera
            "com.samsung.android.gallery", // Samsung Gallery
            "com.sec.android.gallery3d", // Samsung Gallery 3D
            "com.samsung.android.messaging", // Samsung Messages
            "com.samsung.android.app.sbrowser", // Samsung Browser
            "com.samsung.android.video", // Samsung Video

            // Other common apps
            "com.android.incallui", // Phone UI
            "com.android.server.telecom", // Telecom
            "com.android.providers.downloads", // Downloads
            "com.android.providers.media", // Media Storage
            "com.android.externalstorage", // External Storage
            "com.android.providers.contacts", // Contacts Provider
            "com.android.providers.calendar", // Calendar Provider
            "com.android.deskclock", // Clock
            "com.android.calculator", // Calculator
            "com.android.camera", // Camera
            "com.android.gallery", // Gallery
            "com.android.music", // Music
            "com.android.email" // Email
        )

        return commonlyUsedSystemApps.contains(packageName)
    }

    private fun isRegularUserApp(packageName: String): Boolean {
        // For now, let's show all non-system apps that have a proper label
        // This should include all user-installed apps like Instagram, WhatsApp, etc.
        return !isSystemApp(packageName)
    }

    private fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val category = getAppCategory(packageName)

            AppInfo(
                packageName = packageName,
                appName = appName,
                category = category,
                isSystemApp = isSystemApp(packageName),
                icon = null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppCategory(packageName: String): String {
        return when (packageName) {
            in DEFAULT_BLOCKED_APPS -> "social_media"
            "com.android.chrome", "com.android.browser", "org.mozilla.firefox" -> "browser"
            "com.google.android.youtube", "com.netflix.mediaclient" -> "entertainment"
            "com.android.vending", "com.google.android.apps.photos" -> "utility"
            else -> "other"
        }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun applySystemBlocking(packageName: String) {
        // This would integrate with system-level blocking mechanisms
        // For now, we rely on accessibility service to handle blocking
        // TODO: Implement system-level restrictions when possible
    }

    private fun removeSystemBlocking(packageName: String) {
        // Remove system-level blocking
        // TODO: Implement when system-level blocking is added
    }

    private fun calculateNextScheduledTime(
        scheduleType: String,
        startHour: Int?,
        startMinute: Int?,
        daysOfWeek: List<Int>?
    ): Long? {
        if (scheduleType != "time_range" || startHour == null || startMinute == null) {
            return null
        }

        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Set to today's start time
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var scheduledTime = calendar.timeInMillis

        // If the time has already passed today, move to next occurrence
        if (scheduledTime <= currentTime) {
            if (daysOfWeek.isNullOrEmpty()) {
                // Daily schedule - move to tomorrow
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                scheduledTime = calendar.timeInMillis
            } else {
                // Weekly schedule - find next day of week
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                val nextDay = daysOfWeek.firstOrNull { it > currentDay } ?: daysOfWeek.first()

                if (nextDay > currentDay) {
                    // Later this week
                    calendar.add(Calendar.DAY_OF_MONTH, nextDay - currentDay)
                } else {
                    // Next week
                    calendar.add(Calendar.DAY_OF_MONTH, 7 - currentDay + nextDay)
                }
                scheduledTime = calendar.timeInMillis
            }
        }

        return scheduledTime
    }
}

/**
 * Data class for app information
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)
