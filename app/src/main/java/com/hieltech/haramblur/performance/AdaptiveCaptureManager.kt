package com.hieltech.haramblur.performance

import android.graphics.Bitmap
import android.util.Log
import com.hieltech.haramblur.utils.AppConstants.Tags
import java.security.MessageDigest

/**
 * Manages adaptive screen capture intervals based on content activity
 * Reduces battery usage by capturing less frequently when content is stable
 */
class AdaptiveCaptureManager {
    
    private val TAG = "AdaptiveCaptureManager"
    
    // Capture frequencies in milliseconds
    enum class CaptureFrequency(val intervalMs: Long) {
        IDLE(5000L),        // 5s when screen is static
        NORMAL(2000L),      // 2s default
        ACTIVE(500L),       // 0.5s when inappropriate content detected
        HIGH_ALERT(200L)    // 0.2s during high-confidence detection
    }
    
    private var currentFrequency = CaptureFrequency.NORMAL
    private var lastBitmapHash: String? = null
    private var consecutiveSimilarFrames = 0
    private var consecutiveInappropriateDetections = 0
    
    // Thresholds for frequency changes
    private val SIMILAR_FRAME_THRESHOLD = 3  // After 3 similar frames, go to IDLE
    private val INAPPROPRIATE_DETECTION_THRESHOLD = 2  // After 2 detections, go to ACTIVE
    
    /**
     * Get current capture interval
     */
    fun getCaptureInterval(): Long = currentFrequency.intervalMs
    
    /**
     * Analyze frame and adjust capture frequency
     */
    fun analyzeFrame(bitmap: Bitmap, isInappropriate: Boolean) {
        val currentHash = computeBitmapHash(bitmap)
        
        when {
            // High inappropriate content detected
            isInappropriate -> {
                consecutiveInappropriateDetections++
                consecutiveSimilarFrames = 0
                
                if (consecutiveInappropriateDetections >= INAPPROPRIATE_DETECTION_THRESHOLD) {
                    updateFrequency(CaptureFrequency.HIGH_ALERT, "High inappropriate content detected")
                } else {
                    updateFrequency(CaptureFrequency.ACTIVE, "Inappropriate content detected")
                }
            }
            
            // Screen content changed
            currentHash != lastBitmapHash -> {
                consecutiveSimilarFrames = 0
                consecutiveInappropriateDetections = 0
                
                if (currentFrequency == CaptureFrequency.IDLE) {
                    updateFrequency(CaptureFrequency.NORMAL, "Screen content changed")
                }
            }
            
            // Screen content static
            else -> {
                consecutiveSimilarFrames++
                
                if (consecutiveSimilarFrames >= SIMILAR_FRAME_THRESHOLD && 
                    currentFrequency != CaptureFrequency.IDLE) {
                    updateFrequency(CaptureFrequency.IDLE, "Screen content static")
                }
            }
        }
        
        lastBitmapHash = currentHash
    }
    
    /**
     * Force a specific frequency (e.g., for user settings)
     */
    fun setFrequency(frequency: CaptureFrequency, reason: String) {
        updateFrequency(frequency, reason)
        // Reset counters when manually set
        consecutiveSimilarFrames = 0
        consecutiveInappropriateDetections = 0
    }
    
    /**
     * Reset to default frequency
     */
    fun reset() {
        currentFrequency = CaptureFrequency.NORMAL
        consecutiveSimilarFrames = 0
        consecutiveInappropriateDetections = 0
        lastBitmapHash = null
    }
    
    private fun updateFrequency(newFrequency: CaptureFrequency, reason: String) {
        if (newFrequency != currentFrequency) {
            Log.d(TAG, "Capture frequency changed: ${currentFrequency.name} -> ${newFrequency.name} ($reason)")
            currentFrequency = newFrequency
        }
    }
    
    /**
     * Compute a simple hash of bitmap for change detection
     * Uses sampling for performance
     */
    private fun computeBitmapHash(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        
        // Sample pixels at regular intervals (every 10th pixel)
        val sampleStep = 10
        
        val md = MessageDigest.getInstance("MD5")
        
        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                md.update((pixel shr 24).toByte())
                md.update((pixel shr 16).toByte())
                md.update((pixel shr 8).toByte())
                md.update(pixel.toByte())
            }
        }
        
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get current performance metrics
     */
    fun getMetrics(): AdaptiveCaptureMetrics {
        return AdaptiveCaptureMetrics(
            currentFrequency = currentFrequency,
            consecutiveSimilarFrames = consecutiveSimilarFrames,
            consecutiveInappropriateDetections = consecutiveInappropriateDetections,
            estimatedBatterySaving = calculateBatterySaving()
        )
    }
    
    private fun calculateBatterySaving(): Float {
        // Estimate battery saving compared to fixed 2-second interval
        val normalInterval = CaptureFrequency.NORMAL.intervalMs.toFloat()
        val currentInterval = currentFrequency.intervalMs.toFloat()
        return ((currentInterval - normalInterval) / normalInterval) * 100
    }
}

/**
 * Metrics for adaptive capture
 */
data class AdaptiveCaptureMetrics(
    val currentFrequency: AdaptiveCaptureManager.CaptureFrequency,
    val consecutiveSimilarFrames: Int,
    val consecutiveInappropriateDetections: Int,
    val estimatedBatterySaving: Float
)
