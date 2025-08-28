package com.hieltech.haramblur.ui

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.detection.HaramBlurDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-friendly permission request helper for enhanced app blocking.
 *
 * This utility provides centralized permission management with clear user education
 * and step-by-step guidance for granting enhanced blocking permissions.
 */
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _permissionStatusFlow = MutableStateFlow<Map<String, PermissionResult>>(emptyMap())
    val permissionStatusFlow: StateFlow<Map<String, PermissionResult>> = _permissionStatusFlow.asStateFlow()

    /**
     * Request Usage Stats permission
     */
    fun requestUsageStatsPermission(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            activity.startActivity(intent)
        }
    }

    /**
     * Request Device Admin permission
     */
    fun requestDeviceAdminPermission(activity: Activity) {
        val componentName = ComponentName(context, HaramBlurDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Device Admin access enables stronger app blocking by allowing the app to force-close blocked applications.")
        activity.startActivity(intent)
    }

    /**
     * Check Accessibility Service permission status
     */
    fun checkAccessibilityServiceEnabled(): PermissionResult {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        val componentName = ComponentName(context, HaramBlurAccessibilityService::class.java)
        val isEnabled = enabledServices.any { service ->
            service.resolveInfo.serviceInfo.packageName == componentName.packageName &&
            service.resolveInfo.serviceInfo.name == componentName.className
        }

        return if (isEnabled) {
            PermissionResult.Granted("ACCESSIBILITY_SERVICE")
        } else {
            PermissionResult.Denied("ACCESSIBILITY_SERVICE")
        }
    }

    /**
     * Request Accessibility Service permission
     */
    fun requestAccessibilityService(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            activity.startActivity(intent)
        }
    }

    /**
     * Check Usage Stats permission status
     */
    fun checkUsageStatsPermission(): PermissionResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )

                when (mode) {
                    AppOpsManager.MODE_ALLOWED -> PermissionResult.Granted("PACKAGE_USAGE_STATS")
                    AppOpsManager.MODE_DEFAULT -> PermissionResult.Denied("PACKAGE_USAGE_STATS")
                    else -> PermissionResult.Denied("PACKAGE_USAGE_STATS")
                }
            } catch (e: Exception) {
                PermissionResult.Denied("PACKAGE_USAGE_STATS")
            }
        } else {
            PermissionResult.Denied("PACKAGE_USAGE_STATS")
        }
    }

    /**
     * Check Device Admin permission status
     */
    fun checkDeviceAdminPermission(): PermissionResult {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, HaramBlurDeviceAdminReceiver::class.java)

        return if (devicePolicyManager.isAdminActive(componentName)) {
            PermissionResult.Granted("DEVICE_ADMIN")
        } else {
            PermissionResult.Denied("DEVICE_ADMIN")
        }
    }

    /**
     * Update all permission statuses
     */
    fun updatePermissionStatuses() {
        val statuses = mapOf(
            "PACKAGE_USAGE_STATS" to checkUsageStatsPermission(),
            "DEVICE_ADMIN" to checkDeviceAdminPermission(),
            "ACCESSIBILITY_SERVICE" to checkAccessibilityServiceEnabled()
        )
        _permissionStatusFlow.value = statuses
    }

    /**
     * Check if permission is permanently denied
     */
    fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return when (permission) {
            "PACKAGE_USAGE_STATS" -> {
                // Usage Stats permission doesn't have "don't ask again" state
                // but we can check if user has denied it before
                checkUsageStatsPermission() is PermissionResult.Denied
            }
            "DEVICE_ADMIN" -> {
                // Device Admin permission also doesn't have "don't ask again"
                checkDeviceAdminPermission() is PermissionResult.Denied
            }
            "ACCESSIBILITY_SERVICE" -> {
                // Accessibility Service doesn't have "don't ask again" state
                checkAccessibilityServiceEnabled() is PermissionResult.Denied
            }
            else -> false
        }
    }

    /**
     * Request enhanced blocking permissions with guided flow
     * Returns the first permission to request (Usage Stats)
     */
    fun requestEnhancedBlockingPermissions(activity: Activity): PermissionRequestStep {
        // Start with Usage Stats permission
        requestUsageStatsPermission(activity)
        return PermissionRequestStep.USAGE_STATS_REQUESTED
    }

    /**
     * Continue enhanced blocking permission flow after first permission
     * Call this after returning from Usage Stats settings
     */
    fun continueEnhancedBlockingPermissionFlow(activity: Activity): PermissionRequestStep {
        return if (checkUsageStatsPermission() is PermissionResult.Granted) {
            // Usage Stats granted, now request Device Admin
            requestDeviceAdminPermission(activity)
            PermissionRequestStep.DEVICE_ADMIN_REQUESTED
        } else {
            // Usage Stats not granted
            PermissionRequestStep.USAGE_STATS_DENIED
        }
    }

    /**
     * Get the completion status of the enhanced blocking permission flow
     */
    fun getEnhancedBlockingPermissionStatus(): EnhancedBlockingPermissionStatus {
        val usageStatsGranted = checkUsageStatsPermission() is PermissionResult.Granted
        val deviceAdminGranted = checkDeviceAdminPermission() is PermissionResult.Granted
        val accessibilityGranted = checkAccessibilityServiceEnabled() is PermissionResult.Granted

        return EnhancedBlockingPermissionStatus(
            usageStatsGranted = usageStatsGranted,
            deviceAdminGranted = deviceAdminGranted,
            accessibilityServiceGranted = accessibilityGranted,
            isComplete = usageStatsGranted && accessibilityGranted, // Usage Stats and Accessibility are required
            canUseEnhancedBlocking = usageStatsGranted && accessibilityGranted,
            canUseForceClose = usageStatsGranted && deviceAdminGranted
        )
    }

    /**
     * Get the permission status flow for reactive UI updates
     */
    fun getEnhancedBlockingPermissionStatusFlow(): StateFlow<EnhancedBlockingPermissionStatus> {
        // Create a simple StateFlow that can be updated externally
        // In a real implementation, this would be more sophisticated
        return MutableStateFlow(getEnhancedBlockingPermissionStatus()).asStateFlow()
    }

    /**
     * Get explanation for permission benefits
     */
    fun getPermissionExplanation(permission: String): PermissionExplanation {
        return when (permission) {
            "PACKAGE_USAGE_STATS" -> PermissionExplanation(
                title = "Usage Stats Access",
                description = "Allows real-time detection of app launches for immediate blocking",
                benefits = listOf(
                    "Faster blocking response time",
                    "Real-time app launch monitoring",
                    "More reliable detection of blocked apps"
                )
            )
            "DEVICE_ADMIN" -> PermissionExplanation(
                title = "Device Admin Access",
                description = "Enables force-closing of blocked apps for stronger enforcement",
                benefits = listOf(
                    "Force-close blocked applications",
                    "Stronger blocking enforcement",
                    "More effective app restriction"
                )
            )
            "ACCESSIBILITY_SERVICE" -> PermissionExplanation(
                title = "Accessibility Service",
                description = "Enables real-time content detection across all apps for comprehensive protection",
                benefits = listOf(
                    "Real-time content monitoring",
                    "Automatic blur application",
                    "Cross-app protection coverage",
                    "Enhanced content detection capabilities"
                )
            )
            else -> PermissionExplanation(
                title = "Enhanced Permission",
                description = "Enhances app blocking capabilities",
                benefits = emptyList()
            )
        }
    }
}

/**
 * Permission result sealed class
 */
sealed class PermissionResult(open val permission: String) {
    data class Granted(override val permission: String) : PermissionResult(permission)
    data class Denied(override val permission: String) : PermissionResult(permission)
    data class PermanentlyDenied(override val permission: String) : PermissionResult(permission)
    data class SystemSettingsRequired(override val permission: String) : PermissionResult(permission)
}

/**
 * Permission explanation data class
 */
data class PermissionExplanation(
    val title: String,
    val description: String,
    val benefits: List<String>
)

/**
 * Permission request step for guided flow
 */
enum class PermissionRequestStep {
    USAGE_STATS_REQUESTED,
    DEVICE_ADMIN_REQUESTED,
    USAGE_STATS_DENIED,
    COMPLETED
}

/**
 * Enhanced blocking permission status
 */
data class EnhancedBlockingPermissionStatus(
    val usageStatsGranted: Boolean = false,
    val deviceAdminGranted: Boolean = false,
    val accessibilityServiceGranted: Boolean = false,
    val isComplete: Boolean = false,
    val canUseEnhancedBlocking: Boolean = false,
    val canUseForceClose: Boolean = false
)
