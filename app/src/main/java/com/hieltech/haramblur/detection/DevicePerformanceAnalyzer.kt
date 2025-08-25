package com.hieltech.haramblur.detection

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes device performance characteristics and provides automatic quality adjustment
 * based on device capabilities and current system load
 */
@Singleton
class DevicePerformanceAnalyzer @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DevicePerformanceAnalyzer"
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8f // 80% memory usage
        private const val CPU_LOAD_THRESHOLD = 0.7f // 70% CPU usage
        private const val BATTERY_TEMPERATURE_THRESHOLD = 40.0f // 40°C
        private const val PERFORMANCE_CHECK_INTERVAL_MS = 5000L // 5 seconds
    }
    
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // Device characteristics (determined once at startup)
    private lateinit var deviceProfile: DeviceProfile
    
    // Real-time performance state
    private val _performanceState = MutableStateFlow(DevicePerformanceState.UNKNOWN)
    val performanceState: StateFlow<DevicePerformanceState> = _performanceState.asStateFlow()
    
    private val _recommendedQuality = MutableStateFlow(QualityLevel.BALANCED)
    val recommendedQuality: StateFlow<QualityLevel> = _recommendedQuality.asStateFlow()
    
    private val _systemMetrics = MutableStateFlow(SystemMetrics.empty())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()
    
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    /**
     * Initialize device performance analysis
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing device performance analyzer...")
            
            deviceProfile = analyzeDeviceCapabilities()
            _recommendedQuality.value = deviceProfile.recommendedQuality
            
            Log.d(TAG, "Device profile: $deviceProfile")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device performance analyzer", e)
            false
        }
    }
    
    /**
     * Start continuous performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = analysisScope.launch {
            while (isActive && isMonitoring) {
                try {
                    val metrics = collectSystemMetrics()
                    _systemMetrics.value = metrics
                    
                    val newState = analyzePerformanceState(metrics)
                    if (newState != _performanceState.value) {
                        _performanceState.value = newState
                        Log.d(TAG, "Performance state changed to: $newState")
                    }
                    
                    val newRecommendedQuality = calculateRecommendedQuality(metrics, newState)
                    if (newRecommendedQuality != _recommendedQuality.value) {
                        _recommendedQuality.value = newRecommendedQuality
                        Log.d(TAG, "Recommended quality changed to: $newRecommendedQuality")
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Error during performance monitoring", e)
                }
                
                delay(PERFORMANCE_CHECK_INTERVAL_MS)
            }
        }
        
        Log.d(TAG, "Performance monitoring started")
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "Performance monitoring stopped")
    }
    
    /**
     * Get device performance profile
     */
    fun getDeviceProfile(): DeviceProfile = deviceProfile
    
    /**
     * Get performance recommendations for specific operation
     */
    fun getPerformanceRecommendations(
        operationType: OperationType,
        currentMetrics: SystemMetrics = _systemMetrics.value
    ): PerformanceRecommendations {
        val baseRecommendations = when (deviceProfile.tier) {
            DeviceTier.HIGH_END -> PerformanceRecommendations.highEnd()
            DeviceTier.MID_RANGE -> PerformanceRecommendations.midRange()
            DeviceTier.LOW_END -> PerformanceRecommendations.lowEnd()
        }
        
        // Adjust based on current system state
        return adjustRecommendationsForCurrentState(baseRecommendations, currentMetrics, operationType)
    }
    
    private suspend fun analyzeDeviceCapabilities(): DeviceProfile {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        
        // Determine device tier based on specs
        val tier = when {
            totalRamMB >= 6144 && availableProcessors >= 8 -> DeviceTier.HIGH_END
            totalRamMB >= 3072 && availableProcessors >= 4 -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
        
        // Check GPU capabilities
        val hasGPUAcceleration = checkGPUCapabilities()
        
        // Determine recommended quality based on device capabilities
        val recommendedQuality = when (tier) {
            DeviceTier.HIGH_END -> QualityLevel.HIGH
            DeviceTier.MID_RANGE -> QualityLevel.BALANCED
            DeviceTier.LOW_END -> QualityLevel.FAST
        }
        
        return DeviceProfile(
            tier = tier,
            totalRamMB = totalRamMB,
            availableProcessors = availableProcessors,
            hasGPUAcceleration = hasGPUAcceleration,
            androidVersion = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            recommendedQuality = recommendedQuality,
            maxConcurrentOperations = when (tier) {
                DeviceTier.HIGH_END -> 4
                DeviceTier.MID_RANGE -> 2
                DeviceTier.LOW_END -> 1
            }
        )
    }
    
    private fun checkGPUCapabilities(): Boolean {
        return try {
            // Check for GPU acceleration support
            // This is a simplified check - in practice, you'd want more comprehensive GPU detection
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine GPU capabilities", e)
            false
        }
    }
    
    private fun collectSystemMetrics(): SystemMetrics {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val memoryPressure = 1.0f - (availableMemoryMB.toFloat() / totalMemoryMB)
        
        // Get CPU usage (simplified)
        val cpuUsage = getCPUUsage()
        
        // Get battery temperature if available
        val batteryTemperature = getBatteryTemperature()
        
        // Get current app memory usage
        val appMemoryUsageMB = getAppMemoryUsage()
        
        return SystemMetrics(
            availableMemoryMB = availableMemoryMB,
            totalMemoryMB = totalMemoryMB,
            memoryPressure = memoryPressure,
            cpuUsage = cpuUsage,
            batteryTemperature = batteryTemperature,
            appMemoryUsageMB = appMemoryUsageMB,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun getCPUUsage(): Float {
        return try {
            val statFile = File("/proc/stat")
            if (statFile.exists()) {
                // Simplified CPU usage calculation
                // In practice, you'd want to calculate this over time intervals
                0.3f // Placeholder - actual implementation would read /proc/stat
            } else {
                0.0f
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read CPU usage", e)
            0.0f
        }
    }
    
    private fun getBatteryTemperature(): Float {
        return try {
            // Simplified battery temperature reading
            // In practice, you'd register a BatteryManager receiver
            25.0f // Placeholder - actual implementation would use BatteryManager
        } catch (e: Exception) {
            Log.w(TAG, "Could not read battery temperature", e)
            25.0f
        }
    }
    
    private fun getAppMemoryUsage(): Long {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            memoryInfo.totalPss / 1024L // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Could not read app memory usage", e)
            0L
        }
    }
    
    private fun analyzePerformanceState(metrics: SystemMetrics): DevicePerformanceState {
        return when {
            metrics.memoryPressure > MEMORY_PRESSURE_THRESHOLD ||
            metrics.cpuUsage > CPU_LOAD_THRESHOLD ||
            metrics.batteryTemperature > BATTERY_TEMPERATURE_THRESHOLD -> {
                DevicePerformanceState.UNDER_PRESSURE
            }
            metrics.memoryPressure > 0.6f || metrics.cpuUsage > 0.5f -> {
                DevicePerformanceState.MODERATE_LOAD
            }
            else -> DevicePerformanceState.OPTIMAL
        }
    }
    
    private fun calculateRecommendedQuality(
        metrics: SystemMetrics,
        state: DevicePerformanceState
    ): QualityLevel {
        val baseQuality = deviceProfile.recommendedQuality
        
        return when (state) {
            DevicePerformanceState.UNDER_PRESSURE -> {
                // Reduce quality under pressure
                when (baseQuality) {
                    QualityLevel.HIGH -> QualityLevel.FAST
                    QualityLevel.BALANCED -> QualityLevel.ULTRA_FAST
                    QualityLevel.FAST -> QualityLevel.ULTRA_FAST
                    QualityLevel.ULTRA_FAST -> QualityLevel.ULTRA_FAST
                }
            }
            DevicePerformanceState.MODERATE_LOAD -> {
                // Slightly reduce quality under moderate load
                when (baseQuality) {
                    QualityLevel.HIGH -> QualityLevel.BALANCED
                    QualityLevel.BALANCED -> QualityLevel.FAST
                    QualityLevel.FAST -> QualityLevel.FAST
                    QualityLevel.ULTRA_FAST -> QualityLevel.ULTRA_FAST
                }
            }
            DevicePerformanceState.OPTIMAL -> baseQuality
            DevicePerformanceState.UNKNOWN -> QualityLevel.BALANCED
        }
    }
    
    private fun adjustRecommendationsForCurrentState(
        baseRecommendations: PerformanceRecommendations,
        metrics: SystemMetrics,
        operationType: OperationType
    ): PerformanceRecommendations {
        val adjustmentFactor = when (_performanceState.value) {
            DevicePerformanceState.UNDER_PRESSURE -> 0.5f
            DevicePerformanceState.MODERATE_LOAD -> 0.75f
            DevicePerformanceState.OPTIMAL -> 1.0f
            DevicePerformanceState.UNKNOWN -> 0.8f
        }
        
        return baseRecommendations.copy(
            maxConcurrentOperations = (baseRecommendations.maxConcurrentOperations * adjustmentFactor).toInt().coerceAtLeast(1),
            recommendedImageScale = (baseRecommendations.recommendedImageScale * adjustmentFactor).coerceAtLeast(0.25f),
            enableGPUAcceleration = baseRecommendations.enableGPUAcceleration && metrics.batteryTemperature < BATTERY_TEMPERATURE_THRESHOLD,
            maxProcessingTimeMs = (baseRecommendations.maxProcessingTimeMs / adjustmentFactor).toLong()
        )
    }
    
    fun cleanup() {
        stopMonitoring()
        analysisScope.cancel()
        Log.d(TAG, "Device performance analyzer cleaned up")
    }
}

/**
 * Device performance tiers
 */
enum class DeviceTier(val displayName: String) {
    HIGH_END("High-end device"),
    MID_RANGE("Mid-range device"),
    LOW_END("Low-end device")
}

/**
 * Current device performance state
 */
enum class DevicePerformanceState(val displayName: String) {
    OPTIMAL("Optimal performance"),
    MODERATE_LOAD("Moderate system load"),
    UNDER_PRESSURE("System under pressure"),
    UNKNOWN("Unknown state")
}

/**
 * Device profile with capabilities
 */
data class DeviceProfile(
    val tier: DeviceTier,
    val totalRamMB: Long,
    val availableProcessors: Int,
    val hasGPUAcceleration: Boolean,
    val androidVersion: Int,
    val deviceModel: String,
    val recommendedQuality: QualityLevel,
    val maxConcurrentOperations: Int
)

/**
 * Current system metrics
 */
data class SystemMetrics(
    val availableMemoryMB: Long,
    val totalMemoryMB: Long,
    val memoryPressure: Float, // 0.0 to 1.0
    val cpuUsage: Float, // 0.0 to 1.0
    val batteryTemperature: Float, // Celsius
    val appMemoryUsageMB: Long,
    val timestamp: Long
) {
    companion object {
        fun empty() = SystemMetrics(0L, 0L, 0f, 0f, 25f, 0L, 0L)
    }
}

/**
 * Performance recommendations for operations
 */
data class PerformanceRecommendations(
    val maxConcurrentOperations: Int,
    val recommendedImageScale: Float,
    val enableGPUAcceleration: Boolean,
    val maxProcessingTimeMs: Long,
    val recommendedQuality: QualityLevel,
    val enableFrameSkipping: Boolean,
    val cacheSize: Int
) {
    companion object {
        fun highEnd() = PerformanceRecommendations(
            maxConcurrentOperations = 4,
            recommendedImageScale = 1.0f,
            enableGPUAcceleration = true,
            maxProcessingTimeMs = 200L,
            recommendedQuality = QualityLevel.HIGH,
            enableFrameSkipping = false,
            cacheSize = 50
        )
        
        fun midRange() = PerformanceRecommendations(
            maxConcurrentOperations = 2,
            recommendedImageScale = 0.75f,
            enableGPUAcceleration = true,
            maxProcessingTimeMs = 100L,
            recommendedQuality = QualityLevel.BALANCED,
            enableFrameSkipping = true,
            cacheSize = 25
        )
        
        fun lowEnd() = PerformanceRecommendations(
            maxConcurrentOperations = 1,
            recommendedImageScale = 0.5f,
            enableGPUAcceleration = false,
            maxProcessingTimeMs = 50L,
            recommendedQuality = QualityLevel.FAST,
            enableFrameSkipping = true,
            cacheSize = 10
        )
    }
}