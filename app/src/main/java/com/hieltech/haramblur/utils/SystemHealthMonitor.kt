package com.hieltech.haramblur.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.hieltech.haramblur.data.models.SystemHealth
import com.hieltech.haramblur.data.models.HealthIssue
import com.hieltech.haramblur.data.models.IssueSeverity
import com.hieltech.haramblur.data.models.IssueCategory
import com.hieltech.haramblur.data.models.PerformanceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for monitoring system health indicators
 */
@Singleton
class SystemHealthMonitor @Inject constructor(
    private val context: Context
) {
    
    /**
     * Get current system health status
     */
    suspend fun getSystemHealth(): SystemHealth = withContext(Dispatchers.IO) {
        val batteryOptimized = isBatteryOptimized()
        val accessibilityServiceActive = isAccessibilityServiceActive()
        val permissionsGranted = getPermissionStatus()
        val performanceMetrics = getPerformanceMetrics()
        val criticalIssues = identifyCriticalIssues(batteryOptimized, accessibilityServiceActive, permissionsGranted, performanceMetrics)
        val warnings = identifyWarnings(batteryOptimized, accessibilityServiceActive, permissionsGranted, performanceMetrics)
        
        SystemHealth(
            batteryOptimized = batteryOptimized,
            accessibilityServiceActive = accessibilityServiceActive,
            permissionsGranted = permissionsGranted,
            performanceMetrics = performanceMetrics,
            criticalIssues = criticalIssues,
            warnings = warnings,
            lastChecked = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if battery optimization is disabled for the app
     */
    private fun isBatteryOptimized(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                !powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true // Assume optimized for older versions
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if accessibility service is active
     */
    private fun isAccessibilityServiceActive(): Boolean {
        return try {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            ) == 1
            
            if (!accessibilityEnabled) return false
            
            val accessibilityServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            accessibilityServices?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get permission status for required permissions
     */
    private fun getPermissionStatus(): Map<String, Boolean> {
        val requiredPermissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        return requiredPermissions.associateWith { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get current performance metrics
     */
    private fun getPerformanceMetrics(): PerformanceMetrics {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsage = usedMemory.toFloat() / maxMemory.toFloat()
        
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        
        return PerformanceMetrics(
            memoryUsage = memoryUsage,
            cpuUsage = 0.0f, // Would need additional implementation for CPU monitoring
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            networkLatency = 0L, // Would need network monitoring implementation
            storageSpace = getStorageUsage()
        )
    }
    
    /**
     * Get current battery level
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            100
        }
    }
    
    /**
     * Check if device is charging
     */
    private fun isDeviceCharging(): Boolean {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || 
            status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get storage usage percentage
     */
    private fun getStorageUsage(): Float {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val totalSpace = stat.blockCountLong * stat.blockSizeLong
            val freeSpace = stat.availableBlocksLong * stat.blockSizeLong
            val usedSpace = totalSpace - freeSpace
            usedSpace.toFloat() / totalSpace.toFloat()
        } catch (e: Exception) {
            0.0f
        }
    }
    
    /**
     * Identify critical issues that need immediate attention
     */
    private fun identifyCriticalIssues(
        batteryOptimized: Boolean,
        accessibilityServiceActive: Boolean,
        permissionsGranted: Map<String, Boolean>,
        performanceMetrics: PerformanceMetrics
    ): List<HealthIssue> {
        val issues = mutableListOf<HealthIssue>()
        
        // Check accessibility service
        if (!accessibilityServiceActive) {
            issues.add(
                HealthIssue(
                    id = "accessibility_service_inactive",
                    title = "Accessibility Service Inactive",
                    description = "The accessibility service is not active. This is required for face detection and content blocking.",
                    severity = IssueSeverity.CRITICAL,
                    category = IssueCategory.SERVICE,
                    canAutoFix = false,
                    actionRequired = "Enable accessibility service in device settings"
                )
            )
        }
        
        // Check critical permissions
        val criticalPermissions = listOf(
            android.Manifest.permission.CAMERA to "Camera permission is required for face detection",
            android.Manifest.permission.SYSTEM_ALERT_WINDOW to "System alert window permission is required for overlay functionality"
        )
        
        criticalPermissions.forEach { (permission, description) ->
            if (permissionsGranted[permission] != true) {
                issues.add(
                    HealthIssue(
                        id = "permission_${permission.substringAfterLast(".")}",
                        title = "Missing Permission",
                        description = description,
                        severity = IssueSeverity.CRITICAL,
                        category = IssueCategory.PERMISSION,
                        canAutoFix = false,
                        actionRequired = "Grant permission in app settings"
                    )
                )
            }
        }
        
        // Check memory usage
        if (performanceMetrics.memoryUsage > 0.9f) {
            issues.add(
                HealthIssue(
                    id = "high_memory_usage",
                    title = "High Memory Usage",
                    description = "Memory usage is critically high (${(performanceMetrics.memoryUsage * 100).toInt()}%). This may affect app performance.",
                    severity = IssueSeverity.CRITICAL,
                    category = IssueCategory.PERFORMANCE,
                    canAutoFix = true,
                    actionRequired = "Restart the app or clear memory"
                )
            )
        }
        
        // Check storage space
        if (performanceMetrics.storageSpace > 0.95f) {
            issues.add(
                HealthIssue(
                    id = "low_storage_space",
                    title = "Low Storage Space",
                    description = "Storage space is critically low (${((1 - performanceMetrics.storageSpace) * 100).toInt()}% free). This may affect app functionality.",
                    severity = IssueSeverity.CRITICAL,
                    category = IssueCategory.STORAGE,
                    canAutoFix = false,
                    actionRequired = "Free up storage space"
                )
            )
        }
        
        return issues
    }
    
    /**
     * Identify warnings that should be addressed
     */
    private fun identifyWarnings(
        batteryOptimized: Boolean,
        accessibilityServiceActive: Boolean,
        permissionsGranted: Map<String, Boolean>,
        performanceMetrics: PerformanceMetrics
    ): List<HealthIssue> {
        val warnings = mutableListOf<HealthIssue>()
        
        // Check battery optimization
        if (batteryOptimized) {
            warnings.add(
                HealthIssue(
                    id = "battery_optimization_enabled",
                    title = "Battery Optimization Enabled",
                    description = "Battery optimization is enabled for this app. This may affect background functionality.",
                    severity = IssueSeverity.MEDIUM,
                    category = IssueCategory.BATTERY,
                    canAutoFix = false,
                    actionRequired = "Disable battery optimization for better performance"
                )
            )
        }
        
        // Check memory usage
        if (performanceMetrics.memoryUsage > 0.8f && performanceMetrics.memoryUsage <= 0.9f) {
            warnings.add(
                HealthIssue(
                    id = "high_memory_usage_warning",
                    title = "High Memory Usage",
                    description = "Memory usage is high (${(performanceMetrics.memoryUsage * 100).toInt()}%). Consider restarting the app.",
                    severity = IssueSeverity.MEDIUM,
                    category = IssueCategory.PERFORMANCE,
                    canAutoFix = true,
                    actionRequired = "Restart the app"
                )
            )
        }
        
        // Check battery level
        if (performanceMetrics.batteryLevel < 20 && !performanceMetrics.isCharging) {
            warnings.add(
                HealthIssue(
                    id = "low_battery_warning",
                    title = "Low Battery",
                    description = "Battery level is low (${performanceMetrics.batteryLevel}%). This may affect app performance.",
                    severity = IssueSeverity.MEDIUM,
                    category = IssueCategory.BATTERY,
                    canAutoFix = false,
                    actionRequired = "Charge the device"
                )
            )
        }
        
        // Check storage space
        if (performanceMetrics.storageSpace > 0.85f && performanceMetrics.storageSpace <= 0.95f) {
            warnings.add(
                HealthIssue(
                    id = "low_storage_warning",
                    title = "Low Storage Space",
                    description = "Storage space is low (${((1 - performanceMetrics.storageSpace) * 100).toInt()}% free). Consider freeing up space.",
                    severity = IssueSeverity.MEDIUM,
                    category = IssueCategory.STORAGE,
                    canAutoFix = false,
                    actionRequired = "Free up storage space"
                )
            )
        }
        
        return warnings
    }
    
    /**
     * Get health recommendations
     */
    suspend fun getHealthRecommendations(): List<String> {
        val health = getSystemHealth()
        val recommendations = mutableListOf<String>()
        
        if (health.batteryOptimized) {
            recommendations.add("Disable battery optimization for better performance")
        }
        
        if (health.performanceMetrics.memoryUsage > 0.7f) {
            recommendations.add("Consider restarting the app to free up memory")
        }
        
        if (health.performanceMetrics.storageSpace > 0.8f) {
            recommendations.add("Free up storage space for better performance")
        }
        
        if (health.performanceMetrics.batteryLevel < 30 && !health.performanceMetrics.isCharging) {
            recommendations.add("Charge your device for optimal performance")
        }
        
        return recommendations
    }
    
    /**
     * Check if system is healthy
     */
    suspend fun isSystemHealthy(): Boolean {
        val health = getSystemHealth()
        return health.criticalIssues.isEmpty() && 
               health.getOverallHealthScore() > 0.7f
    }
}
