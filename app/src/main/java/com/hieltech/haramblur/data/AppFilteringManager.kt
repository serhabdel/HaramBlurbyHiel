package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.models.DetectionScope
import com.hieltech.haramblur.data.models.DetectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for app-specific detection filtering
 * Provides high-level API for determining which apps should be monitored
 */
@Singleton
class AppFilteringManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appCategoryDetector: AppCategoryDetector,
    private val applicationScope: CoroutineScope
) {

    /**
     * Flow that emits the current detection scope based on settings
     */
    val detectionScopeFlow: StateFlow<DetectionScope> = settingsRepository.settings.map { settings ->
        DetectionScope(
            mode = if (!settings.enableAppSpecificDetection) {
                DetectionMode.ALL_APPS
            } else if (settings.monitoredAppCategories.isEmpty()) {
                DetectionMode.DISABLED
            } else {
                DetectionMode.SPECIFIC_CATEGORIES
            },
            monitoredCategories = settings.monitoredAppCategories,
            customIncludedApps = settings.customMonitoredApps,
            excludedApps = settings.excludedApps
        )
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DetectionScope(
            mode = DetectionMode.ALL_APPS,
            monitoredCategories = emptySet(),
            customIncludedApps = emptySet(),
            excludedApps = emptySet()
        )
    )

    /**
     * Check if a specific app should be monitored for content detection
     */
    suspend fun shouldMonitorApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) {
            // Return true for null/blank apps only when monitoring all apps
            return detectionScopeFlow.value.mode == DetectionMode.ALL_APPS
        }

        val currentScope = detectionScopeFlow.value
        return currentScope.shouldMonitorApp(packageName)
    }

    /**
     * Check if a specific app should be monitored (synchronous version)
     */
    fun shouldMonitorAppSync(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) {
            // Return true for null/blank apps only when monitoring all apps
            return detectionScopeFlow.value.mode == DetectionMode.ALL_APPS
        }

        return detectionScopeFlow.value.shouldMonitorApp(packageName)
    }

    /**
     * Get the category of an app
     */
    fun getAppCategory(packageName: String): AppCategory? {
        return appCategoryDetector.determineAppCategory(packageName)
    }

    /**
     * Check if detection is completely disabled
     */
    fun isDetectionDisabled(): Boolean {
        return detectionScopeFlow.value.isDetectionDisabled()
    }

    /**
     * Check if monitoring all apps (no filtering)
     */
    fun isMonitoringAllApps(): Boolean {
        return detectionScopeFlow.value.isMonitoringAllApps()
    }

    /**
     * Get all package names that should be monitored
     */
    fun getMonitoredPackageNames(): Set<String> {
        return detectionScopeFlow.value.getMonitoredPackageNames()
    }

    /**
     * Check if an app is explicitly excluded from monitoring
     */
    fun isAppExcluded(packageName: String): Boolean {
        return detectionScopeFlow.value.excludedApps.contains(packageName)
    }

    /**
     * Check if an app is in a monitored category
     */
    fun isAppInMonitoredCategory(packageName: String): Boolean {
        val category = getAppCategory(packageName) ?: return false
        return detectionScopeFlow.value.monitoredCategories.contains(category)
    }

    /**
     * Check if an app is a custom monitored app
     */
    fun isCustomMonitoredApp(packageName: String): Boolean {
        return detectionScopeFlow.value.customIncludedApps.contains(packageName)
    }

    /**
     * Get monitoring reason for an app (for logging/debugging)
     */
    fun getMonitoringReason(packageName: String): String {
        val scope = detectionScopeFlow.value

        return when {
            scope.isDetectionDisabled() -> "detection_disabled"
            scope.isMonitoringAllApps() -> "monitoring_all_apps"
            isAppExcluded(packageName) -> "explicitly_excluded"
            isCustomMonitoredApp(packageName) -> "custom_monitored_app"
            isAppInMonitoredCategory(packageName) -> "in_monitored_category_${getAppCategory(packageName)?.name?.lowercase()}"
            else -> "not_monitored"
        }
    }

    /**
     * Get statistics about current monitoring scope
     */
    fun getMonitoringStats(): MonitoringStats {
        val scope = detectionScopeFlow.value
        val monitoredPackages = scope.getMonitoredPackageNames()

        return MonitoringStats(
            mode = scope.mode,
            totalMonitoredApps = if (scope.isMonitoringAllApps()) -1 else monitoredPackages.size,
            monitoredCategories = scope.monitoredCategories.size,
            customApps = scope.customIncludedApps.size,
            excludedApps = scope.excludedApps.size
        )
    }

    /**
     * Check if app filtering settings are properly configured
     */
    fun isFilteringConfigured(): Boolean {
        val scope = detectionScopeFlow.value
        return when (scope.mode) {
            DetectionMode.ALL_APPS -> true
            DetectionMode.SPECIFIC_CATEGORIES -> scope.monitoredCategories.isNotEmpty() || scope.customIncludedApps.isNotEmpty()
            DetectionMode.DISABLED -> true
        }
    }

    /**
     * Get recommended apps to monitor based on high-risk categories
     */
    fun getRecommendedAppsToMonitor(): Set<String> {
        val highRiskCategories = setOf(
            AppCategory.SOCIAL_MEDIA,
            AppCategory.BROWSERS,
            AppCategory.DATING
        )
        return appCategoryDetector.getAllPackageNamesForCategories(highRiskCategories)
    }

    /**
     * Update monitored app categories
     */
    suspend fun updateMonitoredCategories(categories: Set<AppCategory>) {
        settingsRepository.updateMonitoredAppCategories(categories)
    }

    /**
     * Add custom app to monitoring
     */
    suspend fun addCustomMonitoredApp(packageName: String) {
        val currentApps = detectionScopeFlow.value.customIncludedApps
        settingsRepository.updateCustomMonitoredApps(currentApps + packageName)
    }

    /**
     * Remove custom app from monitoring
     */
    suspend fun removeCustomMonitoredApp(packageName: String) {
        val currentApps = detectionScopeFlow.value.customIncludedApps
        settingsRepository.updateCustomMonitoredApps(currentApps - packageName)
    }

    /**
     * Add app to exclusion list
     */
    suspend fun excludeApp(packageName: String) {
        val currentExcluded = detectionScopeFlow.value.excludedApps
        settingsRepository.updateExcludedApps(currentExcluded + packageName)
    }

    /**
     * Remove app from exclusion list
     */
    suspend fun includeApp(packageName: String) {
        val currentExcluded = detectionScopeFlow.value.excludedApps
        settingsRepository.updateExcludedApps(currentExcluded - packageName)
    }
}

/**
 * Data class for monitoring statistics
 */
data class MonitoringStats(
    val mode: DetectionMode,
    val totalMonitoredApps: Int, // -1 means all apps
    val monitoredCategories: Int,
    val customApps: Int,
    val excludedApps: Int
)
