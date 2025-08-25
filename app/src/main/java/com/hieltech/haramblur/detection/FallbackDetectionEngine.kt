package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback detection engine that provides basic content detection
 * when ML models fail to load or are unavailable
 */
@Singleton
class FallbackDetectionEngine @Inject constructor() {
    
    companion object {
        private const val TAG = "FallbackDetectionEngine"
        
        // Heuristic thresholds
        private const val SKIN_COLOR_THRESHOLD = 0.3f
        private const val FLESH_TONE_VARIANCE_THRESHOLD = 0.2f
        private const val SUSPICIOUS_REGION_SIZE_THRESHOLD = 0.1f // 10% of image
        private const val EDGE_DENSITY_THRESHOLD = 0.4f
        
        // Color ranges for skin detection (simplified)
        private val SKIN_COLOR_RANGES = listOf(
            ColorRange(100, 150, 80, 120, 60, 100), // Light skin
            ColorRange(80, 130, 60, 100, 40, 80),   // Medium skin
            ColorRange(60, 110, 40, 80, 20, 60)     // Dark skin
        )
    }
    
    /**
     * Perform heuristic-based NSFW detection
     */
    fun detectNSFWHeuristic(bitmap: Bitmap): HeuristicDetectionResult {
        return try {
            Log.d(TAG, "Performing heuristic NSFW detection on ${bitmap.width}x${bitmap.height} image")
            
            val startTime = System.currentTimeMillis()
            
            // Analyze image characteristics
            val skinPixelRatio = analyzeSkinPixels(bitmap)
            val edgeDensity = analyzeEdgeDensity(bitmap)
            val colorVariance = analyzeColorVariance(bitmap)
            val suspiciousRegions = findSuspiciousRegions(bitmap)
            
            // Calculate confidence based on heuristics
            val confidence = calculateNSFWConfidence(
                skinPixelRatio,
                edgeDensity,
                colorVariance,
                suspiciousRegions.size
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            
            val result = HeuristicDetectionResult(
                isNSFW = confidence > 0.5f,
                confidence = confidence,
                skinPixelRatio = skinPixelRatio,
                edgeDensity = edgeDensity,
                colorVariance = colorVariance,
                suspiciousRegions = suspiciousRegions,
                processingTimeMs = processingTime,
                method = "heuristic_nsfw"
            )
            
            Log.d(TAG, "Heuristic NSFW detection completed: confidence=$confidence, isNSFW=${result.isNSFW}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Heuristic NSFW detection failed", e)
            HeuristicDetectionResult.failed("Heuristic detection failed: ${e.message}")
        }
    }
    
    /**
     * Perform basic face detection using simple heuristics
     */
    fun detectFacesHeuristic(bitmap: Bitmap): HeuristicFaceResult {
        return try {
            Log.d(TAG, "Performing heuristic face detection")
            
            val startTime = System.currentTimeMillis()
            
            // Look for face-like regions using simple heuristics
            val faceRegions = findFaceRegions(bitmap)
            val skinRegions = findSkinRegions(bitmap)
            
            // Combine and filter regions
            val candidateFaces = combineFaceAndSkinRegions(faceRegions, skinRegions)
            val filteredFaces = filterFaceRegions(candidateFaces, bitmap)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            val result = HeuristicFaceResult(
                faceCount = filteredFaces.size,
                faceRegions = filteredFaces,
                confidence = if (filteredFaces.isNotEmpty()) 0.6f else 0.1f,
                processingTimeMs = processingTime,
                method = "heuristic_face"
            )
            
            Log.d(TAG, "Heuristic face detection completed: ${filteredFaces.size} faces found")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Heuristic face detection failed", e)
            HeuristicFaceResult.failed("Heuristic face detection failed: ${e.message}")
        }
    }
    
    /**
     * Perform conservative content blocking (block more to be safe)
     */
    fun performConservativeBlocking(bitmap: Bitmap): ConservativeBlockingResult {
        return try {
            Log.d(TAG, "Performing conservative content blocking")
            
            val startTime = System.currentTimeMillis()
            
            // Use very conservative thresholds
            val skinAnalysis = analyzeSkinPixels(bitmap)
            val suspiciousRegions = findSuspiciousRegions(bitmap)
            
            // Block if any suspicious indicators are found
            val shouldBlock = skinAnalysis > 0.2f || // Lower threshold
                             suspiciousRegions.isNotEmpty() ||
                             hasHighContrastRegions(bitmap)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            val result = ConservativeBlockingResult(
                shouldBlock = shouldBlock,
                confidence = if (shouldBlock) 0.8f else 0.2f,
                blockingReason = determineBlockingReason(skinAnalysis, suspiciousRegions.size),
                affectedRegions = if (shouldBlock) listOf(Rect(0, 0, bitmap.width, bitmap.height)) else emptyList(),
                processingTimeMs = processingTime
            )
            
            Log.d(TAG, "Conservative blocking completed: shouldBlock=$shouldBlock")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Conservative blocking failed", e)
            ConservativeBlockingResult.failed("Conservative blocking failed: ${e.message}")
        }
    }
    
    /**
     * Simple density analysis fallback
     */
    fun analyzeContentDensitySimple(bitmap: Bitmap): SimpleDensityResult {
        return try {
            Log.d(TAG, "Performing simple density analysis")
            
            val startTime = System.currentTimeMillis()
            
            // Divide image into grid and analyze each section
            val gridSize = 4 // 4x4 grid
            val sectionWidth = bitmap.width / gridSize
            val sectionHeight = bitmap.height / gridSize
            
            var suspiciousSections = 0
            val suspiciousRegions = mutableListOf<Rect>()
            
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val left = col * sectionWidth
                    val top = row * sectionHeight
                    val right = minOf(left + sectionWidth, bitmap.width)
                    val bottom = minOf(top + sectionHeight, bitmap.height)
                    
                    val section = Rect(left, top, right, bottom)
                    val sectionBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                    
                    val skinRatio = analyzeSkinPixels(sectionBitmap)
                    if (skinRatio > SKIN_COLOR_THRESHOLD) {
                        suspiciousSections++
                        suspiciousRegions.add(section)
                    }
                    
                    sectionBitmap.recycle()
                }
            }
            
            val densityRatio = suspiciousSections.toFloat() / (gridSize * gridSize)
            val processingTime = System.currentTimeMillis() - startTime
            
            val result = SimpleDensityResult(
                inappropriateContentDensity = densityRatio,
                suspiciousRegions = suspiciousRegions,
                recommendsFullScreenBlur = densityRatio > 0.4f,
                gridAnalysis = SimpleGridAnalysis(gridSize, suspiciousSections),
                processingTimeMs = processingTime
            )
            
            Log.d(TAG, "Simple density analysis completed: density=$densityRatio")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Simple density analysis failed", e)
            SimpleDensityResult.failed("Simple density analysis failed: ${e.message}")
        }
    }
    
    private fun analyzeSkinPixels(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        var skinPixels = 0
        
        // Sample pixels (not every pixel for performance)
        val sampleRate = maxOf(1, totalPixels / 10000) // Sample ~10k pixels max
        
        for (y in 0 until height step sampleRate) {
            for (x in 0 until width step sampleRate) {
                val pixel = bitmap.getPixel(x, y)
                if (isSkinColor(pixel)) {
                    skinPixels++
                }
            }
        }
        
        val sampledPixels = (width / sampleRate) * (height / sampleRate)
        return if (sampledPixels > 0) skinPixels.toFloat() / sampledPixels else 0f
    }
    
    private fun isSkinColor(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        
        return SKIN_COLOR_RANGES.any { range ->
            r in range.rMin..range.rMax &&
            g in range.gMin..range.gMax &&
            b in range.bMin..range.bMax
        }
    }
    
    private fun analyzeEdgeDensity(bitmap: Bitmap): Float {
        // Simplified edge detection using color differences
        val width = bitmap.width
        val height = bitmap.height
        var edgePixels = 0
        val sampleRate = maxOf(1, (width * height) / 5000)
        
        for (y in 1 until height - 1 step sampleRate) {
            for (x in 1 until width - 1 step sampleRate) {
                val center = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + 1, y)
                val bottom = bitmap.getPixel(x, y + 1)
                
                if (colorDifference(center, right) > 50 || colorDifference(center, bottom) > 50) {
                    edgePixels++
                }
            }
        }
        
        val sampledPixels = ((width - 2) / sampleRate) * ((height - 2) / sampleRate)
        return if (sampledPixels > 0) edgePixels.toFloat() / sampledPixels else 0f
    }
    
    private fun colorDifference(color1: Int, color2: Int): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        return kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
    }
    
    private fun analyzeColorVariance(bitmap: Bitmap): Float {
        // Simplified color variance calculation
        val colors = mutableListOf<Int>()
        val sampleRate = maxOf(1, (bitmap.width * bitmap.height) / 1000)
        
        for (y in 0 until bitmap.height step sampleRate) {
            for (x in 0 until bitmap.width step sampleRate) {
                colors.add(bitmap.getPixel(x, y))
            }
        }
        
        if (colors.size < 2) return 0f
        
        // Calculate variance in RGB values
        val avgR = colors.map { (it shr 16) and 0xFF }.average()
        val avgG = colors.map { (it shr 8) and 0xFF }.average()
        val avgB = colors.map { it and 0xFF }.average()
        
        val variance = colors.map { color ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            val rDiff = (r - avgR) * (r - avgR)
            val gDiff = (g - avgG) * (g - avgG)
            val bDiff = (b - avgB) * (b - avgB)
            
            (rDiff + gDiff + bDiff) / 3.0
        }.average()
        
        return (variance / (255 * 255)).toFloat() // Normalize to 0-1
    }
    
    private fun findSuspiciousRegions(bitmap: Bitmap): List<Rect> {
        val regions = mutableListOf<Rect>()
        val gridSize = 8
        val sectionWidth = bitmap.width / gridSize
        val sectionHeight = bitmap.height / gridSize
        
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val left = col * sectionWidth
                val top = row * sectionHeight
                val right = minOf(left + sectionWidth, bitmap.width)
                val bottom = minOf(top + sectionHeight, bitmap.height)
                
                val section = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                val skinRatio = analyzeSkinPixels(section)
                
                if (skinRatio > SUSPICIOUS_REGION_SIZE_THRESHOLD) {
                    regions.add(Rect(left, top, right, bottom))
                }
                
                section.recycle()
            }
        }
        
        return regions
    }
    
    private fun calculateNSFWConfidence(
        skinRatio: Float,
        edgeDensity: Float,
        colorVariance: Float,
        suspiciousRegionCount: Int
    ): Float {
        // Weighted combination of heuristics
        val skinWeight = 0.4f
        val edgeWeight = 0.2f
        val varianceWeight = 0.2f
        val regionWeight = 0.2f
        
        val skinScore = minOf(skinRatio * 2f, 1f) // Amplify skin ratio
        val edgeScore = minOf(edgeDensity * 1.5f, 1f)
        val varianceScore = minOf(colorVariance * 3f, 1f)
        val regionScore = minOf(suspiciousRegionCount / 5f, 1f)
        
        return (skinScore * skinWeight +
                edgeScore * edgeWeight +
                varianceScore * varianceWeight +
                regionScore * regionWeight).coerceIn(0f, 1f)
    }
    
    private fun findFaceRegions(bitmap: Bitmap): List<Rect> {
        // Very basic face region detection using skin color clustering
        val regions = mutableListOf<Rect>()
        val gridSize = 6
        val sectionWidth = bitmap.width / gridSize
        val sectionHeight = bitmap.height / gridSize
        
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val left = col * sectionWidth
                val top = row * sectionHeight
                val right = minOf(left + sectionWidth, bitmap.width)
                val bottom = minOf(top + sectionHeight, bitmap.height)
                
                val section = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                val skinRatio = analyzeSkinPixels(section)
                
                // Face regions typically have moderate skin ratios (not too high, not too low)
                if (skinRatio in 0.3f..0.7f) {
                    regions.add(Rect(left, top, right, bottom))
                }
                
                section.recycle()
            }
        }
        
        return regions
    }
    
    private fun findSkinRegions(bitmap: Bitmap): List<Rect> {
        return findSuspiciousRegions(bitmap) // Reuse suspicious region logic
    }
    
    private fun combineFaceAndSkinRegions(faceRegions: List<Rect>, skinRegions: List<Rect>): List<Rect> {
        val combined = mutableListOf<Rect>()
        
        // Find overlapping regions
        for (faceRegion in faceRegions) {
            for (skinRegion in skinRegions) {
                if (Rect.intersects(faceRegion, skinRegion)) {
                    // Merge overlapping regions
                    val merged = Rect(
                        minOf(faceRegion.left, skinRegion.left),
                        minOf(faceRegion.top, skinRegion.top),
                        maxOf(faceRegion.right, skinRegion.right),
                        maxOf(faceRegion.bottom, skinRegion.bottom)
                    )
                    combined.add(merged)
                }
            }
        }
        
        return combined.distinctBy { "${it.left},${it.top},${it.right},${it.bottom}" }
    }
    
    private fun filterFaceRegions(regions: List<Rect>, bitmap: Bitmap): List<Rect> {
        return regions.filter { region ->
            val width = region.width()
            val height = region.height()
            
            // Filter by aspect ratio (faces are roughly square to rectangular)
            val aspectRatio = width.toFloat() / height
            val isReasonableAspectRatio = aspectRatio in 0.5f..2.0f
            
            // Filter by size (not too small, not too large)
            val area = width * height
            val imageArea = bitmap.width * bitmap.height
            val areaRatio = area.toFloat() / imageArea
            val isReasonableSize = areaRatio in 0.01f..0.3f
            
            isReasonableAspectRatio && isReasonableSize
        }
    }
    
    private fun hasHighContrastRegions(bitmap: Bitmap): Boolean {
        val edgeDensity = analyzeEdgeDensity(bitmap)
        return edgeDensity > EDGE_DENSITY_THRESHOLD
    }
    
    private fun determineBlockingReason(skinRatio: Float, suspiciousRegionCount: Int): String {
        return when {
            skinRatio > 0.4f -> "High skin content detected"
            suspiciousRegionCount > 3 -> "Multiple suspicious regions found"
            else -> "Conservative blocking applied"
        }
    }
}

/**
 * Color range for skin detection
 */
private data class ColorRange(
    val rMin: Int, val rMax: Int,
    val gMin: Int, val gMax: Int,
    val bMin: Int, val bMax: Int
)

/**
 * Heuristic NSFW detection result
 */
data class HeuristicDetectionResult(
    val isNSFW: Boolean,
    val confidence: Float,
    val skinPixelRatio: Float,
    val edgeDensity: Float,
    val colorVariance: Float,
    val suspiciousRegions: List<Rect>,
    val processingTimeMs: Long,
    val method: String,
    val error: String? = null
) {
    companion object {
        fun failed(error: String) = HeuristicDetectionResult(
            isNSFW = false,
            confidence = 0f,
            skinPixelRatio = 0f,
            edgeDensity = 0f,
            colorVariance = 0f,
            suspiciousRegions = emptyList(),
            processingTimeMs = 0L,
            method = "failed",
            error = error
        )
    }
}

/**
 * Heuristic face detection result
 */
data class HeuristicFaceResult(
    val faceCount: Int,
    val faceRegions: List<Rect>,
    val confidence: Float,
    val processingTimeMs: Long,
    val method: String,
    val error: String? = null
) {
    companion object {
        fun failed(error: String) = HeuristicFaceResult(
            faceCount = 0,
            faceRegions = emptyList(),
            confidence = 0f,
            processingTimeMs = 0L,
            method = "failed",
            error = error
        )
    }
}

/**
 * Conservative blocking result
 */
data class ConservativeBlockingResult(
    val shouldBlock: Boolean,
    val confidence: Float,
    val blockingReason: String,
    val affectedRegions: List<Rect>,
    val processingTimeMs: Long,
    val error: String? = null
) {
    companion object {
        fun failed(error: String) = ConservativeBlockingResult(
            shouldBlock = true, // Fail safe - block when in doubt
            confidence = 1f,
            blockingReason = "Error occurred, blocking for safety",
            affectedRegions = emptyList(),
            processingTimeMs = 0L,
            error = error
        )
    }
}

/**
 * Simple density analysis result
 */
data class SimpleDensityResult(
    val inappropriateContentDensity: Float,
    val suspiciousRegions: List<Rect>,
    val recommendsFullScreenBlur: Boolean,
    val gridAnalysis: SimpleGridAnalysis,
    val processingTimeMs: Long,
    val error: String? = null
) {
    companion object {
        fun failed(error: String) = SimpleDensityResult(
            inappropriateContentDensity = 1f, // Assume high density on error
            suspiciousRegions = emptyList(),
            recommendsFullScreenBlur = true,
            gridAnalysis = SimpleGridAnalysis(0, 0),
            processingTimeMs = 0L,
            error = error
        )
    }
}

/**
 * Simple grid analysis information for fallback detection
 */
data class SimpleGridAnalysis(
    val gridSize: Int,
    val suspiciousSections: Int
) {
    val suspiciousRatio: Float
        get() = if (gridSize > 0) suspiciousSections.toFloat() / (gridSize * gridSize) else 0f
}