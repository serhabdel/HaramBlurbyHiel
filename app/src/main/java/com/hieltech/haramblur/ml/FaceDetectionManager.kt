package com.hieltech.haramblur.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
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

@Singleton
class FaceDetectionManager @Inject constructor(
    private val enhancedGenderDetector: EnhancedGenderDetector,
    private val mlModelManager: MLModelManager
) {
    
    companion object {
        private const val TAG = "FaceDetectionManager"
    }
    
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Use accurate mode for better detection
            .setMinFaceSize(0.05f) // Detect smaller faces - 5% of image instead of 10%
            .enableTracking() // Enable face tracking for better performance
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enable all classifications
            .build()
        
        FaceDetection.getClient(options)
    }
    
    suspend fun detectFaces(bitmap: Bitmap, appSettings: AppSettings? = null): FaceDetectionResult {
        return try {
            Log.d(TAG, "Starting face detection on ${bitmap.width}x${bitmap.height} bitmap")
            
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(inputImage).await()
            
            val faceInfo = coroutineScope {
                faces.map { face ->
                    async {
                        val rect = Rect(
                            face.boundingBox.left,
                            face.boundingBox.top,
                            face.boundingBox.right,
                            face.boundingBox.bottom
                        )
                        
                        // Use enhanced gender detection
                        val genderResult = if (enhancedGenderDetector.isReady()) {
                            enhancedGenderDetector.detectGender(face, bitmap)
                        } else {
                            // Fallback to MLModelManager
                            mlModelManager.detectGender(face, bitmap)
                        }
                        
                        DetectedFace(rect, genderResult.gender, genderResult.confidence, genderResult)
                    }
                }.map { it.await() }
            }
            
            Log.d(TAG, "Face detection completed: ${faces.size} faces found")
            
            FaceDetectionResult(
                facesDetected = faces.size,
                detectedFaces = faceInfo,
                success = true,
                error = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed", e)
            FaceDetectionResult(
                facesDetected = 0,
                detectedFaces = emptyList(),
                success = false,
                error = e.message
            )
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
            
        fun getMaleFaces(): List<DetectedFace> = detectedFaces.filter { it.estimatedGender == Gender.MALE }
        fun getFemaleFaces(): List<DetectedFace> = detectedFaces.filter { it.estimatedGender == Gender.FEMALE }
        fun getUnknownGenderFaces(): List<DetectedFace> = detectedFaces.filter { it.estimatedGender == Gender.UNKNOWN }
        
        /**
         * Get faces to blur based on app settings and confidence thresholds
         */
        fun getFacesToBlur(appSettings: AppSettings): List<DetectedFace> {
            val facesToBlur = mutableListOf<DetectedFace>()
            
            detectedFaces.forEach { face ->
                val shouldBlur = when (face.estimatedGender) {
                    Gender.MALE -> {
                        appSettings.blurMaleFaces && 
                        (face.genderConfidence >= 0.8f || shouldUseSaferFiltering(face, appSettings))
                    }
                    Gender.FEMALE -> {
                        appSettings.blurFemaleFaces && 
                        (face.genderConfidence >= 0.8f || shouldUseSaferFiltering(face, appSettings))
                    }
                    Gender.UNKNOWN -> {
                        // For unknown gender, use safer filtering approach
                        shouldUseSaferFiltering(face, appSettings)
                    }
                }
                
                if (shouldBlur) {
                    facesToBlur.add(face)
                }
            }
            
            return facesToBlur
        }
        
        /**
         * Determine if safer filtering should be applied for low confidence cases
         */
        private fun shouldUseSaferFiltering(face: DetectedFace, appSettings: AppSettings): Boolean {
            return when {
                // If confidence is very low, apply safer filtering based on user preferences
                face.genderConfidence < 0.6f -> {
                    // If user has both male and female blur enabled, blur unknown faces
                    appSettings.blurMaleFaces && appSettings.blurFemaleFaces
                }
                // If confidence is moderate, be more conservative
                face.genderConfidence < 0.8f -> {
                    // Apply the more restrictive setting (if either is enabled, blur)
                    appSettings.blurMaleFaces || appSettings.blurFemaleFaces
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
         */
        fun getFilteredFaces(
            includeMales: Boolean,
            includeFemales: Boolean,
            includeUnknown: Boolean,
            minConfidence: Float = 0.7f
        ): List<DetectedFace> {
            return detectedFaces.filter { face ->
                val meetsConfidence = face.genderConfidence >= minConfidence || face.estimatedGender == Gender.UNKNOWN
                val meetsGenderCriteria = when (face.estimatedGender) {
                    Gender.MALE -> includeMales
                    Gender.FEMALE -> includeFemales
                    Gender.UNKNOWN -> includeUnknown
                }
                meetsConfidence && meetsGenderCriteria
            }
        }
    }
}