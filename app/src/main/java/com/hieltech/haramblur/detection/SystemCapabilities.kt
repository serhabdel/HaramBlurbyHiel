package com.hieltech.haramblur.detection

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.hieltech.haramblur.data.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System capabilities detection utility for enhanced app blocking.
 *
 * This class manages detection of available blocking methods based on granted permissions
 * and system capabilities, enabling progressive enhancement of blocking strength.
 */
@Singleton
class SystemCapabilities @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devicePolicyManager: DevicePolicyManager
) {

    private val _capabilityFlow = MutableStateFlow(BlockingCapability.BASIC)
    val capabilityFlow: StateFlow<BlockingCapability> = _capabilityFlow.asStateFlow()

    private val _permissionStatusFlow = MutableStateFlow(PermissionStatus())
    val permissionStatusFlow: StateFlow<PermissionStatus> = _permissionStatusFlow.asStateFlow()

    // Simple caching mechanism
    private var lastCapabilityCheck: Long = 0
    private var cachedCapability: BlockingCapability? = null
    private val CACHE_DURATION_MS = 30000L // 30 seconds cache

    init {
        refreshCapabilities()
    }

    /**
     * Check if Usage Stats permission is granted
     */
    fun isUsageStatsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Check if Device Admin is active
     */
    fun isDeviceAdminActive(componentName: ComponentName): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }

    /**
     * Check if device owner (full admin capabilities)
     */
    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Get set of available blocking methods based on current permissions
     */
    fun getAvailableBlockingMethods(): Set<BlockingMethod> {
        val methods = mutableSetOf(BlockingMethod.ACCESSIBILITY_ONLY)

        if (isUsageStatsPermissionGranted()) {
            methods.add(BlockingMethod.FORCE_CLOSE_PREFERRED)
            methods.add(BlockingMethod.ADAPTIVE)
        }

        return methods
    }

    /**
     * Get blocking strength assessment based on actual capabilities with caching
     */
    fun getBlockingStrength(): BlockingCapability {
        val currentTime = System.currentTimeMillis()

        // Return cached value if still valid
        if (cachedCapability != null && (currentTime - lastCapabilityCheck) < CACHE_DURATION_MS) {
            return cachedCapability!!
        }

        // Calculate fresh capability assessment
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
        val isProfileOwner = devicePolicyManager.isProfileOwnerApp(context.packageName)
        val hasOwnerPrivileges = isDeviceOwner || isProfileOwner

        val capability = when {
            // MAXIMUM: Requires owner privileges (device or profile owner)
            hasOwnerPrivileges && canForceCloseApp("") -> BlockingCapability.MAXIMUM
            // ENHANCED: Has usage stats for monitoring but no owner privileges
            isUsageStatsPermissionGranted() -> BlockingCapability.ENHANCED
            // BASIC: Only accessibility service available
            else -> BlockingCapability.BASIC
        }

        // Update cache
        cachedCapability = capability
        lastCapabilityCheck = currentTime

        return capability
    }

    /**
     * Refresh capability assessment and update flows
     */
    fun refreshCapabilities() {
        // Clear cache to force fresh assessment
        cachedCapability = null
        lastCapabilityCheck = 0

        val capability = getBlockingStrength()
        val permissionStatus = checkPermissionStatus()

        _capabilityFlow.value = capability
        _permissionStatusFlow.value = permissionStatus
    }

    /**
     * Check if app can be force-closed with current capabilities
     * Requires device owner or profile owner privileges with appropriate API level
     */
    fun canForceCloseApp(packageName: String): Boolean {
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
        val isProfileOwner = devicePolicyManager.isProfileOwnerApp(context.packageName)

        return when {
            // Can use setPackagesSuspended (API 24+) with device/profile owner
            (isDeviceOwner || isProfileOwner) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> true
            // Can use setApplicationHidden (API 21+) with device owner only
            isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> true
            // Can use setApplicationSuspended (API 26+) with device/profile owner
            (isDeviceOwner || isProfileOwner) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> true
            // No owner privileges or API level too low
            else -> false
        }
    }

    /**
     * Get recommended blocking method for specific package
     */
    fun getRecommendedBlockingMethod(packageName: String): BlockingMethod {
        return when {
            canForceCloseApp(packageName) -> BlockingMethod.FORCE_CLOSE_PREFERRED
            isUsageStatsPermissionGranted() -> BlockingMethod.ADAPTIVE
            else -> BlockingMethod.ACCESSIBILITY_ONLY
        }
    }

    /**
     * Check permission status for all enhanced blocking permissions
     */
    fun checkPermissionStatus(): PermissionStatus {
        val componentName = ComponentName(context, HaramBlurDeviceAdminReceiver::class.java)
        return PermissionStatus(
            usageStatsGranted = isUsageStatsPermissionGranted(),
            deviceAdminActive = isDeviceAdminActive(componentName)
        )
    }

    /**
     * Get list of missing permissions for stronger blocking
     */
    fun getPermissionGaps(): List<String> {
        val gaps = mutableListOf<String>()

        if (!isUsageStatsPermissionGranted()) {
            gaps.add("PACKAGE_USAGE_STATS")
        }

        val componentName = ComponentName(context, HaramBlurDeviceAdminReceiver::class.java)
        if (!isDeviceAdminActive(componentName)) {
            gaps.add("DEVICE_ADMIN")
        }

        return gaps
    }



    /**
     * Explain benefits of specific permission
     */
    fun explainPermissionBenefits(permission: String): String {
        return when (permission) {
            "PACKAGE_USAGE_STATS" -> "Enables real-time detection of app launches for immediate blocking"
            "DEVICE_ADMIN" -> "Enables force-closing of blocked apps for stronger enforcement"
            else -> "Enhances app blocking capabilities"
        }
    }
}

/**
 * Blocking capability levels
 */
enum class BlockingCapability {
    BASIC,      // Accessibility service only
    ENHANCED,   // + Usage Stats for real-time detection
    MAXIMUM     // + Device Admin for force-close capabilities
}

/**
 * Blocking method preferences
 */
enum class BlockingMethod(val displayName: String, val description: String) {
    ACCESSIBILITY_ONLY("Accessibility Only", "Uses accessibility service for blocking (current method)"),
    FORCE_CLOSE_PREFERRED("Force Close Preferred", "Immediately closes blocked apps when possible"),
    ADAPTIVE("Adaptive", "Automatically uses the strongest available blocking method")
}

/**
 * Permission status data class
 */
data class PermissionStatus(
    val usageStatsGranted: Boolean = false,
    val deviceAdminActive: Boolean = false
)
