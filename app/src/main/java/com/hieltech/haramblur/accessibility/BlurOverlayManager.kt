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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlurOverlayManager @Inject constructor(
    private val warningDialogManager: WarningDialogManager
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
    
    // Callback for warning dialog actions
    var onWarningAction: ((WarningDialogAction) -> Unit)? = null
    
    fun initialize(context: Context) {
        this.context = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "Blur overlay manager initialized")
    }
    
    fun showBlurOverlay(
        blurRegions: List<Rect>,
        blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
        blurStyle: BlurStyle = BlurStyle.PIXELATED,
        contentSensitivity: Float = 0.5f,
        transparency: Float = 0.8f // New transparency parameter (0.0 = fully transparent, 1.0 = fully opaque)
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (windowManager == null || context == null) {
                    Log.w(TAG, "WindowManager or Context not initialized")
                    return@launch
                }
                
                if (isOverlayVisible) {
                    updateBlurOverlay(blurRegions, blurIntensity, blurStyle, contentSensitivity)
                    return@launch
                }
                
                overlayView = BlurOverlayView(
                    context!!,
                    blurRegions,
                    blurIntensity,
                    blurStyle,
                    contentSensitivity,
                    transparency,
                    isFullScreen = false
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
                
                Log.d(TAG, "Blur overlay shown with ${blurRegions.size} regions")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing blur overlay", e)
            }
        }
    }
    
    fun updateBlurOverlay(
        blurRegions: List<Rect>,
        blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
        blurStyle: BlurStyle = BlurStyle.PIXELATED,
        contentSensitivity: Float = 0.5f,
        transparency: Float = 0.8f
    ) {
        overlayView?.updateBlurRegions(blurRegions, blurIntensity, blurStyle, contentSensitivity, transparency)
        Log.d(TAG, "Blur overlay updated with ${blurRegions.size} regions")
    }
    
    fun hideBlurOverlay() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isOverlayVisible && overlayView != null && windowManager != null) {
                    windowManager!!.removeView(overlayView)
                    isOverlayVisible = false
                    overlayView = null
                    Log.d(TAG, "Blur overlay hidden")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding blur overlay", e)
            }
        }
    }
    
    fun isOverlayActive(): Boolean = isOverlayVisible
    
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
    fun showFullScreenBlur(triggeredByRegionCount: Boolean = false) {
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
                
                overlayView = BlurOverlayView(
                    context!!,
                    listOf(fullScreenRect),
                    BlurIntensity.MAXIMUM,
                    BlurStyle.COMBINED,
                    1.0f, // Maximum sensitivity for full-screen
                    isFullScreen = true,
                    triggeredByRegionCount = triggeredByRegionCount
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
            } catch (e: Exception) {
                Log.e(TAG, "Error showing full-screen blur", e)
            }
        }
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
                                    when (action) {
                                        is WarningDialogAction.Close -> {
                                            hideBlockedSiteOverlay()
                                            onAction(action)
                                        }
                                        is WarningDialogAction.Continue -> {
                                            hideBlockedSiteOverlay()
                                            onAction(action)
                                        }
                                        is WarningDialogAction.Dismiss -> {
                                            hideBlockedSiteOverlay()
                                            onAction(action)
                                        }
                                        else -> {
                                            onAction(action)
                                        }
                                    }
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
        private var transparency: Float = 0.8f,
        private val isFullScreen: Boolean = false,
        // NEW: Region-based trigger information
        private val triggeredByRegionCount: Boolean = false
    ) : View(context) {
        
        private val enhancedBlurEffects = EnhancedBlurEffects()

        // Helper method to calculate alpha based on transparency
        private fun calculateAlpha(baseAlpha: Int): Int {
            return (baseAlpha * transparency).toInt().coerceIn(0, 255)
        }

        // Multiple paint types for stronger blur effect with transparency support
        private val blurPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E0E0E0") // Light gray
            alpha = calculateAlpha(STRONG_BLUR_ALPHA)
        }

        private val pixelPaint = Paint().apply {
            isAntiAlias = false // Pixelated effect
            color = Color.parseColor("#D0D0D0") // Slightly darker gray
            alpha = calculateAlpha(200)
        }

        private val noisePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F0F0F0") // Very light gray
            alpha = calculateAlpha(150)
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
                // Use enhanced blur effects for each region
                blurRegions.forEach { rect ->
                    enhancedBlurEffects.applyEnhancedBlur(
                        canvas, 
                        rect, 
                        blurIntensity, 
                        blurStyle, 
                        contentSensitivity
                    )
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