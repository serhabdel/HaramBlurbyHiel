package com.hieltech.haramblur.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
        private const val FEMALE_CONFIDENCE_MIN = 0.35f
        private const val UNKNOWN_CONFIDENCE_MAX = 0.45f
    }
    
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Use accurate mode for better female face detection
            .setMinFaceSize(0.03f) // Detect even smaller faces - 3% of image for better female face coverage
            .enableTracking() // Enable face tracking for better performance
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enable all classifications
            .build()
        
        FaceDetection.getClient(options)
    }
    
    // GPU-accelerated face detector for high performance
    private val gpuFaceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Fast mode for GPU processing
            .setMinFaceSize(0.04f) // Slightly larger minimum for GPU processing
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
                val configuredThreshold = appSettings?.genderConfidenceThreshold ?: 0.4f
                val femaleConfidenceThreshold = max(FEMALE_CONFIDENCE_MIN, configuredThreshold - 0.1f)

                val detectedFaces = if (femaleBlurEnabled) {
                    faceInfo.filter { face ->
                        when (face.estimatedGender) {
                            Gender.FEMALE -> {
                                val keep = face.genderConfidence >= femaleConfidenceThreshold ||
                                        (detectionSensitivity > 0.85f && face.genderConfidence >= FEMALE_CONFIDENCE_MIN)
                                if (keep) {
                                    Log.d(TAG, "✅ FEMALE KEPT: confidence=${"%.2f".format(face.genderConfidence)}")
                                } else {
                                    Log.d(TAG, "⛔ FEMALE DROPPED (low confidence=${"%.2f".format(face.genderConfidence)})")
                                }
                                keep
                            }
                            Gender.UNKNOWN -> {
                                val keep = detectionSensitivity > 0.75f && face.genderConfidence < UNKNOWN_CONFIDENCE_MAX
                                if (keep) {
                                    Log.d(TAG, "❓ POSSIBLE FEMALE (unknown, confidence=${"%.2f".format(face.genderConfidence)})")
                                } else {
                                    Log.d(TAG, "⛔ UNKNOWN EXCLUDED: confidence=${"%.2f".format(face.genderConfidence)}")
                                }
                                keep
                            }
                            Gender.MALE -> {
                                Log.d(TAG, "🚫 MALE EXCLUDED: confidence=${"%.2f".format(face.genderConfidence)}")
                                false
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "♀️ Female face blurring disabled in settings; skipping detected faces")
                    emptyList()
                }

                val femaleCount = faceInfo.count { it.estimatedGender == Gender.FEMALE }
                val maleCount = faceInfo.count { it.estimatedGender == Gender.MALE }
                val unknownCount = faceInfo.count { it.estimatedGender == Gender.UNKNOWN }
                Log.d(TAG, "📊 Gender detection summary - total:${faceInfo.size}, female:$femaleCount, male:$maleCount, unknown:$unknownCount")
                Log.d(TAG, "🎯 FILTERED FOR BLUR: ${detectedFaces.size} female faces retained")
                
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ PRECISION detection completed in ${processingTime}ms")
                Log.d(TAG, "📊 FINAL RESULTS: ${detectedFaces.size} female faces (males excluded)")
                
                // Performance logging for optimization
                if (appSettings?.enableGPUAcceleration == true && processingTime > 100) {
                    Log.w(TAG, "⚠️ GPU detection slower than expected: ${processingTime}ms")
                } else if (appSettings?.enableGPUAcceleration != true && processingTime > 200) {
                    Log.w(TAG, "⚠️ CPU detection slower than expected: ${processingTime}ms")
                }
                
                FaceDetectionResult(
                    facesDetected = detectedFaces.size,
                    detectedFaces = detectedFaces,
                    success = true,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Face detection failed", e)
                Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "   • Error message: ${e.message}")
                FaceDetectionResult(
                    facesDetected = 0,
                    detectedFaces = emptyList(),
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
                baseResult = FaceDetectionResult(0, emptyList(), false, e.message),
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
        val success: Boolean,
        val error: String?
    ) {
        fun hasFaces(): Boolean = facesDetected > 0 && success
        
        val faceRectangles: List<Rect>
            get() = detectedFaces.map { it.boundingBox }
            
        fun getMaleFaces(): List<DetectedFace> = emptyList() // Male faces are completely excluded from detection
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
                        (face.genderConfidence >= 0.35f || shouldUseSaferFiltering(face, appSettings))
                    }
                    Gender.UNKNOWN -> {
                        // Only blur unknown faces if female blurring is enabled and confidence is very low
                        appSettings.blurFemaleFaces &&
                        face.genderConfidence < 0.4f && // Very uncertain
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
                face.estimatedGender == Gender.FEMALE && face.genderConfidence < 0.4f -> {
                    appSettings.blurFemaleFaces && appSettings.detectionSensitivity > 0.8f
                }
                
                // For unknown gender with very low confidence - might be female
                face.estimatedGender == Gender.UNKNOWN && face.genderConfidence < 0.3f -> {
                    appSettings.blurFemaleFaces && appSettings.detectionSensitivity > 0.9f
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
}
