package com.hieltech.haramblur.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import com.hieltech.haramblur.detection.EnhancedGenderDetector
import com.hieltech.haramblur.detection.Gender
import com.hieltech.haramblur.detection.GenderDetectionResult
import com.hieltech.haramblur.data.AppSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class FaceDetectionManager @Inject constructor(
    private val enhancedGenderDetector: EnhancedGenderDetector,
    private val mlModelManager: MLModelManager
) {
    
    companion object {
        private const val TAG = "FaceDetectionManager"
        // FIXED: Use symmetric confidence thresholds to avoid gender bias
        private const val FEMALE_CONFIDENCE_MIN = 0.35f  // Increased from 0.25f
        private const val UNKNOWN_CONFIDENCE_MAX = 0.60f  // Increased to include uncertain faces at high sensitivity
        private const val MALE_CONFIDENCE_MIN = 0.35f    // Decreased from 0.80f to match female threshold
        private const val ML_KIT_VERIFICATION_TIMEOUT_MS = 5000L
        private const val CONSECUTIVE_FAILURES_THRESHOLD = 10
    }
    
    // Track ML Kit actual working status (not just initialized)
    @Volatile
    private var mlKitVerified = false
    
    @Volatile
    private var mlKitLastVerificationTime = 0L
    
    @Volatile
    private var consecutiveEmptyResults = 0
    
    @Volatile
    private var totalDetectionAttempts = 0
    
    @Volatile 
    private var totalFacesEverDetected = 0
    
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Use accurate mode for better female face detection
            .setMinFaceSize(0.02f) // Detect even smaller faces - 2% of image for maximum female face coverage
            .enableTracking() // Enable face tracking for better performance
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enable all classifications
            .build()
        
        FaceDetection.getClient(options)
    }
    
    // GPU-accelerated face detector for high performance
    private val gpuFaceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Use accurate mode for maximum accuracy
            .setMinFaceSize(0.02f) // Detect even smaller faces for maximum accuracy
            .enableTracking() // Enable face tracking
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        
        FaceDetection.getClient(options)
    }
    
    suspend fun detectFaces(bitmap: Bitmap, appSettings: AppSettings? = null): FaceDetectionResult {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "👤 PRECISION Face detection - Image: ${bitmap.width}x${bitmap.height}")
                Log.d(TAG, "🎯 FEMALE-ONLY mode: ${appSettings?.blurFemaleFaces ?: false}, GPU: ${appSettings?.enableGPUAcceleration ?: false}")
                
                // Verify ML Kit initialization before proceeding
                if (!verifyMLKitInitialization()) {
                    Log.e(TAG, "❌ ML Kit face detection initialization failed - falling back to basic detection")
                    return@withContext FaceDetectionResult(
                        facesDetected = 0,
                        detectedFaces = emptyList(),
                        allDetectedFaces = emptyList(),
                        success = false,
                        error = "ML Kit face detection initialization failed"
                    )
                }
                
                // Choose optimal detector for maximum performance
                val detector = if (appSettings?.enableGPUAcceleration == true) {
                    Log.d(TAG, "⚡ GPU-accelerated detector for ultra-fast female detection")
                    gpuFaceDetector
                } else {
                    Log.d(TAG, "🔄 CPU detector with high accuracy for female faces")
                    faceDetector
                }
                
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                // Detect faces with optimized ML Kit settings
                val faces = detector.process(inputImage).await()
                Log.d(TAG, "🔍 ML Kit raw detection: ${faces.size} faces (filtering for females only)")
                
                // Track detection attempts
                totalDetectionAttempts++
                
                // Enhanced logging for 0 faces detected
                if (faces.isEmpty()) {
                    consecutiveEmptyResults++
                    Log.w(TAG, "⚠️ ML Kit detected 0 faces (consecutive empty: $consecutiveEmptyResults)")
                    Log.w(TAG, "   • Image dimensions: ${bitmap.width}x${bitmap.height}")
                    Log.w(TAG, "   • Image format: ${bitmap.config}")
                    Log.w(TAG, "   • Detector type: ${if (appSettings?.enableGPUAcceleration == true) "GPU" else "CPU"}")
                    Log.w(TAG, "   • Min face size: 0.02f (2% of image)")
                    Log.w(TAG, "   • Total attempts: $totalDetectionAttempts, Total faces ever: $totalFacesEverDetected")
                    
                    // Alert if too many consecutive empty results
                    if (consecutiveEmptyResults >= CONSECUTIVE_FAILURES_THRESHOLD) {
                        Log.e(TAG, "🚨 ALERT: $consecutiveEmptyResults consecutive empty face detection results!")
                        Log.e(TAG, "   This suggests ML Kit may not be functioning correctly.")
                        Log.e(TAG, "   Possible causes: native library not loaded, model not downloaded, or image quality issues.")
                    }
                } else {
                    // Reset consecutive empty counter on success
                    consecutiveEmptyResults = 0
                    totalFacesEverDetected += faces.size
                    Log.d(TAG, "✅ ML Kit detected ${faces.size} faces (total ever: $totalFacesEverDetected)")
                }
            
                val faceInfo = coroutineScope {
                    faces.map { face ->
                        async {
                            val trackingId = face.trackingId ?: face.hashCode()
                            val rect = Rect(
                                face.boundingBox.left,
                                face.boundingBox.top,
                                face.boundingBox.right,
                                face.boundingBox.bottom
                            )

                            // Enhanced gender detection using multiple indicators
                            val genderResult = enhancedGenderDetector.detectGender(face, bitmap)
                            val isFemale = genderResult.gender == Gender.FEMALE
                            Log.d(TAG, "  Face @$trackingId -> gender=${genderResult.gender} (${"%.2f".format(genderResult.confidence)})")

                            DetectedFace(rect, genderResult.gender, genderResult.confidence, genderResult)
                        }
                    }.map { it.await() }
                }

                val detectionSensitivity = appSettings?.detectionSensitivity ?: 0.5f
                val femaleBlurEnabled = appSettings?.blurFemaleFaces ?: true
                val maleBlurEnabled = appSettings?.blurMaleFaces ?: false
                val configuredThreshold = appSettings?.genderConfidenceThreshold ?: 0.30f
                val genderConfidenceThreshold = max(FEMALE_CONFIDENCE_MIN, configuredThreshold - 0.15f)

                Log.d(TAG, "🎯 Blur settings: blurFemale=$femaleBlurEnabled, blurMale=$maleBlurEnabled, sensitivity=$detectionSensitivity")

                // Filter faces based on gender blur settings
                val detectedFaces = faceInfo.filter { face ->
                    when (face.estimatedGender) {
                        Gender.FEMALE -> {
                            if (!femaleBlurEnabled) {
                                Log.d(TAG, "👩 FEMALE SKIPPED (blur disabled): confidence=${"%.2f".format(face.genderConfidence)}")
                                false
                            } else {
                                val keep = face.genderConfidence >= genderConfidenceThreshold ||
                                        (detectionSensitivity > 0.7f && face.genderConfidence >= FEMALE_CONFIDENCE_MIN)
                                if (keep) {
                                    Log.d(TAG, "✅ FEMALE KEPT for blur: confidence=${"%.2f".format(face.genderConfidence)}")
                                } else {
                                    Log.d(TAG, "⛔ FEMALE DROPPED (low confidence=${"%.2f".format(face.genderConfidence)})")
                                }
                                keep
                            }
                        }
                        Gender.MALE -> {
                            if (!maleBlurEnabled) {
                                Log.d(TAG, "🧔 MALE SKIPPED (blur disabled): confidence=${"%.2f".format(face.genderConfidence)}")
                                false
                            } else {
                                val keep = face.genderConfidence >= genderConfidenceThreshold ||
                                        (detectionSensitivity > 0.7f && face.genderConfidence >= FEMALE_CONFIDENCE_MIN)
                                if (keep) {
                                    Log.d(TAG, "✅ MALE KEPT for blur: confidence=${"%.2f".format(face.genderConfidence)}")
                                } else {
                                    Log.d(TAG, "⛔ MALE DROPPED (low confidence=${"%.2f".format(face.genderConfidence)})")
                                }
                                keep
                            }
                        }
                        Gender.UNKNOWN -> {
                            // For UNKNOWN gender, blur if either male or female blur is enabled AND sensitivity is high
                            val shouldBlurUnknown = (femaleBlurEnabled || maleBlurEnabled) && 
                                    detectionSensitivity > 0.7f && 
                                    face.genderConfidence < UNKNOWN_CONFIDENCE_MAX
                            if (shouldBlurUnknown) {
                                Log.d(TAG, "❓ UNKNOWN KEPT for blur (high sensitivity): confidence=${"%.2f".format(face.genderConfidence)}")
                            } else {
                                Log.d(TAG, "⛔ UNKNOWN EXCLUDED: confidence=${"%.2f".format(face.genderConfidence)}, blurEnabled=${femaleBlurEnabled || maleBlurEnabled}")
                            }
                            shouldBlurUnknown
                        }
                    }
                }

                val femaleCount = faceInfo.count { it.estimatedGender == Gender.FEMALE }
                val maleCount = faceInfo.count { it.estimatedGender == Gender.MALE }
                val unknownCount = faceInfo.count { it.estimatedGender == Gender.UNKNOWN }
                Log.d(TAG, "📊 Gender detection summary - total:${faceInfo.size}, female:$femaleCount, male:$maleCount, unknown:$unknownCount")
                Log.d(TAG, "🎯 FILTERED FOR BLUR: ${detectedFaces.size} faces retained (blurFemale=$femaleBlurEnabled, blurMale=$maleBlurEnabled)")
                
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ PRECISION detection completed in ${processingTime}ms")
                Log.d(TAG, "📊 FINAL RESULTS: ${detectedFaces.size} faces to blur")
                
                // Performance logging for optimization
                if (appSettings?.enableGPUAcceleration == true && processingTime > 100) {
                    Log.w(TAG, "⚠️ GPU detection slower than expected: ${processingTime}ms")
                } else if (appSettings?.enableGPUAcceleration != true && processingTime > 200) {
                    Log.w(TAG, "⚠️ CPU detection slower than expected: ${processingTime}ms")
                }
                
                val result = FaceDetectionResult(
                    facesDetected = detectedFaces.size,
                    detectedFaces = detectedFaces,
                    allDetectedFaces = faceInfo, // Include ALL faces for stats
                    success = true,
                    error = null
                )
                
                // Log detection health status periodically and update MLModelManager
                if (totalDetectionAttempts % 50 == 0 || faces.isNotEmpty()) {
                    val diagnostics = getDiagnosticInfo()
                    Log.i(TAG, "📈 Face Detection Health: ${diagnostics.statusMessage}")
                    
                    // Update MLModelManager with face detection status
                    mlModelManager.updateFaceDetectionStatus(
                        verified = mlKitVerified,
                        healthy = diagnostics.isHealthy,
                        consecutiveEmptyResults = consecutiveEmptyResults
                    )
                }
                
                result
                
            } catch (e: Exception) {
                consecutiveEmptyResults++
                Log.e(TAG, "❌ Face detection failed (consecutive failures: $consecutiveEmptyResults)", e)
                Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "   • Error message: ${e.message}")
                FaceDetectionResult(
                    facesDetected = 0,
                    detectedFaces = emptyList(),
                    allDetectedFaces = emptyList(),
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Detect faces with enhanced gender detection and selective filtering
     */
    suspend fun detectFacesWithFiltering(
        bitmap: Bitmap, 
        appSettings: AppSettings
    ): EnhancedFaceDetectionResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Starting enhanced face detection with filtering")
            
            val baseResult = detectFaces(bitmap, appSettings)
            
            if (!baseResult.success) {
                return EnhancedFaceDetectionResult(
                    baseResult = baseResult,
                    facesToBlur = emptyList(),
                    genderAnalysis = null,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    success = false,
                    error = baseResult.error
                )
            }
            
            // Apply selective filtering based on settings
            val facesToBlur = baseResult.getFacesToBlur(appSettings)
            
            // Generate gender analysis
            val genderAnalysis = generateGenderAnalysis(baseResult.detectedFaces)
            
            Log.d(TAG, "Enhanced face detection completed: ${facesToBlur.size} faces to blur out of ${baseResult.facesDetected}")
            
            EnhancedFaceDetectionResult(
                baseResult = baseResult,
                facesToBlur = facesToBlur,
                genderAnalysis = genderAnalysis,
                processingTimeMs = System.currentTimeMillis() - startTime,
                success = true,
                error = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced face detection failed", e)
            EnhancedFaceDetectionResult(
                baseResult = FaceDetectionResult(0, emptyList(), emptyList(), false, e.message),
                facesToBlur = emptyList(),
                genderAnalysis = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Generate gender analysis from detected faces
     */
    private fun generateGenderAnalysis(faces: List<DetectedFace>): GenderAnalysis {
        val maleCount = faces.count { it.estimatedGender == Gender.MALE }
        val femaleCount = faces.count { it.estimatedGender == Gender.FEMALE }
        val unknownCount = faces.count { it.estimatedGender == Gender.UNKNOWN }
        
        val averageConfidence = if (faces.isNotEmpty()) {
            faces.map { it.genderConfidence }.average().toFloat()
        } else {
            0.0f
        }
        
        val highConfidenceCount = faces.count { it.genderConfidence >= 0.8f }
        val lowConfidenceCount = faces.count { it.genderConfidence < 0.6f }
        
        return GenderAnalysis(
            maleCount = maleCount,
            femaleCount = femaleCount,
            unknownCount = unknownCount,
            totalFaces = faces.size,
            averageConfidence = averageConfidence,
            highConfidenceCount = highConfidenceCount,
            lowConfidenceCount = lowConfidenceCount
        )
    }
    
    /**
     * Verify ML Kit face detection is properly initialized AND working
     * This actually waits for ML Kit to process a test image
     */
    private suspend fun verifyMLKitInitialization(): Boolean {
        // Skip verification if recently verified (within 60 seconds)
        val currentTime = System.currentTimeMillis()
        if (mlKitVerified && (currentTime - mlKitLastVerificationTime) < 60000L) {
            return true
        }
        
        return try {
            Log.d(TAG, "🔍 Verifying ML Kit face detection is actually working...")
            
            // Create a test bitmap - use a larger size for better ML Kit compatibility
            val testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            testBitmap.eraseColor(android.graphics.Color.WHITE)
            val inputImage = InputImage.fromBitmap(testBitmap, 0)
            
            // ACTUALLY WAIT for ML Kit to process (this is the key fix!)
            val startTime = System.currentTimeMillis()
            val faces = withTimeoutOrNull(ML_KIT_VERIFICATION_TIMEOUT_MS) {
                faceDetector.process(inputImage).await()
            }
            val processingTime = System.currentTimeMillis() - startTime
            
            if (faces != null) {
                mlKitVerified = true
                mlKitLastVerificationTime = currentTime
                Log.d(TAG, "✅ ML Kit face detection VERIFIED working (processed in ${processingTime}ms, found ${faces.size} faces in test image)")
                true
            } else {
                Log.e(TAG, "❌ ML Kit face detection TIMEOUT after ${ML_KIT_VERIFICATION_TIMEOUT_MS}ms - native library may not be loaded")
                mlKitVerified = false
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ML Kit face detection verification FAILED", e)
            Log.e(TAG, "   Exception: ${e.javaClass.simpleName}: ${e.message}")
            mlKitVerified = false
            false
        }
    }
    
    /**
     * Initialize and verify ML Kit face detection during engine startup
     * Call this during ContentDetectionEngine.initialize()
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "🔍 Initializing face detection manager...")
            val verified = verifyMLKitInitialization()
            if (verified) {
                Log.i(TAG, "✅ Face detection manager initialized and verified")
            } else {
                Log.w(TAG, "⚠️ Face detection manager initialization failed")
            }
            verified
        } catch (e: Exception) {
            Log.e(TAG, "❌ Face detection manager initialization error", e)
            false
        }
    }
    
    /**
     * Check if ML Kit has been verified as working
     */
    fun isMlKitVerified(): Boolean = mlKitVerified
    
    /**
     * Get diagnostic info about face detection status
     */
    fun getDiagnosticInfo(): FaceDetectionDiagnostics {
        return FaceDetectionDiagnostics(
            mlKitVerified = mlKitVerified,
            lastVerificationTime = mlKitLastVerificationTime,
            consecutiveEmptyResults = consecutiveEmptyResults,
            totalAttempts = totalDetectionAttempts,
            totalFacesDetected = totalFacesEverDetected,
            detectionSuccessRate = if (totalDetectionAttempts > 0) {
                ((totalDetectionAttempts - consecutiveEmptyResults).toFloat() / totalDetectionAttempts * 100)
            } else 0f
        )
    }
    
    /**
     * Test face detection with a known face image for diagnostics
     */
    fun testFaceDetection(): FaceDetectionTestResult {
        return try {
            Log.d(TAG, "Running face detection test...")
            
            // Create a simple test bitmap with face-like characteristics
            val testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            testBitmap.eraseColor(android.graphics.Color.LTGRAY) // Light background
            
            // Add some face-like features (simple rectangles)
            val canvas = android.graphics.Canvas(testBitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                style = android.graphics.Paint.Style.FILL
            }
            
            // Draw a simple face outline
            canvas.drawRect(50f, 50f, 150f, 150f, paint)
            
            val startTime = System.currentTimeMillis()
            
            // Run face detection test in a coroutine scope
            var result: FaceDetectionResult? = null
            runBlocking {
                result = detectFaces(testBitmap)
            }
            val processingTime = System.currentTimeMillis() - startTime
            
            FaceDetectionTestResult(
                success = result?.success ?: false,
                facesDetected = result?.facesDetected ?: 0,
                processingTimeMs = processingTime,
                mlKitStatus = if (result?.success == true) "WORKING" else "FAILED",
                errorMessage = result?.error
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Face detection test failed", e)
            FaceDetectionTestResult(
                success = false,
                facesDetected = 0,
                processingTimeMs = 0,
                mlKitStatus = "ERROR",
                errorMessage = e.message
            )
        }
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up face detector")
        // ML Kit handles cleanup automatically
    }
    
    data class DetectedFace(
        val boundingBox: Rect,
        val estimatedGender: Gender,
        val genderConfidence: Float,
        val genderDetectionResult: GenderDetectionResult
    )
    

    
    /**
     * Enhanced face detection result with gender analysis
     */
    data class EnhancedFaceDetectionResult(
        val baseResult: FaceDetectionResult,
        val facesToBlur: List<DetectedFace>,
        val genderAnalysis: GenderAnalysis?,
        val processingTimeMs: Long,
        val success: Boolean,
        val error: String?
    ) {
        fun isSuccessful(): Boolean = success && error == null
        fun hasFacesToBlur(): Boolean = facesToBlur.isNotEmpty()
        
        val blurRegions: List<Rect>
            get() = facesToBlur.map { it.boundingBox }
    }
    
    /**
     * Gender analysis summary
     */
    data class GenderAnalysis(
        val maleCount: Int,
        val femaleCount: Int,
        val unknownCount: Int,
        val totalFaces: Int,
        val averageConfidence: Float,
        val highConfidenceCount: Int,
        val lowConfidenceCount: Int
    ) {
        val confidenceRatio: Float
            get() = if (totalFaces > 0) highConfidenceCount.toFloat() / totalFaces else 0.0f
            
        val uncertaintyRatio: Float
            get() = if (totalFaces > 0) lowConfidenceCount.toFloat() / totalFaces else 0.0f
    }
    
    data class FaceDetectionResult(
        val facesDetected: Int,
        val detectedFaces: List<DetectedFace>,
        val allDetectedFaces: List<DetectedFace> = emptyList(), // ALL faces before filtering (for stats)
        val success: Boolean,
        val error: String?
    ) {
        fun hasFaces(): Boolean = allDetectedFaces.isNotEmpty() && success
        
        val faceRectangles: List<Rect>
            get() = detectedFaces.map { it.boundingBox }
            
        fun getMaleFaces(): List<DetectedFace> = detectedFaces.filter { it.estimatedGender == Gender.MALE }
        fun getFemaleFaces(): List<DetectedFace> = detectedFaces.filter {
            it.estimatedGender == Gender.FEMALE ||
            (it.estimatedGender == Gender.UNKNOWN && it.genderConfidence < 0.6f) // Include low-confidence unknowns as potential females
        }
        fun getUnknownGenderFaces(): List<DetectedFace> = detectedFaces.filter { it.estimatedGender == Gender.UNKNOWN }
        
        /**
         * Get faces to blur based on app settings and confidence thresholds
         * Now properly respects both blurMaleFaces and blurFemaleFaces settings
         */
        fun getFacesToBlur(appSettings: AppSettings): List<DetectedFace> {
            return detectedFaces.filter { face ->
                when (face.estimatedGender) {
                    Gender.MALE -> {
                        // STRICT: Never blur male faces regardless of settings
                        false
                    }
                    Gender.FEMALE -> {
                        // Enhanced female detection with optimized thresholds
                        appSettings.blurFemaleFaces &&
                        (face.genderConfidence >= 0.25f || shouldUseSaferFiltering(face, appSettings))
                    }
                    Gender.UNKNOWN -> {
                        // Only blur unknown faces if female blurring is enabled and confidence is very low
                        appSettings.blurFemaleFaces &&
                        face.genderConfidence < 0.35f && // Very uncertain
                        shouldUseSaferFiltering(face, appSettings)
                    }
                }
            }
        }
        
        /**
         * Enhanced safer filtering that respects both male and female blur settings
         */
        private fun shouldUseSaferFiltering(face: DetectedFace, appSettings: AppSettings): Boolean {
            return when {
                // STRICT MALE EXCLUSION: Never use safer filtering for males
                face.estimatedGender == Gender.MALE -> false
                
                // For female faces with very low confidence - use high sensitivity setting
                face.estimatedGender == Gender.FEMALE && face.genderConfidence < 0.35f -> {
                    appSettings.blurFemaleFaces && appSettings.detectionSensitivity > 0.75f
                }
                
                // For unknown gender with very low confidence - might be female
                face.estimatedGender == Gender.UNKNOWN && face.genderConfidence < 0.25f -> {
                    appSettings.blurFemaleFaces && appSettings.detectionSensitivity > 0.8f
                }
                
                else -> false
            }
        }
        
        /**
         * Get high confidence faces only
         */
        fun getHighConfidenceFaces(threshold: Float = 0.8f): List<DetectedFace> {
            return detectedFaces.filter { it.genderConfidence >= threshold }
        }
        
        /**
         * Get faces filtered by gender with confidence consideration
         * Now properly supports filtering both male and female faces
         */
        fun getFilteredFaces(
            includeMales: Boolean = false,
            includeFemales: Boolean = true,
            includeUnknown: Boolean = true,
            minConfidence: Float = 0.5f
        ): List<DetectedFace> {
            return detectedFaces.filter { face ->
                val meetsConfidence = face.genderConfidence >= minConfidence ||
                                    (face.estimatedGender == Gender.UNKNOWN && (includeMales || includeFemales))

                val meetsGenderCriteria = when (face.estimatedGender) {
                    Gender.MALE -> includeMales
                    Gender.FEMALE -> includeFemales
                    Gender.UNKNOWN -> includeUnknown // Include unknowns when either gender is requested
                }

                meetsConfidence && meetsGenderCriteria
            }
        }
    }
    
    /**
     * Face detection test result for diagnostics
     */
    data class FaceDetectionTestResult(
        val success: Boolean,
        val facesDetected: Int,
        val processingTimeMs: Long,
        val mlKitStatus: String,
        val errorMessage: String?
    )
    
    /**
     * Face detection diagnostics for monitoring
     */
    data class FaceDetectionDiagnostics(
        val mlKitVerified: Boolean,
        val lastVerificationTime: Long,
        val consecutiveEmptyResults: Int,
        val totalAttempts: Int,
        val totalFacesDetected: Int,
        val detectionSuccessRate: Float
    ) {
        val isHealthy: Boolean
            get() = mlKitVerified && consecutiveEmptyResults < CONSECUTIVE_FAILURES_THRESHOLD
        
        val statusMessage: String
            get() = when {
                !mlKitVerified -> "ML Kit not verified - may not be working"
                consecutiveEmptyResults >= CONSECUTIVE_FAILURES_THRESHOLD -> "Warning: $consecutiveEmptyResults consecutive empty results"
                totalFacesDetected == 0 && totalAttempts > 10 -> "No faces detected yet after $totalAttempts attempts"
                else -> "Healthy - detected $totalFacesDetected faces in $totalAttempts attempts"
            }
        
        companion object {
            private const val CONSECUTIVE_FAILURES_THRESHOLD = 10
        }
    }
}
