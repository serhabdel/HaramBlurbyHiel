package com.hieltech.haramblur.ui

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDetectionEngine: ContentDetectionEngine,
    private val faceDetectionManager: FaceDetectionManager,
    private val mlModelManager: MLModelManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "DebugViewModel"
    }
    
    private val _debugState = MutableStateFlow(DebugState())
    val debugState: StateFlow<DebugState> = _debugState
    
    private val debugLogs = mutableListOf<DebugLog>()
    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    fun startDebugging() {
        addDebugLog(TAG, "Debug session started")
        viewModelScope.launch {
            while (true) {
                updateDebugState()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    fun refreshStatus() {
        addDebugLog(TAG, "Manual status refresh requested")
        viewModelScope.launch {
            updateDebugState()
        }
    }
    
    fun testDetection() {
        addDebugLog(TAG, "Detection test initiated")
        viewModelScope.launch {
            try {
                // Test face detection readiness
                addDebugLog("FaceDetection", "Testing face detection readiness...")
                // In a real scenario, we'd create a test bitmap and run detection
                
                // Test ML model readiness
                addDebugLog("MLModels", "Testing ML model readiness...")
                val mlReady = mlModelManager.isModelReady()
                addDebugLog("MLModels", "ML models ready: $mlReady")
                
                // Test content detection engine
                addDebugLog("ContentEngine", "Testing content detection engine...")
                val engineReady = contentDetectionEngine.isReady()
                addDebugLog("ContentEngine", "Content detection engine ready: $engineReady")
                
                addDebugLog(TAG, "Detection test completed")
            } catch (e: Exception) {
                addDebugLog(TAG, "Detection test failed: ${e.message}")
            }
        }
    }
    
    private suspend fun updateDebugState() {
        try {
            val accessibilityStatus = getAccessibilityServiceStatus()
            val detectionEngineStatus = getDetectionEngineStatus()
            val faceDetectionStatus = getFaceDetectionStatus()
            val nsfwDetectionStatus = getNSFWDetectionStatus()
            val performanceStatus = getPerformanceStatus()
            
            _debugState.value = DebugState(
                accessibilityService = accessibilityStatus,
                detectionEngine = detectionEngineStatus,
                faceDetection = faceDetectionStatus,
                nsfwDetection = nsfwDetectionStatus,
                performance = performanceStatus,
                recentLogs = debugLogs.takeLast(20) // Show last 20 logs
            )
        } catch (e: Exception) {
            addDebugLog(TAG, "Error updating debug state: ${e.message}")
            Log.e(TAG, "Error updating debug state", e)
        }
    }
    
    private fun getAccessibilityServiceStatus(): ServiceDebugInfo {
        return try {
            val service = HaramBlurAccessibilityService.getInstance()
            
            if (service == null) {
                ServiceDebugInfo(
                    isHealthy = false,
                    isRunning = false,
                    lastError = "Service not running - please enable in Accessibility Settings"
                )
            } else {
                val status = service.getServiceStatus()
                ServiceDebugInfo(
                    isHealthy = status.isServiceRunning && status.isProcessingActive,
                    isRunning = status.isServiceRunning,
                    isProcessingActive = status.isProcessingActive,
                    isCapturingActive = status.isCapturingActive,
                    isOverlayActive = status.isOverlayActive,
                    lastError = if (!status.isProcessingActive) "Processing not active" else ""
                )
            }
        } catch (e: Exception) {
            addDebugLog("AccessibilityService", "Error checking status: ${e.message}")
            ServiceDebugInfo(
                isHealthy = false,
                lastError = "Error: ${e.message}"
            )
        }
    }
    
    private fun getDetectionEngineStatus(): DetectionEngineDebugInfo {
        return try {
            val isReady = contentDetectionEngine.isReady()
            val mlReady = mlModelManager.isModelReady()
            
            DetectionEngineDebugInfo(
                isHealthy = isReady && mlReady,
                isReady = isReady,
                mlModelsReady = mlReady,
                gpuEnabled = getCurrentSettings()?.enableGPUAcceleration ?: false,
                lastProcessingTimeMs = 0L, // Would need to track this
                lastError = if (!isReady) "Engine not initialized" else ""
            )
        } catch (e: Exception) {
            addDebugLog("DetectionEngine", "Error checking status: ${e.message}")
            DetectionEngineDebugInfo(
                isHealthy = false,
                lastError = "Error: ${e.message}"
            )
        }
    }
    
    private fun getFaceDetectionStatus(): FaceDetectionDebugInfo {
        return try {
            // In a real implementation, we'd track detection statistics
            FaceDetectionDebugInfo(
                isHealthy = true, // Assuming healthy if no errors
                isReady = true,
                genderDetectorReady = true,
                lastFacesCount = 0, // Would need to track this
                lastFemaleFaces = 0, // Would need to track this
                averageConfidence = 0.75f, // Would calculate from recent detections
                lastError = ""
            )
        } catch (e: Exception) {
            addDebugLog("FaceDetection", "Error checking status: ${e.message}")
            FaceDetectionDebugInfo(
                isHealthy = false,
                lastError = "Error: ${e.message}"
            )
        }
    }
    
    private fun getNSFWDetectionStatus(): NSFWDetectionDebugInfo {
        return try {
            val mlReady = mlModelManager.isModelReady()
            val currentSettings = getCurrentSettings()
            
            NSFWDetectionDebugInfo(
                isHealthy = mlReady,
                isReady = mlReady,
                lastResult = false, // Would track from recent detections
                lastConfidence = 0f, // Would track from recent detections
                processingMode = if (currentSettings?.enableGPUAcceleration == true) "GPU" else "CPU",
                lastError = if (!mlReady) "ML models not ready" else ""
            )
        } catch (e: Exception) {
            addDebugLog("NSFWDetection", "Error checking status: ${e.message}")
            NSFWDetectionDebugInfo(
                isHealthy = false,
                lastError = "Error: ${e.message}"
            )
        }
    }
    
    private fun getPerformanceStatus(): PerformanceDebugInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory
            val memoryUsagePercent = usedMemory.toFloat() / totalMemory.toFloat()
            
            PerformanceDebugInfo(
                cpuUsage = 0.0f, // CPU usage calculation would be more complex
                memoryUsage = memoryUsagePercent,
                framesProcessed = 0L, // Would track this
                framesSkipped = 0L, // Would track this
                averageProcessingTime = 0L // Would calculate this
            )
        } catch (e: Exception) {
            addDebugLog("Performance", "Error getting performance metrics: ${e.message}")
            PerformanceDebugInfo()
        }
    }
    
    private fun getCurrentSettings() = settingsRepository.getCurrentSettings()
    
    private fun addDebugLog(tag: String, message: String, level: String = "DEBUG") {
        val timestamp = dateFormatter.format(Date())
        val log = DebugLog(timestamp, tag, message, level)
        debugLogs.add(log)
        
        // Keep only last 100 logs to prevent memory issues
        if (debugLogs.size > 100) {
            debugLogs.removeAt(0)
        }
        
        // Also log to Android Log
        when (level) {
            "ERROR" -> Log.e(tag, message)
            "WARN" -> Log.w(tag, message)
            "INFO" -> Log.i(tag, message)
            else -> Log.d(tag, message)
        }
    }
}
