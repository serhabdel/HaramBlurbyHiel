package com.hieltech.haramblur.setup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.testing.FirstRunValidator
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive first-run setup manager that ensures perfect app initialization
 * and optimal performance configuration based on device capabilities
 */
@Singleton
class FirstRunSetupManager @Inject constructor(
    private val contentDetectionEngine: ContentDetectionEngine,
    private val settingsRepository: SettingsRepository,
    private val firstRunValidator: FirstRunValidator
) {
    
    companion object {
        private const val TAG = "FirstRunSetupManager"
        private const val PREFS_NAME = "haramblur_first_run"
        private const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"
        private const val KEY_SETUP_VERSION = "setup_version"
        private const val KEY_DEVICE_PROFILE = "device_profile"
        private const val CURRENT_SETUP_VERSION = 3
        private const val SETUP_TIMEOUT_MS = 60000L // 60 seconds
    }
    
    data class SetupResult(
        val success: Boolean,
        val deviceProfile: DeviceProfile,
        val optimizedSettings: AppSettings,
        val setupTimeMs: Long,
        val issues: List<String>,
        val recommendations: List<String>
    )
    
    data class DeviceProfile(
        val deviceClass: DeviceClass,
        val hasGPUAcceleration: Boolean,
        val availableMemoryMB: Float,
        val cpuPerformanceScore: Float,
        val recommendedProcessingMode: ProcessingMode,
        val maxProcessingTimeMs: Long
    )
    
    enum class DeviceClass {
        HIGH_END,    // Flagship devices (2022+)
        MID_RANGE,   // Mid-range devices (2020+)
        LOW_END,     // Budget devices or older
        UNKNOWN
    }
    
    enum class ProcessingMode {
        ULTRA_FAST,  // Minimal processing, maximum speed
        FAST,        // Balanced speed/accuracy
        BALANCED,    // Default mode
        ACCURATE     // Maximum accuracy, slower
    }
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * Check if first-run setup is needed
     */
    fun isFirstRunNeeded(context: Context): Boolean {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val isCompleted = prefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false)
        val setupVersion = prefs.getInt(KEY_SETUP_VERSION, 0)
        
        // Need setup if never completed or version is outdated
        val needsSetup = !isCompleted || setupVersion < CURRENT_SETUP_VERSION
        
        Log.i(TAG, "First-run check: completed=$isCompleted, version=$setupVersion/$CURRENT_SETUP_VERSION, needs_setup=$needsSetup")
        
        return needsSetup
    }
    
    /**
     * Run comprehensive first-run setup
     */
    suspend fun runFirstRunSetup(context: Context): SetupResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        Log.i(TAG, "🚀 Starting comprehensive first-run setup...")
        
        try {
            withTimeout(SETUP_TIMEOUT_MS) {
                
                // Step 1: Device profiling and capability detection
                Log.i(TAG, "📱 Step 1: Analyzing device capabilities...")
                val deviceProfile = analyzeDeviceCapabilities(context)
                Log.i(TAG, "Device profile: ${deviceProfile.deviceClass}, GPU: ${deviceProfile.hasGPUAcceleration}, Memory: ${deviceProfile.availableMemoryMB}MB")
                
                // Step 2: Initialize core components with optimal settings
                Log.i(TAG, "🔧 Step 2: Initializing core components...")
                val initSuccess = initializeCoreComponents(context, deviceProfile)
                if (!initSuccess) {
                    issues.add("Failed to initialize core components")
                    recommendations.add("Try restarting the app or clearing app data")
                }
                
                // Step 3: Generate optimized settings based on device profile
                Log.i(TAG, "⚙️ Step 3: Generating optimized settings...")
                val optimizedSettings = generateOptimizedSettings(deviceProfile)
                settingsRepository.updateSettings(optimizedSettings)
                Log.i(TAG, "Applied optimized settings: GPU=${optimizedSettings.enableGPUAcceleration}, Mode=${deviceProfile.recommendedProcessingMode}")
                
                // Step 4: Run comprehensive validation
                Log.i(TAG, "✅ Step 4: Running validation tests...")
                val validationResult = firstRunValidator.validateFirstRun(context)
                
                if (!validationResult.isValid) {
                    issues.add("Validation failed with ${validationResult.issues.size} issues")
                    issues.addAll(validationResult.issues.map { "${it.component}: ${it.description}" })
                    recommendations.addAll(validationResult.recommendations)
                }
                
                // Step 5: Performance optimization based on validation results
                Log.i(TAG, "🚀 Step 5: Applying performance optimizations...")
                val finalSettings = applyPerformanceOptimizations(optimizedSettings, validationResult, deviceProfile)
                settingsRepository.updateSettings(finalSettings)
                
                // Step 6: Save setup completion
                Log.i(TAG, "💾 Step 6: Saving setup completion...")
                saveSetupCompletion(deviceProfile)
                
                val setupTime = System.currentTimeMillis() - startTime
                val success = issues.isEmpty() || issues.none { it.contains("Failed to initialize") }
                
                Log.i(TAG, "✅ First-run setup completed in ${setupTime}ms, success: $success")
                
                return@withTimeout SetupResult(
                    success = success,
                    deviceProfile = deviceProfile,
                    optimizedSettings = finalSettings,
                    setupTimeMs = setupTime,
                    issues = issues,
                    recommendations = recommendations
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "❌ First-run setup timed out after ${SETUP_TIMEOUT_MS}ms")
            issues.add("Setup timed out - device may be too slow")
            recommendations.add("Try enabling power saving mode and restart setup")
            
            return@withContext SetupResult(
                success = false,
                deviceProfile = DeviceProfile(DeviceClass.LOW_END, false, 0f, 0f, ProcessingMode.ULTRA_FAST, 1000L),
                optimizedSettings = getFailsafeSettings(),
                setupTimeMs = SETUP_TIMEOUT_MS,
                issues = issues,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ First-run setup failed", e)
            issues.add("Setup crashed: ${e.message}")
            recommendations.add("Restart app and try again")
            
            return@withContext SetupResult(
                success = false,
                deviceProfile = DeviceProfile(DeviceClass.UNKNOWN, false, 0f, 0f, ProcessingMode.ULTRA_FAST, 1000L),
                optimizedSettings = getFailsafeSettings(),
                setupTimeMs = System.currentTimeMillis() - startTime,
                issues = issues,
                recommendations = recommendations
            )
        }
    }
    
    /**
     * Analyze device capabilities and create performance profile
     */
    private suspend fun analyzeDeviceCapabilities(context: Context): DeviceProfile = withContext(Dispatchers.Default) {
        Log.d(TAG, "🔍 Analyzing device capabilities...")
        
        // Memory analysis
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024f * 1024f)
        val availableMemoryMB = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024f * 1024f)
        
        Log.d(TAG, "Memory: ${availableMemoryMB}MB available / ${maxMemoryMB}MB max")
        
        // CPU performance test
        val cpuScore = performCPUBenchmark()
        Log.d(TAG, "CPU performance score: $cpuScore")
        
        // GPU capability detection
        val hasGPU = detectGPUCapabilities(context)
        Log.d(TAG, "GPU acceleration available: $hasGPU")
        
        // Device classification
        val deviceClass = classifyDevice(maxMemoryMB, cpuScore, hasGPU)
        Log.d(TAG, "Device classified as: $deviceClass")
        
        // Determine optimal processing mode
        val processingMode = determineOptimalProcessingMode(deviceClass, cpuScore, availableMemoryMB)
        val maxProcessingTime = determineMaxProcessingTime(deviceClass, processingMode)
        
        return@withContext DeviceProfile(
            deviceClass = deviceClass,
            hasGPUAcceleration = hasGPU,
            availableMemoryMB = availableMemoryMB,
            cpuPerformanceScore = cpuScore,
            recommendedProcessingMode = processingMode,
            maxProcessingTimeMs = maxProcessingTime
        )
    }
    
    /**
     * Initialize core components with device-optimized settings
     */
    private suspend fun initializeCoreComponents(context: Context, deviceProfile: DeviceProfile): Boolean {
        Log.d(TAG, "🔧 Initializing core components for ${deviceProfile.deviceClass} device...")
        
        return try {
            // Initialize with timeout based on device class
            val timeout = when (deviceProfile.deviceClass) {
                DeviceClass.HIGH_END -> 10000L
                DeviceClass.MID_RANGE -> 20000L
                DeviceClass.LOW_END -> 30000L
                DeviceClass.UNKNOWN -> 15000L
            }
            
            withTimeout(timeout) {
                val success = contentDetectionEngine.initialize(context)
                if (success) {
                    Log.d(TAG, "✅ Content detection engine initialized successfully")
                } else {
                    Log.w(TAG, "⚠️ Content detection engine initialization failed")
                }
                success
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "❌ Component initialization timed out")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Component initialization failed", e)
            false
        }
    }
    
    /**
     * Generate optimized settings based on device profile
     */
    private fun generateOptimizedSettings(deviceProfile: DeviceProfile): AppSettings {
        Log.d(TAG, "⚙️ Generating optimized settings for ${deviceProfile.deviceClass} device...")
        
        return when (deviceProfile.deviceClass) {
            DeviceClass.HIGH_END -> AppSettings(
                enableGPUAcceleration = deviceProfile.hasGPUAcceleration,
                enableNSFWDetection = true,
                enableFaceDetection = true,
                blurFemaleFaces = true,
                blurMaleFaces = false, // Focus on female faces only
                maxProcessingTimeMs = 100L,
                processingSpeed = ProcessingSpeed.FAST,
                ultraFastModeEnabled = false,
                enableRealTimeProcessing = true,
                fullScreenWarningEnabled = true,
                enableRegionBasedFullScreen = true,
                nsfwFullScreenRegionThreshold = 6,
                nsfwHighConfidenceThreshold = 0.8f
            )
            
            DeviceClass.MID_RANGE -> AppSettings(
                enableGPUAcceleration = deviceProfile.hasGPUAcceleration,
                enableNSFWDetection = true,
                enableFaceDetection = true,
                blurFemaleFaces = true,
                blurMaleFaces = false,
                maxProcessingTimeMs = 200L,
                processingSpeed = ProcessingSpeed.BALANCED,
                ultraFastModeEnabled = false,
                enableRealTimeProcessing = true,
                fullScreenWarningEnabled = true,
                enableRegionBasedFullScreen = true,
                nsfwFullScreenRegionThreshold = 5,
                nsfwHighConfidenceThreshold = 0.7f
            )
            
            DeviceClass.LOW_END -> AppSettings(
                enableGPUAcceleration = false, // Disable GPU on low-end devices
                enableNSFWDetection = true,
                enableFaceDetection = true,
                blurFemaleFaces = true,
                blurMaleFaces = false,
                maxProcessingTimeMs = 500L,
                processingSpeed = ProcessingSpeed.ULTRA_FAST,
                ultraFastModeEnabled = true,
                enableRealTimeProcessing = true,
                fullScreenWarningEnabled = false, // Disable for performance
                enableRegionBasedFullScreen = true,
                nsfwFullScreenRegionThreshold = 4,
                nsfwHighConfidenceThreshold = 0.6f
            )
            
            DeviceClass.UNKNOWN -> getFailsafeSettings()
        }
    }
    
    /**
     * Apply performance optimizations based on validation results
     */
    private fun applyPerformanceOptimizations(
        baseSettings: AppSettings,
        validationResult: FirstRunValidator.ValidationResult,
        deviceProfile: DeviceProfile
    ): AppSettings {
        Log.d(TAG, "🚀 Applying performance optimizations...")
        
        var optimizedSettings = baseSettings
        
        // If performance is poor, apply optimizations
        if (validationResult.performanceScore < 0.6f) {
            Log.w(TAG, "Poor performance detected (${validationResult.performanceScore}), applying optimizations...")
            
            optimizedSettings = optimizedSettings.copy(
                enableGPUAcceleration = false, // Disable GPU if causing issues
                maxProcessingTimeMs = optimizedSettings.maxProcessingTimeMs * 2, // Increase timeout
                ultraFastModeEnabled = true,
                fullScreenWarningEnabled = false // Disable for performance
            )
        }
        
        // If accuracy is poor, adjust thresholds
        if (validationResult.accuracyScore < 0.8f) {
            Log.w(TAG, "Poor accuracy detected (${validationResult.accuracyScore}), adjusting thresholds...")
            
            optimizedSettings = optimizedSettings.copy(
                nsfwHighConfidenceThreshold = optimizedSettings.nsfwHighConfidenceThreshold - 0.1f,
                nsfwFullScreenRegionThreshold = optimizedSettings.nsfwFullScreenRegionThreshold - 1
            )
        }
        
        // Memory optimization
        if (deviceProfile.availableMemoryMB < 200f) {
            Log.w(TAG, "Low memory detected (${deviceProfile.availableMemoryMB}MB), applying memory optimizations...")
            
            optimizedSettings = optimizedSettings.copy(
                enableFaceDetection = false, // Disable face detection to save memory
                ultraFastModeEnabled = true
            )
        }
        
        return optimizedSettings
    }
    
    /**
     * Save setup completion to preferences
     */
    private fun saveSetupCompletion(deviceProfile: DeviceProfile) {
        prefs.edit()
            .putBoolean(KEY_FIRST_RUN_COMPLETED, true)
            .putInt(KEY_SETUP_VERSION, CURRENT_SETUP_VERSION)
            .putString(KEY_DEVICE_PROFILE, deviceProfile.deviceClass.name)
            .apply()
        
        Log.i(TAG, "💾 First-run setup completion saved")
    }
    
    // Helper methods
    private suspend fun performCPUBenchmark(): Float = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val iterations = 50000
        
        var result = 0.0
        for (i in 0 until iterations) {
            result += Math.sin(i.toDouble()) * Math.cos(i.toDouble())
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        val targetTime = 50L // 50ms target
        
        return@withContext (targetTime.toFloat() / processingTime.toFloat()).coerceAtMost(1.0f)
    }
    
    private fun detectGPUCapabilities(context: Context): Boolean {
        // Simplified GPU detection - in real implementation, check OpenGL ES version, etc.
        return try {
            val runtime = Runtime.getRuntime()
            runtime.maxMemory() > 512 * 1024 * 1024 // Assume devices with >512MB have GPU
        } catch (e: Exception) {
            false
        }
    }
    
    private fun classifyDevice(maxMemoryMB: Float, cpuScore: Float, hasGPU: Boolean): DeviceClass {
        return when {
            maxMemoryMB > 4000f && cpuScore > 0.8f && hasGPU -> DeviceClass.HIGH_END
            maxMemoryMB > 2000f && cpuScore > 0.5f -> DeviceClass.MID_RANGE
            maxMemoryMB > 1000f -> DeviceClass.LOW_END
            else -> DeviceClass.UNKNOWN
        }
    }
    
    private fun determineOptimalProcessingMode(deviceClass: DeviceClass, cpuScore: Float, availableMemoryMB: Float): ProcessingMode {
        return when (deviceClass) {
            DeviceClass.HIGH_END -> if (cpuScore > 0.9f) ProcessingMode.ACCURATE else ProcessingMode.FAST
            DeviceClass.MID_RANGE -> if (availableMemoryMB > 300f) ProcessingMode.BALANCED else ProcessingMode.FAST
            DeviceClass.LOW_END -> ProcessingMode.ULTRA_FAST
            DeviceClass.UNKNOWN -> ProcessingMode.ULTRA_FAST
        }
    }
    
    private fun determineMaxProcessingTime(deviceClass: DeviceClass, processingMode: ProcessingMode): Long {
        return when (deviceClass) {
            DeviceClass.HIGH_END -> when (processingMode) {
                ProcessingMode.ACCURATE -> 200L
                ProcessingMode.FAST -> 100L
                ProcessingMode.BALANCED -> 150L
                ProcessingMode.ULTRA_FAST -> 50L
            }
            DeviceClass.MID_RANGE -> when (processingMode) {
                ProcessingMode.ACCURATE -> 300L
                ProcessingMode.FAST -> 200L
                ProcessingMode.BALANCED -> 250L
                ProcessingMode.ULTRA_FAST -> 100L
            }
            DeviceClass.LOW_END -> when (processingMode) {
                ProcessingMode.ACCURATE -> 500L
                ProcessingMode.FAST -> 400L
                ProcessingMode.BALANCED -> 450L
                ProcessingMode.ULTRA_FAST -> 200L
            }
            DeviceClass.UNKNOWN -> 1000L
        }
    }
    
    private fun getFailsafeSettings(): AppSettings {
        return AppSettings(
            enableGPUAcceleration = false,
            enableNSFWDetection = true,
            enableFaceDetection = false, // Disable for maximum compatibility
            blurFemaleFaces = true,
            blurMaleFaces = false,
            maxProcessingTimeMs = 1000L,
            processingSpeed = ProcessingSpeed.ULTRA_FAST,
            ultraFastModeEnabled = true,
            enableRealTimeProcessing = true,
            fullScreenWarningEnabled = false,
            enableRegionBasedFullScreen = false,
            nsfwFullScreenRegionThreshold = 8,
            nsfwHighConfidenceThreshold = 0.9f
        )
    }
}
