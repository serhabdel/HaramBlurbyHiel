package com.hieltech.haramblur.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for checking dhikr-related permissions and system capabilities
 */
@Singleton
class DhikrPermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "DhikrPermissionHelper"
    }

    /**
     * Check if overlay permission is granted
     */
    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Overlay permission not required before Android M
        }
    }

    /**
     * Check if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Check if accessibility service is running
     */
    fun isAccessibilityServiceRunning(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isEnabled
    }

    /**
     * Get comprehensive permission status
     */
    fun getPermissionStatus(): DhikrPermissionStatus {
        return DhikrPermissionStatus(
            overlayGranted = isOverlayPermissionGranted(),
            notificationGranted = isNotificationPermissionGranted(),
            accessibilityEnabled = isAccessibilityServiceRunning()
        )
    }

    /**
     * Check if dhikr can be displayed via overlay
     */
    fun canShowOverlay(): Boolean {
        return isOverlayPermissionGranted() && isAccessibilityServiceRunning()
    }

    /**
     * Check if dhikr can be displayed via notification
     */
    fun canShowNotification(): Boolean {
        return isNotificationPermissionGranted()
    }

    /**
     * Get recommended display method based on permissions
     */
    fun getRecommendedDisplayMethod(): DhikrDisplayMethod {
        val overlay = canShowOverlay()
        val notification = canShowNotification()
        val accessibility = isAccessibilityServiceRunning()

        android.util.Log.d("DhikrPermissionHelper", "Display method check - Overlay: $overlay, Notification: $notification, Accessibility: $accessibility")

        return when {
            overlay -> DhikrDisplayMethod.OVERLAY
            notification -> DhikrDisplayMethod.NOTIFICATION
            else -> {
                android.util.Log.w("DhikrPermissionHelper", "No display method available - Overlay: $overlay, Notification: $notification, Accessibility: $accessibility")
                DhikrDisplayMethod.NONE
            }
        }
    }
}

/**
 * Data class representing dhikr permission status
 */
data class DhikrPermissionStatus(
    val overlayGranted: Boolean,
    val notificationGranted: Boolean,
    val accessibilityEnabled: Boolean
) {
    val canShowAnything: Boolean
        get() = overlayGranted || notificationGranted

    val preferredMethod: DhikrDisplayMethod
        get() = when {
            overlayGranted && accessibilityEnabled -> DhikrDisplayMethod.OVERLAY
            notificationGranted -> DhikrDisplayMethod.NOTIFICATION
            else -> DhikrDisplayMethod.NONE
        }
}

/**
 * Enum representing available dhikr display methods
 */
enum class DhikrDisplayMethod {
    OVERLAY,        // System overlay (preferred)
    NOTIFICATION,   // Notification with expandable content
    NONE           // No display method available
}