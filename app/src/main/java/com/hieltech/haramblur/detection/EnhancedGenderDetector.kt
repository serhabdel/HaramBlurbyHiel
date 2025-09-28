package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced gender detection interface for improved accuracy in gender classification
 */
interface EnhancedGenderDetector {
    /**
     * Detect gender for a single face with enhanced accuracy
     */
    suspend fun detectGender(face: Face, bitmap: Bitmap): GenderDetectionResult
    
    /**
     * Analyze gender distribution across multiple faces
     */
    suspend fun analyzeGenderDistribution(faces: List<Face>, bitmap: Bitmap): GenderDistributionResult
    
    /**
     * Update the gender classification model
     */
    fun updateGenderModel(modelPath: String): Boolean
    
    /**
     * Check if the enhanced gender detector is ready
     */
    fun isReady(): Boolean
}

/**
 * Provides gender model inference capabilities
 */
interface GenderModelProvider {
    suspend fun detectGender(face: Face, bitmap: Bitmap): GenderDetectionResult
    fun isGenderModelReady(): Boolean
}

/**
 * Implementation of enhanced gender detection with improved algorithms
 */
@Singleton
class EnhancedGenderDetectorImpl @Inject constructor(
    private val genderModelProvider: GenderModelProvider
) : EnhancedGenderDetector {
    
    companion object {
        private const val TAG = "EnhancedGenderDetector"
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.65f
        private const val MAX_ACCURACY_CONFIDENCE_THRESHOLD = 0.30f
        private const val FEMALE_BIAS_FACTOR = 0.15f
        private const val FACIAL_FEATURE_ANALYSIS_ENABLED = true
    }
    
    private var isInitialized = false
    private var modelPath: String? = null
    private val genderCache = mutableMapOf<Int, GenderDetectionResult>()
    
    override suspend fun detectGender(face: Face, bitmap: Bitmap): GenderDetectionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            val cacheKey = face.trackingId ?: face.hashCode()

            Log.d(TAG, "Starting enhanced gender detection for face: $cacheKey")

            // Check cache first for performance
            genderCache[cacheKey]?.let { cachedResult ->
                Log.d(TAG, "Using cached gender result for face: $cacheKey")
                return@withContext cachedResult.copy(
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
            }

            val modelReady = genderModelProvider.isGenderModelReady()
            var usedModel = false

            val result = if (modelReady) {
                Log.d(TAG, "Using TensorFlow Lite gender model for face: $cacheKey")
                try {
                    val modelResult = genderModelProvider.detectGender(face, bitmap)
                    usedModel = true
                    isInitialized = true
                    modelResult.copy(processingTimeMs = System.currentTimeMillis() - startTime)
                } catch (e: Exception) {
                    Log.w(TAG, "ML gender model inference failed, falling back to heuristics", e)
                    isInitialized = false
                    detectGenderWithHeuristics(face, bitmap, startTime)
                }
            } else {
                Log.w(TAG, "Gender model not ready, using heuristic detection for face: $cacheKey")
                isInitialized = false
                detectGenderWithHeuristics(face, bitmap, startTime)
            }

            // Cache result for future frames
            genderCache[cacheKey] = result

            val detectionSource = if (usedModel) "ML" else "heuristic"
            Log.d(TAG, "Gender detection completed ($detectionSource): ${result.gender} (confidence: ${result.confidence})")
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "Enhanced gender detection failed", e)
            return@withContext GenderDetectionResult(
                gender = Gender.UNKNOWN,
                confidence = 0.0f,
                facialFeatures = FacialFeatureAnalysis.default(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    override suspend fun analyzeGenderDistribution(faces: List<Face>, bitmap: Bitmap): GenderDistributionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Analyzing gender distribution for ${faces.size} faces")
            
            val genderResults = faces.map { face ->
                detectGender(face, bitmap)
            }
            
            val maleCount = genderResults.count { it.gender == Gender.MALE && it.confidence >= MAX_ACCURACY_CONFIDENCE_THRESHOLD }
            val femaleCount = genderResults.count { it.gender == Gender.FEMALE && it.confidence >= MAX_ACCURACY_CONFIDENCE_THRESHOLD }
            val unknownCount = faces.size - maleCount - femaleCount
            
            val averageConfidence = if (genderResults.isNotEmpty()) {
                genderResults.map { it.confidence }.average().toFloat()
            } else {
                0.0f
            }
            
            val recommendedAction = determineRecommendedBlurAction(maleCount, femaleCount, unknownCount, averageConfidence)
            
            val result = GenderDistributionResult(
                maleCount = maleCount,
                femaleCount = femaleCount,
                unknownCount = unknownCount,
                averageConfidence = averageConfidence,
                recommendedAction = recommendedAction,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
            
            Log.d(TAG, "Gender distribution analysis completed: M:$maleCount, F:$femaleCount, U:$unknownCount")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Gender distribution analysis failed", e)
            return@withContext GenderDistributionResult(
                maleCount = 0,
                femaleCount = 0,
                unknownCount = faces.size,
                averageConfidence = 0.0f,
                recommendedAction = BlurAction.BLUR_ALL_SAFER,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    override fun updateGenderModel(modelPath: String): Boolean {
        return try {
            Log.d(TAG, "Updating gender model: $modelPath")
            this.modelPath = modelPath
            // TODO: Load TensorFlow Lite model in task 2.2
            isInitialized = genderModelProvider.isGenderModelReady()
            Log.d(TAG, "Gender model updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update gender model", e)
            false
        }
    }
    
    override fun isReady(): Boolean = isInitialized || genderModelProvider.isGenderModelReady()
    
    /**
     * Set maximum accuracy mode for enhanced gender detection
     */
    fun setMaximumAccuracyMode(enabled: Boolean) {
        Log.d(TAG, "Setting maximum accuracy mode: $enabled")
        // This method can be used to switch between standard and maximum accuracy thresholds
        // based on app settings. The actual threshold selection is handled in the detection logic.
    }

    private fun detectGenderWithHeuristics(
        face: Face,
        bitmap: Bitmap,
        startTime: Long
    ): GenderDetectionResult {
        val facialFeatures = if (FACIAL_FEATURE_ANALYSIS_ENABLED) {
            analyzeFacialFeatures(face, bitmap)
        } else {
            FacialFeatureAnalysis.default()
        }

        // Enhanced gender classification using multiple indicators
        val genderClassification = performEnhancedGenderClassification(face, bitmap, facialFeatures)

        val result = GenderDetectionResult(
            gender = genderClassification.gender,
            confidence = genderClassification.confidence,
            facialFeatures = facialFeatures,
            processingTimeMs = System.currentTimeMillis() - startTime
        )

        Log.d(TAG, "Heuristic gender detection: ${result.gender} (confidence: ${result.confidence})")
        return result
    }
    
    /**
     * Analyze facial features for improved gender classification
     */
    private fun analyzeFacialFeatures(face: Face, bitmap: Bitmap): FacialFeatureAnalysis {
        return try {
            val boundingBox = face.boundingBox
            val faceWidth = boundingBox.width()
            val faceHeight = boundingBox.height()
            
            // Facial structure analysis
            val jawlineSharpness = analyzeFaceStructure(face, bitmap)
            val eyebrowThickness = analyzeEyebrowCharacteristics(face)
            val facialHairPresence = analyzeFacialHair(face, bitmap)
            val cheekboneProminence = analyzeCheekbones(face)
            
            FacialFeatureAnalysis(
                jawlineSharpness = jawlineSharpness,
                eyebrowThickness = eyebrowThickness,
                facialHairPresence = facialHairPresence,
                cheekboneProminence = cheekboneProminence,
                faceAspectRatio = faceWidth.toFloat() / faceHeight.toFloat(),
                confidenceScore = calculateFeatureConfidence(jawlineSharpness, eyebrowThickness, facialHairPresence)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Facial feature analysis failed, using defaults", e)
            FacialFeatureAnalysis.default()
        }
    }
    
    /**
     * Perform enhanced gender classification using multiple indicators
     */
    private fun performEnhancedGenderClassification(
        face: Face, 
        bitmap: Bitmap, 
        facialFeatures: FacialFeatureAnalysis
    ): GenderClassification {
        // Enhanced classification algorithm combining multiple factors
        var maleScore = 0.0f
        var femaleScore = 0.0f
        
        // Facial structure indicators
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
        
        // Apply female bias for Islamic compliance BEFORE normalization
        femaleScore += FEMALE_BIAS_FACTOR
        
        // Determine final classification
        val totalScore = maleScore + femaleScore
        val normalizedMaleScore = if (totalScore > 0) maleScore / totalScore else 0.5f
        val normalizedFemaleScore = if (totalScore > 0) femaleScore / totalScore else 0.5f
        
        return when {
            // LOWERED THRESHOLD: Only classify as male if very confident (reduced from 0.75f to 0.65f)
            normalizedMaleScore > normalizedFemaleScore && normalizedMaleScore > 0.65f -> {
                GenderClassification(Gender.MALE, normalizedMaleScore)
            }
            // LOWERED THRESHOLD: Classify as female with lower confidence (reduced from 0.45f to 0.35f)
            normalizedFemaleScore > normalizedMaleScore && normalizedFemaleScore > 0.35f -> {
                GenderClassification(Gender.FEMALE, normalizedFemaleScore)
            }
            else -> {
                // BOOST CONFIDENCE: When uncertain, prefer female classification for Islamic compliance
                val boostedConfidence = maxOf(normalizedMaleScore, normalizedFemaleScore)
                val finalGender = if (boostedConfidence >= 0.30f) {
                    // If we have at least 30% confidence, classify as female (safer for Islamic compliance)
                    Gender.FEMALE
                } else {
                    // Only classify as UNKNOWN if confidence is extremely low
                    Gender.UNKNOWN
                }
                GenderClassification(finalGender, boostedConfidence.coerceAtLeast(0.30f))
            }
        }
    }
    
    // Facial feature analysis helper methods
    private fun analyzeFaceStructure(face: Face, bitmap: Bitmap): Float {
        // Analyze jawline sharpness based on face contour
        // This is a simplified implementation - in production would use more sophisticated analysis
        return try {
            val boundingBox = face.boundingBox
            val aspectRatio = boundingBox.width().toFloat() / boundingBox.height().toFloat()
            // Sharper jawlines tend to have wider face ratios
            minOf(1.0f, maxOf(0.0f, (aspectRatio - 0.7f) * 2.0f))
        } catch (e: Exception) {
            0.5f // Default neutral value
        }
    }
    
    private fun analyzeEyebrowCharacteristics(face: Face): Float {
        // Enhanced eyebrow analysis with better thresholds for thin vs thick distinction
        return try {
            val boundingBox = face.boundingBox
            val faceWidth = boundingBox.width()
            val faceHeight = boundingBox.height()
            val faceArea = faceWidth * faceHeight
            
            // Calculate eyebrow region (upper portion of face)
            val eyebrowRegionHeight = (faceHeight * 0.25f).toInt() // Upper 25% of face
            val eyebrowRegionTop = boundingBox.top
            val eyebrowRegionBottom = eyebrowRegionTop + eyebrowRegionHeight
            
            // Use multiple factors for better eyebrow thickness estimation
            val faceSizeFactor = minOf(1.0f, maxOf(0.2f, faceArea.toFloat() / 40000.0f))
            
            // Aspect ratio factor (wider faces might have thicker eyebrows)
            val aspectRatio = faceWidth.toFloat() / faceHeight.toFloat()
            val aspectRatioFactor = when {
                aspectRatio > 0.9f -> 0.8f // Wide face (likely male, thicker eyebrows)
                aspectRatio > 0.8f -> 0.6f // Medium face
                else -> 0.4f              // Narrow face (likely female, thinner eyebrows)
            }
            
            // Face height factor (taller faces might have more prominent eyebrows)
            val heightFactor = when {
                faceHeight > 180 -> 0.7f // Tall face
                faceHeight > 140 -> 0.5f // Medium face
                else -> 0.3f            // Short face
            }
            
            // Combine factors with weights for better accuracy
            val combinedScore = (faceSizeFactor * 0.4f) + (aspectRatioFactor * 0.4f) + (heightFactor * 0.2f)
            
            Log.d(TAG, "Eyebrow analysis: faceSize=$faceSizeFactor, aspect=$aspectRatioFactor, height=$heightFactor, combined=$combinedScore")
            
            combinedScore.coerceIn(0.1f, 1.0f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced eyebrow analysis", e)
            0.5f // Default neutral value on error
        }
    }
    
    private fun analyzeFacialHair(face: Face, bitmap: Bitmap): Float {
        // Enhanced facial hair detection with texture/variance analysis
        return try {
            val boundingBox = face.boundingBox
            val faceWidth = boundingBox.width()
            val faceHeight = boundingBox.height()
            
            // Focus on lower face region (mouth to chin area)
            val lowerFaceHeight = (faceHeight * 0.4f).toInt() // Lower 40% of face
            val lowerFaceTop = boundingBox.bottom - lowerFaceHeight
            val lowerFaceRect = Rect(
                boundingBox.left,
                maxOf(0, lowerFaceTop),
                boundingBox.right,
                boundingBox.bottom
            )
            
            // Ensure the region is within bitmap bounds
            if (lowerFaceRect.width() <= 0 || lowerFaceRect.height() <= 0 ||
                lowerFaceRect.left >= bitmap.width || lowerFaceRect.top >= bitmap.height) {
                return 0.1f // Low probability if region is invalid
            }
            
            // Extract lower face region safely
            val lowerFaceBitmap = try {
                Bitmap.createBitmap(
                    bitmap,
                    lowerFaceRect.left,
                    lowerFaceRect.top,
                    minOf(lowerFaceRect.width(), bitmap.width - lowerFaceRect.left),
                    minOf(lowerFaceRect.height(), bitmap.height - lowerFaceRect.top)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create lower face bitmap for facial hair analysis", e)
                return 0.1f
            }
            
            // Analyze texture variance and color patterns for facial hair
            val textureScore = analyzeLowerFaceTexture(lowerFaceBitmap)
            val colorScore = analyzeLowerFaceColor(lowerFaceBitmap)
            
            // Clean up
            lowerFaceBitmap.recycle()
            
            // Combine scores with weighted average
            val combinedScore = (textureScore * 0.7f) + (colorScore * 0.3f)
            
            Log.d(TAG, "Facial hair analysis: texture=$textureScore, color=$colorScore, combined=$combinedScore")
            
            combinedScore.coerceIn(0.0f, 1.0f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced facial hair analysis", e)
            0.1f // Default low probability on error
        }
    }
    
    /**
     * Analyze texture variance in lower face region for facial hair detection
     */
    private fun analyzeLowerFaceTexture(lowerFaceBitmap: Bitmap): Float {
        try {
            val sampleStep = 3 // Sample every 3rd pixel for performance
            var totalVariance = 0f
            var sampleCount = 0
            
            // Sample pixels to calculate texture variance
            for (x in 0 until lowerFaceBitmap.width step sampleStep) {
                for (y in 0 until lowerFaceBitmap.height step sampleStep) {
                    val centerPixel = lowerFaceBitmap.getPixel(x, y)
                    
                    // Get neighboring pixels for local variance calculation
                    val neighbors = mutableListOf<Int>()
                    for (dx in -2..2 step 2) {
                        for (dy in -2..2 step 2) {
                            if (dx == 0 && dy == 0) continue // Skip center pixel
                            
                            val nx = x + dx
                            val ny = y + dy
                            
                            if (nx in 0 until lowerFaceBitmap.width && ny in 0 until lowerFaceBitmap.height) {
                                neighbors.add(lowerFaceBitmap.getPixel(nx, ny))
                            }
                        }
                    }
                    
                    if (neighbors.isNotEmpty()) {
                        val variance = calculatePixelVariance(centerPixel, neighbors)
                        totalVariance += variance
                        sampleCount++
                    }
                }
            }
            
            val avgVariance = if (sampleCount > 0) totalVariance / sampleCount else 0f
            
            // Higher variance indicates rougher texture (potential facial hair)
            return when {
                avgVariance > 2000f -> 0.8f // High texture variance (likely facial hair)
                avgVariance > 1000f -> 0.6f // Moderate texture variance
                avgVariance > 500f -> 0.4f  // Some texture variance
                else -> 0.1f                // Low texture variance (smooth skin)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing lower face texture", e)
            return 0.1f
        }
        
        return 0.1f // Default fallback
    }
    
    /**
     * Analyze color patterns in lower face region for facial hair detection
     */
    private fun analyzeLowerFaceColor(lowerFaceBitmap: Bitmap): Float {
        try {
            val sampleStep = 4 // Sample every 4th pixel for performance
            var darkPixelCount = 0
            var totalPixels = 0
            
            // Sample pixels to detect dark colors (facial hair is typically darker)
            for (x in 0 until lowerFaceBitmap.width step sampleStep) {
                for (y in 0 until lowerFaceBitmap.height step sampleStep) {
                    val pixel = lowerFaceBitmap.getPixel(x, y)
                    val brightness = calculatePixelBrightness(pixel)
                    
                    // Count dark pixels (facial hair is typically darker than skin)
                    if (brightness < 100) { // Dark threshold
                        darkPixelCount++
                    }
                    totalPixels++
                }
            }
            
            val darkRatio = if (totalPixels > 0) darkPixelCount.toFloat() / totalPixels else 0f
            
            // Higher dark pixel ratio indicates potential facial hair
            return when {
                darkRatio > 0.3f -> 0.7f // High dark pixel ratio (likely facial hair)
                darkRatio > 0.2f -> 0.5f // Moderate dark pixel ratio
                darkRatio > 0.1f -> 0.3f // Some dark pixels
                else -> 0.1f             // Few dark pixels (likely just skin)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing lower face color", e)
            return 0.1f
        }
        
        return 0.1f // Default fallback
    }
    
    /**
     * Calculate pixel brightness for color analysis
     */
    private fun calculatePixelBrightness(pixel: Int): Float {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return (red * 0.299f + green * 0.587f + blue * 0.114f)
    }
    
    /**
     * Calculate variance between center pixel and neighbors
     */
    private fun calculatePixelVariance(center: Int, neighbors: List<Int>): Float {
        val centerBrightness = calculatePixelBrightness(center)
        var variance = 0f
        
        neighbors.forEach { neighbor ->
            val neighborBrightness = calculatePixelBrightness(neighbor)
            val diff = centerBrightness - neighborBrightness
            variance += diff * diff
        }
        
        return variance / neighbors.size
    }
    
    private fun analyzeCheekbones(face: Face): Float {
        // Analyze cheekbone prominence
        return try {
            val boundingBox = face.boundingBox
            val faceWidth = boundingBox.width()
            val faceHeight = boundingBox.height()
            // Higher cheekbones often correlate with certain face proportions
            val ratio = faceHeight.toFloat() / faceWidth.toFloat()
            minOf(1.0f, maxOf(0.0f, (ratio - 1.0f) * 2.0f + 0.5f))
        } catch (e: Exception) {
            0.5f
        }
    }
    
    private fun calculateFeatureConfidence(
        jawlineSharpness: Float,
        eyebrowThickness: Float,
        facialHairPresence: Float
    ): Float {
        // Calculate overall confidence in facial feature analysis
        val features = listOf(jawlineSharpness, eyebrowThickness, facialHairPresence)
        val variance = features.map { (it - 0.5f) * (it - 0.5f) }.average()
        return minOf(1.0f, maxOf(0.3f, variance.toFloat() * 2.0f + 0.5f))
    }
    
    private fun determineRecommendedBlurAction(
        maleCount: Int,
        femaleCount: Int,
        unknownCount: Int,
        averageConfidence: Float
    ): BlurAction {
        return when {
            averageConfidence < 0.5f -> BlurAction.BLUR_ALL_SAFER
            unknownCount > (maleCount + femaleCount) -> BlurAction.BLUR_ALL_SAFER
            maleCount > 0 && femaleCount > 0 -> BlurAction.SELECTIVE_BLUR
            maleCount > 0 -> BlurAction.BLUR_MALES_ONLY
            femaleCount > 0 -> BlurAction.BLUR_FEMALES_ONLY
            else -> BlurAction.NO_BLUR
        }
    }
    
    /**
     * Clear the gender detection cache
     */
    fun clearCache() {
        genderCache.clear()
        Log.d(TAG, "Gender detection cache cleared")
    }
}

/**
 * Gender classification result
 */
data class GenderClassification(
    val gender: Gender,
    val confidence: Float
)

/**
 * Gender detection result for a single face
 */
data class GenderDetectionResult(
    val gender: Gender,
    val confidence: Float,
    val facialFeatures: FacialFeatureAnalysis,
    val processingTimeMs: Long
)

/**
 * Gender distribution analysis result for multiple faces
 */
data class GenderDistributionResult(
    val maleCount: Int,
    val femaleCount: Int,
    val unknownCount: Int,
    val averageConfidence: Float,
    val recommendedAction: BlurAction,
    val processingTimeMs: Long
)

/**
 * Facial feature analysis data
 */
data class FacialFeatureAnalysis(
    val jawlineSharpness: Float,      // 0.0 to 1.0, higher = more angular/masculine
    val eyebrowThickness: Float,      // 0.0 to 1.0, higher = thicker/more prominent
    val facialHairPresence: Float,    // 0.0 to 1.0, higher = more facial hair detected
    val cheekboneProminence: Float,   // 0.0 to 1.0, higher = more prominent cheekbones
    val faceAspectRatio: Float,       // width/height ratio of face
    val confidenceScore: Float        // Overall confidence in feature analysis
) {
    companion object {
        fun default() = FacialFeatureAnalysis(
            jawlineSharpness = 0.5f,
            eyebrowThickness = 0.5f,
            facialHairPresence = 0.0f,
            cheekboneProminence = 0.5f,
            faceAspectRatio = 0.8f,
            confidenceScore = 0.5f
        )
    }
}

/**
 * Gender enumeration
 */
enum class Gender {
    MALE, FEMALE, UNKNOWN
}

/**
 * Recommended blur actions based on gender analysis
 */
enum class BlurAction {
    NO_BLUR,
    BLUR_MALES_ONLY,
    BLUR_FEMALES_ONLY,
    SELECTIVE_BLUR,
    BLUR_ALL_SAFER
}
