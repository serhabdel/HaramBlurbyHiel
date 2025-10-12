package com.hieltech.haramblur.accessibility

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.data.IslamicGuidance
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.data.WarningDialogState
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.SiteBlockingResult
import com.hieltech.haramblur.ui.components.BlockedSiteDialog
import com.hieltech.haramblur.ui.components.PornBlockingDialog
import com.hieltech.haramblur.ui.components.WarningDialog
import com.hieltech.haramblur.ui.components.WarningDialogManager
import com.hieltech.haramblur.ui.components.BlurAnimationUtils
import com.hieltech.haramblur.ui.effects.EnhancedBlurEffects
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import com.hieltech.haramblur.detection.Language
import android.animation.ValueAnimator
import android.view.ViewPropertyAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlurOverlayManager @Inject constructor(
    private val warningDialogManager: WarningDialogManager,
    private val settingsRepository: com.hieltech.haramblur.data.SettingsRepository
) : LifecycleOwner, SavedStateRegistryOwner {
    
    private val enhancedBlurEffects = EnhancedBlurEffects()
    
    companion object {
        private const val TAG = "BlurOverlayManager"
        private const val DEFAULT_BLUR_INTENSITY = 50f
        private const val STRONG_BLUR_ALPHA = 220 // More opaque
    }
    
    // Lifecycle management for ComposeView overlays
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private var windowManager: WindowManager? = null
    private var context: Context? = null
    private var overlayView: BlurOverlayView? = null
    private var warningOverlayView: ComposeView? = null
    private var blockedSiteOverlayView: ComposeView? = null
    private var isOverlayVisible = false
    private var isWarningVisible = false
    private var isBlockedSiteOverlayVisible = false

    // Auto-close timer for full screen blur
    private var fullScreenBlurJob: kotlinx.coroutines.Job? = null
    private val FULL_SCREEN_BLUR_TIMEOUT = 10000L // 10 seconds (reduced from 30)
    
    // Stuck overlay detection
    private var stuckOverlayDetectionJob: kotlinx.coroutines.Job? = null
    private var lastOverlayCheckTime = 0L
    private var consecutiveStuckDetections = 0
    
    // Overlay health monitoring
    private var overlayHealthMonitorJob: kotlinx.coroutines.Job? = null
    private var overlayCreationTime = 0L
    private var overlayHealthCheckCount = 0
    
    // Animation and performance optimization
    private var currentAnimator: ViewPropertyAnimator? = null
    private var regionTransitionAnimator: ValueAnimator? = null
    private var isAnimating = false
    private var lastFrameTime = 0L
    private val FRAME_TIME_THRESHOLD = 16L // 60fps target
    
    // Cached blur regions for smooth transitions
    private var cachedBlurRegions: List<Rect> = emptyList()
    private var cachedBlurIntensity: BlurIntensity? = null
    private var cachedBlurStyle: BlurStyle? = null
    private var cachedContentSensitivity: Float = 0.5f
    
    // Pending regions for deferred updates when frame skipping
    private var pendingRegions: List<Rect>? = null
    private var pendingBlurIntensity: BlurIntensity? = null
    private var pendingBlurStyle: BlurStyle? = null
    private var pendingContentSensitivity: Float? = null
    private var deferredUpdateJob: kotlinx.coroutines.Job? = null
    
    // Performance monitoring
    private var overlayRenderTime = 0L
    private var frameDropCount = 0
    private var lastPerformanceLogTime = 0L
    
    // Navigation callback for automatic actions
    var onNavigateAwayAction: (() -> Unit)? = null
    
    // Callback for warning dialog actions
    var onWarningAction: ((WarningDialogAction) -> Unit)? = null

    /**
     * Safe execution wrapper to prevent crashes from taking down the accessibility service
     */
    private fun safeExecute(operation: String, action: () -> Unit): Boolean {
        return try {
            action()
            true
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during operation: $operation", e)
            // Emergency cleanup on OOM
            emergencyMemoryCleanup()
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during operation: $operation", e)
            false
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Bad token during operation: $operation - service likely stopping", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state during operation: $operation", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Safe execution failed for operation: $operation", e)
            false
        }
    }
    
    /**
     * Safe execution with retry capability
     */
    private fun safeExecuteWithRetry(operation: String, maxRetries: Int = 2, action: () -> Unit): Boolean {
        repeat(maxRetries) { attempt ->
            val success = safeExecute("$operation (attempt ${attempt + 1})", action)
            if (success) return true
            
            if (attempt < maxRetries - 1) {
                Thread.sleep(100) // Brief delay before retry
            }
        }
        Log.e(TAG, "All retry attempts failed for operation: $operation")
        return false
    }
    
    /**
     * Emergency memory cleanup for overlay manager
     */
    private fun emergencyMemoryCleanup() {
        Log.w(TAG, "🧹 Emergency memory cleanup in overlay manager")
        try {
            // Force immediate cleanup of all overlays
            overlayView = null
            warningOverlayView = null
            blockedSiteOverlayView = null
            
            // Reset all state flags
            isOverlayVisible = false
            isWarningVisible = false
            isBlockedSiteOverlayVisible = false
            
            // Cancel timers
            cancelFullScreenBlurTimer()
            stuckOverlayDetectionJob?.cancel()
            
            // Force GC
            System.gc()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency memory cleanup", e)
        }
    }
    
    /**
     * Start overlay health monitoring to detect issues early
     */
    private fun startOverlayHealthMonitoring() {
        overlayHealthMonitorJob?.cancel()
        overlayCreationTime = System.currentTimeMillis()
        overlayHealthCheckCount = 0
        
        overlayHealthMonitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (isOverlayVisible) {
                try {
                    delay(5000L) // Check every 5 seconds
                    performOverlayHealthCheck()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in overlay health monitoring", e)
                    delay(10000L) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Perform health check on active overlay
     */
    private fun performOverlayHealthCheck() {
        overlayHealthCheckCount++
        val currentTime = System.currentTimeMillis()
        val overlayAge = currentTime - overlayCreationTime
        
        try {
            // Check if overlay view still exists and is valid
            val view = overlayView
            if (view == null) {
                Log.w(TAG, "⚠️ Overlay health check: view is null but state says visible")
                isOverlayVisible = false
                return
            }
            
            // Check if view is attached to window
            val isAttached = view.isAttachedToWindow
            if (!isAttached) {
                Log.w(TAG, "⚠️ Overlay health check: view is not attached to window")
                // Try to recover
                hideBlurOverlay()
                return
            }
            
            // Check for stuck overlay (visible too long)
            if (overlayAge > 300000L) { // 5 minutes
                Log.w(TAG, "⚠️ Overlay health check: overlay has been visible for ${overlayAge/1000}s - potentially stuck")
                emergencyHideAllOverlays()
                return
            }
            
            // Validate window manager state
            if (windowManager == null) {
                Log.w(TAG, "⚠️ Overlay health check: window manager is null")
                isOverlayVisible = false
                overlayView = null
                return
            }
            
            Log.v(TAG, "✅ Overlay health check #$overlayHealthCheckCount passed - age: ${overlayAge/1000}s")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during overlay health check", e)
            // On error, try emergency cleanup
            emergencyHideAllOverlays()
        }
    }
    
    /**
     * Stop overlay health monitoring
     */
    private fun stopOverlayHealthMonitoring() {
        overlayHealthMonitorJob?.cancel()
        overlayHealthMonitorJob = null
        overlayHealthCheckCount = 0
        Log.v(TAG, "Overlay health monitoring stopped")
    }

    /**
     * Clean up all overlays safely
     */
    fun cleanupAllOverlays() {
        safeExecute("cleanupAllOverlays") {
            if (isOverlayVisible) {
                hideBlurOverlay()
            }
            if (isWarningVisible) {
                hideWarningDialog()
            }
            if (isBlockedSiteOverlayVisible) {
                hideBlockedSiteOverlay()
            }
        }
    }

    fun initialize(context: Context) {
        this.context = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Initialize lifecycle components
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        // Start proactive stuck overlay detection
        startStuckOverlayDetection()
        
        Log.d(TAG, "Blur overlay manager initialized with lifecycle support and stuck detection")
    }
    
    /**
     * Show blur overlay with smooth animations and performance optimizations
     */
    fun showBlurOverlay(
        blurRegions: List<Rect>,
        blurIntensity: BlurIntensity? = null,
        blurStyle: BlurStyle? = null,
        contentSensitivity: Float = 0.5f,
        transparency: Float? = null, // Use AppSettings blur intensity if not provided
        smoothTransition: Boolean = true,
        animationDuration: Long = BlurAnimationUtils.OVERLAY_FADE_DURATION
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                // Get current user settings
                val currentSettings = settingsRepository.settings.value
                val userBlurIntensity = blurIntensity ?: currentSettings.blurIntensity
                val userBlurStyle = blurStyle ?: currentSettings.blurStyle
                val userTransparency = transparency ?: (currentSettings.blurIntensity.alphaValue / 255f) // Use blur intensity alpha value as transparency
                
                if (isOverlayVisible) {
                    if (smoothTransition) {
                        updateBlurRegionsSmooth(blurRegions, userBlurIntensity, userBlurStyle, contentSensitivity, animationDuration)
                    } else {
                        updateBlurOverlay(blurRegions, userBlurIntensity, userBlurStyle, contentSensitivity)
                    }
                    return@launch
                }
                
                // Get actual screen dimensions for precise scaling
                val displayMetrics = context!!.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                
                // Scale and validate regions to actual screen resolution
                val scaledRegions = blurRegions.mapNotNull { region ->
                    val scaledRect = Rect(
                        maxOf(0, region.left),
                        maxOf(0, region.top),
                        minOf(screenWidth, region.right),
                        minOf(screenHeight, region.bottom)
                    )
                    
                    // Only include meaningful regions (not too small)
                    if (scaledRect.width() >= 20 && scaledRect.height() >= 20) {
                        scaledRect
                    } else null
                }
                
                if (scaledRegions.isEmpty()) {
                    Log.w(TAG, "❌ No valid scaled regions - skipping blur overlay")
                    return@launch
                }
                
                Log.w(TAG, "✅ Creating BlurOverlayView with ${scaledRegions.size} regions")
                Log.w(TAG, "   Screen resolution: ${screenWidth}x${screenHeight}")
                scaledRegions.forEachIndexed { index, rect ->
                    Log.w(TAG, "   Scaled region $index: [${rect.left},${rect.top}] to [${rect.right},${rect.bottom}] = ${rect.width()}x${rect.height()}")
                }
                
                overlayView = BlurOverlayView(
                    context!!,
                    scaledRegions,
                    userBlurIntensity,
                    userBlurStyle,
                    contentSensitivity,
                    userTransparency,
                    isFullScreen = false,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    triggeredByRegionCount = false,
                    regionCount = 0,
                    maxConfidence = 0.0f,
                    appSettings = currentSettings
                )
                
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, // Enable hardware acceleration
                    PixelFormat.TRANSLUCENT
                )
                
                params.gravity = Gravity.TOP or Gravity.START
                
                // Enable hardware acceleration on the overlay view
                if (settingsRepository.settings.value.enableHardwareBlurAcceleration) {
                    BlurAnimationUtils.enableHardwareAcceleration(overlayView as View, isOverlayView = true)
                    Log.d(TAG, "Hardware acceleration enabled for blur overlay (RenderEffect skipped)")
                }
                
                val success = safeExecuteWithRetry("add_blur_overlay") {
                    windowManager!!.addView(overlayView, params)
                }
                
                if (success) {
                    isOverlayVisible = true
                    Log.w(TAG, "✅✅✅ BLUR OVERLAY SUCCESSFULLY ADDED TO WINDOW MANAGER ✅✅✅")
                    Log.w(TAG, "🎯 PRECISION BLUR: ${scaledRegions.size} regions on ${screenWidth}x${screenHeight} screen")
                    Log.w(TAG, "   Overlay visible flag: $isOverlayVisible")
                    Log.w(TAG, "   Overlay view attached: ${overlayView?.isAttachedToWindow}")
                    
                    // Apply smooth fade-in animation if enabled
                    if (smoothTransition && settingsRepository.settings.value.enableSmoothBlurAnimations) {
                        BlurAnimationUtils.createFadeInAnimation(
                            overlayView!!,
                            animationDuration
                        ) {
                            Log.d(TAG, "Blur overlay fade-in animation completed")
                        }
                    }
                    
                    // Start overlay health monitoring
                    startOverlayHealthMonitoring()
                    
                    // Start performance monitoring
                    startPerformanceMonitoring()
                } else {
                    Log.e(TAG, "Failed to add blur overlay after retries")
                    isOverlayVisible = false
                    overlayView = null
                    return@launch
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing blur overlay", e)
            }
        }
    }
    
    fun updateBlurOverlay(
        blurRegions: List<Rect>,
        blurIntensity: BlurIntensity? = null,
        blurStyle: BlurStyle? = null,
        contentSensitivity: Float = 0.5f,
        transparency: Float = 0.8f
    ) {
        // Get current user settings
        val currentSettings = settingsRepository.settings.value
        val userBlurIntensity = blurIntensity ?: currentSettings.blurIntensity
        val userBlurStyle = blurStyle ?: currentSettings.blurStyle
        
        overlayView?.updateBlurRegions(blurRegions, userBlurIntensity, userBlurStyle, contentSensitivity, transparency)
        Log.d(TAG, "Blur overlay updated with ${blurRegions.size} regions")
    }
    
    fun hideBlurOverlay(smoothTransition: Boolean = true) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Attempting to hide blur overlay - isVisible: $isOverlayVisible, overlayView: ${overlayView != null}")

                // Apply smooth fade-out animation if enabled
                if (smoothTransition && settingsRepository.settings.value.enableSmoothBlurAnimations && overlayView != null) {
                    BlurAnimationUtils.createFadeOutAnimation(
                        overlayView!!,
                        BlurAnimationUtils.OVERLAY_FADE_DURATION
                    ) {
                        // Animation complete - now remove the view
                        removeOverlayView()
                    }
                } else {
                    // Immediate removal
                    removeOverlayView()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical error hiding blur overlay", e)
                // Force reset state even on error
                isOverlayVisible = false
                overlayView = null
            }
        }
    }
    
    /**
     * Remove overlay view with safety checks
     */
    private fun removeOverlayView() {
        try {
            // Force hide any visible overlay with safety wrapper
            if (isOverlayVisible && overlayView != null && windowManager != null) {
                val success = safeExecuteWithRetry("remove_blur_overlay") {
                    windowManager!!.removeView(overlayView)
                }
                
                if (success) {
                    Log.d(TAG, "✅ Blur overlay view removed from window")
                } else {
                    Log.w(TAG, "⚠️ Failed to remove overlay view safely - attempting emergency cleanup")
                    // Try more aggressive cleanup
                    try {
                        windowManager?.removeViewImmediate(overlayView)
                        Log.d(TAG, "✅ Blur overlay removed immediately")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Emergency overlay removal also failed", e)
                    }
                }
            }

            // Also try to hide full-screen blur if it exists
            hideFullScreenBlur()

            // Stop health monitoring
            stopOverlayHealthMonitoring()
            
            // Stop performance monitoring
            stopPerformanceMonitoring()
            
            // Reset all state
            isOverlayVisible = false
            overlayView = null
            Log.d(TAG, "✅ Blur overlay hidden and state reset")

        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view", e)
            isOverlayVisible = false
            overlayView = null
        }
    }

    fun hideFullScreenBlur() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Hiding full-screen blur overlay")

                // Try to remove any remaining overlay views
                try {
                    if (windowManager != null) {
                        // Note: We can't check if overlayView is full-screen since it's private
                        // Just ensure all overlay states are reset
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error in full-screen overlay cleanup", e)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error hiding full-screen blur", e)
            }
        }
    }
    
    fun isOverlayActive(): Boolean = isOverlayVisible
    
    /**
     * Update blur regions with smooth animation and interpolation
     * Enhanced with frame skipping and deferred updates
     */
    fun updateBlurRegionsSmooth(
        newRegions: List<Rect>,
        blurIntensity: BlurIntensity? = null,
        blurStyle: BlurStyle? = null,
        contentSensitivity: Float = 0.5f,
        animationDuration: Long = BlurAnimationUtils.REGION_TRANSITION_DURATION
    ) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Get current user settings
            val currentSettings = settingsRepository.settings.value
            val userBlurIntensity = blurIntensity ?: currentSettings.blurIntensity
            val userBlurStyle = blurStyle ?: currentSettings.blurStyle
            
            // Check if we should skip this update for performance
            if (BlurAnimationUtils.shouldSkipFrame()) {
                Log.d(TAG, "Skipping blur region update for performance - scheduling deferred update")
                
                // Store pending regions for deferred update
                pendingRegions = newRegions
                pendingBlurIntensity = userBlurIntensity
                pendingBlurStyle = userBlurStyle
                pendingContentSensitivity = contentSensitivity
                
                // Schedule a single deferred update via Handler/coroutine delay
                scheduleDeferredBlurUpdate()
                return
            }
            
            // Use ValueAnimator for smooth region transitions
            regionTransitionAnimator?.cancel()
            
            val oldRegions = cachedBlurRegions
            val optimizedRegions = BlurAnimationUtils.optimizeRegionTransitions(newRegions)
            
            regionTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = animationDuration
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    
                    // Interpolate between old and new regions
                    val interpolatedRegions = if (oldRegions.isNotEmpty()) {
                        BlurAnimationUtils.interpolateBlurRegions(oldRegions, optimizedRegions, progress)
                    } else {
                        optimizedRegions
                    }
                    
                    // Update overlay with interpolated regions
                    overlayView?.updateBlurRegions(
                        interpolatedRegions,
                        userBlurIntensity,
                        userBlurStyle,
                        contentSensitivity
                    )
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Final update with actual regions
                        updateBlurOverlay(optimizedRegions, userBlurIntensity, userBlurStyle, contentSensitivity)
                        isAnimating = false
                        
                        val frameTime = System.currentTimeMillis() - startTime
                        logPerformanceMetrics(frameTime)
                    }
                    
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        isAnimating = false
                    }
                })
                start()
            }
            
            isAnimating = true
            Log.d(TAG, "Started smooth blur region transition animation")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in smooth region transition, falling back to direct update", e)
            updateBlurOverlay(newRegions, blurIntensity, blurStyle, contentSensitivity)
        }
    }
    
    /**
     * Animate blur intensity change
     */
    fun animateBlurIntensityChange(
        fromIntensity: BlurIntensity,
        toIntensity: BlurIntensity,
        duration: Long = BlurAnimationUtils.INTENSITY_CHANGE_DURATION
    ) {
        try {
            BlurAnimationUtils.createIntensityTransition(
                fromAlpha = fromIntensity.alphaValue,
                toAlpha = toIntensity.alphaValue,
                duration = duration
            ) { alpha ->
                // Update overlay with interpolated alpha
                overlayView?.let { view ->
                    // Create intermediate intensity based on alpha
                    val intermediateIntensity = when {
                        alpha >= 240 -> BlurIntensity.MAXIMUM
                        alpha >= 180 -> BlurIntensity.STRONG
                        alpha >= 120 -> BlurIntensity.MEDIUM
                        else -> BlurIntensity.LIGHT
                    }
                    
                    view.updateBlurRegions(
                        cachedBlurRegions,
                        intermediateIntensity,
                        cachedBlurStyle ?: BlurStyle.PIXELATED,
                        cachedContentSensitivity
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error animating blur intensity change", e)
            // Fallback to direct intensity update
            updateBlurOverlay(cachedBlurRegions, toIntensity, cachedBlurStyle, cachedContentSensitivity)
        }
    }
    

    /**
     * Start the auto-close timer for full screen blur with enhanced navigation
     */
    private fun startFullScreenBlurTimer() {
        // Cancel any existing timer
        cancelFullScreenBlurTimer()

        fullScreenBlurJob = CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "⏰ Starting 10-second auto-action timer for full screen blur")
            delay(FULL_SCREEN_BLUR_TIMEOUT)

            // Check if overlay is still visible and is full screen
            if (isOverlayVisible && overlayView?.isFullScreenBlur == true) {
                Log.w(TAG, "⏰ Auto-action triggered after 10 seconds - navigating away from inappropriate content")
                try {
                    // First attempt: Navigate away from inappropriate content
                    onNavigateAwayAction?.invoke()
                    
                    // Give navigation time to complete
                    delay(2000L)
                    
                    // If still visible after navigation, force hide overlay
                    if (isOverlayVisible) {
                        Log.w(TAG, "⏰ Navigation completed - hiding overlay")
                        hideFullScreenBlur()
                        hideFullScreenWarning()
                    }

                    Log.w(TAG, "⏰ Full screen blur auto-closed and navigated away after 10 seconds")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in auto-close timer", e)
                    // Emergency fallback
                    emergencyHideAllOverlays()
                }
            }
        }
    }

    /**
     * Cancel the auto-close timer for full screen blur
     */
    private fun cancelFullScreenBlurTimer() {
        fullScreenBlurJob?.cancel()
        fullScreenBlurJob = null
    }



    /**
     * Emergency method to force hide ALL overlays
     * Call this if overlays get stuck or appear on lock screen
     */
    fun emergencyHideAllOverlays() {
        try {
            Log.w(TAG, "🚨 EMERGENCY: Force hiding all overlays - IMMEDIATE EXECUTION")

            // Cancel any running timers first
            cancelFullScreenBlurTimer()

            // AGGRESSIVE CLEANUP - Remove all views immediately
            windowManager?.let { wm ->
                // Hide main blur overlay
                overlayView?.let { view ->
                    try {
                        wm.removeViewImmediate(view)
                        Log.d(TAG, "✅ Main overlay removed immediately")
                    } catch (e: Exception) {
                        try {
                            wm.removeView(view)
                            Log.d(TAG, "✅ Main overlay removed normally after immediate failed")
                        } catch (e2: Exception) {
                            Log.w(TAG, "❌ Failed to remove main overlay: ${e2.message}")
                        }
                    }
                }

                // Hide warning overlay
                warningOverlayView?.let { view ->
                    try {
                        wm.removeViewImmediate(view)
                        Log.d(TAG, "✅ Warning overlay removed immediately")
                    } catch (e: Exception) {
                        try {
                            wm.removeView(view)
                            Log.d(TAG, "✅ Warning overlay removed normally after immediate failed")
                        } catch (e2: Exception) {
                            Log.w(TAG, "❌ Failed to remove warning overlay: ${e2.message}")
                        }
                    }
                }

                // Hide blocked site overlay
                blockedSiteOverlayView?.let { view ->
                    try {
                        wm.removeViewImmediate(view)
                        Log.d(TAG, "✅ Blocked site overlay removed immediately")
                    } catch (e: Exception) {
                        try {
                            wm.removeView(view)
                            Log.d(TAG, "✅ Blocked site overlay removed normally after immediate failed")
                        } catch (e2: Exception) {
                            Log.w(TAG, "❌ Failed to remove blocked site overlay: ${e2.message}")
                        }
                    }
                }
            }

            // FORCE RESET ALL STATES IMMEDIATELY
            isOverlayVisible = false
            isWarningVisible = false
            isBlockedSiteOverlayVisible = false
            overlayView = null
            warningOverlayView = null
            blockedSiteOverlayView = null

            // Move lifecycle to STARTED to prevent stuck composition
            try {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reset lifecycle state: ${e.message}")
            }

            Log.w(TAG, "🚨 EMERGENCY CLEANUP COMPLETED - All overlays forcibly removed")

        } catch (e: Exception) {
            Log.e(TAG, "💥 CRITICAL ERROR in emergency hide", e)
            // ABSOLUTE FALLBACK - Force reset all states
            isOverlayVisible = false
            isWarningVisible = false
            isBlockedSiteOverlayVisible = false
            overlayView = null
            warningOverlayView = null
            blockedSiteOverlayView = null
        }
    }

    /**
     * Manual emergency cleanup trigger - can be called from accessibility service
     * This should be called when the service detects the app is stuck/unresponsive
     */
    fun forceEmergencyCleanup() {
        try {
            Log.e(TAG, "🚨 MANUAL EMERGENCY CLEANUP TRIGGERED - Forcing immediate overlay removal")
            
            // Reset stuck detection counters
            lastOverlayCheckTime = 0L
            consecutiveStuckDetections = 0
            
            // Force emergency cleanup
            emergencyHideAllOverlays()
            
            // Restart stuck detection
            startStuckOverlayDetection()
            
            Log.w(TAG, "🚨 Manual emergency cleanup completed - System should be responsive now")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 CRITICAL: Manual emergency cleanup failed", e)
        }
    }

    /**
     * Start proactive stuck overlay detection - runs every 5 seconds
     * This is our insurance policy against stuck overlays
     */
    private fun startStuckOverlayDetection() {
        stuckOverlayDetectionJob?.cancel()
        stuckOverlayDetectionJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                try {
                    delay(5000L) // Check every 5 seconds
                    detectAndCleanStuckOverlays()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in stuck overlay detection loop", e)
                    delay(10000L) // Wait longer on error
                }
            }
        }
        Log.d(TAG, "🛡️ Stuck overlay detection started - checking every 5 seconds")
    }

    /**
     * Detect and clean stuck overlays - aggressive detection logic
     */
    private fun detectAndCleanStuckOverlays() {
        try {
            val currentTime = System.currentTimeMillis()
            val hasAnyOverlay = isOverlayVisible || isWarningVisible || isBlockedSiteOverlayVisible
            
            if (hasAnyOverlay) {
                // Check if overlay has been visible for too long
                if (lastOverlayCheckTime == 0L) {
                    lastOverlayCheckTime = currentTime
                    consecutiveStuckDetections = 0
                } else {
                    val overlayDuration = currentTime - lastOverlayCheckTime
                    
                    // If overlay has been visible for more than 30 seconds, it's likely stuck
                    if (overlayDuration > 30000L) {
                        consecutiveStuckDetections++
                        Log.w(TAG, "⚠️ Detected potentially stuck overlay - duration: ${overlayDuration}ms, detections: $consecutiveStuckDetections")
                        
                        // If we've detected this for 3+ consecutive checks (15+ seconds), force cleanup
                        if (consecutiveStuckDetections >= 3) {
                            Log.e(TAG, "🚨 STUCK OVERLAY CONFIRMED - Forcing emergency cleanup!")
                            emergencyHideAllOverlays()
                            lastOverlayCheckTime = 0L
                            consecutiveStuckDetections = 0
                        }
                    } else {
                        // Overlay duration is reasonable, reset counters
                        consecutiveStuckDetections = 0
                    }
                }
            } else {
                // No overlays visible, reset tracking
                lastOverlayCheckTime = 0L
                consecutiveStuckDetections = 0
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in stuck overlay detection", e)
        }
    }
    
    /**
     * Check if overlays should be hidden due to app/context change
     * Call this when window state changes or when app goes to background
     */
    fun checkForStuckOverlays(currentPackageName: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // If we have active overlays and the app has changed, hide them
                if ((isOverlayVisible || isWarningVisible || isBlockedSiteOverlayVisible) && 
                    currentPackageName != null && 
                    !isAppRelatedToHaramBlur(currentPackageName)) {
                    
                    Log.w(TAG, "🔄 App changed to $currentPackageName - checking for stuck overlays")
                    
                    // If it's a launcher or system app, definitely hide overlays
                    if (isLauncherOrSystemApp(currentPackageName)) {
                        Log.w(TAG, "🏠 User went to launcher/system - hiding all overlays")
                        emergencyHideAllOverlays()
                    }
                    // If overlay has been visible for more than 60 seconds, hide it
                    else if (overlayView?.isFullScreenBlur == true) {
                        Log.w(TAG, "⚠️ Full screen blur detected in different app context - hiding for user safety")
                        emergencyHideAllOverlays()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for stuck overlays", e)
            }
        }
    }
    
    /**
     * Check if the package is related to HaramBlur functionality
     */
    private fun isAppRelatedToHaramBlur(packageName: String): Boolean {
        return packageName.contains("haramblur", ignoreCase = true) ||
               packageName.contains("com.hieltech", ignoreCase = true)
    }
    
    /**
     * Check if the package is a launcher or system app
     */
    private fun isLauncherOrSystemApp(packageName: String): Boolean {
        val systemApps = setOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.launcher",
            "com.samsung.android.launcher",
            "com.huawei.android.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
            "android",
            "com.android.systemui",
            "com.android.settings"
        )
        
        return systemApps.any { packageName.contains(it, ignoreCase = true) } ||
               packageName.contains("launcher", ignoreCase = true) ||
               packageName.contains("home", ignoreCase = true) ||
               packageName.startsWith("android")
    }
    
    /**
     * Show full-screen warning overlay with blur background
     * Enhanced with region-based trigger information
     */
    fun showFullScreenWarning(
        category: BlockingCategory,
        customMessage: String? = null,
        quranicVerse: QuranicVerse? = null,
        reflectionTimeSeconds: Int? = null,
        // NEW: Region-based trigger information
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f,
        triggeredByRegionCount: Boolean = false
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                // First show full-screen blur
                showFullScreenBlur()
                
                // Then show warning dialog overlay with region-based information
                showWarningDialog(category, customMessage, quranicVerse, reflectionTimeSeconds, nsfwRegionCount, maxNsfwConfidence, triggeredByRegionCount)
                
                Log.d(TAG, "Full-screen warning shown for category: ${category.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing full-screen warning", e)
            }
        }
    }
    
    /**
     * Show full-screen blur without warning dialog
     * Enhanced with region-based trigger information
     */
    fun showFullScreenBlur(
        triggeredByRegionCount: Boolean = false,
        regionCount: Int = 0,
        maxConfidence: Float = 0.0f
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                if (isOverlayVisible) {
                    hideBlurOverlay()
                }
                
                // Create full-screen blur region
                val displayMetrics = context!!.resources.displayMetrics
                val fullScreenRect = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
                
                Log.d(TAG, "Creating full-screen blur - Region trigger: $triggeredByRegionCount, Count: $regionCount, Max confidence: $maxConfidence")

                val currentSettings = settingsRepository.settings.value
                overlayView = BlurOverlayView(
                    context!!,
                    listOf(fullScreenRect),
                    BlurIntensity.MAXIMUM,
                    BlurStyle.COMBINED,
                    1.0f, // contentSensitivity
                    1.0f, // transparency
                    isFullScreen = true,
                    screenWidth = displayMetrics.widthPixels,
                    screenHeight = displayMetrics.heightPixels,
                    triggeredByRegionCount = triggeredByRegionCount,
                    regionCount = regionCount,
                    maxConfidence = maxConfidence,
                    appSettings = currentSettings
                )
                
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                
                params.gravity = Gravity.TOP or Gravity.START
                
                windowManager!!.addView(overlayView, params)
                isOverlayVisible = true

                Log.d(TAG, "Full-screen blur overlay shown")

                // Start auto-close timer for full screen blur (30 seconds)
                startFullScreenBlurTimer()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing full-screen blur", e)
            }
        }
    }

    /**
     * Log blur events for analytics and debugging
     */
    private fun logBlurEvent(triggeredByRegionCount: Boolean, regionCount: Int, maxConfidence: Float) {
        val triggerType = if (triggeredByRegionCount) "REGION_BASED" else "STANDARD"
        Log.i(TAG, "🔒 BLUR_EVENT: type=$triggerType, regions=$regionCount, confidence=$maxConfidence")

        // TODO: Send to analytics service if implemented
    }

    /**
     * Show warning dialog overlay
     * Enhanced with region-based trigger information
     */
    private fun showWarningDialog(
        category: BlockingCategory,
        customMessage: String? = null,
        quranicVerse: QuranicVerse? = null,
        reflectionTimeSeconds: Int? = null,
        // NEW: Region-based trigger information
        nsfwRegionCount: Int = 0,
        maxNsfwConfidence: Float = 0.0f,
        triggeredByRegionCount: Boolean = false
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isWarningVisible) {
                    hideWarningDialog()
                }
                
                val (title, message, verse) = warningDialogManager.createFullScreenWarning(category, customMessage)
                val actualVerse = quranicVerse ?: verse
                val actualReflectionTime = reflectionTimeSeconds ?: warningDialogManager.getReflectionTimeForCategory(category)
                
                // Move to RESUMED state for active overlay
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                
                warningOverlayView = ComposeView(context!!).apply {
                    // Set lifecycle owners for proper Compose management
                    setViewTreeLifecycleOwner(this@BlurOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlurOverlayManager)
                    
                    // Use proper composition strategy
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    
                    setContent {
                         HaramBlurTheme(preferredLanguage = Language.ENGLISH) {
                             var dialogState by remember {
                                mutableStateOf(
                                    WarningDialogState(
                                        isVisible = true,
                                        title = title,
                                        message = message,
                                        quranicVerse = actualVerse,
                                        reflectionTimeSeconds = actualReflectionTime,
                                        remainingTimeSeconds = actualReflectionTime,
                                        canContinue = false,
                                        category = category
                                    )
                                )
                            }
                            
                            // Countdown timer effect
                            LaunchedEffect(dialogState.reflectionTimeSeconds) {
                                if (dialogState.remainingTimeSeconds > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    dialogState = dialogState.copy(
                                        remainingTimeSeconds = dialogState.remainingTimeSeconds - 1,
                                        canContinue = dialogState.remainingTimeSeconds <= 1
                                    )
                                }
                            }
                            
                            WarningDialog(
                                state = dialogState,
                                onAction = { action ->
                                    when (action) {
                                        is WarningDialogAction.Close -> {
                                            hideFullScreenWarning()
                                            onWarningAction?.invoke(action)
                                        }
                                        is WarningDialogAction.Continue -> {
                                            if (dialogState.canContinue) {
                                                hideFullScreenWarning()
                                                onWarningAction?.invoke(action)
                                            }
                                        }
                                        is WarningDialogAction.Dismiss -> {
                                            if (dialogState.canContinue) {
                                                hideFullScreenWarning()
                                            }
                                            onWarningAction?.invoke(action)
                                        }
                                        is WarningDialogAction.ChangeLanguage -> {
                                            dialogState = dialogState.copy(language = action.language)
                                            onWarningAction?.invoke(action)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                
                params.gravity = Gravity.TOP or Gravity.START
                
                windowManager!!.addView(warningOverlayView, params)
                isWarningVisible = true
                
                Log.d(TAG, "Warning dialog overlay shown with lifecycle support")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing warning dialog", e)
            }
        }
    }
    
    /**
     * Hide warning dialog overlay
     */
    fun hideWarningDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isWarningVisible && warningOverlayView != null && windowManager != null) {
                    windowManager!!.removeView(warningOverlayView)
                    isWarningVisible = false
                    warningOverlayView = null
                    
                    // Move back to STARTED state when overlay is hidden
                    if (!isOverlayVisible && !isBlockedSiteOverlayVisible) {
                        lifecycleRegistry.currentState = Lifecycle.State.STARTED
                    }
                    
                    Log.d(TAG, "Warning dialog overlay hidden")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding warning dialog", e)
            }
        }
    }
    
    /**
     * Hide full-screen warning (both blur and dialog)
     */
    fun hideFullScreenWarning() {
        // Cancel the auto-close timer
        cancelFullScreenBlurTimer()

        hideWarningDialog()
        hideBlurOverlay()
        Log.d(TAG, "Full-screen warning hidden")
    }
    
    /**
     * Check if warning dialog is currently visible
     */
    fun isWarningVisible(): Boolean = isWarningVisible
    
    /**
     * Cleanup lifecycle when service is destroyed
     */
    fun cleanup() {
        try {
            // Stop stuck overlay detection first
            stuckOverlayDetectionJob?.cancel()
            stuckOverlayDetectionJob = null
            
            // Emergency hide all overlays
            emergencyHideAllOverlays()
            
            // Cancel full screen blur timer
            cancelFullScreenBlurTimer()
            
            // Move lifecycle to destroyed state
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            
            // Clear all references
            context = null
            windowManager = null
            
            Log.d(TAG, "BlurOverlayManager cleaned up with stuck detection stopped and lifecycle teardown")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Show blocked site overlay with Quranic verse display
     */
    fun showBlockedSiteOverlay(
        blockingResult: SiteBlockingResult,
        guidance: IslamicGuidance? = null,
        onAction: (WarningDialogAction) -> Unit,
        // NEW: Region-based trigger information
        triggeredByRegionCount: Boolean = false
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                if (isBlockedSiteOverlayVisible) {
                    hideBlockedSiteOverlay()
                }
                
                // First show full-screen blur background (enhanced for region-triggered)
                showFullScreenBlur(triggeredByRegionCount)

                // Then show blocked site dialog
                showBlockedSiteDialog(blockingResult, guidance, onAction)
                
                Log.d(TAG, "Blocked site overlay shown for category: ${blockingResult.category}")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing blocked site overlay", e)
            }
        }
        
    }

    /**
     * Start performance monitoring for blur rendering
     */
    private fun startPerformanceMonitoring() {
        overlayRenderTime = System.currentTimeMillis()
        frameDropCount = 0
        lastPerformanceLogTime = System.currentTimeMillis()
        Log.d(TAG, "Started blur performance monitoring")
    }
    
    /**
     * Stop performance monitoring and log results
     */
    private fun stopPerformanceMonitoring() {
        val totalTime = System.currentTimeMillis() - overlayRenderTime
        Log.d(TAG, "Blur performance summary - Total time: ${totalTime}ms, Frame drops: $frameDropCount")
        
        // Clear performance counters
        overlayRenderTime = 0L
        frameDropCount = 0
    }
    
    /**
     * Log performance metrics
     */
    private fun logPerformanceMetrics(frameTime: Long) {
        if (frameTime > FRAME_TIME_THRESHOLD) {
            frameDropCount++
            Log.w(TAG, "Blur frame drop detected - Frame time: ${frameTime}ms (threshold: ${FRAME_TIME_THRESHOLD}ms)")
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPerformanceLogTime > 10000) { // Log every 10 seconds
            val avgFrameTime = if (frameDropCount > 0) frameTime else FRAME_TIME_THRESHOLD
            val fps = if (avgFrameTime > 0) 1000 / avgFrameTime else 0
            
            Log.d(TAG, "Blur performance - Frame drops: $frameDropCount, Avg time: ${avgFrameTime}ms, FPS: $fps")
            lastPerformanceLogTime = currentTime
        }
    }
    
    /**
     * Get blur rendering performance stats
     */
    fun getBlurRenderingStats(): BlurRenderingStats {
        return BlurRenderingStats(
            isAnimating = isAnimating,
            frameDropCount = frameDropCount,
            lastFrameTime = lastFrameTime,
            currentRenderingMode = enhancedBlurEffects.currentRenderingMode.toString()
        )
    }
    
    /**
     * Enable hardware acceleration for blur rendering
     */
    fun enableHardwareAccelerationForOverlay() {
        try {
            overlayView?.let { view ->
                if (settingsRepository.settings.value.enableHardwareBlurAcceleration) {
                    BlurAnimationUtils.enableHardwareAcceleration(view)
                    Log.d(TAG, "Hardware acceleration enabled for current overlay")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable hardware acceleration for overlay", e)
        }
    }
    
    /**
     * Schedule deferred blur update when frame skipping occurs
     */
    private fun scheduleDeferredBlurUpdate() {
        // Cancel any existing deferred update
        deferredUpdateJob?.cancel()
        
        // Schedule new deferred update with small delay
        deferredUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                delay(50) // Small delay to allow frame timing to stabilize
                
                // Apply pending regions if available
                pendingRegions?.let { regions ->
                    val intensity = pendingBlurIntensity ?: settingsRepository.settings.value.blurIntensity
                    val style = pendingBlurStyle ?: settingsRepository.settings.value.blurStyle
                    val sensitivity = pendingContentSensitivity ?: 0.5f
                    
                    Log.d(TAG, "Applying deferred blur update with ${regions.size} regions")
                    updateBlurOverlay(regions, intensity, style, sensitivity)
                    
                    // Clear pending data
                    pendingRegions = null
                    pendingBlurIntensity = null
                    pendingBlurStyle = null
                    pendingContentSensitivity = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in deferred blur update", e)
            }
        }
    }
    
    /**
     * Set performance mode for blur rendering
     */
    fun setBlurPerformanceMode(mode: com.hieltech.haramblur.data.BlurRenderingMode) {
        enhancedBlurEffects.setPerformanceMode(mode)
        Log.d(TAG, "Blur performance mode set to: ${mode.name}")
    }
    
    /**
     * Data class for blur rendering performance statistics
     */
    data class BlurRenderingStats(
        val isAnimating: Boolean,
        val frameDropCount: Int,
        val lastFrameTime: Long,
        val currentRenderingMode: String
    )
    
    /**
     * Show blocked site dialog with Quranic verse
     */
    private fun showBlockedSiteDialog(
        blockingResult: SiteBlockingResult,
        guidance: IslamicGuidance?,
        onAction: (WarningDialogAction) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Move to RESUMED state for active overlay
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                
                blockedSiteOverlayView = ComposeView(context!!).apply {
                    // Set lifecycle owners for proper Compose management
                    setViewTreeLifecycleOwner(this@BlurOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlurOverlayManager)
                    
                    // Use proper composition strategy
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    
                    setContent {
                        HaramBlurTheme(preferredLanguage = Language.ENGLISH) {
                            var selectedLanguage by remember {
                                mutableStateOf(com.hieltech.haramblur.detection.Language.ENGLISH) 
                            }
                            
                            BlockedSiteDialog(
                                blockingResult = blockingResult,
                                guidance = guidance,
                                selectedLanguage = selectedLanguage,
                                enableArabicText = true,
                                onLanguageChange = { language ->
                                    selectedLanguage = language
                                },
                                onAction = { action ->
                                    // Don't hide overlay immediately - let the accessibility service handle it
                                    // after navigation completes to prevent race conditions
                                    Log.d(TAG, "Dialog action triggered: $action")
                                    onAction(action)
                                },
                                onDismiss = {
                                    hideBlockedSiteOverlay()
                                    onAction(WarningDialogAction.Dismiss)
                                }
                            )
                        }
                    }
                }
                
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                
                params.gravity = Gravity.TOP or Gravity.START
                
                windowManager!!.addView(blockedSiteOverlayView, params)
                isBlockedSiteOverlayVisible = true
                
                Log.d(TAG, "Blocked site dialog overlay shown with lifecycle support")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing blocked site dialog", e)
            }
        }
    }
    
    /**
     * Hide blocked site overlay
     */
    fun hideBlockedSiteOverlay() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isBlockedSiteOverlayVisible && blockedSiteOverlayView != null && windowManager != null) {
                    windowManager!!.removeView(blockedSiteOverlayView)
                    isBlockedSiteOverlayVisible = false
                    
                    // Move back to STARTED state when overlay is hidden
                    if (!isOverlayVisible && !isWarningVisible) {
                        lifecycleRegistry.currentState = Lifecycle.State.STARTED
                    }
                    blockedSiteOverlayView = null
                    Log.d(TAG, "Blocked site overlay hidden")
                }
                
                // Also hide the background blur
                hideBlurOverlay()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding blocked site overlay", e)
            }
        }
    }
    
    /**
     * Check if blocked site overlay is currently visible
     */
    fun isBlockedSiteOverlayVisible(): Boolean = isBlockedSiteOverlayVisible

    /**
     * Show enhanced porn blocking overlay with full-screen Quranic verse
     */
    fun showPornBlockingOverlay(
        blockingResult: SiteBlockingResult,
        guidance: IslamicGuidance? = null,
        onAction: (WarningDialogAction) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }

                if (isBlockedSiteOverlayVisible) {
                    hideBlockedSiteOverlay()
                }

                Log.d(TAG, "🚫 Showing enhanced porn blocking overlay")

                // First show full-screen blur with enhanced warning
                showFullScreenBlur(triggeredByRegionCount = true, regionCount = 1, maxConfidence = blockingResult.confidence)

                // Then show enhanced porn blocking dialog
                showPornBlockingDialog(blockingResult, guidance, onAction)

                Log.d(TAG, "✅ Enhanced porn blocking overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing porn blocking overlay", e)
            }
        }
    }

    /**
     * Show enhanced porn blocking dialog with prominent Quranic display
     */
    private fun showPornBlockingDialog(
        blockingResult: SiteBlockingResult,
        guidance: IslamicGuidance?,
        onAction: (WarningDialogAction) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Move lifecycle to RESUMED state for active overlay
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                
                blockedSiteOverlayView = ComposeView(context!!).apply {
                    // Set lifecycle owners for proper Compose management
                    setViewTreeLifecycleOwner(this@BlurOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlurOverlayManager)
                    
                    // Use proper composition strategy
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        HaramBlurTheme(preferredLanguage = Language.ENGLISH) {
                            var selectedLanguage by remember {
                                mutableStateOf(com.hieltech.haramblur.detection.Language.ENGLISH)
                            }

                            PornBlockingDialog(
                                blockingResult = blockingResult,
                                guidance = guidance,
                                selectedLanguage = selectedLanguage,
                                enableArabicText = true,
                                onLanguageChange = { language ->
                                    selectedLanguage = language
                                },
                                onAction = { action ->
                                    Log.d(TAG, "Porn blocking dialog action triggered: $action")
                                    onAction(action)
                                },
                                onDismiss = {
                                    hideBlockedSiteOverlay()
                                    onAction(WarningDialogAction.Dismiss)
                                }
                            )
                        }
                    }
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )

                params.gravity = Gravity.TOP or Gravity.START

                windowManager!!.addView(blockedSiteOverlayView, params)
                isBlockedSiteOverlayVisible = true

                Log.d(TAG, "Enhanced porn blocking dialog overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing porn blocking dialog", e)
            }
        }
    }
    
    /**
     * Force close current app/page (for "Close" action)
     */
    fun closeCurrentApp() {
        // This would be implemented by the accessibility service
        // to navigate back or close the current app
        onWarningAction?.invoke(WarningDialogAction.Close)
        Log.d(TAG, "Request to close current app")
    }
    
    private class BlurOverlayView(
        context: Context,
        private var blurRegions: List<Rect>,
        private var blurIntensity: BlurIntensity,
        private var blurStyle: BlurStyle = BlurStyle.PIXELATED,
        private var contentSensitivity: Float = 0.5f,
        private var transparency: Float = 1.0f,
        private val isFullScreen: Boolean = false,
        // Screen resolution for precise scaling
        private val screenWidth: Int = 1080,
        private val screenHeight: Int = 2400,
        // Region-based trigger information
        private val triggeredByRegionCount: Boolean = false,
        private val regionCount: Int = 0,
        private val maxConfidence: Float = 0.0f,
        // Pass app settings once to avoid memory leak
        private val appSettings: com.hieltech.haramblur.data.AppSettings
    ) : View(context) {

        // Public property to check if this is a full screen blur
        val isFullScreenBlur: Boolean = isFullScreen
        
        private val enhancedBlurEffects = EnhancedBlurEffects()

        // Helper method to calculate alpha based on transparency
        private fun calculateAlpha(baseAlpha: Int): Int {
            return (baseAlpha * transparency).toInt().coerceIn(0, 255)
        }

        // Enhanced paint types for maximum blur effectiveness
        private val blurPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C0C0C0") // Stronger gray for better coverage
            alpha = 255 // Maximum opacity
        }

        private val pixelPaint = Paint().apply {
            isAntiAlias = false // Pixelated effect
            color = Color.parseColor("#B0B0B0") // Darker gray
            alpha = 240 // High opacity
        }

        private val noisePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D0D0D0") // Medium gray
            alpha = 220 // High opacity
        }
        
        // Precision debugging paint (remove in production)
        private val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FF0000") // Red border
            alpha = 100
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        fun updateBlurRegions(
            newRegions: List<Rect>,
            intensity: BlurIntensity,
            style: BlurStyle = BlurStyle.PIXELATED,
            sensitivity: Float = 0.5f,
            newTransparency: Float = 0.8f
        ) {
            blurRegions = newRegions
            blurIntensity = intensity
            blurStyle = style
            contentSensitivity = sensitivity
            transparency = newTransparency

            // Update paint alpha values based on new transparency
            updatePaintAlpha()

            invalidate()
        }

        private fun updatePaintAlpha() {
            blurPaint.alpha = calculateAlpha(STRONG_BLUR_ALPHA)
            pixelPaint.alpha = calculateAlpha(200)
            noisePaint.alpha = calculateAlpha(150)
        }
        
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            if (isFullScreen) {
                // For full-screen mode, draw enhanced blur with warning patterns
                drawFullScreenBlur(canvas)
            } else {
                // PRECISION BLUR: Draw only targeted regions with enhanced effects
                blurRegions.forEach { rect ->
                    // Validate region bounds against actual canvas size
                    val canvasWidth = canvas.width
                    val canvasHeight = canvas.height
                    
                    val boundedRect = Rect(
                        maxOf(0, rect.left),
                        maxOf(0, rect.top),
                        minOf(canvasWidth, rect.right),
                        minOf(canvasHeight, rect.bottom)
                    )
                    
                    if (boundedRect.width() > 0 && boundedRect.height() > 0) {
                        // Apply enhanced precision blur with passed settings (NO MEMORY LEAK!)
                        enhancedBlurEffects.applyEnhancedBlur(
                            canvas,
                            boundedRect,
                            blurIntensity, // Use user's preferred intensity
                            blurStyle, // Use user's preferred style
                            contentSensitivity, // Use passed sensitivity
                            enableEdgeRefinement = true,
                            enableAntiAliasing = true,
                            precision = appSettings.blurBoundaryPrecision,
                            enableBlurEdgeRefinement = appSettings.enableBlurEdgeRefinement,
                            blurEdgeAntiAliasing = appSettings.blurEdgeAntiAliasing,
                            blurBoundaryPrecision = appSettings.blurBoundaryPrecision,
                            enableBlurFrameRateLimiting = appSettings.enableBlurFrameRateLimiting,
                            maxBlurRegionsPerFrame = appSettings.maxBlurRegionsPerFrame,
                            enableBlurRegionInterpolation = appSettings.enableBlurRegionInterpolation,
                            blurAnimationDuration = appSettings.blurAnimationDuration
                        )
                        
                        // Add precision border for debugging
                        drawPrecisionBorder(canvas, boundedRect)
                    }
                }
            }
        }
        
        private fun drawFullScreenBlur(canvas: Canvas) {
            val width = canvas.width
            val height = canvas.height
            val fullRect = Rect(0, 0, width, height)

            // Draw base full-screen blur with different intensity for region-triggered
            val baseColor = if (triggeredByRegionCount) {
                Color.parseColor("#0D0D0D") // Even darker for region-triggered (more critical)
            } else {
                Color.parseColor("#1A1A1A") // Standard dark background
            }

            val fullScreenPaint = Paint().apply {
                isAntiAlias = true
                color = baseColor
                alpha = if (triggeredByRegionCount) 250 else 240 // More opaque for region-triggered
            }
            canvas.drawRect(fullRect, fullScreenPaint)

            // Add different warning patterns based on trigger type
            if (triggeredByRegionCount) {
                drawRegionTriggeredWarningPattern(canvas, fullRect)
            } else {
                drawWarningPattern(canvas, fullRect)
            }

            // Add enhanced Islamic geometric pattern for region-triggered
            if (triggeredByRegionCount) {
                drawEnhancedIslamicPattern(canvas, fullRect)
            } else {
                drawIslamicPattern(canvas, fullRect)
            }

            // Add region count indicator for region-triggered blur
            if (triggeredByRegionCount) {
                drawRegionCountIndicator(canvas, fullRect)
            }
        }
        
        private fun drawWarningPattern(canvas: Canvas, rect: Rect) {
            val patternPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#333333")
                alpha = 100
            }
            
            val spacing = 60
            val lineWidth = 3f
            
            // Draw diagonal warning lines
            for (i in -rect.height() until rect.width() step spacing) {
                canvas.drawLine(
                    i.toFloat(),
                    0f,
                    (i + rect.height()).toFloat(),
                    rect.height().toFloat(),
                    patternPaint.apply { strokeWidth = lineWidth }
                )
            }
        }
        
        private fun drawIslamicPattern(canvas: Canvas, rect: Rect) {
            val patternPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#2A4A2A") // Subtle green
                alpha = 80
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()
            val radius = 100f

            // Draw subtle geometric pattern in center
            for (i in 0 until 8) {
                val angle = (i * 45) * Math.PI / 180
                val startX = centerX + (radius * 0.5f * Math.cos(angle)).toFloat()
                val startY = centerY + (radius * 0.5f * Math.sin(angle)).toFloat()
                val endX = centerX + (radius * Math.cos(angle)).toFloat()
                val endY = centerY + (radius * Math.sin(angle)).toFloat()

                canvas.drawLine(startX, startY, endX, endY, patternPaint)
            }
        }

        /**
         * NEW: Enhanced warning pattern for region-triggered full-screen blur
         */
        private fun drawRegionTriggeredWarningPattern(canvas: Canvas, rect: Rect) {
            val patternPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#662222") // Dark red for critical region-triggered
                alpha = 120
            }

            val spacing = 50 // More frequent lines for region-triggered
            val lineWidth = 4f // Thicker lines

            // Draw diagonal warning lines with more intensity
            for (i in -rect.height() until rect.width() step spacing) {
                canvas.drawLine(
                    i.toFloat(),
                    0f,
                    (i + rect.height()).toFloat(),
                    rect.height().toFloat(),
                    patternPaint.apply { strokeWidth = lineWidth }
                )
            }

            // Add cross-hatch pattern for more visual impact
            for (i in -rect.width() until rect.height() step spacing) {
                canvas.drawLine(
                    0f,
                    i.toFloat(),
                    rect.width().toFloat(),
                    (i + rect.width()).toFloat(),
                    patternPaint.apply { strokeWidth = lineWidth * 0.7f }
                )
            }
        }

        /**
         * NEW: Enhanced Islamic geometric pattern for region-triggered blur
         */
        private fun drawEnhancedIslamicPattern(canvas: Canvas, rect: Rect) {
            val patternPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#4A2A2A") // Darker red-tinted for critical
                alpha = 100
                style = Paint.Style.STROKE
                strokeWidth = 3f // Thicker lines
            }

            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()
            val radius = 120f // Larger pattern

            // Draw enhanced geometric pattern with more complexity
            for (i in 0 until 12) { // More points for complexity
                val angle = (i * 30) * Math.PI / 180
                val startX = centerX + (radius * 0.3f * Math.cos(angle)).toFloat()
                val startY = centerY + (radius * 0.3f * Math.sin(angle)).toFloat()
                val endX = centerX + (radius * Math.cos(angle)).toFloat()
                val endY = centerY + (radius * Math.sin(angle)).toFloat()

                canvas.drawLine(startX, startY, endX, endY, patternPaint)
            }

            // Add inner circle for more visual impact
            val circlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#4A2A2A")
                alpha = 60
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(centerX, centerY, radius * 0.5f, circlePaint)
        }

        /**
         * NEW: Draw region count indicator for region-triggered blur
         */
        private fun drawRegionCountIndicator(canvas: Canvas, rect: Rect) {
            val indicatorPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#FF6B6B") // Bright red for visibility
                alpha = 180
                textSize = 48f
                textAlign = Paint.Align.CENTER
            }

            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()

            // Draw warning icon (exclamation mark in circle)
            val circlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#FF6B6B")
                alpha = 150
            }

            // Draw warning circle
            canvas.drawCircle(centerX, centerY - 150f, 60f, circlePaint)
        }
        
        /**
         * Draw precision border for debugging blur regions
         */
        private fun drawPrecisionBorder(canvas: Canvas, rect: Rect) {
            // DEBUG MODE: Draw red border to visualize blur regions
            canvas.drawRect(rect, borderPaint)

            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()

            // Draw exclamation mark
            // Text overlays removed for cleaner UI per user request
            // Users found "Multiple NSFW Regions Detected" text annoying
        }
        
        private fun drawStrongBlurEffect(canvas: Canvas, rect: Rect) {
            when (blurIntensity) {
                BlurIntensity.LIGHT -> drawLightBlur(canvas, rect)
                BlurIntensity.MEDIUM -> drawMediumBlur(canvas, rect)
                BlurIntensity.STRONG -> drawStrongBlur(canvas, rect)
                BlurIntensity.MAXIMUM -> drawMaximumBlur(canvas, rect)
            }
        }
        
        private fun drawLightBlur(canvas: Canvas, rect: Rect) {
            // Light blur: Just base layer with low opacity
            val lightPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#E0E0E0")
                alpha = calculateAlpha(120) // Light opacity with transparency
            }
            canvas.drawRect(rect, lightPaint)
            drawBlurBorder(canvas, rect)
        }
        
        private fun drawMediumBlur(canvas: Canvas, rect: Rect) {
            // Medium blur: Base layer + pixelation
            canvas.drawRect(rect, blurPaint)
            drawPixelatedPattern(canvas, rect, pixelSize = 20, alpha = calculateAlpha(150))
            drawBlurBorder(canvas, rect)
        }
        
        private fun drawStrongBlur(canvas: Canvas, rect: Rect) {
            // Strong blur: All layers for maximum privacy
            canvas.drawRect(rect, blurPaint)
            drawPixelatedPattern(canvas, rect, pixelSize = 15, alpha = calculateAlpha(200))
            drawNoisePattern(canvas, rect)
            drawBlurBorder(canvas, rect)
        }
        
        private fun drawMaximumBlur(canvas: Canvas, rect: Rect) {
            // Maximum blur: Solid black coverage with transparency support
            val maximumPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                alpha = calculateAlpha(255) // Apply transparency even to maximum blur
            }
            canvas.drawRect(rect, maximumPaint)

            // Add dense pixelation pattern
            drawPixelatedPattern(canvas, rect, pixelSize = 10, alpha = calculateAlpha(255))
            drawNoisePattern(canvas, rect)

            // Strong border with transparency
            val strongBorderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 5f
                color = Color.RED
                alpha = calculateAlpha(255)
                isAntiAlias = true
            }
            canvas.drawRect(rect, strongBorderPaint)
        }
        
        private fun drawPixelatedPattern(canvas: Canvas, rect: Rect, pixelSize: Int = 15, alpha: Int = 200) {
            // Delegate to EnhancedBlurEffects for optimized pixelated rendering with cached patterns
            enhancedBlurEffects.applyPixelatedBlur(canvas, rect, blurIntensity, contentSensitivity, alpha)
        }
        
        private fun drawNoisePattern(canvas: Canvas, rect: Rect) {
            // Delegate to EnhancedBlurEffects for optimized noise rendering using cached bitmaps
            enhancedBlurEffects.applyOptimizedNoiseBlur(canvas, rect, blurIntensity, contentSensitivity)
        }
        
        private fun drawBlurBorder(canvas: Canvas, rect: Rect) {
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.parseColor("#CCCCCC")
                alpha = calculateAlpha(180)
                isAntiAlias = true
            }

            // Draw subtle border around blurred area
            canvas.drawRect(rect, borderPaint)
        }
    }
}