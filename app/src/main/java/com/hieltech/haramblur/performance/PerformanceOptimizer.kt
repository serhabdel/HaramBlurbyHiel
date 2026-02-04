package com.hieltech.haramblur.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes device capabilities and provides optimized settings.
 * Determines device tier and adjusts processing accordingly.
 */
@Singleton
class PerformanceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Device performance tier classification
     */
    enum class DeviceTier {
        LOW,      // Budget devices, 2-3GB RAM
        MID,      // Mid-range, 4-6GB RAM
        HIGH,     // Flagship, 8GB+ RAM
        FLAGSHIP  // Premium, 12GB+ RAM, latest CPU
    }
    
    /**
     * Get device tier based on RAM and CPU
     */
    fun getDeviceTier(): DeviceTier {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        return when {
            totalRamMB >= 12000 && cpuCores >= 8 -> DeviceTier.FLAGSHIP
            totalRamMB >= 8000 && cpuCores >= 6 -> DeviceTier.HIGH
            totalRamMB >= 4000 && cpuCores >= 4 -> DeviceTier.MID
            else -> DeviceTier.LOW
        }
    }
    
    /**
     * Get optimal thread count for ML inference
     */
    fun getOptimalThreadCount(): Int = when (getDeviceTier()) {
        DeviceTier.LOW -> 2
        DeviceTier.MID -> 4
        DeviceTier.HIGH -> 6
        DeviceTier.FLAGSHIP -> 8
    }
    
    /**
     * Get recommended bitmap cache size
     */
    fun getBitmapCacheSize(): Int = when (getDeviceTier()) {
        DeviceTier.LOW -> 2
        DeviceTier.MID -> 4
        DeviceTier.HIGH -> 6
        DeviceTier.FLAGSHIP -> 8
    }
    
    /**
     * Check if GPU acceleration should be enabled
     */
    fun shouldUseGPU(): Boolean {
        return when (getDeviceTier()) {
            DeviceTier.LOW -> false
            DeviceTier.MID -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            DeviceTier.HIGH, DeviceTier.FLAGSHIP -> true
        }
    }
    
    /**
     * Get detection confidence threshold based on tier
     * Lower tier = higher threshold (more filtering, less CPU)
     */
    fun getConfidenceThreshold(): Float = when (getDeviceTier()) {
        DeviceTier.LOW -> 0.75f
        DeviceTier.MID -> 0.70f
        DeviceTier.HIGH -> 0.65f
        DeviceTier.FLAGSHIP -> 0.60f
    }
    
    /**
     * Get current memory usage
     */
    fun getMemoryUsage(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryStats(
            usedHeapMB = usedMem.toInt(),
            maxHeapMB = maxMem.toInt(),
            availableSystemMB = (memoryInfo.availMem / (1024 * 1024)).toInt(),
            isLowMemory = memoryInfo.lowMemory
        )
    }
    
    /**
     * Check if memory cleanup is needed
     */
    fun shouldCleanupMemory(): Boolean {
        val stats = getMemoryUsage()
        val usagePercent = stats.usedHeapMB.toFloat() / stats.maxHeapMB
        return usagePercent > 0.8f || stats.isLowMemory
    }
    
    /**
     * Get complete performance report
     */
    fun getPerformanceReport(): PerformanceReport {
        return PerformanceReport(
            deviceTier = getDeviceTier(),
            recommendedThreads = getOptimalThreadCount(),
            gpuRecommended = shouldUseGPU(),
            memoryStats = getMemoryUsage(),
            confidenceThreshold = getConfidenceThreshold()
        )
    }
    
    data class MemoryStats(
        val usedHeapMB: Int,
        val maxHeapMB: Int,
        val availableSystemMB: Int,
        val isLowMemory: Boolean
    )
    
    data class PerformanceReport(
        val deviceTier: DeviceTier,
        val recommendedThreads: Int,
        val gpuRecommended: Boolean,
        val memoryStats: MemoryStats,
        val confidenceThreshold: Float
    )
}
