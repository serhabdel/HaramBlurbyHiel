package com.hieltech.haramblur.services

import android.content.Context
import android.util.Log
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrRepository
import com.hieltech.haramblur.data.DhikrSettings
import com.hieltech.haramblur.utils.DhikrPermissionHelper
import com.hieltech.haramblur.utils.DhikrDisplayMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DhikrManager @Inject constructor(
    private val dhikrRepository: DhikrRepository,
    private val permissionHelper: DhikrPermissionHelper,
    private val notificationManager: DhikrNotificationManager
) {
    
    companion object {
        private const val TAG = "DhikrManager"
    }
    
    private var context: Context? = null
    private var schedulerJob: Job? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    fun initialize(context: Context) {
        this.context = context
        Log.d(TAG, "DhikrManager initialized (notification-only mode)")
        
        // Check if dhikr should be shown immediately on startup
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                if (settings.enabled) {
                    Log.d(TAG, "Dhikr is enabled, checking if should show dhikr immediately")
                    checkAndShowDhikr()
                } else {
                    Log.d(TAG, "Dhikr is disabled in settings")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization dhikr check", e)
            }
        }
    }
    
    fun startScheduler() {
        schedulerJob?.cancel()
        schedulerJob = serviceScope.launch {
            Log.d(TAG, "Starting dhikr scheduler loop...")
            while (true) {
                try {
                    val settings = dhikrRepository.dhikrSettings.value
                    if (settings.enabled) {
                        checkAndShowDhikr()
                        
                        // Log status for debugging
                        val timeUntil = dhikrRepository.getTimeUntilNextDhikr()
                        if (timeUntil > 0) {
                            val minutesUntil = timeUntil / 1000 / 60
                            Log.d(TAG, "Next dhikr in $minutesUntil minutes")
                        } else {
                            Log.d(TAG, "Dhikr ready to be shown")
                        }
                    } else {
                        Log.v(TAG, "Dhikr disabled, skipping check")
                    }
                    
                    delay(60000) // Check every minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error in dhikr scheduler loop", e)
                    delay(60000) // Wait before retrying on error
                }
            }
        }
        Log.d(TAG, "Dhikr scheduler started")
    }
    
    // Overlay functionality removed - using notifications only
    @Deprecated("Overlay functionality removed, use showDhikrNotification instead")
    fun showDhikrOverlay(dhikr: Dhikr, settings: DhikrSettings) {
        Log.d(TAG, "Overlay display disabled, falling back to notification")
        showDhikrNotification(dhikr, settings)
    }
    
    // Overlay functionality removed
    @Deprecated("Overlay functionality removed")
    fun hideDhikrOverlay() {
        Log.d(TAG, "hideDhikrOverlay called but overlay functionality is disabled")
    }
    
    // Window layout params removed - overlay functionality disabled
    
    fun isOverlayActive(): Boolean = false // Overlay functionality disabled
    
    fun cleanup() {
        try {
            schedulerJob?.cancel()
            
            // Clear all references
            context = null
            
            Log.d(TAG, "DhikrManager cleaned up (notification-only mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    fun stopScheduler() {
        schedulerJob?.cancel()
        Log.d(TAG, "Dhikr scheduler stopped")
    }
    
    fun restartScheduler() {
        stopScheduler()
        startScheduler()
        Log.d(TAG, "Dhikr scheduler restarted")
    }
    
    /**
     * Manually trigger a dhikr display (for testing or user request)
     */
    fun showDhikrNow() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                if (!settings.enabled) {
                    Log.d(TAG, "Dhikr is disabled, cannot show now")
                    return@launch
                }
                
                val dhikr = dhikrRepository.getNextDhikr()
                if (dhikr != null) {
                    showDhikrNotification(dhikr, settings)
                } else {
                    Log.d(TAG, "No dhikr available for current time")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dhikr now", e)
            }
        }
    }
    
    /**
     * Force hide any visible dhikr (notification-only mode)
     */
    fun forceHide() {
        notificationManager.cancelDhikrNotification()
        Log.d(TAG, "Dhikr notification force hidden")
    }
    
    /**
     * Check if dhikr should be displayed and show it if conditions are met
     */
    fun checkAndShowDhikr() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                if (settings.enabled && dhikrRepository.shouldShowDhikr()) {
                    val dhikr = dhikrRepository.getNextDhikr()
                    if (dhikr != null) {
                        showDhikrNotification(dhikr, settings)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking and showing dhikr", e)
            }
        }
    }
    
    fun getDhikrStatus(): String {
        val settings = dhikrRepository.dhikrSettings.value
        return when {
            !settings.enabled -> "Dhikr disabled"
            dhikrRepository.shouldShowDhikr() -> "Ready to show dhikr"
            else -> {
                val timeUntil = dhikrRepository.getTimeUntilNextDhikr() / 1000 / 60
                "Next dhikr in ${timeUntil} minutes"
            }
        }
    }

    // ===== ENHANCED METHODS =====

    /**
     * Show dhikr using notification (overlay functionality removed)
     */
    fun showDhikrWithFallback(dhikr: Dhikr, settings: DhikrSettings) {
        Log.d(TAG, "Using notification display method for dhikr: ${dhikr.id}")
        if (permissionHelper.canShowNotification()) {
            showDhikrNotification(dhikr, settings)
        } else {
            Log.w(TAG, "No notification permission available for dhikr: ${dhikr.id}")
            showErrorNotification("Dhikr display unavailable - check permissions")
        }
    }

    /**
     * Show error notification when dhikr cannot be displayed
     */
    private fun showErrorNotification(message: String) {
        serviceScope.launch {
            try {
                val errorNotification = NotificationCompat.Builder(context!!, "dhikr_channel")
                    .setSmallIcon(com.hieltech.haramblur.R.drawable.ic_shield_islamic)
                    .setContentTitle("Dhikr Error")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                // Use the notification manager from the service
                val nm = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(System.currentTimeMillis().toInt(), errorNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show error notification", e)
            }
        }
    }

    /**
     * Show dhikr as notification
     */
    private fun showDhikrNotification(dhikr: Dhikr, settings: DhikrSettings) {
        serviceScope.launch {
            try {
                notificationManager.showDhikrNotification(dhikr, settings)
                dhikrRepository.showDhikr(dhikr)
                Log.d(TAG, "Dhikr notification shown: ${dhikr.id}")

                // Auto-dismiss notification after display duration
                delay(settings.displayDurationSeconds * 1000L)
                notificationManager.cancelDhikrNotification()
                dhikrRepository.hideDhikr()

            } catch (e: Exception) {
                Log.e(TAG, "Error showing dhikr notification", e)
            }
        }
    }

    /**
     * Enhanced scheduler with better error handling
     */
    private fun startEnhancedScheduler() {
        schedulerJob?.cancel()
        schedulerJob = serviceScope.launch {
            while (true) {
                try {
                    val settings = dhikrRepository.dhikrSettings.value
                    if (settings.enabled && dhikrRepository.shouldShowDhikr()) {
                        val dhikr = dhikrRepository.getNextDhikr()
                        if (dhikr != null) {
                            showDhikrWithFallback(dhikr, settings)
                        }
                    }

                    // Status notification updates disabled to prevent stuck notifications
                    // updateStatusNotification() - DISABLED

                    // Check every minute
                    delay(60_000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in enhanced dhikr scheduler", e)
                    delay(60_000L) // Wait before retrying
                }
            }
        }
        Log.d(TAG, "Enhanced dhikr scheduler started")
    }

    /**
     * Update status notification with current dhikr information
     */
    private fun updateStatusNotification() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                val dailyCount = dhikrRepository.getDailyDhikrCount()
                val timeUntil = dhikrRepository.getTimeUntilNextDhikr()
                val nextTimeText = if (timeUntil > 0) {
                    val minutes = timeUntil / 1000 / 60
                    "${minutes}m"
                } else {
                    "Now"
                }

                notificationManager.showStatusNotification(
                    nextDhikrTime = nextTimeText,
                    dailyCount = dailyCount,
                    isEnabled = settings.enabled
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status notification", e)
            }
        }
    }

    /**
     * Get comprehensive system status
     */
    fun getSystemStatus(): DhikrSystemStatus {
        val permissionStatus = permissionHelper.getPermissionStatus()
        val settings = dhikrRepository.dhikrSettings.value
        val dailyCount = dhikrRepository.getDailyDhikrCount()
        val timeUntil = dhikrRepository.getTimeUntilNextDhikr()

        return DhikrSystemStatus(
            isEnabled = settings.enabled,
            overlayPermissionGranted = permissionStatus.overlayGranted,
            notificationPermissionGranted = permissionStatus.notificationGranted,
            accessibilityServiceRunning = permissionStatus.accessibilityEnabled,
            isOverlayVisible = false, // Overlay functionality disabled
            dailyDhikrCount = dailyCount,
            timeUntilNextDhikr = timeUntil,
            currentTimeWindow = dhikrRepository.getCurrentTimeType(),
            recommendedDisplayMethod = permissionStatus.preferredMethod
        )
    }

    /**
     * Test dhikr display methods
     */
    fun testDhikrDisplay(displayMethod: DhikrDisplayMethod) {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                val dhikr = dhikrRepository.getNextDhikr() ?: return@launch

                // Only notification method available now
                showDhikrNotification(dhikr, settings)
            } catch (e: Exception) {
                Log.e(TAG, "Error testing dhikr display", e)
            }
        }
    }

    /**
     * Force show dhikr now (for testing)
     */
    fun forceShowDhikrNow() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                val dhikr = dhikrRepository.getNextDhikr()
                if (dhikr != null) {
                    showDhikrWithFallback(dhikr, settings)
                } else {
                    Log.d(TAG, "No dhikr available for current time")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forcing dhikr display", e)
            }
        }
    }

    /**
     * Enhanced cleanup with notification cleanup
     */
    fun enhancedCleanup() {
        try {
            schedulerJob?.cancel()
            notificationManager.cancelAllNotifications()
            Log.d(TAG, "Enhanced DhikrManager cleaned up (notification-only mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error during enhanced cleanup", e)
        }
    }

    /**
     * Get next dhikr in sequence for "Next" button functionality
     */
    fun getNextDhikrInSequence(): com.hieltech.haramblur.data.Dhikr? {
        return dhikrRepository.getNextDhikr()
    }

    /**
     * Get current dhikr settings for notification actions
     */
    fun getCurrentSettings(): com.hieltech.haramblur.data.DhikrSettings {
        return dhikrRepository.dhikrSettings.value
    }
    
    /**
     * Debug method to test dhikr notifications immediately (bypasses timing checks)
     */
    fun testDhikrNotificationNow() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                Log.d(TAG, "Testing dhikr notification now - enabled: ${settings.enabled}")
                
                if (!settings.enabled) {
                    Log.w(TAG, "Dhikr is disabled, cannot test notification")
                    showErrorNotification("Dhikr is disabled in settings")
                    return@launch
                }
                
                val dhikr = dhikrRepository.getNextDhikr() ?: run {
                    // Try to get any dhikr for testing
                    val allDhikr = dhikrRepository.getAllDhikr()
                    if (allDhikr.isNotEmpty()) {
                        allDhikr.random()
                    } else {
                        Log.w(TAG, "No dhikr available for testing")
                        showErrorNotification("No dhikr available for current time")
                        return@launch
                    }
                }
                
                Log.d(TAG, "Showing test dhikr: ${dhikr.id} - ${dhikr.time}")
                showDhikrNotification(dhikr, settings)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing dhikr notification", e)
                showErrorNotification("Error testing dhikr: ${e.message}")
            }
        }
    }
}

/**
 * Data class for comprehensive dhikr system status
 */
data class DhikrSystemStatus(
    val isEnabled: Boolean,
    val overlayPermissionGranted: Boolean,
    val notificationPermissionGranted: Boolean,
    val accessibilityServiceRunning: Boolean,
    val isOverlayVisible: Boolean,
    val dailyDhikrCount: Int,
    val timeUntilNextDhikr: Long,
    val currentTimeWindow: com.hieltech.haramblur.data.DhikrTime,
    val recommendedDisplayMethod: DhikrDisplayMethod
) {
    val canShowDhikr: Boolean
        get() = isEnabled && (overlayPermissionGranted || notificationPermissionGranted)

    val statusDescription: String
        get() = when {
            !isEnabled -> "Dhikr feature is disabled"
            !canShowDhikr -> "Missing permissions for dhikr display"
            isOverlayVisible -> "Dhikr overlay is currently displayed"
            timeUntilNextDhikr <= 0 -> "Ready to show next dhikr"
            else -> {
                val minutes = timeUntilNextDhikr / 1000 / 60
                "Next dhikr in $minutes minutes"
            }
        }
}