package com.hieltech.haramblur.recovery

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.accessibility.BlurOverlayManager
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.accessibility.ScreenCaptureManager
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.ContentDetectionEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive crash recovery system for HaramBlur
 * Monitors system health and automatically recovers from crashes and stuck states
 */
@Singleton
class CrashRecoverySystem @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private const val TAG = "CrashRecoverySystem"
        private const val HEALTH_CHECK_INTERVAL = 10000L // 10 seconds
        private const val CRITICAL_FAILURE_THRESHOLD = 3
        private const val SERVICE_RESTART_DELAY = 5000L // 5 seconds
        private const val MAX_RECOVERY_ATTEMPTS = 3
    }
    
    enum class SystemHealth {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        RECOVERING,
        FAILED
    }
    
    data class HealthStatus(
        val overall: SystemHealth,
        val serviceStatus: String,
        val overlayStatus: String,
        val detectionStatus: String,
        val screenCaptureStatus: String,
        val lastRecoveryTime: Long,
        val recoveryAttempts: Int,
        val criticalErrors: List<String>
    )
    
    private val recoveryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _healthStatus = MutableStateFlow(
        HealthStatus(
            overall = SystemHealth.HEALTHY,
            serviceStatus = "Unknown",
            overlayStatus = "Unknown", 
            detectionStatus = "Unknown",
            screenCaptureStatus = "Unknown",
            lastRecoveryTime = 0L,
            recoveryAttempts = 0,
            criticalErrors = emptyList()
        )
    )
    val healthStatus: StateFlow<HealthStatus> = _healthStatus
    
    private var healthMonitorJob: Job? = null
    private var recoveryAttempts = 0
    private var lastRecoveryTime = 0L
    private val criticalErrors = mutableListOf<String>()
    
    // Component references (injected when available)
    private var blurOverlayManager: BlurOverlayManager? = null
    private var screenCaptureManager: ScreenCaptureManager? = null
    private var contentDetectionEngine: ContentDetectionEngine? = null
    
    /**
     * Initialize the crash recovery system
     */
    fun initialize() {
        Log.i(TAG, "🛡️ Initializing Crash Recovery System")
        startHealthMonitoring()
    }
    
    /**
     * Register component references for monitoring
     */
    fun registerComponents(
        overlayManager: BlurOverlayManager? = null,
        captureManager: ScreenCaptureManager? = null,
        detectionEngine: ContentDetectionEngine? = null
    ) {
        blurOverlayManager = overlayManager
        screenCaptureManager = captureManager
        contentDetectionEngine = detectionEngine
        Log.d(TAG, "✅ Components registered for crash recovery")
    }
    
    /**
     * Report a critical error to the recovery system
     */
    fun reportCriticalError(component: String, error: String, exception: Exception? = null) {
        val errorMsg = "$component: $error"
        Log.e(TAG, "🚨 Critical error reported: $errorMsg", exception)
        
        synchronized(criticalErrors) {
            criticalErrors.add(errorMsg)
            // Keep only recent errors
            if (criticalErrors.size > 10) {
                criticalErrors.removeAt(0)
            }
        }
        
        // Trigger recovery if needed
        recoveryScope.launch {
            handleCriticalError(component, error)
        }
    }
    
    /**
     * Start continuous health monitoring
     */
    private fun startHealthMonitoring() {
        healthMonitorJob?.cancel()
        healthMonitorJob = recoveryScope.launch {
            Log.i(TAG, "🔍 Starting health monitoring")
            
            while (isActive) {
                try {
                    performHealthCheck()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in health monitoring", e)
                    delay(HEALTH_CHECK_INTERVAL * 2) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Perform comprehensive health check of all components
     */
    private suspend fun performHealthCheck() {
        try {
            val serviceStatus = checkAccessibilityServiceHealth()
            val overlayStatus = checkOverlayManagerHealth()
            val detectionStatus = checkDetectionEngineHealth()
            val captureStatus = checkScreenCaptureHealth()
            
            val overallHealth = determineOverallHealth(
                serviceStatus, overlayStatus, detectionStatus, captureStatus
            )
            
            val currentStatus = HealthStatus(
                overall = overallHealth,
                serviceStatus = serviceStatus,
                overlayStatus = overlayStatus,
                detectionStatus = detectionStatus,
                screenCaptureStatus = captureStatus,
                lastRecoveryTime = lastRecoveryTime,
                recoveryAttempts = recoveryAttempts,
                criticalErrors = synchronized(criticalErrors) { criticalErrors.toList() }
            )
            
            _healthStatus.value = currentStatus
            
            // Log health status periodically
            if (System.currentTimeMillis() % (HEALTH_CHECK_INTERVAL * 6) < HEALTH_CHECK_INTERVAL) {
                Log.d(TAG, "🏥 Health check: $overallHealth - Service: $serviceStatus, Overlay: $overlayStatus, Detection: $detectionStatus, Capture: $captureStatus")
            }
            
            // Trigger recovery if needed
            if (overallHealth == SystemHealth.CRITICAL) {
                triggerRecovery("Critical system health detected")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during health check", e)
        }
    }
    
    /**
     * Check accessibility service health
     */
    private fun checkAccessibilityServiceHealth(): String {
        return try {
            val service = HaramBlurAccessibilityService.getInstance()
            when {
                service == null -> "Service not running"
                !HaramBlurAccessibilityService.isServiceRunning() -> "Service instance null"
                else -> {
                    val status = service.getServiceStatus()
                    when {
                        status.lastError.isNotEmpty() -> "Service has errors: ${status.lastError}"
                        !status.isProcessingActive -> "Processing inactive"
                        status.averageProcessingTime > 5000f -> "Slow processing (${status.averageProcessingTime.toInt()}ms)"
                        else -> "Healthy"
                    }
                }
            }
        } catch (e: Exception) {
            "Error checking service: ${e.message}"
        }
    }
    
    /**
     * Check overlay manager health
     */
    private fun checkOverlayManagerHealth(): String {
        return try {
            val manager = blurOverlayManager
            when {
                manager == null -> "Manager not registered"
                else -> {
                    // Check if overlay is stuck (this would need to be exposed by overlay manager)
                    "Healthy" // Simplified for now
                }
            }
        } catch (e: Exception) {
            "Error checking overlay: ${e.message}"
        }
    }
    
    /**
     * Check detection engine health  
     */
    private fun checkDetectionEngineHealth(): String {
        return try {
            val engine = contentDetectionEngine
            when {
                engine == null -> "Engine not registered"
                !engine.isEngineReady() -> "Engine not ready"
                else -> "Healthy"
            }
        } catch (e: Exception) {
            "Error checking detection: ${e.message}"
        }
    }
    
    /**
     * Check screen capture health
     */
    private fun checkScreenCaptureHealth(): String {
        return try {
            val capture = screenCaptureManager
            when {
                capture == null -> "Capture not registered"
                !capture.isCapturingActive() -> "Not capturing"
                else -> "Healthy"
            }
        } catch (e: Exception) {
            "Error checking capture: ${e.message}"
        }
    }
    
    /**
     * Determine overall system health based on component status
     */
    private fun determineOverallHealth(
        serviceStatus: String,
        overlayStatus: String, 
        detectionStatus: String,
        captureStatus: String
    ): SystemHealth {
        val healthyComponents = listOf(serviceStatus, overlayStatus, detectionStatus, captureStatus)
            .count { it == "Healthy" }
        
        val hasErrors = listOf(serviceStatus, overlayStatus, detectionStatus, captureStatus)
            .any { it.contains("Error") || it.contains("error") }
        
        return when {
            hasErrors || healthyComponents == 0 -> SystemHealth.CRITICAL
            healthyComponents == 4 -> SystemHealth.HEALTHY
            healthyComponents >= 2 -> SystemHealth.DEGRADED
            else -> SystemHealth.CRITICAL
        }
    }
    
    /**
     * Handle critical error reports
     */
    private suspend fun handleCriticalError(component: String, error: String) {
        try {
            Log.w(TAG, "🛠️ Handling critical error in $component: $error")
            
            when (component.lowercase()) {
                "accessibility", "service" -> handleServiceError()
                "overlay", "blur" -> handleOverlayError() 
                "detection", "engine" -> handleDetectionError()
                "capture", "screenshot" -> handleCaptureError()
                else -> triggerGeneralRecovery()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling critical error", e)
        }
    }
    
    /**
     * Handle accessibility service errors
     */
    private suspend fun handleServiceError() {
        try {
            Log.w(TAG, "🔧 Attempting service recovery")
            
            val service = HaramBlurAccessibilityService.getInstance()
            service?.let {
                // Try to reset service state
                it.emergencyHideAllOverlays()
                
                // Send emergency reset
                HaramBlurAccessibilityService.sendEmergencyResetBroadcast(context)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Service recovery failed", e)
        }
    }
    
    /**
     * Handle overlay manager errors
     */
    private suspend fun handleOverlayError() {
        try {
            Log.w(TAG, "🔧 Attempting overlay recovery")
            
            blurOverlayManager?.let { manager ->
                manager.emergencyHideAllOverlays()
                delay(1000) // Wait for cleanup
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Overlay recovery failed", e)
        }
    }
    
    /**
     * Handle detection engine errors
     */
    private suspend fun handleDetectionError() {
        try {
            Log.w(TAG, "🔧 Attempting detection engine recovery")
            
            contentDetectionEngine?.let { engine ->
                // This would need to be implemented in the detection engine
                // For now, just log
                Log.d(TAG, "Detection engine recovery not yet implemented")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Detection recovery failed", e)
        }
    }
    
    /**
     * Handle screen capture errors
     */
    private suspend fun handleCaptureError() {
        try {
            Log.w(TAG, "🔧 Attempting screen capture recovery")
            
            screenCaptureManager?.let { capture ->
                capture.stopCapturing()
                delay(2000) // Wait for cleanup
                
                // Try to restart capture if service is available
                val service = HaramBlurAccessibilityService.getInstance()
                if (service != null) {
                    // This would need to be re-implemented to restart capture
                    Log.d(TAG, "Screen capture restart not yet implemented")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Capture recovery failed", e)
        }
    }
    
    /**
     * Trigger comprehensive system recovery
     */
    private suspend fun triggerRecovery(reason: String) {
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            Log.e(TAG, "❌ Max recovery attempts reached - system may be permanently damaged")
            _healthStatus.value = _healthStatus.value.copy(overall = SystemHealth.FAILED)
            return
        }
        
        try {
            Log.w(TAG, "🚨 Triggering system recovery: $reason")
            _healthStatus.value = _healthStatus.value.copy(overall = SystemHealth.RECOVERING)
            
            recoveryAttempts++
            lastRecoveryTime = System.currentTimeMillis()
            
            // Comprehensive recovery sequence
            performComprehensiveRecovery()
            
            // Wait and check if recovery was successful
            delay(SERVICE_RESTART_DELAY)
            
            val newHealth = _healthStatus.value
            if (newHealth.overall == SystemHealth.HEALTHY || newHealth.overall == SystemHealth.DEGRADED) {
                Log.w(TAG, "✅ System recovery successful")
                recoveryAttempts = 0 // Reset on success
            } else {
                Log.w(TAG, "⚠️ System recovery incomplete - health is still ${newHealth.overall}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed", e)
        }
    }
    
    /**
     * Trigger general recovery for unknown errors
     */
    private suspend fun triggerGeneralRecovery() {
        triggerRecovery("General error recovery")
    }
    
    /**
     * Perform comprehensive system recovery
     */
    private suspend fun performComprehensiveRecovery() {
        try {
            Log.w(TAG, "🏥 Performing comprehensive system recovery")
            
            // 1. Emergency overlay cleanup
            handleOverlayError()
            
            // 2. Service state reset
            handleServiceError()
            
            // 3. Clear caches and reset states
            System.gc() // Force garbage collection
            
            // 4. Clear error tracking
            synchronized(criticalErrors) {
                criticalErrors.clear()
            }
            
            Log.w(TAG, "🏥 Comprehensive recovery completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Comprehensive recovery failed", e)
        }
    }
    
    /**
     * Get current system health
     */
    fun getCurrentHealth(): SystemHealth {
        return _healthStatus.value.overall
    }
    
    /**
     * Force a health check
     */
    fun forceHealthCheck() {
        recoveryScope.launch {
            performHealthCheck()
        }
    }
    
    /**
     * Shutdown the crash recovery system
     */
    fun shutdown() {
        Log.i(TAG, "🛡️ Shutting down Crash Recovery System")
        healthMonitorJob?.cancel()
        recoveryScope.cancel()
    }
}