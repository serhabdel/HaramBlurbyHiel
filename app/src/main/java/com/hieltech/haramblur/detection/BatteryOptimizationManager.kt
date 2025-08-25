package com.hieltech.haramblur.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages battery usage optimization for detection algorithms
 * Automatically adjusts performance based on battery level and power state
 */
@Singleton
class BatteryOptimizationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "BatteryOptimizationManager"
        private const val LOW_BATTERY_THRESHOLD = 20 // 20%
        private const val CRITICAL_BATTERY_THRESHOLD = 10 // 10%
        private const val HIGH_TEMPERATURE_THRESHOLD = 40.0f // 40°C
        private const val CRITICAL_TEMPERATURE_THRESHOLD = 45.0f // 45°C
        private const val BATTERY_CHECK_INTERVAL_MS = 30000L // 30 seconds
    }
    
    private val optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // Battery state tracking
    private val _batteryState = MutableStateFlow(BatteryState.UNKNOWN)
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    private val _batteryMetrics = MutableStateFlow(BatteryMetrics.empty())
    val batteryMetrics: StateFlow<BatteryMetrics> = _batteryMetrics.asStateFlow()
    
    private val _optimizationLevel = MutableStateFlow(OptimizationLevel.NONE)
    val optimizationLevel: StateFlow<OptimizationLevel> = _optimizationLevel.asStateFlow()
    
    private var batteryReceiver: BatteryBroadcastReceiver? = null
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    // Battery optimization strategies
    private val optimizationStrategies = mapOf(
        OptimizationLevel.NONE to OptimizationStrategy(
            maxConcurrentOperations = 4,
            processingFrequencyMs = 100L,
            enableGPUAcceleration = true,
            qualityReduction = 0.0f,
            enableFrameSkipping = false,
            description = "No optimization"
        ),
        OptimizationLevel.LIGHT to OptimizationStrategy(
            maxConcurrentOperations = 3,
            processingFrequencyMs = 150L,
            enableGPUAcceleration = true,
            qualityReduction = 0.1f,
            enableFrameSkipping = true,
            description = "Light battery optimization"
        ),
        OptimizationLevel.MODERATE to OptimizationStrategy(
            maxConcurrentOperations = 2,
            processingFrequencyMs = 250L,
            enableGPUAcceleration = false,
            qualityReduction = 0.25f,
            enableFrameSkipping = true,
            description = "Moderate battery optimization"
        ),
        OptimizationLevel.AGGRESSIVE to OptimizationStrategy(
            maxConcurrentOperations = 1,
            processingFrequencyMs = 500L,
            enableGPUAcceleration = false,
            qualityReduction = 0.5f,
            enableFrameSkipping = true,
            description = "Aggressive battery optimization"
        ),
        OptimizationLevel.EXTREME to OptimizationStrategy(
            maxConcurrentOperations = 1,
            processingFrequencyMs = 1000L,
            enableGPUAcceleration = false,
            qualityReduction = 0.75f,
            enableFrameSkipping = true,
            description = "Extreme battery saving"
        )
    )
    
    /**
     * Initialize battery optimization monitoring
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing battery optimization manager...")
            
            // Register battery state receiver
            batteryReceiver = BatteryBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            context.registerReceiver(batteryReceiver, filter)
            
            // Get initial battery state
            updateBatteryState()
            
            Log.d(TAG, "Battery optimization manager initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize battery optimization manager", e)
            false
        }
    }
    
    /**
     * Start battery monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = optimizationScope.launch {
            while (isActive && isMonitoring) {
                try {
                    updateBatteryState()
                    delay(BATTERY_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Error during battery monitoring", e)
                }
            }
        }
        
        Log.d(TAG, "Battery monitoring started")
    }
    
    /**
     * Stop battery monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "Battery monitoring stopped")
    }
    
    /**
     * Get current optimization strategy
     */
    fun getCurrentOptimizationStrategy(): OptimizationStrategy {
        return optimizationStrategies[_optimizationLevel.value] ?: optimizationStrategies[OptimizationLevel.NONE]!!
    }
    
    /**
     * Get battery-optimized performance recommendations
     */
    fun getBatteryOptimizedRecommendations(
        baseRecommendations: PerformanceRecommendations
    ): PerformanceRecommendations {
        val strategy = getCurrentOptimizationStrategy()
        val batteryMetrics = _batteryMetrics.value
        
        return baseRecommendations.copy(
            maxConcurrentOperations = minOf(
                baseRecommendations.maxConcurrentOperations,
                strategy.maxConcurrentOperations
            ),
            enableGPUAcceleration = baseRecommendations.enableGPUAcceleration && 
                strategy.enableGPUAcceleration && 
                batteryMetrics.temperature < HIGH_TEMPERATURE_THRESHOLD,
            recommendedImageScale = baseRecommendations.recommendedImageScale * (1.0f - strategy.qualityReduction),
            maxProcessingTimeMs = maxOf(
                baseRecommendations.maxProcessingTimeMs,
                strategy.processingFrequencyMs
            ),
            enableFrameSkipping = baseRecommendations.enableFrameSkipping || strategy.enableFrameSkipping,
            cacheSize = if (_optimizationLevel.value >= OptimizationLevel.MODERATE) {
                baseRecommendations.cacheSize / 2
            } else {
                baseRecommendations.cacheSize
            }
        )
    }
    
    /**
     * Check if operation should be throttled based on battery state
     */
    fun shouldThrottleOperation(operationType: OperationType): ThrottleDecision {
        val batteryMetrics = _batteryMetrics.value
        val strategy = getCurrentOptimizationStrategy()
        
        return when {
            batteryMetrics.level <= CRITICAL_BATTERY_THRESHOLD -> {
                ThrottleDecision(
                    shouldThrottle = true,
                    delayMs = strategy.processingFrequencyMs * 2,
                    reason = "Critical battery level (${batteryMetrics.level}%)"
                )
            }
            batteryMetrics.temperature >= CRITICAL_TEMPERATURE_THRESHOLD -> {
                ThrottleDecision(
                    shouldThrottle = true,
                    delayMs = strategy.processingFrequencyMs * 3,
                    reason = "Critical temperature (${batteryMetrics.temperature}°C)"
                )
            }
            _optimizationLevel.value >= OptimizationLevel.MODERATE -> {
                ThrottleDecision(
                    shouldThrottle = true,
                    delayMs = strategy.processingFrequencyMs,
                    reason = "Battery optimization active (${_optimizationLevel.value})"
                )
            }
            else -> {
                ThrottleDecision(
                    shouldThrottle = false,
                    delayMs = 0L,
                    reason = "No throttling needed"
                )
            }
        }
    }
    
    /**
     * Get battery usage statistics
     */
    fun getBatteryStats(): BatteryStats {
        val metrics = _batteryMetrics.value
        val strategy = getCurrentOptimizationStrategy()
        
        return BatteryStats(
            currentLevel = metrics.level,
            isCharging = metrics.isCharging,
            temperature = metrics.temperature,
            voltage = metrics.voltage,
            powerSaveMode = metrics.powerSaveMode,
            optimizationLevel = _optimizationLevel.value,
            estimatedImpactReduction = calculateImpactReduction(strategy),
            batteryState = _batteryState.value
        )
    }
    
    /**
     * Force optimization level (for testing or manual control)
     */
    fun setOptimizationLevel(level: OptimizationLevel) {
        _optimizationLevel.value = level
        Log.d(TAG, "Optimization level manually set to: $level")
    }
    
    private fun updateBatteryState() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent == null) {
            Log.w(TAG, "Could not get battery information")
            return
        }
        
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }
        
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
        val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val powerSaveMode = powerManager.isPowerSaveMode
        
        // Update battery metrics
        val newMetrics = BatteryMetrics(
            level = batteryPct,
            isCharging = isCharging,
            temperature = temperature,
            voltage = voltage,
            powerSaveMode = powerSaveMode,
            timestamp = System.currentTimeMillis()
        )
        _batteryMetrics.value = newMetrics
        
        // Determine battery state
        val newBatteryState = when {
            batteryPct <= CRITICAL_BATTERY_THRESHOLD -> BatteryState.CRITICAL
            batteryPct <= LOW_BATTERY_THRESHOLD -> BatteryState.LOW
            temperature >= CRITICAL_TEMPERATURE_THRESHOLD -> BatteryState.OVERHEATING
            temperature >= HIGH_TEMPERATURE_THRESHOLD -> BatteryState.HOT
            isCharging -> BatteryState.CHARGING
            powerSaveMode -> BatteryState.POWER_SAVE
            else -> BatteryState.NORMAL
        }
        
        if (newBatteryState != _batteryState.value) {
            _batteryState.value = newBatteryState
            Log.d(TAG, "Battery state changed to: $newBatteryState")
        }
        
        // Update optimization level based on battery state
        val newOptimizationLevel = calculateOptimizationLevel(newBatteryState, newMetrics)
        if (newOptimizationLevel != _optimizationLevel.value) {
            _optimizationLevel.value = newOptimizationLevel
            Log.d(TAG, "Optimization level changed to: $newOptimizationLevel")
        }
    }
    
    private fun calculateOptimizationLevel(
        batteryState: BatteryState,
        metrics: BatteryMetrics
    ): OptimizationLevel {
        return when (batteryState) {
            BatteryState.CRITICAL -> OptimizationLevel.EXTREME
            BatteryState.LOW -> OptimizationLevel.AGGRESSIVE
            BatteryState.OVERHEATING -> OptimizationLevel.EXTREME
            BatteryState.HOT -> OptimizationLevel.AGGRESSIVE
            BatteryState.POWER_SAVE -> OptimizationLevel.MODERATE
            BatteryState.CHARGING -> OptimizationLevel.LIGHT
            BatteryState.NORMAL -> OptimizationLevel.NONE
            BatteryState.UNKNOWN -> OptimizationLevel.LIGHT
        }
    }
    
    private fun calculateImpactReduction(strategy: OptimizationStrategy): Float {
        // Estimate battery impact reduction based on optimization strategy
        val baseImpact = 1.0f
        val reductionFactors = listOf(
            1.0f - strategy.qualityReduction,
            strategy.processingFrequencyMs / 100.0f,
            if (strategy.enableGPUAcceleration) 1.0f else 0.7f,
            strategy.maxConcurrentOperations / 4.0f
        )
        
        return 1.0f - (reductionFactors.reduce { acc, factor -> acc * factor })
    }
    
    fun cleanup() {
        stopMonitoring()
        batteryReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering battery receiver", e)
            }
        }
        optimizationScope.cancel()
        Log.d(TAG, "Battery optimization manager cleaned up")
    }
    
    /**
     * Broadcast receiver for battery state changes
     */
    private inner class BatteryBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    optimizationScope.launch {
                        updateBatteryState()
                    }
                }
            }
        }
    }
}

/**
 * Battery states
 */
enum class BatteryState(val displayName: String) {
    NORMAL("Normal battery level"),
    LOW("Low battery"),
    CRITICAL("Critical battery level"),
    CHARGING("Charging"),
    HOT("Battery hot"),
    OVERHEATING("Battery overheating"),
    POWER_SAVE("Power save mode"),
    UNKNOWN("Unknown battery state")
}

/**
 * Battery optimization levels
 */
enum class OptimizationLevel(val displayName: String) {
    NONE("No optimization"),
    LIGHT("Light optimization"),
    MODERATE("Moderate optimization"),
    AGGRESSIVE("Aggressive optimization"),
    EXTREME("Extreme optimization")
}

/**
 * Battery optimization strategy
 */
data class OptimizationStrategy(
    val maxConcurrentOperations: Int,
    val processingFrequencyMs: Long,
    val enableGPUAcceleration: Boolean,
    val qualityReduction: Float, // 0.0 to 1.0
    val enableFrameSkipping: Boolean,
    val description: String
)

/**
 * Battery metrics
 */
data class BatteryMetrics(
    val level: Int, // 0-100
    val isCharging: Boolean,
    val temperature: Float, // Celsius
    val voltage: Int, // mV
    val powerSaveMode: Boolean,
    val timestamp: Long
) {
    companion object {
        fun empty() = BatteryMetrics(100, false, 25.0f, 4000, false, 0L)
    }
}

/**
 * Throttle decision for operations
 */
data class ThrottleDecision(
    val shouldThrottle: Boolean,
    val delayMs: Long,
    val reason: String
)

/**
 * Battery usage statistics
 */
data class BatteryStats(
    val currentLevel: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val voltage: Int,
    val powerSaveMode: Boolean,
    val optimizationLevel: OptimizationLevel,
    val estimatedImpactReduction: Float,
    val batteryState: BatteryState
)