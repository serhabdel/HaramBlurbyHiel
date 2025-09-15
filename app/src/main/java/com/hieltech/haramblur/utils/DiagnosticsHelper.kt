package com.hieltech.haramblur.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.detection.AppBlockingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to help diagnose common HaramBlur issues
 */
object DiagnosticsHelper {
    
    private const val TAG = "DiagnosticsHelper"
    
    /**
     * Comprehensive system diagnostics
     */
    suspend fun runComprehensiveDiagnostics(
        context: Context,
        settingsRepository: SettingsRepository,
        siteBlockingManager: EnhancedSiteBlockingManager,
        appBlockingManager: AppBlockingManager
    ): DiagnosticReport = withContext(Dispatchers.Default) {
        
        Log.i(TAG, "Running comprehensive diagnostics...")
        
        val report = DiagnosticReport()
        
        // 1. Check accessibility service status
        report.accessibilityStatus = checkAccessibilityServiceStatus(context)
        
        // 2. Check overlay permission
        report.overlayPermissionStatus = checkOverlayPermission(context)
        
        // 3. Check settings persistence
        report.settingsStatus = checkSettingsPersistence(settingsRepository)
        
        // 4. Check site blocking database
        report.siteBlockingStatus = checkSiteBlockingDatabase(siteBlockingManager)
        
        // 5. Check app filtering
        report.appFilteringStatus = checkAppFiltering(appBlockingManager)
        
        // 6. Check service instance
        report.serviceInstanceStatus = checkServiceInstance()
        
        // 7. Performance checks
        report.performanceStatus = checkPerformance(context)
        
        Log.i(TAG, "Diagnostics complete. Issues found: ${report.getTotalIssues()}")
        
        return@withContext report
    }
    
    private fun checkAccessibilityServiceStatus(context: Context): AccessibilityStatus {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            
            val isEnabled = enabledServices.contains("com.hieltech.haramblur")
            val serviceInstance = HaramBlurAccessibilityService.getInstance()
            
            AccessibilityStatus(
                isEnabled = isEnabled,
                hasInstance = serviceInstance != null,
                isServiceRunning = HaramBlurAccessibilityService.isServiceRunning(),
                serviceStatus = serviceInstance?.getServiceStatus(),
                issues = mutableListOf<String>().apply {
                    if (!isEnabled) add("Accessibility service not enabled in system settings")
                    if (serviceInstance == null) add("Service instance is null")
                    if (!HaramBlurAccessibilityService.isServiceRunning()) add("Service not running")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status", e)
            AccessibilityStatus(
                isEnabled = false,
                hasInstance = false,
                isServiceRunning = false,
                serviceStatus = null,
                issues = listOf("Error checking accessibility service: ${e.message}")
            )
        }
    }
    
    private fun checkOverlayPermission(context: Context): OverlayPermissionStatus {
        return try {
            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true // Always granted on older versions
            }
            
            OverlayPermissionStatus(
                isGranted = hasPermission,
                issues = if (!hasPermission) listOf("Overlay permission not granted") else emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overlay permission", e)
            OverlayPermissionStatus(
                isGranted = false,
                issues = listOf("Error checking overlay permission: ${e.message}")
            )
        }
    }
    
    private suspend fun checkSettingsPersistence(settingsRepository: SettingsRepository): SettingsStatus {
        return try {
            val currentSettings = settingsRepository.getCurrentSettings()
            val isServicePaused = currentSettings.isServicePaused
            
            // Test settings persistence
            settingsRepository.toggleServicePause(!isServicePaused)
            kotlinx.coroutines.delay(100) // Allow time for persistence
            
            val updatedSettings = settingsRepository.getCurrentSettings()
            val persistenceWorks = updatedSettings.isServicePaused != isServicePaused
            
            // Restore original state
            settingsRepository.toggleServicePause(isServicePaused)
            
            SettingsStatus(
                canReadSettings = true,
                persistenceWorks = persistenceWorks,
                currentServicePaused = currentSettings.isServicePaused,
                issues = mutableListOf<String>().apply {
                    if (!persistenceWorks) add("Settings persistence not working properly")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking settings persistence", e)
            SettingsStatus(
                canReadSettings = false,
                persistenceWorks = false,
                currentServicePaused = false,
                issues = listOf("Error checking settings: ${e.message}")
            )
        }
    }
    
    private suspend fun checkSiteBlockingDatabase(siteBlockingManager: EnhancedSiteBlockingManager): SiteBlockingStatus {
        return try {
            // Test basic site blocking functionality
            val testUrl = "https://example.com"
            val result = siteBlockingManager.checkUrl(testUrl)
            
            val customSitesCount = try {
                var count = 0
                siteBlockingManager.getCustomBlockedWebsitesCount().collect { 
                    count = it
                    return@collect
                }
                count
            } catch (e: Exception) {
                -1 // Indicates error
            }
            
            val cacheStats = siteBlockingManager.getCacheStats()
            
            SiteBlockingStatus(
                canCheckUrls = true,
                customSitesCount = customSitesCount,
                cacheSize = cacheStats["cacheSize"] as? Int ?: 0,
                issues = mutableListOf<String>().apply {
                    if (customSitesCount == -1) add("Cannot access custom sites database")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking site blocking", e)
            SiteBlockingStatus(
                canCheckUrls = false,
                customSitesCount = -1,
                cacheSize = 0,
                issues = listOf("Error checking site blocking: ${e.message}")
            )
        }
    }
    
    private suspend fun checkAppFiltering(appBlockingManager: AppBlockingManager): AppFilteringStatus {
        return try {
            val installedApps = appBlockingManager.getInstalledApps()
            val appsCount = installedApps.size
            
            // Test filtering for common apps
            val chromePackage = "com.android.chrome"
            val isMonitored = appBlockingManager.isAppInMonitoredCategories(chromePackage)
            
            AppFilteringStatus(
                canGetInstalledApps = true,
                installedAppsCount = appsCount,
                testAppMonitored = isMonitored,
                issues = mutableListOf<String>().apply {
                    if (appsCount == 0) add("No installed apps detected - may indicate permission issue")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app filtering", e)
            AppFilteringStatus(
                canGetInstalledApps = false,
                installedAppsCount = 0,
                testAppMonitored = false,
                issues = listOf("Error checking app filtering: ${e.message}")
            )
        }
    }
    
    private fun checkServiceInstance(): ServiceInstanceStatus {
        return try {
            val instance = HaramBlurAccessibilityService.getInstance()
            val isRunning = HaramBlurAccessibilityService.isServiceRunning()
            val serviceStatus = instance?.getServiceStatus()
            
            ServiceInstanceStatus(
                hasInstance = instance != null,
                isRunning = isRunning,
                serviceDetails = serviceStatus,
                issues = mutableListOf<String>().apply {
                    if (instance == null) add("Service instance is null")
                    if (!isRunning) add("Service is not running")
                    serviceStatus?.let { status ->
                        if (status.lastError.isNotEmpty()) add("Service error: ${status.lastError}")
                        if (!status.isProcessingActive) add("Content processing is not active")
                        if (!status.isCapturingActive) add("Screen capture is not active")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service instance", e)
            ServiceInstanceStatus(
                hasInstance = false,
                isRunning = false,
                serviceDetails = null,
                issues = listOf("Error checking service instance: ${e.message}")
            )
        }
    }
    
    private fun checkPerformance(context: Context): PerformanceStatus {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
            
            PerformanceStatus(
                memoryUsagePercent = memoryUsagePercent,
                totalMemoryMB = (totalMemory / 1024 / 1024).toInt(),
                usedMemoryMB = (usedMemory / 1024 / 1024).toInt(),
                maxMemoryMB = (maxMemory / 1024 / 1024).toInt(),
                issues = mutableListOf<String>().apply {
                    if (memoryUsagePercent > 80) add("High memory usage: ${memoryUsagePercent.toInt()}%")
                    if ((usedMemory / 1024 / 1024) > 200) add("App using more than 200MB memory")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking performance", e)
            PerformanceStatus(
                memoryUsagePercent = 0f,
                totalMemoryMB = 0,
                usedMemoryMB = 0,
                maxMemoryMB = 0,
                issues = listOf("Error checking performance: ${e.message}")
            )
        }
    }
    
    /**
     * Generate a user-friendly report summary
     */
    fun generateReportSummary(report: DiagnosticReport): String {
        val summary = StringBuilder()
        summary.appendLine("=== HaramBlur Diagnostics Report ===")
        summary.appendLine()
        
        // Overall health
        val totalIssues = report.getTotalIssues()
        summary.appendLine("Overall Health: ${if (totalIssues == 0) "✅ Good" else "⚠️ ${totalIssues} issues found"}")
        summary.appendLine()
        
        // Accessibility Service
        summary.appendLine("🔧 Accessibility Service:")
        summary.appendLine("  Enabled: ${if (report.accessibilityStatus.isEnabled) "✅" else "❌"}")
        summary.appendLine("  Running: ${if (report.accessibilityStatus.isServiceRunning) "✅" else "❌"}")
        report.accessibilityStatus.issues.forEach { 
            summary.appendLine("  ⚠️ $it") 
        }
        summary.appendLine()
        
        // Overlay Permission
        summary.appendLine("🖼️ Overlay Permission:")
        summary.appendLine("  Granted: ${if (report.overlayPermissionStatus.isGranted) "✅" else "❌"}")
        report.overlayPermissionStatus.issues.forEach { 
            summary.appendLine("  ⚠️ $it") 
        }
        summary.appendLine()
        
        // Settings
        summary.appendLine("⚙️ Settings:")
        summary.appendLine("  Can Read: ${if (report.settingsStatus.canReadSettings) "✅" else "❌"}")
        summary.appendLine("  Persistence: ${if (report.settingsStatus.persistenceWorks) "✅" else "❌"}")
        summary.appendLine("  Service Paused: ${report.settingsStatus.currentServicePaused}")
        report.settingsStatus.issues.forEach { 
            summary.appendLine("  ⚠️ $it") 
        }
        summary.appendLine()
        
        // Site Blocking
        summary.appendLine("🚫 Site Blocking:")
        summary.appendLine("  Can Check URLs: ${if (report.siteBlockingStatus.canCheckUrls) "✅" else "❌"}")
        summary.appendLine("  Custom Sites: ${report.siteBlockingStatus.customSitesCount}")
        summary.appendLine("  Cache Size: ${report.siteBlockingStatus.cacheSize}")
        report.siteBlockingStatus.issues.forEach { 
            summary.appendLine("  ⚠️ $it") 
        }
        summary.appendLine()
        
        // Performance
        summary.appendLine("📊 Performance:")
        summary.appendLine("  Memory Usage: ${report.performanceStatus.memoryUsagePercent.toInt()}%")
        summary.appendLine("  Used Memory: ${report.performanceStatus.usedMemoryMB}MB")
        report.performanceStatus.issues.forEach { 
            summary.appendLine("  ⚠️ $it") 
        }
        
        return summary.toString()
    }
}

// Data classes for diagnostic results
data class DiagnosticReport(
    var accessibilityStatus: AccessibilityStatus = AccessibilityStatus(),
    var overlayPermissionStatus: OverlayPermissionStatus = OverlayPermissionStatus(),
    var settingsStatus: SettingsStatus = SettingsStatus(),
    var siteBlockingStatus: SiteBlockingStatus = SiteBlockingStatus(),
    var appFilteringStatus: AppFilteringStatus = AppFilteringStatus(),
    var serviceInstanceStatus: ServiceInstanceStatus = ServiceInstanceStatus(),
    var performanceStatus: PerformanceStatus = PerformanceStatus()
) {
    fun getTotalIssues(): Int {
        return accessibilityStatus.issues.size +
               overlayPermissionStatus.issues.size +
               settingsStatus.issues.size +
               siteBlockingStatus.issues.size +
               appFilteringStatus.issues.size +
               serviceInstanceStatus.issues.size +
               performanceStatus.issues.size
    }
}

data class AccessibilityStatus(
    val isEnabled: Boolean = false,
    val hasInstance: Boolean = false,
    val isServiceRunning: Boolean = false,
    val serviceStatus: HaramBlurAccessibilityService.ServiceStatus? = null,
    val issues: List<String> = emptyList()
)

data class OverlayPermissionStatus(
    val isGranted: Boolean = false,
    val issues: List<String> = emptyList()
)

data class SettingsStatus(
    val canReadSettings: Boolean = false,
    val persistenceWorks: Boolean = false,
    val currentServicePaused: Boolean = false,
    val issues: List<String> = emptyList()
)

data class SiteBlockingStatus(
    val canCheckUrls: Boolean = false,
    val customSitesCount: Int = 0,
    val cacheSize: Int = 0,
    val issues: List<String> = emptyList()
)

data class AppFilteringStatus(
    val canGetInstalledApps: Boolean = false,
    val installedAppsCount: Int = 0,
    val testAppMonitored: Boolean = false,
    val issues: List<String> = emptyList()
)

data class ServiceInstanceStatus(
    val hasInstance: Boolean = false,
    val isRunning: Boolean = false,
    val serviceDetails: HaramBlurAccessibilityService.ServiceStatus? = null,
    val issues: List<String> = emptyList()
)

data class PerformanceStatus(
    val memoryUsagePercent: Float = 0f,
    val totalMemoryMB: Int = 0,
    val usedMemoryMB: Int = 0,
    val maxMemoryMB: Int = 0,
    val issues: List<String> = emptyList()
)