package com.hieltech.haramblur.detection

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors and validates performance requirements for ultra-fast content detection
 * Ensures <50ms processing time requirement is met
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val devicePerformanceAnalyzer: DevicePerformanceAnalyzer,
    private val memoryManager: MemoryManager,
    private val batteryOptimizationManager: BatteryOptimizationManager
) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val ULTRA_FAST_TARGET_MS = 50L
        private const val FAST_TARGET_MS = 100L
        private const val BALANCED_TARGET_MS = 200L
        private const val MONITORING_WINDOW_MS = 10000L // 10 seconds
        private const val MAX_SAMPLES = 100
        private const val ALERT_THRESHOLD_VIOLATIONS = 5 // Alert after 5 consecutive violations
    }
    
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Performance tracking
    private val performanceSamples = ConcurrentLinkedQueue<PerformanceSample>()
    private val totalMeasurements = AtomicLong(0)
    private val violationCount = AtomicLong(0)
    private val consecutiveViolations = AtomicLong(0)
    
    // Real-time performance state
    private val _performanceState = MutableStateFlow(PerformanceState.OPTIMAL)
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    private val _currentMetrics = MutableStateFlow(PerformanceMetrics.empty())
    val currentMetrics: StateFlow<PerformanceMetrics> = _currentMetrics.asStateFlow()
    
    // Automatic quality adjustment
    private var autoAdjustmentEnabled = true
    private var lastAdjustmentTime = 0L
    private val adjustmentCooldownMs = 2000L // 2 seconds between adjustments
    
    /**
     * Initialize performance monitoring with integrated components
     */
    suspend fun initialize(): Boolean {
        return try {
            devicePerformanceAnalyzer.initialize()
            batteryOptimizationManager.initialize()
            
            // Start monitoring
            devicePerformanceAnalyzer.startMonitoring()
            batteryOptimizationManager.startMonitoring()
            
            Log.d(TAG, "Performance monitor initialized with integrated components")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize performance monitor", e)
            false
        }
    }

    /**
     * Record a performance measurement with integrated optimization
     */
    fun recordMeasurement(
        processingTimeMs: Long,
        targetTimeMs: Long,
        operationType: OperationType,
        qualityLevel: QualityLevel,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val sample = PerformanceSample(
            timestamp = System.currentTimeMillis(),
            processingTimeMs = processingTimeMs,
            targetTimeMs = targetTimeMs,
            operationType = operationType,
            qualityLevel = qualityLevel,
            isViolation = processingTimeMs > targetTimeMs,
            additionalData = additionalData
        )
        
        // Add sample and maintain window size
        performanceSamples.offer(sample)
        if (performanceSamples.size > MAX_SAMPLES) {
            performanceSamples.poll()
        }
        
        totalMeasurements.incrementAndGet()
        
        // Track violations
        if (sample.isViolation) {
            violationCount.incrementAndGet()
            consecutiveViolations.incrementAndGet()
        } else {
            consecutiveViolations.set(0)
        }
        
        // Update real-time metrics
        updateCurrentMetrics()
        
        // Check for performance alerts
        checkPerformanceAlerts(sample)
        
        // Check battery throttling
        val throttleDecision = batteryOptimizationManager.shouldThrottleOperation(operationType)
        if (throttleDecision.shouldThrottle) {
            Log.d(TAG, "Operation throttled: ${throttleDecision.reason}")
        }
        
        Log.d(TAG, "Performance recorded: ${processingTimeMs}ms (target: ${targetTimeMs}ms, type: $operationType, quality: $qualityLevel)")
    }
    
    /**
     * Get integrated performance recommendations
     */
    fun getIntegratedRecommendations(operationType: OperationType): PerformanceRecommendations {
        val deviceRecommendations = devicePerformanceAnalyzer.getPerformanceRecommendations(operationType)
        val batteryOptimizedRecommendations = batteryOptimizationManager.getBatteryOptimizedRecommendations(deviceRecommendations)
        
        return batteryOptimizedRecommendations
    }

    /**
     * Get comprehensive performance report with integrated metrics
     */
    fun getPerformanceReport(): PerformanceReport {
        val samples = performanceSamples.toList()
        val cutoffTime = System.currentTimeMillis() - MONITORING_WINDOW_MS
        val recentSamples = samples.filter { it.timestamp >= cutoffTime }
        
        if (recentSamples.isEmpty()) {
            return PerformanceReport.empty()
        }
        
        // Calculate statistics
        val processingTimes = recentSamples.map { it.processingTimeMs }
        val avgProcessingTime = processingTimes.average()
        val minProcessingTime = processingTimes.minOrNull() ?: 0L
        val maxProcessingTime = processingTimes.maxOrNull() ?: 0L
        val medianProcessingTime = processingTimes.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
        }
        
        // Calculate percentiles
        val sortedTimes = processingTimes.sorted()
        val p95 = sortedTimes.getOrNull((sortedTimes.size * 0.95).toInt()) ?: maxProcessingTime
        val p99 = sortedTimes.getOrNull((sortedTimes.size * 0.99).toInt()) ?: maxProcessingTime
        
        // Violation statistics
        val violations = recentSamples.count { it.isViolation }
        val violationRate = violations.toFloat() / recentSamples.size
        
        // Performance by operation type
        val operationStats = OperationType.values().associateWith { opType ->
            val opSamples = recentSamples.filter { it.operationType == opType }
            if (opSamples.isNotEmpty()) {
                OperationStats(
                    count = opSamples.size,
                    avgTimeMs = opSamples.map { it.processingTimeMs }.average(),
                    violationRate = opSamples.count { it.isViolation }.toFloat() / opSamples.size
                )
            } else {
                OperationStats.empty()
            }
        }
        
        // Performance by quality level
        val qualityStats = QualityLevel.values().associateWith { quality ->
            val qualitySamples = recentSamples.filter { it.qualityLevel == quality }
            if (qualitySamples.isNotEmpty()) {
                QualityStats(
                    count = qualitySamples.size,
                    avgTimeMs = qualitySamples.map { it.processingTimeMs }.average(),
                    violationRate = qualitySamples.count { it.isViolation }.toFloat() / qualitySamples.size,
                    targetAchievementRate = qualitySamples.count { !it.isViolation }.toFloat() / qualitySamples.size
                )
            } else {
                QualityStats.empty()
            }
        }
        
        // Get integrated metrics
        val deviceProfile = devicePerformanceAnalyzer.getDeviceProfile()
        val memoryStats = memoryManager.getMemoryStats()
        val batteryStats = batteryOptimizationManager.getBatteryStats()
        
        return PerformanceReport(
            windowStartTime = cutoffTime,
            windowEndTime = System.currentTimeMillis(),
            totalSamples = recentSamples.size,
            avgProcessingTimeMs = avgProcessingTime,
            minProcessingTimeMs = minProcessingTime,
            maxProcessingTimeMs = maxProcessingTime,
            medianProcessingTimeMs = medianProcessingTime.toLong(),
            p95ProcessingTimeMs = p95,
            p99ProcessingTimeMs = p99,
            violationCount = violations,
            violationRate = violationRate,
            consecutiveViolations = consecutiveViolations.get(),
            operationStats = operationStats,
            qualityStats = qualityStats,
            currentState = _performanceState.value,
            recommendations = generateIntegratedRecommendations(recentSamples, deviceProfile, memoryStats, batteryStats),
            deviceProfile = deviceProfile,
            memoryStats = memoryStats,
            batteryStats = batteryStats
        )
    }
    
    /**
     * Validate ultra-fast mode performance (<50ms requirement)
     */
    fun validateUltraFastPerformance(): ValidationResult {
        val samples = performanceSamples.toList()
        val ultraFastSamples = samples.filter { 
            it.qualityLevel == QualityLevel.ULTRA_FAST && 
            it.timestamp >= System.currentTimeMillis() - MONITORING_WINDOW_MS 
        }
        
        if (ultraFastSamples.isEmpty()) {
            return ValidationResult(
                isValid = false,
                reason = "No ultra-fast samples available for validation",
                avgProcessingTime = 0.0,
                maxProcessingTime = 0L,
                violationRate = 0.0f,
                sampleCount = 0
            )
        }
        
        val processingTimes = ultraFastSamples.map { it.processingTimeMs }
        val avgTime = processingTimes.average()
        val maxTime = processingTimes.maxOrNull() ?: 0L
        val violations = ultraFastSamples.count { it.processingTimeMs > ULTRA_FAST_TARGET_MS }
        val violationRate = violations.toFloat() / ultraFastSamples.size
        
        val isValid = violationRate < 0.1f && avgTime < ULTRA_FAST_TARGET_MS
        
        return ValidationResult(
            isValid = isValid,
            reason = if (isValid) {
                "Ultra-fast performance validated: ${avgTime.toInt()}ms avg, ${(violationRate * 100).toInt()}% violations"
            } else {
                "Ultra-fast performance failed: ${avgTime.toInt()}ms avg (target: ${ULTRA_FAST_TARGET_MS}ms), ${(violationRate * 100).toInt()}% violations"
            },
            avgProcessingTime = avgTime,
            maxProcessingTime = maxTime,
            violationRate = violationRate,
            sampleCount = ultraFastSamples.size
        )
    }
    
    /**
     * Enable or disable automatic quality adjustment
     */
    fun setAutoAdjustmentEnabled(enabled: Boolean) {
        autoAdjustmentEnabled = enabled
        Log.d(TAG, "Auto adjustment ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Handle memory pressure through integrated memory manager
     */
    suspend fun handleMemoryPressure(level: MemoryPressureLevel) {
        memoryManager.handleMemoryPressure(level)
        Log.d(TAG, "Memory pressure handled: $level")
    }

    /**
     * Clear all performance data
     */
    fun clearData() {
        performanceSamples.clear()
        totalMeasurements.set(0)
        violationCount.set(0)
        consecutiveViolations.set(0)
        _performanceState.value = PerformanceState.OPTIMAL
        _currentMetrics.value = PerformanceMetrics.empty()
        memoryManager.clearAllCaches()
        Log.d(TAG, "Performance data cleared")
    }
    
    private fun updateCurrentMetrics() {
        val samples = performanceSamples.toList()
        val recentSamples = samples.takeLast(10) // Last 10 samples
        
        if (recentSamples.isEmpty()) return
        
        val avgTime = recentSamples.map { it.processingTimeMs }.average()
        val recentViolations = recentSamples.count { it.isViolation }
        val violationRate = recentViolations.toFloat() / recentSamples.size
        
        _currentMetrics.value = PerformanceMetrics(
            avgProcessingTimeMs = avgTime,
            recentViolationRate = violationRate,
            consecutiveViolations = consecutiveViolations.get(),
            totalMeasurements = totalMeasurements.get(),
            isPerformingWell = violationRate < 0.2f && avgTime < 150.0
        )
    }
    
    private fun checkPerformanceAlerts(sample: PerformanceSample) {
        val currentState = when {
            consecutiveViolations.get() >= ALERT_THRESHOLD_VIOLATIONS -> PerformanceState.CRITICAL
            consecutiveViolations.get() >= 3 -> PerformanceState.WARNING
            sample.processingTimeMs > sample.targetTimeMs * 1.5f -> PerformanceState.DEGRADED
            else -> PerformanceState.OPTIMAL
        }
        
        if (currentState != _performanceState.value) {
            _performanceState.value = currentState
            Log.w(TAG, "Performance state changed to: $currentState")
            
            // Trigger automatic adjustment if enabled
            if (autoAdjustmentEnabled && currentState != PerformanceState.OPTIMAL) {
                triggerAutoAdjustment(currentState)
            }
        }
    }
    
    private fun triggerAutoAdjustment(state: PerformanceState) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdjustmentTime < adjustmentCooldownMs) {
            return // Too soon for another adjustment
        }
        
        lastAdjustmentTime = currentTime
        
        monitoringScope.launch {
            when (state) {
                PerformanceState.CRITICAL -> {
                    Log.w(TAG, "Critical performance detected, suggesting quality reduction")
                    // Could trigger callback to reduce quality
                }
                PerformanceState.WARNING -> {
                    Log.w(TAG, "Performance warning, monitoring closely")
                }
                PerformanceState.DEGRADED -> {
                    Log.w(TAG, "Performance degraded, considering optimizations")
                }
                PerformanceState.OPTIMAL -> {
                    // No action needed
                }
            }
        }
    }
    
    private fun generateRecommendations(samples: List<PerformanceSample>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (samples.isEmpty()) return recommendations
        
        val avgTime = samples.map { it.processingTimeMs }.average()
        val violationRate = samples.count { it.isViolation }.toFloat() / samples.size
        
        // Performance recommendations
        if (violationRate > 0.3f) {
            recommendations.add("High violation rate (${(violationRate * 100).toInt()}%) - consider reducing quality level")
        }
        
        if (avgTime > 200.0) {
            recommendations.add("Average processing time is high (${avgTime.toInt()}ms) - enable GPU acceleration or reduce image resolution")
        }
        
        val ultraFastSamples = samples.filter { it.qualityLevel == QualityLevel.ULTRA_FAST }
        if (ultraFastSamples.isNotEmpty()) {
            val ultraFastAvg = ultraFastSamples.map { it.processingTimeMs }.average()
            if (ultraFastAvg > ULTRA_FAST_TARGET_MS) {
                recommendations.add("Ultra-fast mode not meeting <50ms target (${ultraFastAvg.toInt()}ms avg)")
            }
        }
        
        // Quality level recommendations
        val qualityDistribution = samples.groupBy { it.qualityLevel }
        if (qualityDistribution[QualityLevel.HIGH]?.size ?: 0 > samples.size * 0.5f && violationRate > 0.2f) {
            recommendations.add("Consider using BALANCED quality level instead of HIGH for better performance")
        }
        
        return recommendations
    }
    
    private fun generateIntegratedRecommendations(
        samples: List<PerformanceSample>,
        deviceProfile: DeviceProfile,
        memoryStats: MemoryStats,
        batteryStats: BatteryStats
    ): List<String> {
        val recommendations = generateRecommendations(samples).toMutableList()
        
        // Add device-specific recommendations
        if (deviceProfile.tier == DeviceTier.LOW_END) {
            recommendations.add("Consider using ULTRA_FAST mode on low-end device")
        }
        
        // Add memory-specific recommendations
        if (memoryStats.cacheHitRate < 0.5f) {
            recommendations.add("Low cache hit rate (${(memoryStats.cacheHitRate * 100).toInt()}%) - consider increasing cache size")
        }
        
        if (memoryStats.usedMemoryMB.toFloat() / memoryStats.maxMemoryMB > 0.8f) {
            recommendations.add("High memory usage - consider reducing image resolution or cache size")
        }
        
        // Add battery-specific recommendations
        if (batteryStats.currentLevel < 20) {
            recommendations.add("Low battery (${batteryStats.currentLevel}%) - enable aggressive battery optimization")
        }
        
        if (batteryStats.temperature > 40.0f) {
            recommendations.add("High battery temperature (${batteryStats.temperature}°C) - disable GPU acceleration")
        }
        
        return recommendations
    }
    
    fun cleanup() {
        devicePerformanceAnalyzer.stopMonitoring()
        batteryOptimizationManager.stopMonitoring()
        memoryManager.cleanup()
        devicePerformanceAnalyzer.cleanup()
        batteryOptimizationManager.cleanup()
        Log.d(TAG, "Performance monitor cleaned up")
    }
}

/**
 * Performance sample data
 */
private data class PerformanceSample(
    val timestamp: Long,
    val processingTimeMs: Long,
    val targetTimeMs: Long,
    val operationType: OperationType,
    val qualityLevel: QualityLevel,
    val isViolation: Boolean,
    val additionalData: Map<String, Any>
)

/**
 * Current performance state
 */
enum class PerformanceState(val displayName: String, val description: String) {
    OPTIMAL("Optimal", "Performance is meeting all targets"),
    DEGRADED("Degraded", "Performance is below optimal but acceptable"),
    WARNING("Warning", "Performance issues detected, monitoring closely"),
    CRITICAL("Critical", "Severe performance issues, immediate action needed")
}

/**
 * Real-time performance metrics
 */
data class PerformanceMetrics(
    val avgProcessingTimeMs: Double,
    val recentViolationRate: Float,
    val consecutiveViolations: Long,
    val totalMeasurements: Long,
    val isPerformingWell: Boolean
) {
    companion object {
        fun empty() = PerformanceMetrics(0.0, 0f, 0L, 0L, true)
    }
}

/**
 * Operation types for performance tracking
 */
enum class OperationType {
    FAST_DETECTION,
    DENSITY_ANALYSIS,
    FACE_DETECTION,
    NSFW_DETECTION,
    GENDER_DETECTION,
    FULL_PIPELINE
}

/**
 * Statistics for specific operations
 */
data class OperationStats(
    val count: Int,
    val avgTimeMs: Double,
    val violationRate: Float
) {
    companion object {
        fun empty() = OperationStats(0, 0.0, 0f)
    }
}

/**
 * Statistics for quality levels
 */
data class QualityStats(
    val count: Int,
    val avgTimeMs: Double,
    val violationRate: Float,
    val targetAchievementRate: Float
) {
    companion object {
        fun empty() = QualityStats(0, 0.0, 0f, 0f)
    }
}

/**
 * Comprehensive performance report with integrated metrics
 */
data class PerformanceReport(
    val windowStartTime: Long,
    val windowEndTime: Long,
    val totalSamples: Int,
    val avgProcessingTimeMs: Double,
    val minProcessingTimeMs: Long,
    val maxProcessingTimeMs: Long,
    val medianProcessingTimeMs: Long,
    val p95ProcessingTimeMs: Long,
    val p99ProcessingTimeMs: Long,
    val violationCount: Int,
    val violationRate: Float,
    val consecutiveViolations: Long,
    val operationStats: Map<OperationType, OperationStats>,
    val qualityStats: Map<QualityLevel, QualityStats>,
    val currentState: PerformanceState,
    val recommendations: List<String>,
    val deviceProfile: DeviceProfile? = null,
    val memoryStats: MemoryStats? = null,
    val batteryStats: BatteryStats? = null
) {
    companion object {
        fun empty() = PerformanceReport(
            windowStartTime = 0L,
            windowEndTime = 0L,
            totalSamples = 0,
            avgProcessingTimeMs = 0.0,
            minProcessingTimeMs = 0L,
            maxProcessingTimeMs = 0L,
            medianProcessingTimeMs = 0L,
            p95ProcessingTimeMs = 0L,
            p99ProcessingTimeMs = 0L,
            violationCount = 0,
            violationRate = 0f,
            consecutiveViolations = 0L,
            operationStats = emptyMap(),
            qualityStats = emptyMap(),
            currentState = PerformanceState.OPTIMAL,
            recommendations = emptyList(),
            deviceProfile = null,
            memoryStats = null,
            batteryStats = null
        )
    }
}

/**
 * Ultra-fast performance validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val reason: String,
    val avgProcessingTime: Double,
    val maxProcessingTime: Long,
    val violationRate: Float,
    val sampleCount: Int
)