package com.hieltech.haramblur.data.models

/**
 * Represents performance metrics for the system
 */
data class PerformanceMetrics(
    val memoryUsage: Float = 0.0f,
    val cpuUsage: Float = 0.0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val networkLatency: Long = 0L,
    val storageSpace: Float = 0.0f
) {
    /**
     * Check if memory usage is high
     */
    fun isMemoryUsageHigh(): Boolean {
        return memoryUsage > 0.8f
    }

    /**
     * Check if CPU usage is high
     */
    fun isCpuUsageHigh(): Boolean {
        return cpuUsage > 0.8f
    }

    /**
     * Check if battery level is low
     */
    fun isBatteryLow(): Boolean {
        return batteryLevel < 20 && !isCharging
    }

    /**
     * Check if storage space is low
     */
    fun isStorageLow(): Boolean {
        return storageSpace > 0.9f
    }

    /**
     * Get overall performance score (0.0 to 1.0)
     */
    fun getPerformanceScore(): Float {
        var score = 1.0f

        // Deduct for high memory usage
        if (memoryUsage > 0.9f) score -= 0.3f
        else if (memoryUsage > 0.7f) score -= 0.1f

        // Deduct for high CPU usage
        if (cpuUsage > 0.8f) score -= 0.2f
        else if (cpuUsage > 0.6f) score -= 0.1f

        // Deduct for low battery
        if (batteryLevel < 15 && !isCharging) score -= 0.2f
        else if (batteryLevel < 25 && !isCharging) score -= 0.1f

        // Deduct for low storage
        if (storageSpace > 0.95f) score -= 0.3f
        else if (storageSpace > 0.85f) score -= 0.1f

        return score.coerceIn(0.0f, 1.0f)
    }
}
