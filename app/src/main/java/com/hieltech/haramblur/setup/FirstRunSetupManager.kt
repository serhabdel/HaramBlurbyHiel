package com.hieltech.haramblur.setup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.QualityMode
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
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
        private const val KEY_GPU_FAILURE_COUNT = "gpu_failure_count"
        private const val CURRENT_SETUP_VERSION = 3
        private const val SETUP_TIMEOUT_MS = 60000L // 60 seconds
        private const val MAX_GPU_FAILURES_BEFORE_DISABLE = 3 // Disable GPU after 3 consecutive failures
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
     * BYPASS DEVICE ANALYSIS - Always return HIGH_END device profile for maximum performance
     */
    private suspend fun analyzeDeviceCapabilities(context: Context): DeviceProfile = withContext(Dispatchers.Default) {
        Log.d(TAG, "🔍 Analyzing device capabilities... BYPASSED - Using HIGH_END profile")
        
        // Bypass device analysis and always return HIGH_END device profile
        return@withContext DeviceProfile(
            deviceClass = DeviceClass.HIGH_END,
            hasGPUAcceleration = true, // Force GPU acceleration enabled
            availableMemoryMB = 8000f, // Assume high memory for maximum performance
            cpuPerformanceScore = 1.0f, // Assume maximum CPU performance
            recommendedProcessingMode = ProcessingMode.ACCURATE, // Use most accurate mode
            maxProcessingTimeMs = 50L // Use fastest processing time
        )
    }
    
    /**
     * Initialize core components with device-optimized settings
     * Add GPU capability checks and fallback logic
     */
    private suspend fun initializeCoreComponents(context: Context, deviceProfile: DeviceProfile): Boolean {
        Log.d(TAG, "🔧 Initializing core components for ${deviceProfile.deviceClass} device...")
        
        return try {
            // Check GPU failure history
            val gpuFailureCount = prefs.getInt(KEY_GPU_FAILURE_COUNT, 0)
            if (gpuFailureCount >= MAX_GPU_FAILURES_BEFORE_DISABLE) {
                Log.w(TAG, "⚠️ GPU disabled due to $gpuFailureCount consecutive failures")
                // Temporarily disable GPU acceleration for this session
                val currentSettings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(currentSettings.copy(enableGPUAcceleration = false))
            }
            
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
                    // Reset GPU failure count on successful initialization
                    if (gpuFailureCount > 0) {
                        prefs.edit().putInt(KEY_GPU_FAILURE_COUNT, 0).apply()
                        Log.d(TAG, "🔄 GPU failure count reset after successful initialization")
                    }
                } else {
                    Log.w(TAG, "⚠️ Content detection engine initialization failed")
                    handleGPUFailure()
                }
                success
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "❌ Component initialization timed out")
            handleGPUFailure()
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Component initialization failed", e)
            handleGPUFailure()
            false
        }
    }

    /**
     * Handle GPU failure by incrementing failure count and potentially disabling GPU
     */
    private fun handleGPUFailure() {
        val currentFailures = prefs.getInt(KEY_GPU_FAILURE_COUNT, 0)
        val newFailureCount = currentFailures + 1
        prefs.edit().putInt(KEY_GPU_FAILURE_COUNT, newFailureCount).apply()
        
        Log.w(TAG, "⚠️ GPU failure detected. Failure count: $newFailureCount")
        
        if (newFailureCount >= MAX_GPU_FAILURES_BEFORE_DISABLE) {
            Log.e(TAG, "❌ GPU will be disabled for future sessions due to repeated failures")
            // Disable GPU acceleration in current settings
            val currentSettings = settingsRepository.getCurrentSettings()
            settingsRepository.updateSettings(currentSettings.copy(enableGPUAcceleration = false))
        }
    }
    
    /**
     * Generate optimized settings based on device profile
     * Use SettingsRepository.applyMaximumPerformanceSettings() to avoid duplication
     */
    private suspend fun generateOptimizedSettings(deviceProfile: DeviceProfile): AppSettings {
        Log.d(TAG, "⚙️ Generating optimized settings using SettingsRepository.applyMaximumPerformanceSettings()")
        
        // Apply maximum performance settings using the centralized method
        settingsRepository.applyMaximumPerformanceSettings()
        
        // Return the updated settings with preserved gender preferences
        val currentSettings = settingsRepository.getCurrentSettings()
        return currentSettings.copy(
            blurMaleFaces = currentSettings.blurMaleFaces,
            blurFemaleFaces = currentSettings.blurFemaleFaces,
            userGender = currentSettings.userGender
        )
    }
    
    /**
     * Apply performance optimizations based on validation results
     * REMOVE PERFORMANCE-BASED OPTIMIZATIONS - Always maintain maximum performance settings
     */
    private fun applyPerformanceOptimizations(
        baseSettings: AppSettings,
        validationResult: FirstRunValidator.ValidationResult,
        deviceProfile: DeviceProfile
    ): AppSettings {
        Log.d(TAG, "🚀 Applying performance optimizations... BYPASSED - Maintaining maximum performance settings")
        
        // BYPASS performance-based optimizations - never downgrade from maximum settings
        // Always return the original maximum performance settings regardless of validation results
        return baseSettings
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
        // BYPASS DEVICE CLASSIFICATION - Always return HIGH_END for maximum performance
        Log.d(TAG, "Device classification bypassed - forcing HIGH_END")
        return DeviceClass.HIGH_END
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
        Log.d(TAG, "Getting failsafe settings... USING SAFER CONFIGURATION FOR WEAK DEVICES")
        // Return safer configuration that maintains protection but reduces performance requirements
        return AppSettings(
            qualityMode = QualityMode.BALANCED, // Use balanced mode for better compatibility
            enableGPUAcceleration = false, // Disable GPU to prevent crashes on weak devices
            enableNSFWDetection = true,
            enableFaceDetection = true,
            blurFemaleFaces = true,
            blurMaleFaces = false,
            maxProcessingTimeMs = 150L, // Increased timeout for slower devices
            processingSpeed = ProcessingSpeed.BALANCED, // Use balanced processing speed
            ultraFastModeEnabled = false,
            enableRealTimeProcessing = true,
            fullScreenWarningEnabled = true,
            enableRegionBasedFullScreen = true,
            nsfwFullScreenRegionThreshold = 5, // Conservative threshold
            nsfwHighConfidenceThreshold = 0.7f, // Conservative confidence threshold
            // Safer performance settings
            frameSkipThreshold = 2, // Allow some frame skipping for stability
            imageDownscaleRatio = 0.6f, // Moderate downscaling for performance
            detectionSensitivity = 0.7f, // Moderate sensitivity
            blurIntensity = BlurIntensity.MEDIUM, // Medium blur intensity for performance
            blurStyle = BlurStyle.PIXELATED // Simpler blur style for performance
        )
    }
}
