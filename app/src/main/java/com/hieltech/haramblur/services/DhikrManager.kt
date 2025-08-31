package com.hieltech.haramblur.services

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrPosition
import com.hieltech.haramblur.data.DhikrRepository
import com.hieltech.haramblur.data.DhikrSettings
import com.hieltech.haramblur.ui.components.DhikrOverlay
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
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
    
    private var windowManager: WindowManager? = null
    private var context: Context? = null
    private var dhikrOverlayView: ComposeView? = null
    private var isOverlayVisible = false
    private var schedulerJob: Job? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    fun initialize(context: Context) {
        this.context = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "DhikrManager initialized")

        // Start the enhanced scheduler
        startEnhancedScheduler()
    }
    
    private fun startScheduler() {
        schedulerJob?.cancel()
        schedulerJob = serviceScope.launch {
            while (true) {
                try {
                    val settings = dhikrRepository.dhikrSettings.value
                    if (settings.enabled && dhikrRepository.shouldShowDhikr()) {
                        val dhikr = dhikrRepository.getNextDhikr()
                        if (dhikr != null && !isOverlayVisible) {
                            showDhikrOverlay(dhikr, settings)
                        }
                    }
                    // Check every minute
                    delay(60_000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in dhikr scheduler", e)
                    delay(60_000L) // Wait before retrying
                }
            }
        }
        Log.d(TAG, "Dhikr scheduler started")
    }
    
    fun showDhikrOverlay(dhikr: Dhikr, settings: DhikrSettings) {
        if (isOverlayVisible) {
            Log.d(TAG, "Dhikr overlay already visible, skipping")
            return
        }
        
        serviceScope.launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                dhikrOverlayView = ComposeView(context!!).apply {
                    // Use DisposeOnDetachedFromWindow for overlay views since they don't have a proper lifecycle owner
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        HaramBlurTheme {
                            var remainingTime by remember { mutableIntStateOf(settings.displayDurationSeconds) }
                            
                            // Countdown timer
                            LaunchedEffect(dhikr) {
                                while (remainingTime > 0) {
                                    delay(1000)
                                    remainingTime--
                                }
                            }
                            
                            DhikrOverlay(
                                dhikr = dhikr,
                                settings = settings,
                                onDismiss = { 
                                    hideDhikrOverlay()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                
                val params = createWindowLayoutParams(settings.displayPosition)
                
                windowManager!!.addView(dhikrOverlayView, params)
                isOverlayVisible = true
                
                // Update repository state
                dhikrRepository.showDhikr(dhikr)
                
                Log.d(TAG, "Dhikr overlay shown: ${dhikr.id}")
                
                // Auto-hide after duration
                serviceScope.launch {
                    delay(settings.displayDurationSeconds * 1000L + 500L) // Small buffer
                    if (isOverlayVisible) {
                        hideDhikrOverlay()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dhikr overlay", e)
            }
        }
    }
    
    fun hideDhikrOverlay() {
        serviceScope.launch {
            try {
                if (isOverlayVisible && dhikrOverlayView != null && windowManager != null) {
                    windowManager!!.removeView(dhikrOverlayView)
                    isOverlayVisible = false
                    dhikrOverlayView = null
                    
                    // Update repository state
                    dhikrRepository.hideDhikr()
                    
                    Log.d(TAG, "Dhikr overlay hidden")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding dhikr overlay", e)
                // Force reset state
                isOverlayVisible = false
                dhikrOverlayView = null
            }
        }
    }
    
    private fun createWindowLayoutParams(position: DhikrPosition): WindowManager.LayoutParams {
        val gravity = when (position) {
            DhikrPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
            DhikrPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
            DhikrPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            DhikrPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            DhikrPosition.CENTER -> Gravity.CENTER
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            // Add margins based on position
            when (position) {
                DhikrPosition.TOP_RIGHT, DhikrPosition.TOP_LEFT -> {
                    y = 100 // Top margin
                }
                DhikrPosition.BOTTOM_RIGHT, DhikrPosition.BOTTOM_LEFT -> {
                    y = -100 // Bottom margin
                }
                else -> {
                    // Center position
                }
            }
            x = when {
                position == DhikrPosition.TOP_RIGHT || position == DhikrPosition.BOTTOM_RIGHT -> -20
                position == DhikrPosition.TOP_LEFT || position == DhikrPosition.BOTTOM_LEFT -> 20
                else -> 0
            }
        }
    }
    
    fun isOverlayActive(): Boolean = isOverlayVisible
    
    fun cleanup() {
        try {
            schedulerJob?.cancel()
            if (isOverlayVisible) {
                hideDhikrOverlay()
            }
            Log.d(TAG, "DhikrManager cleaned up")
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
                    showDhikrOverlay(dhikr, settings)
                } else {
                    Log.d(TAG, "No dhikr available for current time")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dhikr now", e)
            }
        }
    }
    
    /**
     * Force hide any visible dhikr overlay
     */
    fun forceHide() {
        if (isOverlayVisible) {
            hideDhikrOverlay()
            Log.d(TAG, "Dhikr overlay force hidden")
        }
    }
    
    /**
     * Check if dhikr should be displayed and show it if conditions are met
     */
    fun checkAndShowDhikr() {
        serviceScope.launch {
            try {
                val settings = dhikrRepository.dhikrSettings.value
                if (settings.enabled && dhikrRepository.shouldShowDhikr() && !isOverlayVisible) {
                    val dhikr = dhikrRepository.getNextDhikr()
                    if (dhikr != null) {
                        showDhikrOverlay(dhikr, settings)
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
            isOverlayVisible -> "Dhikr currently displayed"
            dhikrRepository.shouldShowDhikr() -> "Ready to show dhikr"
            else -> {
                val timeUntil = dhikrRepository.getTimeUntilNextDhikr() / 1000 / 60
                "Next dhikr in ${timeUntil} minutes"
            }
        }
    }

    // ===== ENHANCED METHODS =====

    /**
     * Enhanced method that tries overlay first, then falls back to notification
     */
    fun showDhikrWithFallback(dhikr: Dhikr, settings: DhikrSettings) {
        val displayMethod = permissionHelper.getRecommendedDisplayMethod()

        when (displayMethod) {
            DhikrDisplayMethod.OVERLAY -> {
                Log.d(TAG, "Using overlay display method for dhikr: ${dhikr.id}")
                try {
                    showDhikrOverlay(dhikr, settings)
                } catch (e: Exception) {
                    Log.e(TAG, "Overlay display failed, falling back to notification", e)
                    showDhikrNotification(dhikr, settings)
                }
            }
            DhikrDisplayMethod.NOTIFICATION -> {
                Log.d(TAG, "Using notification display method for dhikr: ${dhikr.id}")
                showDhikrNotification(dhikr, settings)
            }
            DhikrDisplayMethod.NONE -> {
                Log.w(TAG, "No display method available for dhikr: ${dhikr.id}")
                showErrorNotification("Dhikr display unavailable - check permissions")
            }
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
                        if (dhikr != null && !isOverlayVisible) {
                            showDhikrWithFallback(dhikr, settings)
                        }
                    }

                    // Update status notification periodically
                    updateStatusNotification()

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
            isOverlayVisible = isOverlayVisible,
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

                when (displayMethod) {
                    DhikrDisplayMethod.OVERLAY -> showDhikrOverlay(dhikr, settings)
                    DhikrDisplayMethod.NOTIFICATION -> showDhikrNotification(dhikr, settings)
                    DhikrDisplayMethod.NONE -> Log.d(TAG, "No display method available")
                }
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
            if (isOverlayVisible) {
                hideDhikrOverlay()
            }
            notificationManager.cancelAllNotifications()
            Log.d(TAG, "Enhanced DhikrManager cleaned up")
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