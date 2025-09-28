package com.hieltech.haramblur.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewPropertyAnimator
import android.animation.ValueAnimator
import android.animation.AnimatorListenerAdapter
import android.animation.Animator
import android.util.Log
import android.graphics.Rect
import androidx.annotation.RequiresApi
import kotlin.math.abs

/**
 * Utility class for smooth blur animations and transitions
 * Provides hardware-accelerated animations and performance-aware transitions
 */
object BlurAnimationUtils {
    
    private const val TAG = "BlurAnimationUtils"
    
    // Animation duration constants
    const val OVERLAY_FADE_DURATION = 250L
    const val INTENSITY_CHANGE_DURATION = 150L
    const val REGION_TRANSITION_DURATION = 200L
    const val MIN_ANIMATION_DURATION = 50L
    const val MAX_ANIMATION_DURATION = 1000L
    
    // Performance thresholds
    private const val FRAME_DROP_THRESHOLD = 16L // 60fps target
    private const val PERFORMANCE_DEGRADATION_THRESHOLD = 3 // Number of consecutive frame drops
    
    private var consecutiveFrameDrops = 0
    private var lastFrameTime = 0L
    
    /**
     * Animation state tracking
     */
    data class BlurAnimationState(
        val isAnimating: Boolean = false,
        val animationType: String = "",
        val startTime: Long = 0L,
        val duration: Long = 0L,
        val progress: Float = 0f
    )
    
    private var currentAnimationState = BlurAnimationState()
    
    /**
     * Create smooth fade-in animation for blur overlays
     */
    fun createFadeInAnimation(
        view: View,
        duration: Long = OVERLAY_FADE_DURATION,
        onComplete: () -> Unit = {}
    ): ViewPropertyAnimator {
        currentAnimationState = BlurAnimationState(
            isAnimating = true,
            animationType = "fade_in",
            startTime = System.currentTimeMillis(),
            duration = duration
        )
        
        return view.animate()
            .alpha(1f)
            .setDuration(duration)
            .withStartAction {
                view.alpha = 0f
                view.visibility = View.VISIBLE
                Log.d(TAG, "Starting fade-in animation")
            }
            .withEndAction {
                currentAnimationState = BlurAnimationState()
                onComplete()
                Log.d(TAG, "Fade-in animation completed")
            }
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    currentAnimationState = BlurAnimationState()
                    Log.d(TAG, "Fade-in animation cancelled")
                }
            })
    }
    
    /**
     * Create smooth fade-out animation with cleanup callback
     */
    fun createFadeOutAnimation(
        view: View,
        duration: Long = OVERLAY_FADE_DURATION,
        onComplete: () -> Unit = {}
    ): ViewPropertyAnimator {
        currentAnimationState = BlurAnimationState(
            isAnimating = true,
            animationType = "fade_out",
            startTime = System.currentTimeMillis(),
            duration = duration
        )
        
        return view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                view.visibility = View.GONE
                currentAnimationState = BlurAnimationState()
                onComplete()
                Log.d(TAG, "Fade-out animation completed")
            }
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    currentAnimationState = BlurAnimationState()
                    Log.d(TAG, "Fade-out animation cancelled")
                }
            })
    }
    
    /**
     * Create smooth blur intensity transitions
     */
    fun createIntensityTransition(
        fromAlpha: Int,
        toAlpha: Int,
        duration: Long = INTENSITY_CHANGE_DURATION,
        onUpdate: (Int) -> Unit
    ): ValueAnimator {
        currentAnimationState = BlurAnimationState(
            isAnimating = true,
            animationType = "intensity_transition",
            startTime = System.currentTimeMillis(),
            duration = duration
        )
        
        return ValueAnimator.ofInt(fromAlpha, toAlpha).apply {
            this.duration = duration
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Int
                onUpdate(alpha)
                
                // Update progress
                val progress = animator.animatedFraction
                currentAnimationState = currentAnimationState.copy(progress = progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimationState = BlurAnimationState()
                    Log.d(TAG, "Intensity transition completed")
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    currentAnimationState = BlurAnimationState()
                    Log.d(TAG, "Intensity transition cancelled")
                }
            })
        }
    }
    
    /**
     * Smooth interpolation between blur region sets
     */
    fun interpolateBlurRegions(
        oldRegions: List<Rect>,
        newRegions: List<Rect>,
        progress: Float
    ): List<Rect> {
        val maxRegions = maxOf(oldRegions.size, newRegions.size)
        val interpolatedRegions = mutableListOf<Rect>()
        
        for (i in 0 until maxRegions) {
            val oldRect = oldRegions.getOrNull(i)
            val newRect = newRegions.getOrNull(i)
            
            if (oldRect != null && newRect != null) {
                // Interpolate between existing regions
                val interpolated = interpolateRect(oldRect, newRect, progress)
                interpolatedRegions.add(interpolated)
            } else if (newRect != null) {
                // Fade in new region
                val alpha = progress.coerceIn(0f, 1f)
                if (alpha > 0.1f) { // Only add if visible
                    interpolatedRegions.add(newRect)
                }
            } else if (oldRect != null) {
                // Fade out old region
                val alpha = 1f - progress.coerceIn(0f, 1f)
                if (alpha > 0.1f) { // Only add if still visible
                    interpolatedRegions.add(oldRect)
                }
            }
        }
        
        return interpolatedRegions
    }
    
    /**
     * Calculate intermediate positions for smooth region movement
     */
    fun calculateRegionTransitionPath(from: Rect, to: Rect): List<Rect> {
        val steps = 10 // Number of intermediate steps
        val path = mutableListOf<Rect>()
        
        for (i in 1 until steps) {
            val progress = i.toFloat() / steps
            val intermediate = interpolateRect(from, to, progress)
            path.add(intermediate)
        }
        
        return path
    }
    
    /**
     * Optimize region transitions to minimize visual jarring
     */
    fun optimizeRegionTransitions(regions: List<Rect>): List<Rect> {
        if (regions.size < 2) return regions
        
        val optimized = mutableListOf<Rect>()
        optimized.add(regions[0])
        
        for (i in 1 until regions.size) {
            val current = regions[i]
            val previous = optimized.last()
            
            // Skip regions that are too similar to avoid flickering
            if (!areRectsSimilar(current, previous, threshold = 5)) {
                optimized.add(current)
            }
        }
        
        return optimized
    }
    
    /**
     * Create performance-aware animation that adapts to device performance
     */
    fun createPerformanceAwareAnimator(
        duration: Long,
        onFrame: (Float) -> Unit
    ): ValueAnimator {
        val adjustedDuration = getOptimalAnimationDuration(1) // Default complexity
        
        return ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = adjustedDuration
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Check if we should skip this frame
                if (shouldSkipFrame()) {
                    Log.d(TAG, "Skipping animation frame for performance")
                    return@addUpdateListener
                }
                
                onFrame(progress)
            }
        }
    }
    
    /**
     * Frame skipping logic for maintaining smooth 60fps
     */
    fun shouldSkipFrame(): Boolean {
        val currentTime = System.currentTimeMillis()
        val frameTime = currentTime - lastFrameTime
        
        return if (frameTime < FRAME_DROP_THRESHOLD) {
            false // Don't skip, frame timing is good
        } else {
            consecutiveFrameDrops++
            Log.w(TAG, "Frame drop detected (${frameTime}ms), consecutive drops: $consecutiveFrameDrops")
            
            if (consecutiveFrameDrops >= PERFORMANCE_DEGRADATION_THRESHOLD) {
                Log.w(TAG, "Performance degradation detected, skipping frame")
                consecutiveFrameDrops = 0
                true
            } else {
                false
            }
        }.also {
            if (!it) {
                consecutiveFrameDrops = 0 // Reset on good frame
            }
            lastFrameTime = currentTime
        }
    }
    
    /**
     * Calculate optimal animation duration based on blur complexity
     */
    fun getOptimalAnimationDuration(complexity: Int): Long {
        val baseDuration = when (complexity) {
            0 -> 150L // Simple
            1 -> 250L // Medium
            2 -> 350L // Complex
            else -> 500L // Very complex
        }
        
        // Adjust based on current performance
        val performanceMultiplier = if (consecutiveFrameDrops > 0) 1.5f else 1.0f
        
        return (baseDuration * performanceMultiplier).toLong().coerceIn(MIN_ANIMATION_DURATION, MAX_ANIMATION_DURATION)
    }
    
    /**
     * Enable hardware acceleration with fallback handling
     * Note: RenderEffect is NOT applied to overlay views to avoid blurring overlay content instead of background
     */
    fun enableHardwareAcceleration(view: View, isOverlayView: Boolean = false): Boolean {
        return try {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // For API 31+, create hardware blur effect - but skip for overlay views
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isOverlayView) {
                createHardwareBlurEffect(10f)?.let { effect ->
                    view.setRenderEffect(effect)
                    Log.d(TAG, "Hardware blur effect applied")
                }
            } else if (isOverlayView) {
                Log.d(TAG, "Hardware acceleration enabled for overlay view (RenderEffect skipped)")
            }
            
            Log.d(TAG, "Hardware acceleration enabled for view")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable hardware acceleration, falling back to software", e)
            
            // Fallback to software acceleration
            try {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                Log.d(TAG, "Fallback to software acceleration")
                true
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Failed to enable software acceleration", fallbackError)
                false
            }
        }
    }
    
    /**
     * Create hardware blur effect for API 31+ with null fallback
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun createHardwareBlurEffect(radius: Float): RenderEffect? {
        return try {
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create hardware blur effect", e)
            null
        }
    }
    
    /**
     * Check if hardware acceleration is available and working
     */
    fun isHardwareAccelerationAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Test if we can create a blur effect
                createHardwareBlurEffect(1f) != null
            } catch (e: Exception) {
                Log.e(TAG, "Hardware acceleration test failed", e)
                false
            }
        } else {
            // For older versions, check if hardware layers are supported
            true // Assume available, will fallback if needed
        }
    }
    
    /**
     * Cancel all running blur animations safely
     */
    fun cancelAllBlurAnimations(view: View) {
        try {
            view.animate().cancel()
            Log.d(TAG, "All blur animations cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel animations", e)
        } finally {
            currentAnimationState = BlurAnimationState()
        }
    }
    
    /**
     * Check if blur animations are currently active
     */
    fun isBlurAnimationRunning(view: View): Boolean {
        return currentAnimationState.isAnimating
    }
    
    /**
     * Get current animation state
     */
    fun getAnimationState(): BlurAnimationState {
        return currentAnimationState.copy()
    }
    
    /**
     * Interpolate between two rectangles
     */
    private fun interpolateRect(from: Rect, to: Rect, progress: Float): Rect {
        val left = from.left + ((to.left - from.left) * progress).toInt()
        val top = from.top + ((to.top - from.top) * progress).toInt()
        val right = from.right + ((to.right - from.right) * progress).toInt()
        val bottom = from.bottom + ((to.bottom - from.bottom) * progress).toInt()
        
        return Rect(left, top, right, bottom)
    }
    
    /**
     * Check if two rectangles are similar within a threshold
     */
    private fun areRectsSimilar(rect1: Rect, rect2: Rect, threshold: Int): Boolean {
        return abs(rect1.left - rect2.left) <= threshold &&
               abs(rect1.top - rect2.top) <= threshold &&
               abs(rect1.right - rect2.right) <= threshold &&
               abs(rect1.bottom - rect2.bottom) <= threshold
    }
}