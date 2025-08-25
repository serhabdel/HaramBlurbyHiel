package com.hieltech.haramblur.detection

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages GPU acceleration and hardware optimization for TensorFlow Lite models
 */
@Singleton
class GPUAccelerationManager @Inject constructor() {
    
    companion object {
        private const val TAG = "GPUAccelerationManager"
    }
    
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var isGPUSupported = false
    private var isNNAPISupported = false
    
    /**
     * Initialize GPU acceleration capabilities
     */
    fun initialize(context: Context): Boolean {
        return try {
            Log.d(TAG, "Initializing GPU acceleration...")
            
            // Try GPU initialization with proper fallback
            try {
                val compatibilityList = CompatibilityList()
                val isDeviceSupported = compatibilityList.isDelegateSupportedOnThisDevice
                
                if (isDeviceSupported) {
                    Log.d(TAG, "Device supports GPU delegate, attempting initialization")
                    initializeGPUDelegate()
                } else {
                    Log.w(TAG, "Device does not support GPU delegate")
                    isGPUSupported = false
                    gpuDelegate = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPU compatibility check failed, attempting direct initialization", e)
                initializeGPUDelegate() // Try anyway as fallback
            }
            
            // Check NNAPI compatibility with safe fallback
            try {
                initializeNNAPIDelegate()
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI acceleration unavailable", e)
                isNNAPISupported = false
                nnApiDelegate = null
            }
            
            Log.d(TAG, "GPU acceleration manager initialized - GPU: $isGPUSupported, NNAPI: $isNNAPISupported")
            true // Always return true, even if acceleration is unavailable
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GPU acceleration manager", e)
            // Even if initialization fails, we can still use CPU-only mode
            isGPUSupported = false
            isNNAPISupported = false
            true
        }
    }
    
    /**
     * Create optimized interpreter options with GPU acceleration
     */
    fun createOptimizedInterpreterOptions(enableGPU: Boolean = true): Interpreter.Options {
        val options = Interpreter.Options()
        
        // Set number of threads for CPU processing
        options.setNumThreads(4)
        
        // Enable XNNPACK for CPU optimization
        options.setUseXNNPACK(true)
        
        // Add GPU delegate if supported and requested
        if (enableGPU && isGPUSupported && gpuDelegate != null) {
            try {
                options.addDelegate(gpuDelegate!!)
                Log.d(TAG, "GPU delegate added to interpreter options")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add GPU delegate, falling back to CPU", e)
            }
        }
        
        // Add NNAPI delegate if GPU is not available
        if (!enableGPU || !isGPUSupported) {
            if (isNNAPISupported && nnApiDelegate != null) {
                try {
                    options.addDelegate(nnApiDelegate!!)
                    Log.d(TAG, "NNAPI delegate added to interpreter options")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add NNAPI delegate", e)
                }
            }
        }
        
        return options
    }
    
    /**
     * Create fast inference options optimized for ultra-fast mode
     */
    fun createFastInferenceOptions(): Interpreter.Options {
        val options = Interpreter.Options()
        
        // Optimize for speed over accuracy
        options.setNumThreads(2) // Fewer threads for faster startup
        options.setUseXNNPACK(true)
        
        // Use GPU if available for maximum speed
        if (isGPUSupported && gpuDelegate != null) {
            try {
                options.addDelegate(gpuDelegate!!)
                Log.d(TAG, "Fast inference using GPU acceleration")
            } catch (e: Exception) {
                Log.w(TAG, "GPU not available for fast inference, using CPU", e)
            }
        }
        
        return options
    }
    
    /**
     * Get performance characteristics of available acceleration
     */
    fun getAccelerationInfo(): AccelerationInfo {
        return AccelerationInfo(
            gpuSupported = isGPUSupported,
            nnApiSupported = isNNAPISupported,
            recommendedMode = when {
                isGPUSupported -> AccelerationType.GPU
                isNNAPISupported -> AccelerationType.NNAPI
                else -> AccelerationType.CPU_OPTIMIZED
            },
            estimatedSpeedupFactor = when {
                isGPUSupported -> 3.0f // GPU typically 3x faster
                isNNAPISupported -> 1.5f // NNAPI typically 1.5x faster
                else -> 1.0f // CPU baseline
            }
        )
    }
    
    /**
     * Check if GPU acceleration is currently active
     */
    fun isGPUActive(): Boolean {
        return isGPUSupported && gpuDelegate != null
    }
    
    /**
     * Check if NNAPI acceleration is currently active
     */
    fun isNNAPIActive(): Boolean {
        return isNNAPISupported && nnApiDelegate != null
    }
    
    /**
     * Cleanup acceleration resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up GPU acceleration resources")
        
        gpuDelegate?.close()
        gpuDelegate = null
        
        nnApiDelegate?.close()
        nnApiDelegate = null
        
        isGPUSupported = false
        isNNAPISupported = false
    }
    
    private fun initializeGPUDelegate() {
        try {
            Log.d(TAG, "Attempting to initialize GPU delegate...")
            
            // Check if GPU delegate classes are available at runtime
            val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
            Log.d(TAG, "GPU delegate class found: ${gpuDelegateClass.name}")
            
            // Try to create the GPU delegate instance
            val constructor = gpuDelegateClass.getDeclaredConstructor()
            gpuDelegate = constructor.newInstance() as GpuDelegate
            
            Log.d(TAG, "GPU delegate initialized successfully")
            isGPUSupported = true
            
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "GPU delegate class not found: ${e.message}")
            gpuDelegate = null
            isGPUSupported = false
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "GPU delegate constructor not available: ${e.message}")
            gpuDelegate = null
            isGPUSupported = false
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "GPU delegate native libraries not found: ${e.message}")
            gpuDelegate = null
            isGPUSupported = false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize GPU delegate: ${e.message}")
            gpuDelegate = null
            isGPUSupported = false
        }
        
        Log.d(TAG, "GPU acceleration ${if (isGPUSupported) "ENABLED" else "DISABLED"}")
    }
    
    private fun initializeNNAPIDelegate() {
        try {
            val nnApiOptions = NnApiDelegate.Options().apply {
                // Allow fallback to CPU if NNAPI fails
                setAllowFp16(true)
                setUseNnapiCpu(false) // Use dedicated NNAPI hardware
            }
            
            nnApiDelegate = NnApiDelegate(nnApiOptions)
            isNNAPISupported = true
            Log.d(TAG, "NNAPI delegate initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "NNAPI delegate not available", e)
            isNNAPISupported = false
        }
    }
}

/**
 * Information about available hardware acceleration
 */
data class AccelerationInfo(
    val gpuSupported: Boolean,
    val nnApiSupported: Boolean,
    val recommendedMode: AccelerationType,
    val estimatedSpeedupFactor: Float
)

/**
 * Types of hardware acceleration available
 */
enum class AccelerationType(val displayName: String, val description: String) {
    GPU("GPU", "Graphics Processing Unit acceleration"),
    NNAPI("NNAPI", "Android Neural Networks API acceleration"),
    CPU_OPTIMIZED("CPU+", "Optimized CPU processing with XNNPACK"),
    CPU_BASIC("CPU", "Basic CPU processing")
}