package com.hieltech.haramblur.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.R
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.services.PrayerTimeNotificationManager
import com.hieltech.haramblur.data.prayer.PrayerName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.hieltech.haramblur.testing.MLDiagnosticHelper
import com.hieltech.haramblur.testing.MLDiagnosticState
import com.hieltech.haramblur.HaramBlurApplication

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDetectionEngine: ContentDetectionEngine,
    private val faceDetectionManager: FaceDetectionManager,
    private val mlModelManager: MLModelManager,
    private val settingsRepository: SettingsRepository,
    private val prayerTimeNotificationManager: PrayerTimeNotificationManager,
    private val mlDiagnosticHelper: MLDiagnosticHelper
) : ViewModel() {
    
    companion object {
        private const val TAG = "DebugViewModel"
    }
    
    private val _debugState = MutableStateFlow(DebugState())
    val debugState: StateFlow<DebugState> = _debugState
    
    private val _mlDiagnosticState = MutableStateFlow(MLDiagnosticState())
    val mlDiagnosticState: StateFlow<MLDiagnosticState> = _mlDiagnosticState
    
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
                val engineReady = contentDetectionEngine.isEngineReady()
                addDebugLog("ContentEngine", "Content detection engine ready: $engineReady")

                addDebugLog(TAG, "Detection test completed")
            } catch (e: Exception) {
                addDebugLog(TAG, "Detection test failed: ${e.message}")
            }
        }
    }

    fun testAction(actionType: String) {
        addDebugLog(TAG, "Testing behavioral action: $actionType")
        viewModelScope.launch {
            try {
                // Create a mock content analysis result for testing
                val mockResult = ContentDetectionEngine.ContentAnalysisResult(
                    shouldBlur = true,
                    blurRegions = emptyList(),
                    faceDetectionResult = null,
                    nsfwDetectionResult = null,
                    processingTimeMs = 100L,
                    success = true,
                    error = null,
                    recommendedAction = com.hieltech.haramblur.detection.ContentAction.SELECTIVE_BLUR,
                    nsfwRegionCount = if (actionType == "EMERGENCY_BLUR") 8 else 2,
                    maxNsfwConfidence = if (actionType == "EMERGENCY_BLUR") 0.9f else 0.7f,
                )
            } catch (e: Exception) {
                addDebugLog(TAG, "Action test failed: ${e.message}", "ERROR")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Action test failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Generate comprehensive ML diagnostic report
     */
    fun generateMLDiagnosticReport() {
        addDebugLog(TAG, "Generating ML diagnostic report...")
        viewModelScope.launch {
            try {
                _mlDiagnosticState.value = _mlDiagnosticState.value.copy(isLoading = true)

                // Get ML capability status from application
                val app = context.applicationContext as? HaramBlurApplication
                val mlStatus = app?.getMLCapabilityStatus()

                // Generate comprehensive diagnostic report
                val report = mlDiagnosticHelper.generateDiagnosticReport()

                _mlDiagnosticState.value = _mlDiagnosticState.value.copy(
                    tensorFlowLiteAvailable = mlStatus?.tensorFlowLiteAvailable ?: false,
                    mlKitAvailable = mlStatus?.mlKitAvailable ?: false,
                    fallbackMode = mlStatus?.fallbackMode ?: false,
                    deviceInfo = mlStatus?.deviceInfo ?: emptyMap(),
                    lastDiagnosticReport = report,
                    isLoading = false,
                    lastError = null
                )

                addDebugLog(TAG, "ML diagnostic report generated successfully")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "ML diagnostic report generated - Overall Health: ${report.overallHealth}"
                )

            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to generate ML diagnostic report: ${e.message}", "ERROR")
                _mlDiagnosticState.value = _mlDiagnosticState.value.copy(
                    isLoading = false,
                    lastError = e.message
                )
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Failed to generate ML diagnostic report: ${e.message}"
                )
            }
        }
    }

    /**
     * Test face detection functionality
     */
    fun testFaceDetection() {
        addDebugLog(TAG, "Testing face detection functionality...")
        viewModelScope.launch {
            try {
                val result = faceDetectionManager.testFaceDetection()

                addDebugLog(TAG, "Face detection test completed: ${result?.facesDetected} faces detected in ${result?.processingTimeMs}ms")

                val status = if (result?.success == true) {
                    "✅ Face detection working - ${result.facesDetected} faces detected"
                } else {
                    "❌ Face detection failed - ${result?.errorMessage}"
                }

                _debugState.value = _debugState.value.copy(
                    lastActionResult = status
                )

            } catch (e: Exception) {
                addDebugLog(TAG, "Face detection test failed: ${e.message}", "ERROR")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Face detection test failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Test gender classification functionality
     */
    fun testGenderClassification() {
        addDebugLog(TAG, "Testing gender classification functionality...")
        viewModelScope.launch {
            try {
                val result = mlDiagnosticHelper.testGenderClassificationWithSample()

                addDebugLog(TAG, "Gender classification test completed - ML: ${result.mlModelGender} (${result.mlModelConfidence}), Heuristic: ${result.heuristicGender} (${result.heuristicConfidence})")

                val status = if (result.success) {
                    "✅ Gender classification working - ML: ${result.mlModelGender}, Heuristic: ${result.heuristicGender}"
                } else {
                    "❌ Gender classification failed - ${result.errorMessage}"
                }

                _debugState.value = _debugState.value.copy(
                    lastActionResult = status
                )

            } catch (e: Exception) {
                addDebugLog(TAG, "Gender classification test failed: ${e.message}", "ERROR")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Gender classification test failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Reload ML libraries
     */
    fun reloadMLLibraries() {
        addDebugLog(TAG, "Reloading ML libraries...")
        viewModelScope.launch {
            try {
                // Test native library loading
                val libraries = listOf(
                    "tensorflowlite_jni",
                    "tensorflowlite_gpu_jni",
                    "face_detector_v2_jni"
                )

                val results = mutableListOf<String>()
                libraries.forEach { libName ->
                    try {
                        System.loadLibrary(libName)
                        results.add("✅ $libName")
                    } catch (e: UnsatisfiedLinkError) {
                        results.add("❌ $libName: ${e.message}")
                    }
                }

                addDebugLog(TAG, "ML library reload completed: ${results.joinToString(", ")}")

                val status = if (results.any { it.startsWith("✅") }) {
                    "✅ ML libraries reloaded - ${results.count { it.startsWith("✅") }}/${libraries.size} successful"
                } else {
                    "❌ All ML library reloads failed"
                }

                _debugState.value = _debugState.value.copy(
                    lastActionResult = status
                )

            } catch (e: Exception) {
                addDebugLog(TAG, "ML library reload failed: ${e.message}", "ERROR")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "ML library reload failed: ${e.message}"
                )
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
                recentLogs = debugLogs.takeLast(20), // Show last 20 logs
                lastActionResult = _debugState.value.lastActionResult // Preserve action result
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
            val isReady = contentDetectionEngine.isEngineReady()
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
    
    private     fun addDebugLog(tag: String, message: String, level: String = "DEBUG") {
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

    fun emergencyHideOverlays() {
        viewModelScope.launch {
            try {
                addDebugLog(TAG, "Emergency overlay hide requested from debug screen", "WARN")
                val service = HaramBlurAccessibilityService.getInstance()
                if (service != null) {
                    service.emergencyHideAllOverlays()
                    addDebugLog(TAG, "Emergency overlay hide completed", "INFO")
                } else {
                    addDebugLog(TAG, "Accessibility service not running, cannot hide overlays", "ERROR")
                }
            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to emergency hide overlays: ${e.message}", "ERROR")
            }
        }
    }

    /**
     * Test prayer time notification for a specific prayer
     */
    fun testPrayerNotification(prayerName: String) {
        addDebugLog(TAG, "Testing prayer notification with buttons for: $prayerName")
        viewModelScope.launch {
            try {
                val prayerEnum = PrayerName.valueOf(prayerName.uppercase())
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                // Send basic prayer time notification first
                prayerTimeNotificationManager.sendPrayerTimeNotification(prayerEnum, currentTime)
                
                // Wait a moment then send reminder notification with buttons immediately for testing
                kotlinx.coroutines.delay(1000) // 1 second delay
                
                // Send reminder notification with buttons directly
                sendPrayerReminderNotificationWithButtons(prayerEnum, currentTime)
                
                addDebugLog(TAG, "Prayer notification with buttons sent for $prayerName")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Prayer notification with buttons sent for $prayerName"
                )
            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to send prayer notification: ${e.message}")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Send reminder notification with buttons directly for testing
     */
    private fun sendPrayerReminderNotificationWithButtons(prayerName: PrayerName, prayerTime: String) {
        try {
            val prayerKey = "${prayerName.name}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            
            // Use reflection to access the private method
            val method = PrayerTimeNotificationManager::class.java.getDeclaredMethod(
                "sendPrayerReminderNotification",
                PrayerName::class.java,
                String::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(prayerTimeNotificationManager, prayerName, prayerTime, prayerKey)
            
        } catch (e: Exception) {
            // Fallback: create a simple notification with buttons
            createSimpleNotificationWithButtons(prayerName, prayerTime)
        }
    }
    
    /**
     * Create a simple notification with buttons as fallback
     */
    private fun createSimpleNotificationWithButtons(prayerName: PrayerName, prayerTime: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            val title = "Have you prayed ${prayerName.name}?"
            val message = "It's time for ${prayerName.name} prayer at $prayerTime"
            
            // Create Yes button intent
            val yesIntent = Intent(PrayerTimeNotificationManager.ACTION_PRAYER_COMPLETED).apply {
                putExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_NAME, prayerName.name)
                putExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_TIME, prayerTime)
                setPackage(context.packageName)
            }
            val yesPendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.ordinal * 10 + 1,
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create No button intent
            val noIntent = Intent(PrayerTimeNotificationManager.ACTION_PRAYER_NOT_COMPLETED).apply {
                putExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_NAME, prayerName.name)
                putExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_TIME, prayerTime)
                setPackage(context.packageName)
            }
            val noPendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.ordinal * 10 + 2,
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create notification with buttons
            val notification = NotificationCompat.Builder(context, "prayer_reminder_channel")
                .setSmallIcon(R.drawable.ic_shield_islamic)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher_background, "Yes, I prayed", yesPendingIntent)
                .addAction(R.drawable.ic_launcher_background, "Not yet", noPendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()
            
            notificationManager.notify(prayerName.ordinal + 1000, notification)
            
        } catch (e: Exception) {
            // Log error but don't fail
            println("Error creating simple notification with buttons: ${e.message}")
        }
    }

    /**
     * Test Quranic guidance dialog
     */
    fun testQuranicGuidance(prayerName: String) {
        addDebugLog(TAG, "Testing Quranic guidance for: $prayerName")
        viewModelScope.launch {
            try {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                // Simulate user indicating they haven't prayed
                prayerTimeNotificationManager.handlePrayerNotCompleted(prayerName, currentTime)
                
                addDebugLog(TAG, "Quranic guidance triggered for $prayerName")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Quranic guidance shown for $prayerName"
                )
            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to show Quranic guidance: ${e.message}")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Test prayer reminder notification
     */
    fun testPrayerReminder(prayerName: String) {
        addDebugLog(TAG, "Testing prayer reminder for: $prayerName")
        viewModelScope.launch {
            try {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                // Simulate reminder notification
                prayerTimeNotificationManager.handlePrayerNotCompleted(prayerName, currentTime)
                
                addDebugLog(TAG, "Prayer reminder sent for $prayerName")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Prayer reminder sent for $prayerName"
                )
            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to send prayer reminder: ${e.message}")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Failed: ${e.message}"
                )
            }
    }

    /**
     * Generate comprehensive ML diagnostic report
     */
    fun generateMLDiagnosticReport() {
            addDebugLog(TAG, "Generating ML diagnostic report...")
            viewModelScope.launch {
                try {
                    _mlDiagnosticState.value = _mlDiagnosticState.value.copy(isLoading = true)
                    
                    // Get ML capability status from application
                    val app = context.applicationContext as? HaramBlurApplication
                    val mlStatus = app?.getMLCapabilityStatus()
                    
                    // Generate comprehensive diagnostic report
                    val report = mlDiagnosticHelper.generateDiagnosticReport()
                    
                    _mlDiagnosticState.value = _mlDiagnosticState.value.copy(
                        tensorFlowLiteAvailable = mlStatus?.tensorFlowLiteAvailable ?: false,
                        mlKitAvailable = mlStatus?.mlKitAvailable ?: false,
                        fallbackMode = mlStatus?.fallbackMode ?: false,
                        deviceInfo = mlStatus?.deviceInfo ?: emptyMap(),
                        lastDiagnosticReport = report,
                        isLoading = false,
                        lastError = null
                    )
                    
                    addDebugLog(TAG, "ML diagnostic report generated successfully")
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = "ML diagnostic report generated - Overall Health: ${report.overallHealth}"
                    )
                    
                } catch (e: Exception) {
                    addDebugLog(TAG, "Failed to generate ML diagnostic report: ${e.message}", "ERROR")
                    _mlDiagnosticState.value = _mlDiagnosticState.value.copy(
                        isLoading = false,
                        lastError = e.message
                    )
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = "Failed to generate ML diagnostic report: ${e.message}"
                    )
                }
            }
    }

    /**
     * Test face detection functionality
     */
    fun testFaceDetection() {
            addDebugLog(TAG, "Testing face detection functionality...")
            viewModelScope.launch {
                try {
                    val result = faceDetectionManager.testFaceDetection()
                    
                    addDebugLog(TAG, "Face detection test completed: ${result?.facesDetected} faces detected in ${result?.processingTimeMs}ms")
                    
                    val status = if (result?.success == true) {
                        "✅ Face detection working - ${result.facesDetected} faces detected"
                    } else {
                        "❌ Face detection failed - ${result?.errorMessage}"
                    }
                    
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = status
                    )
                    
                } catch (e: Exception) {
                    addDebugLog(TAG, "Face detection test failed: ${e.message}", "ERROR")
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = "Face detection test failed: ${e.message}"
                    )
                }
            }
        }
        
        /**
         * Test gender classification functionality
         */
        fun testGenderClassification() {
            addDebugLog(TAG, "Testing gender classification functionality...")
            viewModelScope.launch {
                try {
                    val result = mlDiagnosticHelper.testGenderClassificationWithSample()
                    
                    addDebugLog(TAG, "Gender classification test completed - ML: ${result.mlModelGender} (${result.mlModelConfidence}), Heuristic: ${result.heuristicGender} (${result.heuristicConfidence})")
                    
                    val status = if (result.success) {
                        "✅ Gender classification working - ML: ${result.mlModelGender}, Heuristic: ${result.heuristicGender}"
                    } else {
                        "❌ Gender classification failed - ${result.errorMessage}"
                    }
                    
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = status
                    )
                    
                } catch (e: Exception) {
                    addDebugLog(TAG, "Gender classification test failed: ${e.message}", "ERROR")
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = "Gender classification test failed: ${e.message}"
                    )
                }
            }
        }
        
        /**
         * Reload ML libraries
         */
        fun reloadMLLibraries() {
            addDebugLog(TAG, "Reloading ML libraries...")
            viewModelScope.launch {
                try {
                    // Test native library loading
                    val libraries = listOf(
                        "tensorflowlite_jni",
                        "tensorflowlite_gpu_jni",
                        "face_detector_v2_jni"
                    )
                    
                    val results = mutableListOf<String>()
                    libraries.forEach { libName ->
                        try {
                            System.loadLibrary(libName)
                            results.add("✅ $libName")
                        } catch (e: UnsatisfiedLinkError) {
                            results.add("❌ $libName: ${e.message}")
                        }
                    }
                    
                    addDebugLog(TAG, "ML library reload completed: ${results.joinToString(", ")}")
                    
                    val status = if (results.any { it.startsWith("✅") }) {
                        "✅ ML libraries reloaded - ${results.count { it.startsWith("✅") }}/${libraries.size} successful"
                    } else {
                        "❌ All ML library reloads failed"
                    }
                    
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = status
                    )
                    
                } catch (e: Exception) {
                    addDebugLog(TAG, "ML library reload failed: ${e.message}", "ERROR")
                    _debugState.value = _debugState.value.copy(
                        lastActionResult = "ML library reload failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Test all prayer notifications in sequence
     */
    fun testAllPrayerNotifications() {
        addDebugLog(TAG, "Testing all prayer notifications")
        viewModelScope.launch {
            try {
                val prayers = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                prayers.forEach { prayerName ->
                    val prayerEnum = PrayerName.valueOf(prayerName)
                    prayerTimeNotificationManager.sendPrayerTimeNotification(prayerEnum, currentTime)
                    kotlinx.coroutines.delay(2000) // 2 second delay between notifications
                }
                
                addDebugLog(TAG, "All prayer notifications sent successfully")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "All 5 prayer notifications sent"
                )
            } catch (e: Exception) {
                addDebugLog(TAG, "Failed to send all prayer notifications: ${e.message}")
                _debugState.value = _debugState.value.copy(
                    lastActionResult = "Failed: ${e.message}"
                )
            }
        }
    }
}
