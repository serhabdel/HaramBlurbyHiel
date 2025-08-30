package com.hieltech.haramblur.detection

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.hieltech.haramblur.data.AppRegistry
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.LogRepository.LogCategory
import com.hieltech.haramblur.data.database.*
import com.hieltech.haramblur.utils.SocialMediaDetector
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
    suspend fun hasSchedules(packageName: String): Boolean
    suspend fun enforceBlock(packageName: String): Boolean

    // Bulk operations
    suspend fun blockMultipleApps(packageNames: List<String>): Boolean
    suspend fun unblockMultipleApps(packageNames: List<String>): Boolean

    // Popular apps management
    suspend fun initializePopularApps(): Int
    suspend fun getPopularApps(): List<BlockedAppEntity>
    suspend fun getSuggestedAppsToBlock(): List<String>
    suspend fun blockDefaultApps(): Int
    suspend fun getAppsByCategory(category: String): List<BlockedAppEntity>

    // Social media specific methods
    suspend fun getInstalledSocialMediaApps(): List<AppInfo>
    suspend fun getSocialMediaAppsBySubcategory(subcategory: String): List<BlockedAppEntity>
    suspend fun blockAllSocialMediaApps(): Int
    suspend fun unblockAllSocialMediaApps(): Int
    suspend fun suggestSocialMediaAppsToBlock(): List<String>
    data class SocialMediaStats(
        val totalSocialMediaApps: Int,
        val blockedSocialMediaApps: Int,
        val mostUsedCategory: String,
        val totalBlockedTime: Long
    )
    suspend fun getSocialMediaBlockingStats(): SocialMediaStats
}

/**
 * Implementation of app blocking manager
 */
@Singleton
class AppBlockingManagerImpl @Inject constructor(
    private val database: SiteBlockingDatabase,
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val logRepository: LogRepository
) : AppBlockingManager {

    private val blockedAppDao = database.blockedAppDao()
    private val scheduleDao = database.blockingScheduleDao()

    @Inject
    lateinit var systemCapabilities: SystemCapabilities

    @Inject
    lateinit var foregroundAppMonitor: ForegroundAppMonitor

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
            val appInfos = mutableListOf<AppInfo>()
            
            // Android 11+ enhanced detection approach
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Method 1: Try to get all installed applications (requires QUERY_ALL_PACKAGES)
                try {
                    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    android.util.Log.d(TAG, "Android 11+ full detection: Found ${installedApps.size} apps")
                    
                    for (app in installedApps) {
                        // Skip system apps that are not user-facing or popular
                        if (isSystemApp(app.packageName) &&
                            !AppRegistry.ALL_POPULAR_APPS.containsKey(app.packageName) &&
                            !isUserFacingSystemApp(app.packageName) &&
                            !isCommonlyUsedSystemApp(app.packageName)) {
                            continue
                        }

                        // Skip the app itself
                        if (app.packageName == "com.hieltech.haramblur") {
                            continue
                        }

                        val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val category = getAppCategory(app.packageName)

                        appInfos.add(AppInfo(
                            packageName = app.packageName,
                            appName = appName,
                            category = category,
                            isSystemApp = isSystemApp(app.packageName),
                            icon = packageManager.getApplicationIcon(appInfo)
                        ))
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Full app detection failed, falling back to targeted detection: ${e.message}")
                    
                    // Method 2: Fallback - Check specific known packages (uses queries in manifest)
                    val knownPackages = AppRegistry.ALL_POPULAR_APPS.keys + 
                                      SocialMediaDetector.getAllSocialMediaPackageNames()
                    
                    for (packageName in knownPackages) {
                        try {
                            // Skip the app itself
                            if (packageName == "com.hieltech.haramblur") {
                                continue
                            }
                            
                            val appInfo = packageManager.getApplicationInfo(packageName, 0)
                            val appName = packageManager.getApplicationLabel(appInfo).toString()
                            val category = getAppCategory(packageName)
                            
                            appInfos.add(AppInfo(
                                packageName = packageName,
                                appName = appName,
                                category = category,
                                isSystemApp = isSystemApp(packageName),
                                icon = packageManager.getApplicationIcon(appInfo)
                            ))
                            android.util.Log.d(TAG, "Found installed app: $appName ($packageName)")
                        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                            // App not installed, continue
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Error checking package $packageName: ${e.message}")
                        }
                    }
                }
            } else {
                // Pre-Android 11 approach
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                
                for (app in installedApps) {
                    if (isSystemApp(app.packageName) &&
                        !AppRegistry.ALL_POPULAR_APPS.containsKey(app.packageName) &&
                        !isUserFacingSystemApp(app.packageName) &&
                        !isCommonlyUsedSystemApp(app.packageName)) {
                        continue
                    }

                    // Skip the app itself
                    if (app.packageName == "com.hieltech.haramblur") {
                        continue
                    }

                    val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val category = getAppCategory(app.packageName)

                    appInfos.add(AppInfo(
                        packageName = app.packageName,
                        appName = appName,
                        category = category,
                        isSystemApp = isSystemApp(app.packageName),
                        icon = packageManager.getApplicationIcon(appInfo)
                    ))
                }
            }

            val sortedApps = appInfos.sortedBy { it.appName }
            android.util.Log.d(TAG, "Retrieved ${sortedApps.size} installed apps")
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

    override suspend fun hasSchedules(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val schedules = scheduleDao.getSchedulesForApp(packageName)
            return@withContext schedules.isNotEmpty()
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override suspend fun enforceBlock(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check if app is blocked
            if (!isAppBlocked(packageName)) {
                return@withContext false
            }

            // Check if systemCapabilities is initialized
            if (!::systemCapabilities.isInitialized) {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "systemCapabilities not initialized, falling back to accessibility service for $packageName",
                    category = LogCategory.BLOCKING
                )
                return@withContext blockAppWithAccessibilityService(packageName)
            }

            // Apply system-level blocking based on capabilities
            val recommendedMethod = systemCapabilities.getRecommendedBlockingMethod(packageName)

            return@withContext when (recommendedMethod) {
                com.hieltech.haramblur.detection.BlockingMethod.FORCE_CLOSE_PREFERRED -> {
                    forceCloseApp(packageName)
                }
                com.hieltech.haramblur.detection.BlockingMethod.ADAPTIVE -> {
                    if (systemCapabilities.canForceCloseApp(packageName)) {
                        forceCloseApp(packageName)
                    } else {
                        // Fall back to accessibility service
                        blockAppWithAccessibilityService(packageName)
                    }
                }
                com.hieltech.haramblur.detection.BlockingMethod.ACCESSIBILITY_ONLY -> {
                    // Use accessibility service to go back to home screen
                    blockAppWithAccessibilityService(packageName)
                }
            }
        } catch (e: Exception) {
            logRepository.logInfo(
                tag = "AppBlockingManager",
                message = "Error enforcing block for $packageName: ${e.message}",
                category = LogCategory.BLOCKING
            )
            false
        }
    }

    fun forceCloseApp(packageName: String): Boolean {
        return try {
            // Check if systemCapabilities is initialized
            if (!::systemCapabilities.isInitialized) {
                runBlocking {
                    logRepository.logInfo(
                        tag = "AppBlockingManager",
                        message = "systemCapabilities not initialized, cannot force close $packageName",
                        category = LogCategory.BLOCKING
                    )
                }
                return false
            }

            if (!systemCapabilities.canForceCloseApp(packageName)) {
                return false
            }

            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, HaramBlurDeviceAdminReceiver::class.java)

            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
            val isProfileOwner = devicePolicyManager.isProfileOwnerApp(context.packageName)

            val success = when {
                // Use setPackagesSuspended for device/profile owners (API 24+)
                (isDeviceOwner || isProfileOwner) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    try {
                        devicePolicyManager.setPackagesSuspended(componentName, arrayOf(packageName), true)
                        true
                    } catch (e: SecurityException) {
                        false
                    }
                }
                // Use setApplicationHidden for device owners (API 21+)
                isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    try {
                        devicePolicyManager.setApplicationHidden(componentName, packageName, true)
                        true
                    } catch (e: SecurityException) {
                        false
                    }
                }
                else -> {
                    false
                }
            }

            if (success) {
                runBlocking {
                    logRepository.logInfo(
                        tag = "AppBlockingManager",
                        message = "Force closed app: $packageName",
                        category = LogCategory.BLOCKING
                    )
                }
            }

            success
        } catch (e: Exception) {
            runBlocking {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Error force closing app $packageName: ${e.message}",
                    category = LogCategory.BLOCKING
                )
            }
            false
        }
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

    // Social media specific method implementations

    override suspend fun getInstalledSocialMediaApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val allApps = getInstalledApps()
            return@withContext SocialMediaDetector.getInstalledSocialMediaApps(allApps)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting installed social media apps", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getSocialMediaAppsBySubcategory(subcategory: String): List<BlockedAppEntity> = withContext(Dispatchers.IO) {
        try {
            val allApps = blockedAppDao.getAllApps()
            return@withContext allApps.filter { app ->
                val appSubcategory = AppRegistry.getSocialMediaSubcategory(app.packageName)
                appSubcategory == subcategory
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting social media apps by subcategory", e)
            return@withContext emptyList()
        }
    }

    override suspend fun blockAllSocialMediaApps(): Int = withContext(Dispatchers.IO) {
        try {
            // Get installed apps and filter to social media apps only
            val installedApps = getInstalledApps()
            val installedSocialMediaApps = SocialMediaDetector.getInstalledSocialMediaApps(installedApps)
            val socialMediaPackageNames = installedSocialMediaApps.map { it.packageName }
            var blockedCount = 0

            socialMediaPackageNames.forEach { packageName ->
                val success = blockApp(packageName, "bulk_social_media")
                if (success) blockedCount++
            }

            android.util.Log.d(TAG, "Blocked $blockedCount social media apps")
            return@withContext blockedCount
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error blocking all social media apps", e)
            return@withContext 0
        }
    }

    override suspend fun unblockAllSocialMediaApps(): Int = withContext(Dispatchers.IO) {
        try {
            // Get installed apps and filter to social media apps only
            val installedApps = getInstalledApps()
            val installedSocialMediaApps = SocialMediaDetector.getInstalledSocialMediaApps(installedApps)
            val socialMediaPackageNames = installedSocialMediaApps.map { it.packageName }
            var unblockedCount = 0

            socialMediaPackageNames.forEach { packageName ->
                val success = unblockApp(packageName)
                if (success) unblockedCount++
            }

            android.util.Log.d(TAG, "Unblocked $unblockedCount social media apps")
            return@withContext unblockedCount
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error unblocking all social media apps", e)
            return@withContext 0
        }
    }

    override suspend fun suggestSocialMediaAppsToBlock(): List<String> = withContext(Dispatchers.IO) {
        try {
            val installedApps = getInstalledApps()
            val installedPackageNames = installedApps.map { it.packageName }.toSet()

            // Get apps that are commonly suggested for blocking but not already blocked
            val suggestedPackages = AppRegistry.SUGGESTED_BLOCKED_APPS.filter { packageName ->
                installedPackageNames.contains(packageName) &&
                blockedAppDao.getAppByPackageName(packageName)?.isBlocked != true
            }.toMutableList()

            // Add additional social media apps that might be worth blocking
            val additionalSuggestions = listOf(
                "com.instagram.android",
                "com.snapchat.android",
                "com.zhiliaoapp.musically",
                "com.google.android.youtube"
            ).filter { packageName ->
                installedPackageNames.contains(packageName) &&
                blockedAppDao.getAppByPackageName(packageName)?.isBlocked != true &&
                !suggestedPackages.contains(packageName)
            }

            suggestedPackages.addAll(additionalSuggestions)

            android.util.Log.d(TAG, "Suggested ${suggestedPackages.size} social media apps to block")
            return@withContext suggestedPackages
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting social media app suggestions", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getSocialMediaBlockingStats(): AppBlockingManager.SocialMediaStats = withContext(Dispatchers.IO) {
        try {
            val allApps = blockedAppDao.getAllApps()
            val socialMediaApps = allApps.filter { app ->
                AppRegistry.isSocialMediaRelated(app.packageName)
            }

            val blockedSocialMediaApps = socialMediaApps.filter { it.isBlocked }

            // Calculate most used category
            val categoryCount = socialMediaApps.groupBy { app ->
                AppRegistry.getSocialMediaSubcategory(app.packageName) ?: "other"
            }.maxByOrNull { it.value.size }?.key ?: "other"

            // Calculate total blocked time (simplified - would need more complex logic for real implementation)
            val totalBlockedTime = blockedSocialMediaApps.sumOf { app ->
                // Simplified calculation - in reality you'd track actual usage time
                app.blockCount?.toLong() ?: 0L
            } * 60000L // Assume 1 minute per block

            return@withContext AppBlockingManager.SocialMediaStats(
                totalSocialMediaApps = socialMediaApps.size,
                blockedSocialMediaApps = blockedSocialMediaApps.size,
                mostUsedCategory = categoryCount,
                totalBlockedTime = totalBlockedTime
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting social media blocking stats", e)
            return@withContext AppBlockingManager.SocialMediaStats(0, 0, "unknown", 0L)
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

    /**
     * Block app using accessibility service by returning to home screen
     */
    private fun blockAppWithAccessibilityService(packageName: String): Boolean {
        return try {
            // Create intent to go to home screen
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            // Start home activity to effectively "close" the blocked app
            context.startActivity(homeIntent)
            
            runBlocking {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Blocked app $packageName by returning to home screen",
                    category = LogCategory.BLOCKING
                )
            }
            true
        } catch (e: Exception) {
            runBlocking {
                logRepository.logInfo(
                    tag = "AppBlockingManager",
                    message = "Error blocking app $packageName with accessibility service: ${e.message}",
                    category = LogCategory.BLOCKING
                )
            }
            false
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
