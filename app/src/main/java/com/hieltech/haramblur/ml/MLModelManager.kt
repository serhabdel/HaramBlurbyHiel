package com.hieltech.haramblur.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.hieltech.haramblur.detection.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MLModelManager @Inject constructor(
    private val gpuAccelerationManager: GPUAccelerationManager,
    private val performanceMonitor: PerformanceMonitor
): GenderModelProvider {
    
    companion object {
        private const val TAG = "MLModelManager"
        // Model paths - prefer quantized versions
        private const val NSFW_MODEL_QUANTIZED = "models/quantized/nsfw_mobilenet_v2_140_224_quantized.tflite"
        private const val NSFW_MODEL_FALLBACK = "models/nsfw_mobilenet_v2_140_224.1.tflite"
        private const val GENDER_MODEL_PATH = "models/model_lite_gender_q.tflite"
        private const val INPUT_SIZE = 224
        private const val GENDER_INPUT_SIZE = 128 // Gender model expects 128x128 input
        
        // THRESHOLD MIGRATION: Now using DetectionThresholds for centralized config
        // These legacy constants are kept for backward compatibility but should use DetectionThresholds
        @Deprecated("Use DetectionThresholds.DEFAULT_NSFW_THRESHOLD instead")
        private const val CONFIDENCE_THRESHOLD = 0.45f // Updated from 0.25f to reduce false positives
        
        // FIXED: Lower female threshold to counteract model bias
        private const val GENDER_CONFIDENCE_THRESHOLD = 0.50f // Reduced from 0.55f for better female detection
        private const val FEMALE_CONFIDENCE_THRESHOLD = 0.45f // Even lower threshold for female detection
        
        @Deprecated("Use DetectionThresholds.MIN_NSFW_THRESHOLD instead")
        private const val MAX_ACCURACY_NSFW_THRESHOLD = 0.30f // Updated from 0.20f - was too aggressive
        
        private const val DEFAULT_TIMEOUT_MS = 5000L
        private const val FAST_TIMEOUT_MS = 100L
        private const val ULTRA_FAST_TIMEOUT_MS = 50L
        private const val MIN_TILE_SIZE = 64 // Minimum tile size for granular analysis
        private const val MAX_OVERLAP_PERCENTAGE = 0.6f // Maximum overlap for thorough coverage
    }
    
    private var nsfwInterpreter: Interpreter? = null
    private var genderInterpreter: Interpreter? = null
    private var fastNsfwInterpreter: Interpreter? = null // Optimized for speed
    private var imageProcessor: ImageProcessor? = null
    private var genderImageProcessor: ImageProcessor? = null
    private var fastImageProcessor: ImageProcessor? = null // Smaller input for speed
    private var isInitialized = false
    private var isGenderModelReady = false
    private var currentPerformanceMode = PerformanceMode.BALANCED
    private var isNsfwModelQuantized = false // Track if using INT8 quantized model
    
    // Gender prediction cache for performance
    private val genderCache = mutableMapOf<Int, GenderCacheEntry>()
    private val nsfwCache = mutableMapOf<Int, NSFWCacheEntry>()
    private val cacheExpirationMs = 5000L // 5 seconds cache
    
    // ML Status exposed as StateFlow for UI observation
    private val _mlStatus = MutableStateFlow(MLStatus())
    val mlStatus: StateFlow<MLStatus> = _mlStatus.asStateFlow()
    
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Initializing ML models with GPU acceleration...")
            
            // Verify native libraries are loaded before proceeding
            if (!verifyNativeLibraries()) {
                Log.e(TAG, "❌ Native library verification failed - ML models cannot initialize")
                return@withContext false
            }
            
            // Initialize GPU acceleration
            gpuAccelerationManager.initialize(context)
            
            // Initialize image processors for different performance modes
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build()
                
            genderImageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(GENDER_INPUT_SIZE, GENDER_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build()
            
            // Fast processor for ultra-fast mode (smaller input size)
            fastImageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE / 2, INPUT_SIZE / 2, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .build()
            
            // Load NSFW models with different performance configurations
            initializeNSFWModel(context)
            initializeFastNSFWModel(context)
            
            // Load gender classification model
            initializeGenderModel(context)
            
            isInitialized = true
            Log.d(TAG, "ML models initialized successfully with GPU support: ${gpuAccelerationManager.isGPUActive()}")
            
            // Update ML status for UI observation
            updateMLStatus()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ML models", e)
            updateMLStatus(initializationError = e.message)
            false
        }
    }
    
    /**
     * Update the ML status StateFlow for UI observation
     */
    private fun updateMLStatus(initializationError: String? = null) {
        val nsfwAvailable = nsfwInterpreter != null
        val genderAvailable = genderInterpreter != null
        val gpuActive = gpuAccelerationManager.isGPUActive()
        
        // Determine detection mode based on what's available
        val detectionMode = when {
            !isInitialized -> "Not initialized"
            nsfwAvailable && genderAvailable -> "ML Models"
            nsfwAvailable || genderAvailable -> "Hybrid (ML + Heuristic)"
            else -> "Heuristic Only"
        }
        
        val status = MLStatus(
            isInitialized = isInitialized,
            nsfwModelAvailable = nsfwAvailable,
            genderModelAvailable = genderAvailable,
            gpuAccelerationActive = gpuActive,
            initializationError = initializationError,
            overallHealth = when {
                !isInitialized -> MLHealth.NOT_INITIALIZED
                !nsfwAvailable && !genderAvailable -> MLHealth.CRITICAL
                !nsfwAvailable || !genderAvailable -> MLHealth.DEGRADED
                !gpuActive -> MLHealth.REDUCED_PERFORMANCE
                else -> MLHealth.HEALTHY
            },
            statusMessage = buildStatusMessage(nsfwAvailable, genderAvailable, gpuActive, initializationError),
            // Enhanced fields
            nsfwUsingHeuristics = !nsfwAvailable,
            genderUsingHeuristics = !genderAvailable,
            faceDetectionAvailable = isInitialized, // ML Kit is available when initialized
            detectionMode = detectionMode,
            nsfwCacheSize = nsfwCache.size,
            genderCacheSize = genderCache.size
        )
        
        _mlStatus.value = status
        Log.d(TAG, "ML Status updated: ${status.overallHealth} - ${status.statusMessage}")
    }
    
    /**
     * Update face detection status from FaceDetectionManager
     * This allows the ML status to reflect actual face detection health
     */
    fun updateFaceDetectionStatus(
        verified: Boolean,
        healthy: Boolean,
        consecutiveEmptyResults: Int
    ) {
        val currentStatus = _mlStatus.value
        _mlStatus.value = currentStatus.copy(
            faceDetectionVerified = verified,
            faceDetectionHealthy = healthy,
            consecutiveEmptyFaceResults = consecutiveEmptyResults
        )
        Log.d(TAG, "Face detection status updated: verified=$verified, healthy=$healthy, consecutiveEmpty=$consecutiveEmptyResults")
    }
    
    private fun buildStatusMessage(
        nsfwAvailable: Boolean,
        genderAvailable: Boolean,
        gpuActive: Boolean,
        error: String?
    ): String {
        return when {
            error != null -> "Initialization failed: $error"
            !nsfwAvailable && !genderAvailable -> "All ML models unavailable - using heuristic detection only"
            !nsfwAvailable -> "NSFW model unavailable - using heuristic NSFW detection"
            !genderAvailable -> "Gender model unavailable - using heuristic gender detection"
            !gpuActive -> "GPU acceleration unavailable - detection may be slower"
            else -> "All systems operational"
        }
    }
    
    /**
     * Verify that native libraries are properly loaded
     */
    private fun verifyNativeLibraries(): Boolean {
        val libraries = listOf(
            "tensorflowlite_jni",
            "tensorflowlite_gpu_jni"
        )
        
        libraries.forEach { libName ->
            try {
                System.loadLibrary(libName)
                Log.d(TAG, "✅ Successfully loaded library: $libName")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load library: $libName", e)
                return false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error loading library: $libName", e)
                return false
            }
        }
        return true
    }
    
    /**
     * Check if using quantized model
     */
    fun isUsingQuantizedModel(context: Context): Boolean {
        return try {
            context.assets.open(NSFW_MODEL_QUANTIZED).use { 
                Log.d(TAG, "Quantized model file found: $NSFW_MODEL_QUANTIZED")
                true 
            }
        } catch (e: Exception) {
            Log.d(TAG, "Quantized model not found: $NSFW_MODEL_QUANTIZED")
            false
        }
    }
    
    /**
     * Check if NSFW model is loaded and ready
     */
    fun isNsfwModelLoaded(): Boolean = nsfwInterpreter != null

    /**
     * Get diagnostic information about ML model status
     */
    fun getDiagnosticInfo(context: Context): MLDiagnosticInfo {
        return MLDiagnosticInfo(
            isInitialized = isInitialized,
            isGenderModelReady = isGenderModelReady,
            gpuAccelerationActive = gpuAccelerationManager.isGPUActive(),
            nsfwInterpreterAvailable = nsfwInterpreter != null,
            genderInterpreterAvailable = genderInterpreter != null,
            currentPerformanceMode = currentPerformanceMode,
            genderCacheSize = genderCache.size,
            nsfwCacheSize = nsfwCache.size,
            usingQuantizedModel = isUsingQuantizedModel(context)
        )
    }
    
    private fun initializeNSFWModel(context: Context) {
        try {
            // Try quantized model first (4MB vs 16.5MB)
            val (modelBuffer, isQuantized) = try {
                Log.d(TAG, "Trying quantized NSFW model...")
                Pair(FileUtil.loadMappedFile(context, NSFW_MODEL_QUANTIZED), true)
            } catch (e: IOException) {
                // Fall back to full model
                Log.w(TAG, "Quantized model not found, using full model")
                Pair(FileUtil.loadMappedFile(context, NSFW_MODEL_FALLBACK), false)
            }
            
            // Create interpreter options - INT8 models work better without GPU delegate
            val options = if (isQuantized) {
                Log.i(TAG, "✅ Using QUANTIZED NSFW model (INT8)")
                // For INT8 quantized models, use CPU with XNNPACK (faster than GPU for INT8)
                Interpreter.Options().apply {
                    numThreads = 4
                    useXNNPACK = true
                    // Don't use GPU delegate for INT8 - it can cause compatibility issues
                }
            } else {
                Log.i(TAG, "Using full NSFW model (FP32)")
                gpuAccelerationManager.createOptimizedInterpreterOptions(enableGPU = true)
            }
            
            nsfwInterpreter = Interpreter(modelBuffer, options)
            isNsfwModelQuantized = isQuantized
            Log.i(TAG, "✅ NSFW model loaded successfully (quantized=$isQuantized)")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing NSFW model", e)
            // Log detailed error for debugging
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
        }
    }
    
    private fun initializeFastNSFWModel(context: Context) {
        try {
            // Initialize fast NSFW model optimized for ultra-fast processing
            val options = gpuAccelerationManager.createFastInferenceOptions()
            
            // Note: Fast NSFW model not yet implemented - falls back to standard model with lower resolution
            // To implement: create quantized version of NSFW model using TFLite Model Optimizer
            Log.d(TAG, "Fast NSFW model not implemented - using standard model with optimized settings")
            
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing fast NSFW model", e)
        }
    }
    
    private fun initializeGenderModel(context: Context) {
        try {
            Log.d(TAG, "Initializing gender classification model...")

            // Use optimized options with GPU acceleration
            val options = gpuAccelerationManager.createOptimizedInterpreterOptions(enableGPU = true)

            // Load the actual gender model file
            try {
                val modelBuffer = FileUtil.loadMappedFile(context, GENDER_MODEL_PATH)
                genderInterpreter = Interpreter(modelBuffer, options)
                isGenderModelReady = true
                Log.d(TAG, "✅ Gender model loaded successfully from: $GENDER_MODEL_PATH")
            } catch (e: IOException) {
                Log.w(TAG, "⚠️ Gender model file not found at $GENDER_MODEL_PATH, falling back to heuristics", e)
                isGenderModelReady = false
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ TensorFlow Lite native library loading failed for gender model", e)
                isGenderModelReady = false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error loading gender model", e)
                isGenderModelReady = false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing gender model", e)
            isGenderModelReady = false
        }
    }
    
    suspend fun detectNSFW(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.IO) {
        return@withContext detectNSFWWithTimeout(bitmap, DEFAULT_TIMEOUT_MS, false)
    }
    
    /**
     * Fast NSFW detection optimized for performance
     */
    suspend fun detectNSFWFast(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.IO) {
        val timeoutMs = when (currentPerformanceMode) {
            PerformanceMode.ULTRA_FAST -> ULTRA_FAST_TIMEOUT_MS
            PerformanceMode.FAST -> FAST_TIMEOUT_MS
            else -> DEFAULT_TIMEOUT_MS
        }
        
        return@withContext detectNSFWWithTimeout(bitmap, timeoutMs, true)
    }
    
    private suspend fun detectNSFWWithTimeout(
        bitmap: Bitmap, 
        timeoutMs: Long, 
        useFastMode: Boolean
    ): DetectionResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.w(TAG, "ML models not initialized")
            return@withContext DetectionResult(false, 0.0f, "Model not initialized")
        }
        
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Check cache first
            val bitmapHash = calculateBitmapHash(bitmap)
            nsfwCache[bitmapHash]?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < cacheExpirationMs) {
                    Log.d(TAG, "Using cached NSFW result")
                    return@withContext cached.result
                } else {
                    nsfwCache.remove(bitmapHash)
                }
            }
            
            val result = withTimeoutOrNull(timeoutMs) {
                performNSFWDetection(bitmap, useFastMode)
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            if (result == null) {
                Log.w(TAG, "NSFW detection timed out after ${timeoutMs}ms")
                performanceMonitor.recordMeasurement(
                    processingTimeMs = timeoutMs,
                    targetTimeMs = timeoutMs,
                    operationType = OperationType.NSFW_DETECTION,
                    qualityLevel = getQualityLevelFromPerformanceMode(),
                    additionalData = mapOf("timeout" to true)
                )
                return@withContext DetectionResult(true, 0.5f, "Detection timed out - defaulting to safe")
            }
            
            // Cache the result
            nsfwCache[bitmapHash] = NSFWCacheEntry(result, System.currentTimeMillis())
            
            // Record performance
            performanceMonitor.recordMeasurement(
                processingTimeMs = processingTime,
                targetTimeMs = timeoutMs,
                operationType = OperationType.NSFW_DETECTION,
                qualityLevel = getQualityLevelFromPerformanceMode(),
                additionalData = mapOf("fastMode" to useFastMode, "cached" to false)
            )
            
            Log.d(TAG, "NSFW detection completed in ${processingTime}ms: ${result.isNSFW} (confidence: ${result.confidence})")
            return@withContext result
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "Error during NSFW detection", e)
            
            performanceMonitor.recordMeasurement(
                processingTimeMs = processingTime,
                targetTimeMs = timeoutMs,
                operationType = OperationType.NSFW_DETECTION,
                qualityLevel = getQualityLevelFromPerformanceMode(),
                additionalData = mapOf("error" to true, "errorType" to e.javaClass.simpleName)
            )
            
            return@withContext DetectionResult(true, 0.5f, "Detection failed - defaulting to safe: ${e.message}")
        }
    }
    
    private suspend fun performNSFWDetection(bitmap: Bitmap, useFastMode: Boolean): DetectionResult {
        // Choose appropriate processor based on mode
        val processor = if (useFastMode && fastImageProcessor != null) {
            fastImageProcessor!!
        } else {
            imageProcessor ?: throw IllegalStateException("Image processor not initialized")
        }

        // Process the input image
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = processor.process(tensorImage)

        // Perform sliding-window region detection for enhanced full-screen triggering
        val regionAnalysis = performSlidingWindowRegionDetection(bitmap, useFastMode)

        // Try to use actual ML model first, fall back to heuristics if not available
        val confidence = if (nsfwInterpreter != null) {
            try {
                Log.d(TAG, "Using actual NSFW model for detection")
                runNSFWInference(processedImage, useFastMode)
            } catch (e: Exception) {
                Log.w(TAG, "ML inference failed, falling back to heuristics", e)
                if (useFastMode) simulateNSFWDetectionFast(bitmap) else simulateNSFWDetection(bitmap)
            }
        } else {
            Log.d(TAG, "NSFW model not loaded, using heuristic detection")
            if (useFastMode) simulateNSFWDetectionFast(bitmap) else simulateNSFWDetection(bitmap)
        }

        // Use lower threshold for maximum accuracy mode
        val effectiveThreshold = if (currentPerformanceMode == PerformanceMode.QUALITY) {
            MAX_ACCURACY_NSFW_THRESHOLD
        } else {
            CONFIDENCE_THRESHOLD
        }
        
        val isNSFW = confidence > effectiveThreshold

        return DetectionResult(
            isNSFW,
            confidence,
            if (nsfwInterpreter != null) {
                if (useFastMode) "Fast ML-based detection" else "Standard ML-based detection"
            } else {
                if (useFastMode) "Fast heuristic detection" else "Standard heuristic detection"
            },
            regionAnalysis.regionCount,
            regionAnalysis.regionRects,
            regionAnalysis.perRegionConfidences,
            regionAnalysis.maxRegionConfidence
        )
    }
    
    private fun simulateNSFWDetection(bitmap: Bitmap): Float {
        // Enhanced heuristic-based detection for nudity and inappropriate content
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // Analyze image characteristics that might indicate inappropriate content
        var confidenceScore = 0.0f
        
        // 1. Skin tone detection (simplified)
        val skinToneConfidence = analyzeSkinToneDistribution(bitmap)
        confidenceScore += skinToneConfidence * 0.4f
        
        // 2. Color distribution analysis
        val colorAnalysis = analyzeColorDistribution(bitmap)
        confidenceScore += colorAnalysis * 0.3f
        
        // 3. Texture and pattern analysis
        val textureAnalysis = analyzeTexturePatterns(bitmap)
        confidenceScore += textureAnalysis * 0.2f
        
        // 4. Aspect ratio and composition analysis
        val compositionScore = if (aspectRatio > 0.6f && aspectRatio < 1.6f) 0.3f else 0.1f
        confidenceScore += compositionScore * 0.1f
        
        // Ensure score stays within bounds
        return confidenceScore.coerceIn(0.0f, 1.0f)
    }
    
    private fun analyzeSkinToneDistribution(bitmap: Bitmap): Float {
        try {
            // Sample pixels from the bitmap to analyze skin tone presence with enhanced sensitivity
            val sampleSize = minOf(bitmap.width, bitmap.height, 100)
            val step = maxOf(bitmap.width / sampleSize, bitmap.height / sampleSize, 2) // Reduced step for better accuracy
            
            var skinPixelCount = 0
            var totalPixels = 0
            
            for (x in 0 until bitmap.width step step) {
                for (y in 0 until bitmap.height step step) {
                    val pixel = bitmap.getPixel(x, y)
                    if (isSkinTonePixel(pixel)) {
                        skinPixelCount++
                    }
                    totalPixels++
                }
            }
            
            val skinRatio = skinPixelCount.toFloat() / totalPixels
            
            // Enhanced skin tone detection with lower thresholds for maximum sensitivity
            return when {
                skinRatio > 0.35f -> 0.85f // Very high skin tone (lowered threshold)
                skinRatio > 0.25f -> 0.75f // Moderate skin tone (increased sensitivity)
                skinRatio > 0.15f -> 0.65f // Some skin tone (increased sensitivity)
                skinRatio > 0.08f -> 0.45f // Low skin tone (new threshold)
                else -> 0.15f             // Very low skin tone (increased from 0.1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing skin tone", e)
            return 0.15f
        }
    }
    
    private fun isSkinTonePixel(pixel: Int): Boolean {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Enhanced skin tone detection ranges
        return when {
            // Light skin tones
            red in 180..255 && green in 120..210 && blue in 90..170 -> true
            // Medium skin tones  
            red in 140..200 && green in 100..160 && blue in 70..130 -> true
            // Darker skin tones
            red in 100..160 && green in 70..120 && blue in 50..100 -> true
            // Additional ranges for various skin tones
            red in 160..220 && green in 110..170 && blue in 80..140 -> true
            else -> false
        }
    }
    
    private fun analyzeColorDistribution(bitmap: Bitmap): Float {
        try {
            // Analyze color variance and distribution
            val colors = mutableMapOf<Int, Int>()
            val sampleSize = 50
            val step = maxOf(bitmap.width / sampleSize, bitmap.height / sampleSize, 1)
            
            for (x in 0 until bitmap.width step step) {
                for (y in 0 until bitmap.height step step) {
                    val pixel = bitmap.getPixel(x, y)
                    // Reduce color precision for clustering
                    val reducedColor = reduceColorPrecision(pixel)
                    colors[reducedColor] = colors.getOrDefault(reducedColor, 0) + 1
                }
            }
            
            // Calculate color distribution metrics
            val dominantColors = colors.values.sortedDescending().take(5)
            val totalPixels = colors.values.sum()
            
            // If few colors dominate (like skin tones), higher suspicion
            val dominanceRatio = dominantColors.sum().toFloat() / totalPixels
            
            return when {
                dominanceRatio > 0.7f -> 0.6f  // Very dominated by few colors
                dominanceRatio > 0.5f -> 0.4f  // Moderately dominated
                else -> 0.2f                   // Well distributed colors
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing color distribution", e)
            return 0.2f
        }
    }
    
    private fun analyzeTexturePatterns(bitmap: Bitmap): Float {
        try {
            // Simple texture analysis based on local variance
            var totalVariance = 0f
            var sampleCount = 0
            val step = 20
            
            for (x in step until bitmap.width - step step step) {
                for (y in step until bitmap.height - step step step) {
                    val centerPixel = bitmap.getPixel(x, y)
                    val neighbors = listOf(
                        bitmap.getPixel(x-step, y),
                        bitmap.getPixel(x+step, y),
                        bitmap.getPixel(x, y-step),
                        bitmap.getPixel(x, y+step)
                    )
                    
                    val variance = calculatePixelVariance(centerPixel, neighbors)
                    totalVariance += variance
                    sampleCount++
                }
            }
            
            val avgVariance = if (sampleCount > 0) totalVariance / sampleCount else 0f
            
            // Lower variance (smoother textures) might indicate skin
            return when {
                avgVariance < 500f -> 0.5f    // Very smooth (skin-like)
                avgVariance < 1500f -> 0.3f   // Moderately smooth
                else -> 0.1f                  // Textured (less likely skin)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing texture patterns", e)
            return 0.1f
        }
    }
    
    private fun reduceColorPrecision(pixel: Int): Int {
        val red = ((pixel shr 16) and 0xFF) / 32 * 32
        val green = ((pixel shr 8) and 0xFF) / 32 * 32
        val blue = (pixel and 0xFF) / 32 * 32
        return (red shl 16) or (green shl 8) or blue
    }
    
    private fun calculatePixelVariance(center: Int, neighbors: List<Int>): Float {
        val centerBrightness = getPixelBrightness(center)
        var variance = 0f
        
        neighbors.forEach { neighbor ->
            val neighborBrightness = getPixelBrightness(neighbor)
            val diff = centerBrightness - neighborBrightness
            variance += diff * diff
        }
        
        return variance / neighbors.size
    }
    
    private fun getPixelBrightness(pixel: Int): Float {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return (red * 0.299f + green * 0.587f + blue * 0.114f)
    }
    
    /**
     * Detect gender for a face using TensorFlow Lite model with caching
     */
    override suspend fun detectGender(face: Face, bitmap: Bitmap): GenderDetectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Check cache first
            face.trackingId?.let { trackingId ->
                genderCache[trackingId]?.let { cacheEntry ->
                    if (System.currentTimeMillis() - cacheEntry.timestamp < cacheExpirationMs) {
                        Log.d(TAG, "Using cached gender result for face: $trackingId")
                        return@withContext cacheEntry.result.copy(
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        // Remove expired cache entry
                        genderCache.remove(trackingId)
                    }
                }
            }
            
            val result = if (isGenderModelReady && genderInterpreter != null) {
                // Use TensorFlow Lite model inference
                performTensorFlowGenderDetection(face, bitmap)
            } else {
                // Fall back to enhanced heuristic detection
                performHeuristicGenderDetection(face, bitmap)
            }
            
            // Cache the result
            face.trackingId?.let { trackingId ->
                genderCache[trackingId] = GenderCacheEntry(result, System.currentTimeMillis())
            }
            
            Log.d(TAG, "Gender detection completed: ${result.gender} (confidence: ${result.confidence})")
            return@withContext result.copy(processingTimeMs = System.currentTimeMillis() - startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Gender detection failed", e)
            return@withContext GenderDetectionResult(
                gender = Gender.UNKNOWN,
                confidence = 0.0f,
                facialFeatures = FacialFeatureAnalysis.default(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Perform gender detection using TensorFlow Lite model
     */
    private suspend fun performTensorFlowGenderDetection(face: Face, bitmap: Bitmap): GenderDetectionResult {
        return try {
            // Extract face region from bitmap
            val faceRegion = extractFaceRegion(face, bitmap)
            
            // Process image for model input (128x128)
            val tensorImage = TensorImage.fromBitmap(faceRegion)
            val processedImage = genderImageProcessor!!.process(tensorImage)
            
            // Prepare input buffer (4 bytes per float * 128 * 128 * 3 channels = 196608 bytes)
            val inputBuffer = ByteBuffer.allocateDirect(4 * GENDER_INPUT_SIZE * GENDER_INPUT_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            // Convert processed image to input buffer
            val imageArray = processedImage.tensorBuffer.floatArray
            inputBuffer.rewind()
            for (pixel in imageArray) {
                inputBuffer.putFloat(pixel)
            }
            
            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4 * 2) // 2 classes: male, female
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Run actual gender model inference
            val (maleConfidence, femaleConfidence) = if (genderInterpreter != null && isGenderModelReady) {
                try {
                    genderInterpreter?.run(inputBuffer, outputBuffer)
                    outputBuffer.rewind()
                    val male = outputBuffer.getFloat()
                    val female = outputBuffer.getFloat()
                    Log.d(TAG, "Gender model inference: male=$male, female=$female")
                    Pair(male, female)
                } catch (e: Exception) {
                    Log.w(TAG, "Gender model inference failed, using heuristics", e)
                    val simulated = simulateGenderModelOutput(face, bitmap)
                    Pair(simulated[0], simulated[1])
                }
            } else {
                Log.d(TAG, "Gender model not ready, using heuristics")
                val simulated = simulateGenderModelOutput(face, bitmap)
                Pair(simulated[0], simulated[1])
            }
            
            // FIXED: Use asymmetric thresholds to counteract model bias toward male detection
            val gender = when {
                maleConfidence > femaleConfidence && maleConfidence > GENDER_CONFIDENCE_THRESHOLD -> Gender.MALE
                femaleConfidence > maleConfidence && femaleConfidence > FEMALE_CONFIDENCE_THRESHOLD -> Gender.FEMALE
                else -> Gender.UNKNOWN
            }
            
            val confidence = maxOf(maleConfidence, femaleConfidence)
            val facialFeatures = analyzeFacialFeaturesFromFace(face, bitmap)
            
            GenderDetectionResult(
                gender = gender,
                confidence = confidence,
                facialFeatures = facialFeatures,
                processingTimeMs = 0L // Will be set by caller
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "TensorFlow gender detection failed, falling back to heuristics", e)
            performHeuristicGenderDetection(face, bitmap)
        }
    }
    
    /**
     * Perform gender detection using enhanced heuristics
     */
    private fun performHeuristicGenderDetection(face: Face, bitmap: Bitmap): GenderDetectionResult {
        val facialFeatures = analyzeFacialFeaturesFromFace(face, bitmap)
        
        // Enhanced heuristic scoring
        var maleScore = 0.0f
        var femaleScore = 0.0f
        
        // Facial structure analysis
        if (facialFeatures.jawlineSharpness > 0.6f) maleScore += 0.3f
        if (facialFeatures.jawlineSharpness < 0.4f) femaleScore += 0.3f
        
        // Eyebrow characteristics
        if (facialFeatures.eyebrowThickness > 0.7f) maleScore += 0.2f
        if (facialFeatures.eyebrowThickness < 0.5f) femaleScore += 0.2f
        
        // Facial hair presence
        if (facialFeatures.facialHairPresence > 0.5f) maleScore += 0.4f
        
        // Cheekbone prominence
        if (facialFeatures.cheekboneProminence > 0.6f) femaleScore += 0.2f
        if (facialFeatures.cheekboneProminence < 0.4f) maleScore += 0.1f
        
        // Face aspect ratio
        if (facialFeatures.faceAspectRatio > 0.85f) maleScore += 0.1f
        if (facialFeatures.faceAspectRatio < 0.75f) femaleScore += 0.1f
        
        val totalScore = maleScore + femaleScore
        val normalizedMaleScore = if (totalScore > 0) maleScore / totalScore else 0.5f
        val normalizedFemaleScore = if (totalScore > 0) femaleScore / totalScore else 0.5f
        
        val gender = when {
            normalizedMaleScore > normalizedFemaleScore && normalizedMaleScore > 0.6f -> Gender.MALE
            normalizedFemaleScore > normalizedMaleScore && normalizedFemaleScore > 0.6f -> Gender.FEMALE
            else -> Gender.UNKNOWN
        }
        
        val confidence = maxOf(normalizedMaleScore, normalizedFemaleScore)
        
        return GenderDetectionResult(
            gender = gender,
            confidence = confidence,
            facialFeatures = facialFeatures,
            processingTimeMs = 0L
        )
    }
    
    /**
     * Extract face region from bitmap
     */
    private fun extractFaceRegion(face: Face, bitmap: Bitmap): Bitmap {
        val boundingBox = face.boundingBox
        
        // Expand bounding box slightly for better context
        val expansion = 20
        val left = maxOf(0, boundingBox.left - expansion)
        val top = maxOf(0, boundingBox.top - expansion)
        val right = minOf(bitmap.width, boundingBox.right + expansion)
        val bottom = minOf(bitmap.height, boundingBox.bottom + expansion)
        
        val width = right - left
        val height = bottom - top
        
        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } else {
            // Fallback to center crop if bounding box is invalid
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            Bitmap.createBitmap(bitmap, x, y, size, size)
        }
    }
    
    /**
     * Analyze facial features from face detection result
     */
    private fun analyzeFacialFeaturesFromFace(face: Face, bitmap: Bitmap): FacialFeatureAnalysis {
        val boundingBox = face.boundingBox
        val faceWidth = boundingBox.width()
        val faceHeight = boundingBox.height()
        
        // Analyze facial structure
        val jawlineSharpness = analyzeFaceStructure(face, bitmap)
        val eyebrowThickness = analyzeEyebrowCharacteristics(face)
        val facialHairPresence = analyzeFacialHair(face, bitmap)
        val cheekboneProminence = analyzeCheekbones(face)
        
        return FacialFeatureAnalysis(
            jawlineSharpness = jawlineSharpness,
            eyebrowThickness = eyebrowThickness,
            facialHairPresence = facialHairPresence,
            cheekboneProminence = cheekboneProminence,
            faceAspectRatio = faceWidth.toFloat() / faceHeight.toFloat(),
            confidenceScore = calculateFeatureConfidence(jawlineSharpness, eyebrowThickness, facialHairPresence)
        )
    }
    
    /**
     * Simulate gender model output for testing
     */
    private fun simulateGenderModelOutput(face: Face, bitmap: Bitmap): FloatArray {
        // Simulate TensorFlow Lite model output based on heuristics
        val features = analyzeFacialFeaturesFromFace(face, bitmap)
        
        var maleScore = 0.5f
        var femaleScore = 0.5f
        
        // Adjust scores based on features
        if (features.jawlineSharpness > 0.6f) maleScore += 0.2f
        if (features.facialHairPresence > 0.3f) maleScore += 0.3f
        if (features.eyebrowThickness > 0.7f) maleScore += 0.1f
        if (features.cheekboneProminence > 0.6f) femaleScore += 0.2f
        
        // Normalize scores
        val total = maleScore + femaleScore
        maleScore /= total
        femaleScore /= total
        
        return floatArrayOf(maleScore, femaleScore)
    }
    
    // Helper methods for facial feature analysis
    private fun analyzeFaceStructure(face: Face, bitmap: Bitmap): Float {
        val boundingBox = face.boundingBox
        val aspectRatio = boundingBox.width().toFloat() / boundingBox.height().toFloat()
        return minOf(1.0f, maxOf(0.0f, (aspectRatio - 0.7f) * 2.0f))
    }
    
    private fun analyzeEyebrowCharacteristics(face: Face): Float {
        val faceArea = face.boundingBox.width() * face.boundingBox.height()
        return minOf(1.0f, maxOf(0.0f, faceArea.toFloat() / 50000.0f))
    }
    
    private fun analyzeFacialHair(face: Face, bitmap: Bitmap): Float {
        // Simplified facial hair detection
        return 0.3f // Default low probability
    }
    
    private fun analyzeCheekbones(face: Face): Float {
        val boundingBox = face.boundingBox
        val faceWidth = boundingBox.width()
        val faceHeight = boundingBox.height()
        val ratio = faceHeight.toFloat() / faceWidth.toFloat()
        return minOf(1.0f, maxOf(0.0f, (ratio - 1.0f) * 2.0f + 0.5f))
    }
    
    private fun calculateFeatureConfidence(
        jawlineSharpness: Float,
        eyebrowThickness: Float,
        facialHairPresence: Float
    ): Float {
        val features = listOf(jawlineSharpness, eyebrowThickness, facialHairPresence)
        val variance = features.map { (it - 0.5f) * (it - 0.5f) }.average()
        return minOf(1.0f, maxOf(0.3f, variance.toFloat() * 2.0f + 0.5f))
    }
    
    /**
     * Run NSFW inference using the loaded TensorFlow Lite model
     * Memory-optimized with proper resource management
     * Supports both FP32 and INT8 quantized models
     */
    private fun runNSFWInference(tensorImage: TensorImage, useFastMode: Boolean): Float {
        return nsfwInterpreter?.let { interpreter ->
            var inputBuffer: ByteBuffer? = null
            var outputBuffer: ByteBuffer? = null

            try {
                // Prepare input buffer based on model type
                val imageArray = tensorImage.tensorBuffer.floatArray
                
                if (isNsfwModelQuantized) {
                    // INT8 quantized model - input should be INT8 (1 byte per pixel)
                    inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3)
                    inputBuffer.order(ByteOrder.nativeOrder())
                    
                    // Convert float [0.0, 1.0] to int8 [0, 255]
                    inputBuffer.rewind()
                    for (pixel in imageArray) {
                        // Convert to unsigned int8 [0, 255]
                        val int8Pixel = (pixel * 255.0f).toInt().coerceIn(0, 255).toByte()
                        inputBuffer.put(int8Pixel)
                    }
                    
                    // INT8 output buffer (5 classes, 1 byte each)
                    outputBuffer = ByteBuffer.allocateDirect(5)
                    outputBuffer.order(ByteOrder.nativeOrder())
                    
                } else {
                    // FP32 model - input is float (4 bytes per pixel)
                    inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
                    inputBuffer.order(ByteOrder.nativeOrder())
                    
                    // Copy float values
                    inputBuffer.rewind()
                    for (pixel in imageArray) {
                        inputBuffer.putFloat(pixel)
                    }
                    
                    // FP32 output buffer (5 classes, 4 bytes each)
                    outputBuffer = ByteBuffer.allocateDirect(20)
                    outputBuffer.order(ByteOrder.nativeOrder())
                }

                // Run inference
                interpreter.run(inputBuffer, outputBuffer)

                // Get result
                outputBuffer.rewind()
                val nsfwProbability = if (isNsfwModelQuantized) {
                    // For INT8 output, read byte and convert to float probability
                    val byteVal = outputBuffer.get(0)
                    (byteVal.toInt() and 0xFF) / 255.0f
                } else {
                    outputBuffer.getFloat()
                }

                // Ensure result is within valid range
                nsfwProbability.coerceIn(0.0f, 1.0f)

            } catch (e: Exception) {
                Log.e(TAG, "Error running NSFW inference: ${e.message}", e)
                // Return safe default instead of throwing
                Log.w(TAG, "Falling back to safe default (0.1f) due to inference error")
                return 0.1f // Safe default - low NSFW probability
            } finally {
                // Explicit cleanup of buffers
                inputBuffer?.clear()
                outputBuffer?.clear()
            }
        } ?: throw IllegalStateException("NSFW interpreter not initialized")
    }

    /**
     * Fast NSFW detection simulation optimized for speed
     */
    private fun simulateNSFWDetectionFast(bitmap: Bitmap): Float {
        // Simplified heuristic for ultra-fast mode
        val width = bitmap.width
        val height = bitmap.height

        // Quick skin tone check with reduced sampling
        var confidenceScore = 0.0f
        val sampleStep = 8 // Larger step for faster processing

        var skinPixels = 0
        var totalPixels = 0

        for (x in 0 until width step sampleStep) {
            for (y in 0 until height step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                if (isSkinTonePixelFast(pixel)) {
                    skinPixels++
                }
                totalPixels++
            }
        }

        val skinRatio = if (totalPixels > 0) skinPixels.toFloat() / totalPixels else 0.0f

        // Simplified scoring for speed
        confidenceScore = when {
            skinRatio > 0.4f -> 0.7f
            skinRatio > 0.25f -> 0.5f
            skinRatio > 0.15f -> 0.3f
            else -> 0.1f
        }

        return confidenceScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Perform sliding-window region detection for enhanced full-screen blur triggering
     * Divides the bitmap into overlapping tiles and analyzes each for NSFW content
     */
    private fun performSlidingWindowRegionDetection(bitmap: Bitmap, useFastMode: Boolean): RegionAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate tile size based on bitmap dimensions and performance mode with enhanced granularity
        val tileSize = calculateAdaptiveTileSize(width, height, useFastMode)
        val overlapPercentage = 0.6f // Use 60% overlap for both modes as per accuracy-first plan

        val stepSize = (tileSize * (1.0f - overlapPercentage)).toInt()
        val highConfidenceRegions = mutableListOf<Rect>()
        val regionConfidences = mutableListOf<Float>()
        var maxConfidence = 0.0f

        // Use lower threshold for maximum accuracy mode
        val effectiveThreshold = if (currentPerformanceMode == PerformanceMode.QUALITY) {
            MAX_ACCURACY_NSFW_THRESHOLD
        } else {
            CONFIDENCE_THRESHOLD * 0.8f // Lower threshold for region detection
        }

        // Slide window across the bitmap
        for (x in 0 until width step stepSize) {
            for (y in 0 until height step stepSize) {
                val tileRect = Rect(
                    x,
                    y,
                    minOf(x + tileSize, width),
                    minOf(y + tileSize, height)
                )

                // Skip if tile is too small (less than 25% of intended size)
                if (tileRect.width() < tileSize * 0.25f || tileRect.height() < tileSize * 0.25f) {
                    continue
                }

                // Extract tile bitmap with safety checks
                val tileBitmap = try {
                    Bitmap.createBitmap(
                        bitmap,
                        tileRect.left,
                        tileRect.top,
                        tileRect.width(),
                        tileRect.height()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create tile bitmap at ${tileRect}, skipping", e)
                    continue // Skip this tile if bitmap creation fails
                }

                // Analyze tile for NSFW content with edge-aware processing
                val confidence = analyzeTileForNSFWWithEdgeAwareness(tileBitmap, useFastMode, tileRect)

                // Always recycle the tile bitmap
                tileBitmap.recycle()

                // Check if this tile meets high confidence threshold (lowered for sensitivity)
                if (confidence >= effectiveThreshold) {
                    highConfidenceRegions.add(tileRect)
                    regionConfidences.add(confidence)
                    maxConfidence = maxOf(maxConfidence, confidence)
                }

                // Early termination for performance - stop if we already have many regions
                if (highConfidenceRegions.size >= 15) { // Increased from 10 for better coverage
                    break
                }
            }

            // Early termination for performance
            if (highConfidenceRegions.size >= 15) { // Increased from 10
                break
            }
        }

        // Merge overlapping regions with edge-aware merging
        val mergedRegions = mergeOverlappingRegionsWithEdgeAwareness(highConfidenceRegions, regionConfidences, bitmap)

        return RegionAnalysisResult(
            regionCount = mergedRegions.size,
            regionRects = mergedRegions.map { it.first },
            perRegionConfidences = mergedRegions.map { it.second },
            maxRegionConfidence = maxConfidence
        )
    }

    /**
     * Calculate adaptive tile size based on bitmap dimensions and performance mode
     */
    private fun calculateAdaptiveTileSize(width: Int, height: Int, useFastMode: Boolean): Int {
        val minDimension = minOf(width, height)

        return when {
            useFastMode -> {
                // Smaller tiles for faster processing in fast mode, but with minimum size for accuracy
                when {
                    minDimension >= 1080 -> 80  // Smaller tiles for large screens
                    minDimension >= 720 -> 72   // Medium tiles for medium screens
                    else -> MIN_TILE_SIZE        // Minimum tile size for small screens
                }
            }
            else -> {
                // Standard mode - smaller tiles for more granular analysis
                when {
                    minDimension >= 1080 -> 128 // Smaller tiles for detailed analysis
                    minDimension >= 720 -> 96   // Medium tiles for balanced analysis
                    else -> MIN_TILE_SIZE        // Minimum tile size for small screens
                }
            }
        }
    }

    /**
     * Analyze a tile bitmap for NSFW content
     */
    private fun analyzeTileForNSFW(tileBitmap: Bitmap, useFastMode: Boolean): Float {
        return if (useFastMode) {
            simulateNSFWDetectionFast(tileBitmap)
        } else {
            simulateNSFWDetection(tileBitmap)
        }
    }
    
    /**
     * Analyze a tile bitmap for NSFW content with edge-aware processing
     */
    private fun analyzeTileForNSFWWithEdgeAwareness(tileBitmap: Bitmap, useFastMode: Boolean, tileRect: Rect): Float {
        // Get base confidence from standard analysis
        val baseConfidence = if (useFastMode) {
            simulateNSFWDetectionFast(tileBitmap)
        } else {
            simulateNSFWDetection(tileBitmap)
        }
        
        // Apply edge-aware boost for tiles with content boundaries
        val edgeBoost = calculateEdgeAwarenessBoost(tileBitmap, tileRect)
        
        // Combine base confidence with edge awareness
        val combinedConfidence = (baseConfidence * 0.8f) + (edgeBoost * 0.2f)
        
        return combinedConfidence.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate edge-awareness boost for tiles containing content boundaries
     */
    private fun calculateEdgeAwarenessBoost(tileBitmap: Bitmap, tileRect: Rect): Float {
        try {
            var edgeDensity = 0f
            val sampleStep = 4
            
            // Sample edges of the tile to detect content boundaries
            for (x in 0 until tileBitmap.width step sampleStep) {
                // Top edge
                if (tileRect.top > 0) {
                    val pixel = tileBitmap.getPixel(x, 0)
                    edgeDensity += calculateEdgeStrength(pixel)
                }
                // Bottom edge
                if (tileRect.bottom < tileBitmap.height) {
                    val pixel = tileBitmap.getPixel(x, tileBitmap.height - 1)
                    edgeDensity += calculateEdgeStrength(pixel)
                }
            }
            
            for (y in 0 until tileBitmap.height step sampleStep) {
                // Left edge
                if (tileRect.left > 0) {
                    val pixel = tileBitmap.getPixel(0, y)
                    edgeDensity += calculateEdgeStrength(pixel)
                }
                // Right edge
                if (tileRect.right < tileBitmap.width) {
                    val pixel = tileBitmap.getPixel(tileBitmap.width - 1, y)
                    edgeDensity += calculateEdgeStrength(pixel)
                }
            }
            
            // Normalize edge density
            val maxSamples = (tileBitmap.width / sampleStep + tileBitmap.height / sampleStep) * 2
            val normalizedEdgeDensity = if (maxSamples > 0) edgeDensity / maxSamples else 0f
            
            // Convert to boost factor (higher edge density = higher boost)
            return normalizedEdgeDensity * 0.5f // Scale down to avoid over-boosting
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating edge awareness boost", e)
            return 0f
        }
    }
    
    /**
     * Calculate edge strength for a pixel based on color variance
     */
    private fun calculateEdgeStrength(pixel: Int): Float {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Calculate color variance as edge strength indicator
        val brightness = getPixelBrightness(pixel)
        val colorVariance = Math.abs(red - brightness) + Math.abs(green - brightness) + Math.abs(blue - brightness)
        
        return (colorVariance / 255.0f).coerceIn(0.0f, 1.0f)
    }

    /**
     * Merge overlapping regions and consolidate their confidence scores
     */
    private fun mergeOverlappingRegions(
        regions: List<Rect>,
        confidences: List<Float>
    ): List<Pair<Rect, Float>> {
        if (regions.isEmpty()) return emptyList()

        val merged = mutableListOf<Pair<Rect, Float>>()

        regions.forEachIndexed { index, region ->
            val confidence = confidences[index]

            // Check if this region overlaps significantly with any existing merged region
            val overlappingIndex = merged.indexOfFirst { (existingRegion, _) ->
                calculateOverlapRatio(region, existingRegion) > 0.3f // 30% overlap threshold
            }

            if (overlappingIndex >= 0) {
                // Merge with existing region, keeping the higher confidence
                val (existingRegion, existingConfidence) = merged[overlappingIndex]
                val mergedRect = Rect(
                    minOf(region.left, existingRegion.left),
                    minOf(region.top, existingRegion.top),
                    maxOf(region.right, existingRegion.right),
                    maxOf(region.bottom, existingRegion.bottom)
                )
                val mergedConfidence = maxOf(confidence, existingConfidence)
                merged[overlappingIndex] = Pair(mergedRect, mergedConfidence)
            } else {
                // Add as new region
                merged.add(Pair(region, confidence))
            }
        }

        return merged
    }
    
    /**
     * Merge overlapping regions with edge-awareness for better precision
     */
    private fun mergeOverlappingRegionsWithEdgeAwareness(
        regions: List<Rect>,
        confidences: List<Float>,
        bitmap: Bitmap
    ): List<Pair<Rect, Float>> {
        if (regions.isEmpty()) return emptyList()

        val merged = mutableListOf<Pair<Rect, Float>>()

        regions.forEachIndexed { index, region ->
            val confidence = confidences[index]

            // Check if this region overlaps significantly with any existing merged region
            val overlappingIndex = merged.indexOfFirst { (existingRegion, _) ->
                calculateOverlapRatio(region, existingRegion) > 0.25f // Lower threshold for more precise merging
            }

            if (overlappingIndex >= 0) {
                // Merge with existing region using confidence-weighted merging
                val (existingRegion, existingConfidence) = merged[overlappingIndex]
                
                // Calculate weighted average confidence
                val totalConfidence = confidence + existingConfidence
                val weightedConfidence = if (totalConfidence > 0) {
                    (confidence * confidence + existingConfidence * existingConfidence) / totalConfidence
                } else {
                    maxOf(confidence, existingConfidence)
                }
                
                // Create merged rectangle with edge-aware refinement
                val mergedRect = Rect(
                    minOf(region.left, existingRegion.left),
                    minOf(region.top, existingRegion.top),
                    maxOf(region.right, existingRegion.right),
                    maxOf(region.bottom, existingRegion.bottom)
                )
                
                // Apply edge refinement to the merged region
                val refinedRect = refineRegionWithEdgeDetection(mergedRect, bitmap)
                
                merged[overlappingIndex] = Pair(refinedRect, weightedConfidence)
            } else {
                // Add as new region with edge refinement
                val refinedRect = refineRegionWithEdgeDetection(region, bitmap)
                merged.add(Pair(refinedRect, confidence))
            }
        }

        return merged
    }
    
    /**
     * Refine region boundaries using edge detection
     */
    private fun refineRegionWithEdgeDetection(region: Rect, bitmap: Bitmap): Rect {
        try {
            var refinedLeft = region.left
            var refinedRight = region.right
            var refinedTop = region.top
            var refinedBottom = region.bottom
            
            // Refine left boundary
            if (region.left > 0) {
                refinedLeft = findContentBoundary(region.left, region.right, region.top, region.bottom, bitmap, true)
            }
            
            // Refine right boundary
            if (region.right < bitmap.width) {
                refinedRight = findContentBoundary(region.right, region.left, region.top, region.bottom, bitmap, false)
            }
            
            // Refine top boundary
            if (region.top > 0) {
                refinedTop = findContentBoundary(region.top, region.bottom, region.left, region.right, bitmap, true)
            }
            
            // Refine bottom boundary
            if (region.bottom < bitmap.height) {
                refinedBottom = findContentBoundary(region.bottom, region.top, region.left, region.right, bitmap, false)
            }
            
            return Rect(
                refinedLeft.coerceAtLeast(0),
                refinedTop.coerceAtLeast(0),
                refinedRight.coerceAtMost(bitmap.width),
                refinedBottom.coerceAtMost(bitmap.height)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refining region with edge detection", e)
            return region
        }
    }
    
    /**
     * Find content boundary using edge detection
     */
    private fun findContentBoundary(
        start: Int,
        end: Int,
        fixedStart: Int,
        fixedEnd: Int,
        bitmap: Bitmap,
        isHorizontal: Boolean
    ): Int {
        try {
            val step = if (isHorizontal) 1 else 1
            val direction = if (start < end) 1 else -1
            var currentPos = start
            
            // Sample pixels along the boundary to find content edges
            var edgeCount = 0
            var totalSamples = 0
            
            while (currentPos != end) {
                for (fixed in fixedStart until fixedEnd step 5) {
                    val x = if (isHorizontal) currentPos else fixed
                    val y = if (isHorizontal) fixed else currentPos
                    
                    if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(x, y)
                        val edgeStrength = calculateEdgeStrength(pixel)
                        
                        if (edgeStrength > 0.3f) {
                            edgeCount++
                        }
                        totalSamples++
                    }
                }
                
                // If we find significant edge density, this is likely the boundary
                if (totalSamples > 0) {
                    val edgeDensity = edgeCount.toFloat() / totalSamples
                    if (edgeDensity > 0.4f) {
                        return currentPos
                    }
                }
                
                currentPos += direction * step
            }
            
            return start // Fallback to original position
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding content boundary", e)
            return start
        }
    }

    /**
     * Calculate overlap ratio between two rectangles
     */
    private fun calculateOverlapRatio(rect1: Rect, rect2: Rect): Float {
        if (!rect1.intersect(rect2)) return 0.0f

        val intersectionArea = (rect1.width() * rect1.height()).toFloat()
        val smallerArea = minOf(
            rect1.width() * rect1.height(),
            rect2.width() * rect2.height()
        ).toFloat()

        return if (smallerArea > 0) intersectionArea / smallerArea else 0.0f
    }

    /**
     * Data class for region analysis results
     */
    private data class RegionAnalysisResult(
        val regionCount: Int,
        val regionRects: List<Rect>,
        val perRegionConfidences: List<Float>,
        val maxRegionConfidence: Float
    )
    
    private fun isSkinTonePixelFast(pixel: Int): Boolean {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Very simplified skin tone detection for speed
        return red > green && green > blue && red > 100
    }
    
    private fun calculateBitmapHash(bitmap: Bitmap): Int {
        // Simple hash for caching
        var hash = bitmap.width * 31 + bitmap.height
        
        // Sample a few pixels for content-based hashing
        val samplePoints = listOf(
            Pair(bitmap.width / 4, bitmap.height / 4),
            Pair(bitmap.width / 2, bitmap.height / 2),
            Pair(3 * bitmap.width / 4, 3 * bitmap.height / 4)
        )
        
        samplePoints.forEach { (x, y) ->
            if (x < bitmap.width && y < bitmap.height) {
                hash = hash * 31 + bitmap.getPixel(x, y)
            }
        }
        
        return hash
    }
    
    private fun getQualityLevelFromPerformanceMode(): QualityLevel {
        return when (currentPerformanceMode) {
            PerformanceMode.ULTRA_FAST -> QualityLevel.ULTRA_FAST
            PerformanceMode.FAST -> QualityLevel.FAST
            PerformanceMode.BALANCED -> QualityLevel.BALANCED
            PerformanceMode.QUALITY -> QualityLevel.HIGH
        }
    }
    
    /**
     * Clear gender detection cache
     */
    fun clearGenderCache() {
        genderCache.clear()
        Log.d(TAG, "Gender detection cache cleared")
    }
    
    /**
     * Set performance mode for ML processing
     */
    fun setPerformanceMode(mode: PerformanceMode) {
        currentPerformanceMode = mode
        Log.d(TAG, "Performance mode set to: ${mode.displayName}")
        
        // Clear caches when performance mode changes for consistency
        clearAllCaches()
    }
    
    /**
     * Get current performance mode
     */
    fun getCurrentPerformanceMode(): PerformanceMode = currentPerformanceMode
    
    /**
     * Check if GPU acceleration is enabled and active
     */
    fun isGPUAccelerationEnabled(): Boolean = gpuAccelerationManager.isGPUActive()
    
    /**
     * Get GPU acceleration information
     */
    fun getAccelerationInfo(): AccelerationInfo = gpuAccelerationManager.getAccelerationInfo()
    
    /**
     * Configure processing timeout based on performance requirements
     */
    fun configureProcessingTimeout(timeoutMs: Long) {
        Log.d(TAG, "Processing timeout configured to: ${timeoutMs}ms")
        // Timeout is now handled per-operation in detectNSFWWithTimeout
    }
    
    /**
     * Check if gender model is ready
     */
    override fun isGenderModelReady(): Boolean = isGenderModelReady
    
    /**
     * Clear all caches for memory management
     */
    fun clearAllCaches() {
        genderCache.clear()
        nsfwCache.clear()
        Log.d(TAG, "All ML caches cleared")
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up ML models and resources")

        try {
            // Close all interpreters with proper error handling
            nsfwInterpreter?.let { interpreter ->
                try {
                    interpreter.close()
                    Log.d(TAG, "NSFW interpreter closed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing NSFW interpreter", e)
                }
            }

            genderInterpreter?.let { interpreter ->
                try {
                    interpreter.close()
                    Log.d(TAG, "Gender interpreter closed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing gender interpreter", e)
                }
            }

            fastNsfwInterpreter?.let { interpreter ->
                try {
                    interpreter.close()
                    Log.d(TAG, "Fast NSFW interpreter closed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing fast NSFW interpreter", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during ML model cleanup", e)
        } finally {
            // Always null out references regardless of close() success
            nsfwInterpreter = null
            genderInterpreter = null
            fastNsfwInterpreter = null

            // Clear all caches to free memory
            clearAllCaches()

            // Cleanup GPU acceleration resources
            gpuAccelerationManager.cleanup()

            // Reset initialization flags
            isInitialized = false
            isGenderModelReady = false

            Log.d(TAG, "ML model cleanup completed")
        }
    }
    
    fun isModelReady(): Boolean = isInitialized
    
    data class DetectionResult(
        val isNSFW: Boolean,
        val confidence: Float,
        val details: String,
        // Enhanced region-based information for full-screen blur triggering
        val regionCount: Int = 0, // Number of distinct NSFW regions detected
        val regionRects: List<Rect> = emptyList(), // Bounding boxes of detected regions
        val perRegionConfidences: List<Float> = emptyList(), // Confidence score for each region
        val maxRegionConfidence: Float = 0.0f // Highest confidence among all regions
    )
    
    /**
     * Cache entry for gender detection results
     */
    private data class GenderCacheEntry(
        val result: GenderDetectionResult,
        val timestamp: Long
    )
    
    /**
     * Cache entry for NSFW detection results
     */
    private data class NSFWCacheEntry(
        val result: DetectionResult,
        val timestamp: Long
    )
    
    /**
     * ML diagnostic information
     */
    data class MLDiagnosticInfo(
        val isInitialized: Boolean,
        val isGenderModelReady: Boolean,
        val gpuAccelerationActive: Boolean,
        val nsfwInterpreterAvailable: Boolean,
        val genderInterpreterAvailable: Boolean,
        val currentPerformanceMode: PerformanceMode,
        val genderCacheSize: Int,
        val nsfwCacheSize: Int,
        val usingQuantizedModel: Boolean = false
    )
    
    /**
     * ML Status for UI observation - indicates overall health of ML subsystem
     */
    data class MLStatus(
        val isInitialized: Boolean = false,
        val nsfwModelAvailable: Boolean = false,
        val genderModelAvailable: Boolean = false,
        val gpuAccelerationActive: Boolean = false,
        val initializationError: String? = null,
        val overallHealth: MLHealth = MLHealth.NOT_INITIALIZED,
        val statusMessage: String = "ML models not yet initialized",
        // Enhanced status fields for detailed monitoring
        val nsfwUsingHeuristics: Boolean = true,      // True when NSFW falls back to skin-tone detection
        val genderUsingHeuristics: Boolean = true,    // True when gender uses heuristics
        val faceDetectionAvailable: Boolean = false,  // ML Kit face detection status
        val faceDetectionVerified: Boolean = false,   // True only after ML Kit successfully detected faces
        val faceDetectionHealthy: Boolean = false,    // True if face detection is working well
        val consecutiveEmptyFaceResults: Int = 0,     // Track consecutive empty results
        val detectionMode: String = "Not initialized", // "ML", "Heuristic", or "Hybrid"
        val nsfwCacheSize: Int = 0,
        val genderCacheSize: Int = 0
    ) {
        /**
         * Returns true if detection quality is degraded and user should be notified
         */
        fun shouldShowWarning(): Boolean = overallHealth in listOf(
            MLHealth.DEGRADED,
            MLHealth.CRITICAL,
            MLHealth.NOT_INITIALIZED
        )
        
        /**
         * Returns true if the system can still perform detection (even if degraded)
         */
        fun canPerformDetection(): Boolean = isInitialized || overallHealth != MLHealth.CRITICAL
    }
    
    /**
     * Health status levels for ML subsystem
     */
    enum class MLHealth {
        /** All models loaded and GPU active */
        HEALTHY,
        /** Models loaded but GPU not available - slower performance */
        REDUCED_PERFORMANCE,
        /** Some models unavailable - using heuristic fallbacks */
        DEGRADED,
        /** All models unavailable - detection severely limited */
        CRITICAL,
        /** Models not yet initialized */
        NOT_INITIALIZED
    }
}
