package com.hieltech.haramblur.ui

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
     * Request Usage Stats permission with improved navigation
     */
    fun requestUsageStatsPermission(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(intent)

            Log.d("PermissionHelper", "Opened usage access settings for package: ${context.packageName}")
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Failed to open usage access settings", e)
            try {
                // Fallback to general usage access settings
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)

                Log.d("PermissionHelper", "Opened general usage access settings as fallback")
            } catch (fallbackException: Exception) {
                Log.e("PermissionHelper", "Failed to open general usage access settings", fallbackException)
                try {
                    // Last fallback to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${context.packageName}")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                } catch (lastException: Exception) {
                    Log.e("PermissionHelper", "Failed to open any settings for usage stats", lastException)
                    showPermissionError(activity, "Unable to open settings. Please manually enable Usage Access in Android Settings > Apps > Special access > Usage access > HaramBlur.")
                }
            }
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
     * Request Location permission with improved navigation
     */
    fun requestLocationPermission(activity: Activity) {
        try {
            // Try to open app-specific permission settings first
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(intent)

            Log.d("PermissionHelper", "Opened app settings for location permission")
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Failed to open app settings for location", e)
            try {
                // Fallback to location settings
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)

                Log.d("PermissionHelper", "Opened location settings as fallback")
            } catch (fallbackException: Exception) {
                Log.e("PermissionHelper", "Failed to open location settings", fallbackException)
                try {
                    // Last fallback to general settings
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    activity.startActivity(intent)
                } catch (lastException: Exception) {
                    Log.e("PermissionHelper", "Failed to open any settings for location", lastException)
                    showPermissionError(activity, "Unable to open settings. Please manually enable Location permission in Android Settings > Apps > HaramBlur > Permissions > Location.")
                }
            }
        }
    }

    /**
     * Check Accessibility Service permission status with multiple verification methods
     */
    fun checkAccessibilityServiceEnabled(): PermissionResult {
        try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

            val componentName = ComponentName(context, HaramBlurAccessibilityService::class.java)

            // Method 1: Check if service is in enabled services list
            val isInEnabledList = enabledServices.any { service ->
                service.resolveInfo.serviceInfo.packageName == componentName.packageName &&
                service.resolveInfo.serviceInfo.name == componentName.className
            }

            // Method 2: Check if service instance exists and is running
            val isServiceRunning = HaramBlurAccessibilityService.isServiceRunning()

            // Method 3: Check if accessibility is enabled globally
            val isAccessibilityEnabled = accessibilityManager.isEnabled

            // Consider service enabled if any method confirms it
            val isEnabled = isInEnabledList && isServiceRunning && isAccessibilityEnabled

            Log.d("PermissionHelper", "Accessibility check - InList: $isInEnabledList, Running: $isServiceRunning, Enabled: $isAccessibilityEnabled, Final: $isEnabled")

            return if (isEnabled) {
                PermissionResult.Granted("ACCESSIBILITY_SERVICE")
            } else {
                PermissionResult.Denied("ACCESSIBILITY_SERVICE")
            }
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Error checking accessibility service", e)
            return PermissionResult.Denied("ACCESSIBILITY_SERVICE")
        }
    }

    /**
     * Request Accessibility Service permission with error handling
     */
    fun requestAccessibilityService(activity: Activity) {
        try {
            // Try to open accessibility settings directly
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(intent)

            Log.d("PermissionHelper", "Opened accessibility settings successfully")
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Failed to open accessibility settings", e)
            try {
                // Fallback: Try to open app-specific settings where accessibility might be listed
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)

                Log.d("PermissionHelper", "Opened app settings as fallback")
            } catch (fallbackException: Exception) {
                Log.e("PermissionHelper", "Failed to open app settings fallback", fallbackException)
                try {
                    // Last fallback to general settings
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    activity.startActivity(intent)
                } catch (lastException: Exception) {
                    Log.e("PermissionHelper", "Failed to open any settings", lastException)
                    // Show error message
                    showPermissionError(activity, "Unable to open settings. Please manually enable Accessibility Service in Android Settings > Accessibility > HaramBlur.")
                }
            }
        }
    }

    /**
     * Show permission error message to user
     */
    private fun showPermissionError(activity: Activity, message: String) {
        // Show a Toast message to provide immediate user feedback
        try {
            android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_LONG).show()
            Log.w("PermissionHelper", "Permission error shown to user: $message")
        } catch (e: Exception) {
            // Fallback to just logging if Toast fails
            Log.w("PermissionHelper", "Permission error (Toast failed): $message", e)
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
     * Check Location permission status
     */
    fun checkLocationPermission(): PermissionResult {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (fineLocationGranted || coarseLocationGranted) {
            PermissionResult.Granted("LOCATION_PERMISSION")
        } else {
            PermissionResult.Denied("LOCATION_PERMISSION")
        }
    }

    /**
     * Check Notification permission status
     */
    fun checkNotificationPermission(): PermissionResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                PermissionResult.Granted("NOTIFICATION_PERMISSION")
            } else {
                PermissionResult.Denied("NOTIFICATION_PERMISSION")
            }
        } else {
            // For older Android versions, notifications are enabled by default
            PermissionResult.Granted("NOTIFICATION_PERMISSION")
        }
    }

    /**
     * Request Notification permission with improved navigation
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)

                Log.d("PermissionHelper", "Opened notification settings for package: ${context.packageName}")
            } catch (e: Exception) {
                Log.e("PermissionHelper", "Failed to open notification settings", e)
                try {
                    // Fallback to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${context.packageName}")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)

                    Log.d("PermissionHelper", "Opened app settings for notifications as fallback")
                } catch (fallbackException: Exception) {
                    Log.e("PermissionHelper", "Failed to open app settings for notifications", fallbackException)
                    try {
                        // Last fallback to general settings
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        activity.startActivity(intent)
                    } catch (lastException: Exception) {
                        Log.e("PermissionHelper", "Failed to open any settings for notifications", lastException)
                        showPermissionError(activity, "Unable to open settings. Please manually enable Notification permission in Android Settings > Apps > HaramBlur > Notifications.")
                    }
                }
            }
        } else {
            Log.d("PermissionHelper", "Notification permission not required for Android version < 13")
        }
    }

    /**
     * Update all permission statuses
     */
    fun updatePermissionStatuses() {
        val statuses = mapOf(
            "PACKAGE_USAGE_STATS" to checkUsageStatsPermission(),
            "DEVICE_ADMIN" to checkDeviceAdminPermission(),
            "ACCESSIBILITY_SERVICE" to checkAccessibilityServiceEnabled(),
            "LOCATION_PERMISSION" to checkLocationPermission(),
            "NOTIFICATION_PERMISSION" to checkNotificationPermission()
        )
        _permissionStatusFlow.value = statuses
    }

    /**
     * Retry permission check with delay for better reliability
     */
    suspend fun retryPermissionCheck(permissionType: String, maxRetries: Int = 3, delayMs: Long = 1000): PermissionResult {
        repeat(maxRetries) { attempt ->
            val result = when (permissionType) {
                "ACCESSIBILITY_SERVICE" -> checkAccessibilityServiceEnabled()
                "PACKAGE_USAGE_STATS" -> checkUsageStatsPermission()
                "DEVICE_ADMIN" -> checkDeviceAdminPermission()
                "LOCATION_PERMISSION" -> checkLocationPermission()
                else -> PermissionResult.Denied(permissionType)
            }

            if (result is PermissionResult.Granted) {
                return result
            }

            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(delayMs)
            }
        }

        // Return the last check result
        return when (permissionType) {
            "ACCESSIBILITY_SERVICE" -> checkAccessibilityServiceEnabled()
            "PACKAGE_USAGE_STATS" -> checkUsageStatsPermission()
            "DEVICE_ADMIN" -> checkDeviceAdminPermission()
            "LOCATION_PERMISSION" -> checkLocationPermission()
            else -> PermissionResult.Denied(permissionType)
        }
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
            "LOCATION_PERMISSION" -> {
                // Location permission can be permanently denied
                checkLocationPermission() is PermissionResult.Denied
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
        val locationGranted = checkLocationPermission() is PermissionResult.Granted
        val notificationGranted = checkNotificationPermission() is PermissionResult.Granted

        return EnhancedBlockingPermissionStatus(
            usageStatsGranted = usageStatsGranted,
            deviceAdminGranted = deviceAdminGranted,
            accessibilityServiceGranted = accessibilityGranted,
            locationGranted = locationGranted,
            notificationGranted = notificationGranted,
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
            "LOCATION_PERMISSION" -> PermissionExplanation(
                title = "Location Access",
                description = "Enables accurate prayer times and Islamic calendar for your location",
                benefits = listOf(
                    "Accurate prayer times for your city",
                    "Precise Qibla direction",
                    "Location-based Islamic calendar",
                    "Personalized Islamic features"
                )
            )
            "NOTIFICATION_PERMISSION" -> PermissionExplanation(
                title = "Notification Access",
                description = "Enables dhikr notifications and Islamic alerts",
                benefits = listOf(
                    "Dhikr reminders and notifications",
                    "Islamic prayer time alerts",
                    "Spiritual feature notifications",
                    "Complete Islamic app experience"
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
    val locationGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val isComplete: Boolean = false,
    val canUseEnhancedBlocking: Boolean = false,
    val canUseForceClose: Boolean = false
)
