package com.hieltech.haramblur.accessibility

import android.graphics.Rect
import android.util.Log
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.utils.AppConstants.Tags

/**
 * Manages blur overlay state and operations
 * Handles showing/hiding overlays with proper state tracking
 */
class OverlayStateManager(
    private val blurOverlayManager: com.hieltech.haramblur.accessibility.BlurOverlayManager,
    private val onStateChanged: (Boolean) -> Unit
) {
    private val TAG = Tags.ACCESSIBILITY_SERVICE
    
    // State tracking
    private var isCurrentlyBlurred = false
    private var lastBlurStartTime: Long = 0
    private var minBlurDuration = 500L // Minimum 0.5 seconds for faster response
    
    /**
     * Check if overlay is currently active
     */
    fun isBlurred(): Boolean = isCurrentlyBlurred
    
    /**
     * Get last blur start time
     */
    fun getLastBlurStartTime(): Long = lastBlurStartTime
    
    /**
     * Show blur overlay with regions
     */
    fun showBlurOverlay(
        regions: List<Rect>,
        settings: AppSettings,
        contentSensitivity: Float,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ): Boolean {
        return try {
            Log.w(TAG, "🎯 ===== ACTIVATING BLUR OVERLAY =====")
            Log.w(TAG, "   Regions: ${regions.size}")
            regions.forEachIndexed { index, rect ->
                Log.w(TAG, "   Region $index: [${rect.width()}x${rect.height()}]")
            }
            Log.w(TAG, "   Intensity: ${settings.blurIntensity}")
            Log.w(TAG, "=====================================")
            
            blurOverlayManager.showBlurOverlay(
                blurRegions = regions,
                blurIntensity = settings.blurIntensity,
                blurStyle = settings.blurStyle,
                contentSensitivity = contentSensitivity,
                smoothTransition = settings.enableSmoothBlurAnimations
            )
            
            isCurrentlyBlurred = true
            lastBlurStartTime = System.currentTimeMillis()
            onStateChanged(true)
            
            Log.w(TAG, "✅ Blur overlay shown successfully")
            onSuccess()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL: Failed to show blur overlay", e)
            onError(e)
            false
        }
    }
    
    /**
     * Hide blur overlay safely
     */
    fun hideBlurOverlay(reason: String = "unknown"): Boolean {
        return try {
            Log.d(TAG, "Safely hiding overlay: $reason")
            blurOverlayManager.hideBlurOverlay()
            
            isCurrentlyBlurred = false
            onStateChanged(false)
            
            Log.d(TAG, "✅ Blur overlay hidden: $reason")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay ($reason), attempting emergency cleanup", e)
            try {
                blurOverlayManager.emergencyHideAllOverlays()
                isCurrentlyBlurred = false
                onStateChanged(false)
                true
            } catch (emergencyError: Exception) {
                Log.e(TAG, "Emergency cleanup also failed", emergencyError)
                false
            }
        }
    }
    
    /**
     * Show full screen warning
     */
    fun showFullScreenWarning(
        category: BlockingCategory,
        customMessage: String,
        reflectionTimeSeconds: Int,
        nsfwRegionCount: Int,
        maxNsfwConfidence: Float,
        triggeredByRegionCount: Boolean
    ): Boolean {
        return try {
            Log.d(TAG, "🚨 Showing full screen warning")
            
            blurOverlayManager.showFullScreenWarning(
                category = category,
                customMessage = customMessage,
                reflectionTimeSeconds = reflectionTimeSeconds,
                nsfwRegionCount = nsfwRegionCount,
                maxNsfwConfidence = maxNsfwConfidence,
                triggeredByRegionCount = triggeredByRegionCount
            )
            
            isCurrentlyBlurred = true
            lastBlurStartTime = System.currentTimeMillis()
            onStateChanged(true)
            
            Log.d(TAG, "🚨 Full screen warning displayed successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show full screen warning", e)
            
            // Emergency fallback
            try {
                blurOverlayManager.emergencyHideAllOverlays()
            } catch (_: Exception) {}
            
            isCurrentlyBlurred = false
            onStateChanged(false)
            false
        }
    }
    
    /**
     * Emergency hide all overlays
     */
    fun emergencyHideAllOverlays() {
        try {
            Log.w(TAG, "EMERGENCY: Force hiding all overlays")
            blurOverlayManager.emergencyHideAllOverlays()
            
            isCurrentlyBlurred = false
            lastBlurStartTime = 0
            onStateChanged(false)
            
            Log.w(TAG, "EMERGENCY: All overlays hidden and state reset")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency hide overlays", e)
        }
    }
    
    /**
     * Check if minimum blur duration has passed
     */
    fun isMinBlurDurationPassed(): Boolean {
        if (!isCurrentlyBlurred) return true
        val timeSinceBlurStart = System.currentTimeMillis() - lastBlurStartTime
        return timeSinceBlurStart >= minBlurDuration
    }
    
    /**
     * Get remaining blur duration
     */
    fun getRemainingBlurDuration(): Long {
        if (!isCurrentlyBlurred) return 0
        val elapsed = System.currentTimeMillis() - lastBlurStartTime
        return maxOf(0, minBlurDuration - elapsed)
    }
    
    /**
     * Reset state without hiding overlay
     */
    fun resetState() {
        isCurrentlyBlurred = false
        lastBlurStartTime = 0
        onStateChanged(false)
        Log.d(TAG, "🔄 Overlay state reset")
    }
    
    /**
     * Set minimum blur duration
     */
    fun setMinBlurDuration(durationMs: Long) {
        minBlurDuration = durationMs
        Log.d(TAG, "⏱️ Minimum blur duration set to ${durationMs}ms")
    }
}
