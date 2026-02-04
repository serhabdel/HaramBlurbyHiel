package com.hieltech.haramblur.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors battery state and adjusts processing intensity accordingly.
 * Helps extend battery life during heavy content detection sessions.
 */
@Singleton
class BatteryAwareProcessor @Inject constructor(
    private val context: Context
) {
    private val powerManager: PowerManager = 
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private var batteryLevel: Float = 100f
    private var isCharging: Boolean = false
    private var batteryTemperature: Float = 0f
    
    /**
     * Get the optimal capture interval based on battery state.
     * @return interval in milliseconds
     */
    fun getOptimalCaptureInterval(): Long {
        return when {
            isCharging -> FAST_INTERVAL // Fast when charging
            powerManager.isPowerSaveMode -> POWER_SAVE_INTERVAL
            batteryLevel < LOW_BATTERY_THRESHOLD -> SLOW_INTERVAL
            batteryLevel < MEDIUM_BATTERY_THRESHOLD -> MEDIUM_INTERVAL
            else -> DEFAULT_INTERVAL
        }
    }
    
    /**
     * Check if processing should be skipped to conserve battery.
     */
    fun shouldSkipProcessing(): Boolean {
        return batteryLevel < CRITICAL_BATTERY_THRESHOLD && !isCharging
    }
    
    /**
     * Get processing quality level based on battery state.
     * Higher quality = more battery usage.
     */
    fun getProcessingQuality(): ProcessingQuality {
        return when {
            isCharging -> ProcessingQuality.HIGH
            powerManager.isPowerSaveMode -> ProcessingQuality.LOW
            batteryLevel < LOW_BATTERY_THRESHOLD -> ProcessingQuality.LOW
            batteryLevel < MEDIUM_BATTERY_THRESHOLD -> ProcessingQuality.MEDIUM
            else -> ProcessingQuality.HIGH
        }
    }
    
    fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        batteryLevel = (level * 100 / scale.toFloat()).coerceIn(0f, 100f)
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                     status == BatteryManager.BATTERY_STATUS_FULL
        
        batteryTemperature = intent.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, 0
        ) / 10f
    }
    
    enum class ProcessingQuality {
        LOW,    // Fast inference, less accurate
        MEDIUM, // Balanced
        HIGH    // Slower, more accurate
    }
    
    companion object {
        // Battery thresholds
        private const val CRITICAL_BATTERY_THRESHOLD = 10f
        private const val LOW_BATTERY_THRESHOLD = 20f
        private const val MEDIUM_BATTERY_THRESHOLD = 50f
        
        // Capture intervals (milliseconds)
        private const val FAST_INTERVAL = 1000L       // Charging
        private const val DEFAULT_INTERVAL = 2000L    // Normal
        private const val MEDIUM_INTERVAL = 3000L     // Medium battery
        private const val SLOW_INTERVAL = 5000L       // Low battery
        private const val POWER_SAVE_INTERVAL = 7000L // Power save mode
    }
}
