package com.hieltech.haramblur.ml

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages TensorFlow Lite models with automatic fallback to quantized versions.
 * Provides optimized inference with GPU acceleration and multi-threading.
 */
@Singleton
class OptimizedModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OptimizedModelManager"
        
        // Model paths - quantized versions preferred
        private const val NSFW_MODEL_QUANTIZED = "models/quantized/nsfw_mobilenet_v2_140_224_quantized.tflite"
        private const val NSFW_MODEL_FALLBACK = "models/nsfw_mobilenet_v2_140_224.1.tflite"
        private const val GENDER_MODEL = "models/model_lite_gender_q.tflite"
    }
    
    private var nsfwInterpreter: Interpreter? = null
    private var genderInterpreter: Interpreter? = null
    private val gpuDelegate: GpuDelegate? by lazy { createGPUDelegate() }
    
    /**
     * Initialize models. Call this before using detections.
     */
    fun initialize() {
        if (nsfwInterpreter == null) {
            nsfwInterpreter = createNsfwInterpreter()
        }
        if (genderInterpreter == null) {
            genderInterpreter = createGenderInterpreter()
        }
    }
    
    /**
     * Release resources. Call when service is destroyed.
     */
    fun release() {
        nsfwInterpreter?.close()
        nsfwInterpreter = null
        genderInterpreter?.close()
        genderInterpreter = null
        gpuDelegate?.close()
    }
    
    /**
     * Check if quantized model is available and working.
     */
    fun isQuantizedModelAvailable(): Boolean {
        return try {
            context.assets.open(NSFW_MODEL_QUANTIZED).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get model information for diagnostics.
     */
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "nsfw_quantized_available" to isQuantizedModelAvailable(),
            "nsfw_loaded" to (nsfwInterpreter != null),
            "gender_loaded" to (genderInterpreter != null),
            "gpu_acceleration" to (gpuDelegate != null),
            "optimal_threads" to getOptimalThreadCount()
        )
    }
    
    private fun createNsfwInterpreter(): Interpreter? {
        return try {
            // Try quantized model first
            val modelFile = try {
                FileUtil.loadMappedFile(context, NSFW_MODEL_QUANTIZED)
            } catch (e: Exception) {
                Log.w(TAG, "Quantized model not found, using fallback", e)
                FileUtil.loadMappedFile(context, NSFW_MODEL_FALLBACK)
            }
            
            Interpreter(modelFile, createInterpreterOptions()).also {
                Log.i(TAG, "NSFW model loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load NSFW model", e)
            null
        }
    }
    
    private fun createGenderInterpreter(): Interpreter? {
        return try {
            val modelFile = FileUtil.loadMappedFile(context, GENDER_MODEL)
            Interpreter(modelFile, createInterpreterOptions()).also {
                Log.i(TAG, "Gender model loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load gender model", e)
            null
        }
    }
    
    private fun createInterpreterOptions(): Interpreter.Options {
        return Interpreter.Options().apply {
            // Thread count based on CPU cores
            numThreads = getOptimalThreadCount()
            
            // XNNPACK delegate for faster CPU inference
            useXNNPACK = true
            
            // GPU acceleration if available
            gpuDelegate?.let { addDelegate(it) }
            
            // NNAPI for newer devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                useNNAPI = true
            }
        }
    }
    
    private fun createGPUDelegate(): GpuDelegate? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GpuDelegate()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate not available", e)
            null
        }
    }
    
    private fun getOptimalThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores <= 4 -> 2
            cores <= 8 -> 4
            else -> 6
        }
    }
}
