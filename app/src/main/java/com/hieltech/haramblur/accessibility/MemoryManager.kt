package com.hieltech.haramblur.accessibility

import android.util.Log
import com.hieltech.haramblur.utils.AppConstants.Tags

/**
 * Manages memory for the accessibility service
 * Handles caches, cleanup, and memory pressure responses
 */
class MemoryManager(
    private val onClearCaches: () -> Unit,
    private val onHideOverlay: () -> Unit
) {
    
    private val TAG = Tags.ACCESSIBILITY_SERVICE
    
    // Memory thresholds (in MB)
    companion object {
        const val MEMORY_WARNING_THRESHOLD_MB = 100
        const val MEMORY_CRITICAL_THRESHOLD_MB = 150
        const val MAX_CACHE_SIZE = 100
    }
    
    // Tracking
    private var lastCleanupTime: Long = 0
    private val cleanupCooldownMs = 5000L // 5 seconds between cleanups
    
    /**
     * Perform emergency memory cleanup
     */
    fun emergencyCleanup() {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown to avoid excessive cleanups
        if (currentTime - lastCleanupTime < cleanupCooldownMs) {
            Log.d(TAG, "Memory cleanup skipped (cooldown)")
            return
        }
        
        lastCleanupTime = currentTime
        
        try {
            Log.w(TAG, "🧹 Performing emergency memory cleanup")
            
            // Clear detection caches via callback
            onClearCaches()
            
            // Hide overlays
            onHideOverlay()
            
            // Request garbage collection
            System.gc()
            
            Log.w(TAG, "🧹 Emergency memory cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency memory cleanup", e)
        }
    }
    
    /**
     * Check if memory cleanup is needed based on current memory usage
     */
    fun checkMemoryPressure(): MemoryPressure {
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        
        return when {
            usedMemoryMB > maxMemoryMB - MEMORY_CRITICAL_THRESHOLD_MB -> {
                Log.w(TAG, "🔴 Critical memory pressure: ${usedMemoryMB}MB / ${maxMemoryMB}MB")
                MemoryPressure.CRITICAL
            }
            usedMemoryMB > maxMemoryMB - MEMORY_WARNING_THRESHOLD_MB -> {
                Log.w(TAG, "🟡 Warning memory pressure: ${usedMemoryMB}MB / ${maxMemoryMB}MB")
                MemoryPressure.WARNING
            }
            else -> MemoryPressure.NORMAL
        }
    }
    
    /**
     * Trim memory based on level
     */
    fun onTrimMemory(level: Int) {
        when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "🔴 Critical trim memory level: $level")
                emergencyCleanup()
            }
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                Log.w(TAG, "🟡 Moderate trim memory level: $level")
                onClearCaches()
            }
            else -> {
                Log.d(TAG, "ℹ️ Trim memory level: $level")
            }
        }
    }
    
    /**
     * Memory pressure levels
     */
    enum class MemoryPressure {
        NORMAL,
        WARNING,
        CRITICAL
    }
}
