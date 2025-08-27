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
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.data.IslamicGuidance
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.data.WarningDialogState
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.SiteBlockingResult
import com.hieltech.haramblur.ui.components.BlockedSiteDialog
import com.hieltech.haramblur.ui.components.WarningDialog
import com.hieltech.haramblur.ui.components.WarningDialogManager
import com.hieltech.haramblur.ui.effects.EnhancedBlurEffects
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
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
) {
    
    private val enhancedBlurEffects = EnhancedBlurEffects()
    
    companion object {
        private const val TAG = "BlurOverlayManager"
        private const val DEFAULT_BLUR_INTENSITY = 50f
        private const val STRONG_BLUR_ALPHA = 220 // More opaque
    }
    
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
    
    // Navigation callback for automatic actions
    var onNavigateAwayAction: (() -> Unit)? = null
    
    // Callback for warning dialog actions
    var onWarningAction: ((WarningDialogAction) -> Unit)? = null
    
    fun initialize(context: Context) {
        this.context = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "Blur overlay manager initialized")
    }
    
    fun showBlurOverlay(
        blurRegions: List<Rect>,
        blurIntensity: BlurIntensity? = null,
        blurStyle: BlurStyle? = null,
        contentSensitivity: Float = 0.5f,
        transparency: Float = 1.0f // Maximum opacity for better coverage
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
                
                if (isOverlayVisible) {
                    updateBlurOverlay(blurRegions, userBlurIntensity, userBlurStyle, contentSensitivity)
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
                    Log.w(TAG, "No valid scaled regions - skipping blur overlay")
                    return@launch
                }
                
                overlayView = BlurOverlayView(
                    context!!,
                    scaledRegions,
                    userBlurIntensity,
                    userBlurStyle,
                    contentSensitivity,
                    transparency,
                    isFullScreen = false,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
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
                
                Log.d(TAG, "🎯 PRECISION BLUR: ${scaledRegions.size} regions on ${screenWidth}x${screenHeight} screen")
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
    
    fun hideBlurOverlay() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Attempting to hide blur overlay - isVisible: $isOverlayVisible, overlayView: ${overlayView != null}")

                // Force hide any visible overlay
                if (isOverlayVisible && overlayView != null && windowManager != null) {
                    try {
                        windowManager!!.removeView(overlayView)
                        Log.d(TAG, "Blur overlay view removed from window")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error removing overlay view (might already be removed)", e)
                    }
                }

                // Also try to hide full-screen blur if it exists
                hideFullScreenBlur()

                // Reset all state
                isOverlayVisible = false
                overlayView = null
                Log.d(TAG, "Blur overlay hidden and state reset")

            } catch (e: Exception) {
                Log.e(TAG, "Critical error hiding blur overlay", e)
                // Force reset state even on error
                isOverlayVisible = false
                overlayView = null
            }
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
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.w(TAG, "EMERGENCY: Force hiding all overlays")

                // Cancel any running timers first
                cancelFullScreenBlurTimer()

                // Hide main blur overlay
                if (overlayView != null && windowManager != null) {
                    try {
                        windowManager!!.removeView(overlayView)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error in emergency hide", e)
                    }
                }

                // Hide warning overlay
                if (warningOverlayView != null && windowManager != null) {
                    try {
                        windowManager!!.removeView(warningOverlayView)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error hiding warning overlay in emergency", e)
                    }
                }

                // Hide blocked site overlay
                if (blockedSiteOverlayView != null && windowManager != null) {
                    try {
                        windowManager!!.removeView(blockedSiteOverlayView)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error hiding blocked site overlay in emergency", e)
                    }
                }

                // Reset all states
                isOverlayVisible = false
                isWarningVisible = false
                isBlockedSiteOverlayVisible = false
                overlayView = null
                warningOverlayView = null
                blockedSiteOverlayView = null

                Log.w(TAG, "EMERGENCY: All overlays hidden and states reset")

            } catch (e: Exception) {
                Log.e(TAG, "Critical error in emergency hide", e)
                // Force reset states even on critical error
                isOverlayVisible = false
                isWarningVisible = false
                isBlockedSiteOverlayVisible = false
                overlayView = null
                warningOverlayView = null
                blockedSiteOverlayView = null
            }
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

                overlayView = BlurOverlayView(
                    context!!,
                    listOf(fullScreenRect),
                    BlurIntensity.MAXIMUM,
                    BlurStyle.COMBINED,
                    1.0f, // Maximum sensitivity for full-screen
                    isFullScreen = true,
                    triggeredByRegionCount = triggeredByRegionCount,
                    regionCount = regionCount,
                    maxConfidence = maxConfidence
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
                
                warningOverlayView = ComposeView(context!!).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        HaramBlurTheme {
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
                
                Log.d(TAG, "Warning dialog overlay shown")
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
     * Show blocked site dialog with Quranic verse
     */
    private fun showBlockedSiteDialog(
        blockingResult: SiteBlockingResult,
        guidance: IslamicGuidance?,
        onAction: (WarningDialogAction) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                blockedSiteOverlayView = ComposeView(context!!).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        HaramBlurTheme {
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
                
                Log.d(TAG, "Blocked site dialog overlay shown")
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
        private val maxConfidence: Float = 0.0f
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
                        // Apply enhanced precision blur with user settings
                        enhancedBlurEffects.applyEnhancedBlur(
                            canvas, 
                            boundedRect, 
                            blurIntensity, // Use user's preferred intensity
                            blurStyle, // Use user's preferred style
                            contentSensitivity // Use passed sensitivity
                        )
                        
                        // Add precision border for debugging (remove in production)
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
            // Only draw in debug mode - comment out for production
            // canvas.drawRect(rect, borderPaint)

            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()

            // Draw exclamation mark
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                alpha = 255
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("!", centerX, centerY - 140f, textPaint)

            // Draw "Multiple NSFW Regions" text
            val messagePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#FFCCCC") // Light red text
                alpha = 200
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("Multiple NSFW Regions Detected", centerX, centerY - 80f, messagePaint)
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
            
            for (x in rect.left until rect.right step pixelSize) {
                for (y in rect.top until rect.bottom step pixelSize) {
                    val pixelRect = Rect(
                        x,
                        y,
                        minOf(x + pixelSize, rect.right),
                        minOf(y + pixelSize, rect.bottom)
                    )
                    
                    // Vary the color slightly for each pixel
                    val variation = (Math.random() * 40 - 20).toInt()
                    val adjustedColor = Color.rgb(
                        (208 + variation).coerceIn(180, 240),
                        (208 + variation).coerceIn(180, 240),
                        (208 + variation).coerceIn(180, 240)
                    )
                    
                    val dynamicPaint = Paint().apply {
                        isAntiAlias = false
                        color = adjustedColor
                        setAlpha(alpha)
                    }
                    canvas.drawRect(pixelRect, dynamicPaint)
                }
            }
        }
        
        private fun drawNoisePattern(canvas: Canvas, rect: Rect) {
            val noiseSize = 4 // Size of noise dots
            val density = 0.3f // How many noise dots (30% coverage)
            
            val numDotsX = (rect.width() / noiseSize * density).toInt()
            val numDotsY = (rect.height() / noiseSize * density).toInt()
            
            repeat(numDotsX * numDotsY) {
                val x = rect.left + (Math.random() * rect.width()).toInt()
                val y = rect.top + (Math.random() * rect.height()).toInt()
                
                // Random brightness for noise
                val brightness = (Math.random() * 100 + 180).toInt()
                noisePaint.color = Color.rgb(brightness, brightness, brightness)
                
                canvas.drawCircle(x.toFloat(), y.toFloat(), noiseSize.toFloat(), noisePaint)
            }
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