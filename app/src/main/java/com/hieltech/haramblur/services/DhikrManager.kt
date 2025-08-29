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
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrPosition
import com.hieltech.haramblur.data.DhikrRepository
import com.hieltech.haramblur.data.DhikrSettings
import com.hieltech.haramblur.ui.components.DhikrOverlay
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DhikrManager @Inject constructor(
    private val dhikrRepository: DhikrRepository
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
        
        // Start the scheduler
        startScheduler()
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
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
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
}