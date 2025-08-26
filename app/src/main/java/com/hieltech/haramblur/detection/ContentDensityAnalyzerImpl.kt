package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.ml.MLModelManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ContentDensityAnalyzerImpl @Inject constructor(
    private val mlModelManager: MLModelManager
) : ContentDensityAnalyzer {
    
    companion object {
        private const val TAG = "ContentDensityAnalyzer"
        private const val DEFAULT_GRID_SIZE = 6 // 6x6 grid for detailed analysis
        private const val SKIN_TONE_THRESHOLD = 0.3f
        private const val HIGH_DENSITY_THRESHOLD = 0.6f
        private const val MEDIUM_DENSITY_THRESHOLD = 0.3f
        private const val FULL_SCREEN_THRESHOLD = 0.4f
        private const val PROCESSING_TIMEOUT_MS = 2000L
    }
    
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override suspend fun analyzeScreenContent(bitmap: Bitmap): DensityAnalysisResult = 
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            
            try {
                Log.d(TAG, "Starting screen content analysis for ${bitmap.width}x${bitmap.height} image")
                
                withTimeout(PROCESSING_TIMEOUT_MS) {
                    // Perform parallel analysis
                    val gridAnalysisJob = async { performGridAnalysis(bitmap) }
                    val spatialDistributionJob = async { analyzeSpatialDistribution(bitmap) }
                    val criticalRegionsJob = async { identifyCriticalRegions(bitmap) }
                    
                    // Wait for all analysis to complete
                    val gridAnalysis = gridAnalysisJob.await()
                    val spatialDistribution = spatialDistributionJob.await()
                    val criticalRegions = criticalRegionsJob.await()
                    
                    // Calculate overall density from grid analysis
                    val overallDensity = gridAnalysis.averageCellDensity
                    
                    // Calculate blur coverage
                    val screenSize = Size(bitmap.width, bitmap.height)
                    val blurCoverage = calculateBlurCoverage(criticalRegions, screenSize)
                    
                    // Determine warning level
                    val warningLevel = calculateWarningLevel(overallDensity, criticalRegions.size, spatialDistribution)
                    
                    // Calculate recommended action
                    val recommendedAction = determineRecommendedAction(
                        overallDensity, 
                        spatialDistribution, 
                        criticalRegions.size,
                        warningLevel
                    )
                    
                    val processingTime = System.currentTimeMillis() - startTime
                    
                    val result = DensityAnalysisResult(
                        inappropriateContentDensity = overallDensity,
                        spatialDistribution = spatialDistribution,
                        recommendedAction = recommendedAction,
                        warningLevel = warningLevel,
                        criticalRegions = criticalRegions,
                        blurCoveragePercentage = blurCoverage,
                        processingTimeMs = processingTime,
                        gridAnalysis = gridAnalysis
                    )
                    
                    Log.d(TAG, "Content analysis completed in ${processingTime}ms: density=$overallDensity, action=$recommendedAction")
                    return@withTimeout result
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Content analysis timed out")
                return@withContext createTimeoutResult(startTime)
            } catch (e: Exception) {
                Log.e(TAG, "Content analysis failed", e)
                return@withContext createErrorResult(startTime, e)
            }
        }
    
    override fun calculateBlurCoverage(regions: List<Rect>, screenSize: Size): Float {
        if (regions.isEmpty() || screenSize.width <= 0 || screenSize.height <= 0) {
            return 0.0f
        }
        
        val totalScreenArea = screenSize.width * screenSize.height
        
        // Calculate total area covered by blur regions (handling overlaps)
        val coveredArea = calculateUnionArea(regions, screenSize)
        
        val coverage = coveredArea.toFloat() / totalScreenArea
        
        Log.d(TAG, "Blur coverage calculated: ${(coverage * 100).toInt()}% (${regions.size} regions)")
        return coverage.coerceIn(0.0f, 1.0f)
    }
    
    override fun shouldTriggerFullScreenWarning(density: Float, settings: AppSettings): Boolean {
        val threshold = settings.contentDensityThreshold
        val shouldTrigger = density >= threshold && settings.fullScreenWarningEnabled
        
        Log.d(TAG, "Full-screen warning check: density=$density, threshold=$threshold, enabled=${settings.fullScreenWarningEnabled}, trigger=$shouldTrigger")
        return shouldTrigger
    }
    
    override suspend fun analyzeSpatialDistribution(bitmap: Bitmap): SpatialDistribution = 
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            
            // Define regions
            val halfWidth = width / 2
            val halfHeight = height / 2
            val quarterWidth = width / 4
            val quarterHeight = height / 4
            
            // Analyze each quadrant
            val topLeft = analyzeRegionDensity(bitmap, Rect(0, 0, halfWidth, halfHeight))
            val topRight = analyzeRegionDensity(bitmap, Rect(halfWidth, 0, width, halfHeight))
            val bottomLeft = analyzeRegionDensity(bitmap, Rect(0, halfHeight, halfWidth, height))
            val bottomRight = analyzeRegionDensity(bitmap, Rect(halfWidth, halfHeight, width, height))
            
            // Analyze center region
            val center = analyzeRegionDensity(bitmap, Rect(quarterWidth, quarterHeight, 3 * quarterWidth, 3 * quarterHeight))
            
            // Analyze edge regions (combined)
            val edgeRegions = listOf(
                Rect(0, 0, width, quarterHeight), // Top edge
                Rect(0, 3 * quarterHeight, width, height), // Bottom edge
                Rect(0, quarterHeight, quarterWidth, 3 * quarterHeight), // Left edge
                Rect(3 * quarterWidth, quarterHeight, width, 3 * quarterHeight) // Right edge
            )
            val edges = edgeRegions.map { analyzeRegionDensity(bitmap, it) }.average().toFloat()
            
            // Calculate statistics
            val densities = listOf(topLeft, topRight, bottomLeft, bottomRight, center)
            val maxDensity = densities.maxOrNull() ?: 0.0f
            val avgDensity = densities.average().toFloat()
            val variance = densities.map { (it - avgDensity) * (it - avgDensity) }.average().toFloat()
            
            return@withContext SpatialDistribution(
                topLeft = topLeft,
                topRight = topRight,
                bottomLeft = bottomLeft,
                bottomRight = bottomRight,
                center = center,
                edges = edges,
                maxQuadrantDensity = maxDensity,
                distributionVariance = sqrt(variance)
            )
        }
    
    override fun calculateRecommendedAction(analysisResult: DensityAnalysisResult, settings: AppSettings): ContentAction {
        val density = analysisResult.inappropriateContentDensity
        val warningLevel = analysisResult.warningLevel
        val spatialDistribution = analysisResult.spatialDistribution
        val criticalRegions = analysisResult.criticalRegions.size

        return when {
            // Critical level - immediate action required
            warningLevel == WarningLevel.CRITICAL || density > 0.8f -> {
                ContentAction.IMMEDIATE_CLOSE
            }

            // High level - full screen blur with warning
            warningLevel == WarningLevel.HIGH || density > settings.contentDensityThreshold -> {
                ContentAction.FULL_SCREEN_BLUR
            }

            // Medium level - check distribution
            warningLevel == WarningLevel.MEDIUM || density > 0.3f -> {
                if (spatialDistribution.isContentDistributed() || criticalRegions > 4) {
                    ContentAction.FULL_SCREEN_BLUR
                } else {
                    ContentAction.SELECTIVE_BLUR
                }
            }

            // Low level - selective blur
            warningLevel == WarningLevel.LOW || density > 0.1f -> {
                ContentAction.SELECTIVE_BLUR
            }

            // Minimal or no inappropriate content
            else -> {
                ContentAction.NO_ACTION
            }
        }
    }

    override suspend fun analyzeRegionSpatialDistribution(nsfwRegionRects: List<Rect>, bitmap: Bitmap): RegionSpatialDistribution = withContext(Dispatchers.Default) {
        if (nsfwRegionRects.isEmpty()) {
            return@withContext RegionSpatialDistribution(
                totalRegions = 0,
                clusteredRegions = 0,
                distributedRegions = 0,
                centerWeightedRegions = 0,
                edgeWeightedRegions = 0,
                clusteringScore = 0.0f,
                coveragePercentage = 0.0f,
                dominantLocation = "none"
            )
        }

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate region centroids
        val centroids = nsfwRegionRects.map { rect ->
            Pair(
                rect.centerX().toFloat() / bitmapWidth,
                rect.centerY().toFloat() / bitmapHeight
            )
        }

        // Analyze clustering
        val clusteringScore = calculateClusteringScore(centroids)
        val totalRegions = nsfwRegionRects.size

        // Determine region distribution
        var centerWeightedRegions = 0
        var edgeWeightedRegions = 0

        centroids.forEach { (x, y) ->
            if (x > 0.25f && x < 0.75f && y > 0.25f && y < 0.75f) {
                centerWeightedRegions++
            } else {
                edgeWeightedRegions++
            }
        }

        // Calculate coverage percentage
        val totalCoverage = nsfwRegionRects.sumOf { it.width() * it.height() }.toFloat()
        val screenArea = bitmapWidth * bitmapHeight
        val coveragePercentage = totalCoverage / screenArea

        // Determine dominant location
        val dominantLocation = when {
            centerWeightedRegions > edgeWeightedRegions -> "center"
            edgeWeightedRegions > centerWeightedRegions -> "edges"
            else -> "distributed"
        }

        return@withContext RegionSpatialDistribution(
            totalRegions = totalRegions,
            clusteredRegions = if (clusteringScore > 0.7f) totalRegions else 0,
            distributedRegions = if (clusteringScore < 0.3f) totalRegions else 0,
            centerWeightedRegions = centerWeightedRegions,
            edgeWeightedRegions = edgeWeightedRegions,
            clusteringScore = clusteringScore,
            coveragePercentage = coveragePercentage,
            dominantLocation = dominantLocation
        )
    }

    override fun shouldTriggerByRegionCount(
        nsfwRegionCount: Int,
        maxNsfwConfidence: Float,
        settings: AppSettings
    ): Boolean {
        return settings.enableRegionBasedFullScreen &&
               nsfwRegionCount >= settings.nsfwFullScreenRegionThreshold &&
               maxNsfwConfidence >= settings.nsfwHighConfidenceThreshold
    }

    override fun mergeOverlappingRegions(
        regionRects: List<Rect>,
        regionConfidences: List<Float>
    ): List<Pair<Rect, Float>> {
        if (regionRects.isEmpty()) return emptyList()

        val merged = mutableListOf<Pair<Rect, Float>>()

        regionRects.forEachIndexed { index, region ->
            val confidence = regionConfidences[index]

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

    override fun calculateRegionCoverage(regionRects: List<Rect>, screenSize: Size): Float {
        if (regionRects.isEmpty() || screenSize.width <= 0 || screenSize.height <= 0) {
            return 0.0f
        }

        val totalScreenArea = screenSize.width * screenSize.height.toFloat()
        val totalRegionArea = regionRects.sumOf { it.width() * it.height() }.toFloat()

        return (totalRegionArea / totalScreenArea).coerceIn(0.0f, 1.0f)
    }

    // Helper methods for region analysis

    private fun calculateClusteringScore(centroids: List<Pair<Float, Float>>): Float {
        if (centroids.size < 2) return 0.0f

        // Calculate centroid of all points
        val centerX = centroids.map { it.first }.average().toFloat()
        val centerY = centroids.map { it.second }.average().toFloat()

        // Calculate average distance from center
        val avgDistance = centroids.map { (x, y) ->
            val dx = x - centerX
            val dy = y - centerY
            sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        }.average().toFloat()

        // Lower average distance = more clustered
        return (1.0f - avgDistance).coerceIn(0.0f, 1.0f)
    }

    private fun calculateOverlapRatio(rect1: Rect, rect2: Rect): Float {
        if (!rect1.intersect(rect2)) return 0.0f

        val intersectionArea = (rect1.width() * rect1.height()).toFloat()
        val smallerArea = minOf(
            rect1.width() * rect1.height(),
            rect2.width() * rect2.height()
        ).toFloat()

        return if (smallerArea > 0) intersectionArea / smallerArea else 0.0f
    }
    
    // Private helper methods
    
    private suspend fun performGridAnalysis(bitmap: Bitmap): GridAnalysis = withContext(Dispatchers.Default) {
        val gridSize = DEFAULT_GRID_SIZE
        val cellWidth = bitmap.width / gridSize
        val cellHeight = bitmap.height / gridSize
        
        val cellDensities = Array(gridSize) { Array(gridSize) { 0.0f } }
        var highDensityCells = 0
        var mediumDensityCells = 0
        var lowDensityCells = 0
        var totalDensity = 0.0f
        var maxDensity = 0.0f
        var minDensity = 1.0f
        
        // Analyze each grid cell
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = col * cellWidth
                val y = row * cellHeight
                val cellRect = Rect(x, y, 
                    minOf(x + cellWidth, bitmap.width), 
                    minOf(y + cellHeight, bitmap.height)
                )
                
                val cellDensity = analyzeRegionDensity(bitmap, cellRect)
                cellDensities[row][col] = cellDensity
                totalDensity += cellDensity
                
                maxDensity = maxOf(maxDensity, cellDensity)
                minDensity = minOf(minDensity, cellDensity)
                
                // Categorize cell density
                when {
                    cellDensity > HIGH_DENSITY_THRESHOLD -> highDensityCells++
                    cellDensity > MEDIUM_DENSITY_THRESHOLD -> mediumDensityCells++
                    else -> lowDensityCells++
                }
            }
        }
        
        val averageDensity = totalDensity / (gridSize * gridSize)
        
        return@withContext GridAnalysis(
            gridSize = gridSize,
            cellDensities = cellDensities,
            highDensityCells = highDensityCells,
            mediumDensityCells = mediumDensityCells,
            lowDensityCells = lowDensityCells,
            averageCellDensity = averageDensity,
            maxCellDensity = maxDensity,
            minCellDensity = minDensity
        )
    }
    
    private suspend fun identifyCriticalRegions(bitmap: Bitmap): List<Rect> = withContext(Dispatchers.Default) {
        val criticalRegions = mutableListOf<Rect>()
        val gridSize = DEFAULT_GRID_SIZE
        val cellWidth = bitmap.width / gridSize
        val cellHeight = bitmap.height / gridSize
        
        // Scan grid for high-density regions
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = col * cellWidth
                val y = row * cellHeight
                val cellRect = Rect(x, y, 
                    minOf(x + cellWidth, bitmap.width), 
                    minOf(y + cellHeight, bitmap.height)
                )
                
                val cellDensity = analyzeRegionDensity(bitmap, cellRect)
                
                if (cellDensity > HIGH_DENSITY_THRESHOLD) {
                    // Expand region slightly for better coverage
                    val expandedRect = expandRect(cellRect, 20, bitmap.width, bitmap.height)
                    criticalRegions.add(expandedRect)
                }
            }
        }
        
        // Merge overlapping regions
        return@withContext mergeOverlappingRegions(criticalRegions)
    }
    
    private fun analyzeRegionDensity(bitmap: Bitmap, region: Rect): Float {
        if (region.isEmpty || region.left >= bitmap.width || region.top >= bitmap.height) {
            return 0.0f
        }
        
        // Ensure region is within bitmap bounds
        val clampedRegion = Rect(
            maxOf(0, region.left),
            maxOf(0, region.top),
            minOf(bitmap.width, region.right),
            minOf(bitmap.height, region.bottom)
        )
        
        if (clampedRegion.isEmpty) return 0.0f
        
        // Sample pixels in the region
        val sampleStep = 3 // Sample every 3rd pixel for performance
        var skinPixels = 0
        var totalPixels = 0
        var colorVarianceSum = 0.0f
        val colors = mutableListOf<Int>()
        
        for (x in clampedRegion.left until clampedRegion.right step sampleStep) {
            for (y in clampedRegion.top until clampedRegion.bottom step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                colors.add(pixel)
                
                if (isSkinTonePixel(pixel)) {
                    skinPixels++
                }
                totalPixels++
            }
        }
        
        if (totalPixels == 0) return 0.0f
        
        val skinRatio = skinPixels.toFloat() / totalPixels
        
        // Calculate color variance (low variance might indicate smooth skin areas)
        val colorVariance = calculateColorVariance(colors)
        
        // Combine metrics for density score
        var densityScore = 0.0f
        
        // High skin tone ratio increases density
        if (skinRatio > SKIN_TONE_THRESHOLD) {
            densityScore += skinRatio * 0.7f
        }
        
        // Low color variance with high skin ratio suggests inappropriate content
        if (colorVariance < 0.3f && skinRatio > 0.2f) {
            densityScore += 0.3f
        }
        
        // Additional heuristics could be added here (edge detection, texture analysis, etc.)
        
        return densityScore.coerceIn(0.0f, 1.0f)
    }
    
    private fun isSkinTonePixel(pixel: Int): Boolean {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Enhanced skin tone detection
        return when {
            // Light skin tones
            red in 180..255 && green in 120..200 && blue in 90..170 && red > green && green > blue -> true
            // Medium skin tones
            red in 140..200 && green in 100..160 && blue in 70..130 && red > green && green > blue -> true
            // Darker skin tones
            red in 100..160 && green in 70..120 && blue in 50..100 && red > green && green > blue -> true
            else -> false
        }
    }
    
    private fun calculateColorVariance(colors: List<Int>): Float {
        if (colors.size < 2) return 0.0f
        
        val avgRed = colors.map { (it shr 16) and 0xFF }.average()
        val avgGreen = colors.map { (it shr 8) and 0xFF }.average()
        val avgBlue = colors.map { it and 0xFF }.average()
        
        val variance = colors.map { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            
            val redDiff = red - avgRed
            val greenDiff = green - avgGreen
            val blueDiff = blue - avgBlue
            
            redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff
        }.average()
        
        return (variance / 65536.0).toFloat() // Normalize to 0-1 range
    }
    
    private fun calculateUnionArea(regions: List<Rect>, screenSize: Size): Int {
        if (regions.isEmpty()) return 0
        
        // Simple approach: create a boolean grid to track covered pixels
        // For large screens, we'll use a scaled-down grid for performance
        val scaleFactor = 4 // Scale down by factor of 4
        val gridWidth = (screenSize.width + scaleFactor - 1) / scaleFactor
        val gridHeight = (screenSize.height + scaleFactor - 1) / scaleFactor
        
        val covered = Array(gridHeight) { BooleanArray(gridWidth) }
        
        // Mark covered areas
        regions.forEach { rect ->
            val scaledLeft = rect.left / scaleFactor
            val scaledTop = rect.top / scaleFactor
            val scaledRight = minOf(gridWidth, (rect.right + scaleFactor - 1) / scaleFactor)
            val scaledBottom = minOf(gridHeight, (rect.bottom + scaleFactor - 1) / scaleFactor)
            
            for (y in scaledTop until scaledBottom) {
                for (x in scaledLeft until scaledRight) {
                    if (y in 0 until gridHeight && x in 0 until gridWidth) {
                        covered[y][x] = true
                    }
                }
            }
        }
        
        // Count covered pixels and scale back up
        var coveredPixels = 0
        covered.forEach { row ->
            row.forEach { isCovered ->
                if (isCovered) coveredPixels++
            }
        }
        
        return coveredPixels * scaleFactor * scaleFactor
    }
    
    private fun expandRect(rect: Rect, expansion: Int, maxWidth: Int, maxHeight: Int): Rect {
        return Rect(
            maxOf(0, rect.left - expansion),
            maxOf(0, rect.top - expansion),
            minOf(maxWidth, rect.right + expansion),
            minOf(maxHeight, rect.bottom + expansion)
        )
    }
    
    private fun mergeOverlappingRegions(regions: List<Rect>): List<Rect> {
        if (regions.size <= 1) return regions
        
        val merged = mutableListOf<Rect>()
        val sorted = regions.sortedBy { it.left }
        
        var current = sorted[0]
        
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            
            if (Rect.intersects(current, next)) {
                // Merge overlapping rectangles
                current = Rect(
                    minOf(current.left, next.left),
                    minOf(current.top, next.top),
                    maxOf(current.right, next.right),
                    maxOf(current.bottom, next.bottom)
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        
        merged.add(current)
        return merged
    }
    
    private fun calculateWarningLevel(
        density: Float, 
        criticalRegions: Int, 
        spatialDistribution: SpatialDistribution
    ): WarningLevel {
        return when {
            density > 0.8f || criticalRegions > 15 || spatialDistribution.maxQuadrantDensity > 0.9f -> {
                WarningLevel.CRITICAL
            }
            density > 0.6f || criticalRegions > 10 || spatialDistribution.maxQuadrantDensity > 0.7f -> {
                WarningLevel.HIGH
            }
            density > FULL_SCREEN_THRESHOLD || criticalRegions > 6 || spatialDistribution.maxQuadrantDensity > 0.5f -> {
                WarningLevel.MEDIUM
            }
            density > 0.2f || criticalRegions > 2 || spatialDistribution.maxQuadrantDensity > 0.3f -> {
                WarningLevel.LOW
            }
            density > 0.05f || criticalRegions > 0 -> {
                WarningLevel.MINIMAL
            }
            else -> {
                WarningLevel.NONE
            }
        }
    }
    
    private fun determineRecommendedAction(
        density: Float,
        spatialDistribution: SpatialDistribution,
        criticalRegions: Int,
        warningLevel: WarningLevel
    ): ContentAction {
        return when (warningLevel) {
            WarningLevel.CRITICAL -> ContentAction.IMMEDIATE_CLOSE
            WarningLevel.HIGH -> ContentAction.FULL_SCREEN_BLUR
            WarningLevel.MEDIUM -> {
                if (spatialDistribution.isContentDistributed() || criticalRegions > 4) {
                    ContentAction.FULL_SCREEN_BLUR
                } else {
                    ContentAction.SELECTIVE_BLUR
                }
            }
            WarningLevel.LOW -> ContentAction.SELECTIVE_BLUR
            WarningLevel.MINIMAL -> ContentAction.SELECTIVE_BLUR
            WarningLevel.NONE -> ContentAction.NO_ACTION
        }
    }
    
    private fun createTimeoutResult(startTime: Long): DensityAnalysisResult {
        val processingTime = System.currentTimeMillis() - startTime
        return DensityAnalysisResult(
            inappropriateContentDensity = 0.0f,
            spatialDistribution = createEmptySpatialDistribution(),
            recommendedAction = ContentAction.NO_ACTION,
            warningLevel = WarningLevel.NONE,
            criticalRegions = emptyList(),
            blurCoveragePercentage = 0.0f,
            processingTimeMs = processingTime,
            gridAnalysis = createEmptyGridAnalysis()
        )
    }
    
    private fun createErrorResult(startTime: Long, error: Exception): DensityAnalysisResult {
        Log.e(TAG, "Creating error result for density analysis", error)
        val processingTime = System.currentTimeMillis() - startTime
        return DensityAnalysisResult(
            inappropriateContentDensity = 0.0f,
            spatialDistribution = createEmptySpatialDistribution(),
            recommendedAction = ContentAction.NO_ACTION,
            warningLevel = WarningLevel.NONE,
            criticalRegions = emptyList(),
            blurCoveragePercentage = 0.0f,
            processingTimeMs = processingTime,
            gridAnalysis = createEmptyGridAnalysis()
        )
    }
    
    private fun createEmptySpatialDistribution(): SpatialDistribution {
        return SpatialDistribution(
            topLeft = 0.0f,
            topRight = 0.0f,
            bottomLeft = 0.0f,
            bottomRight = 0.0f,
            center = 0.0f,
            edges = 0.0f,
            maxQuadrantDensity = 0.0f,
            distributionVariance = 0.0f
        )
    }
    
    private fun createEmptyGridAnalysis(): GridAnalysis {
        return GridAnalysis(
            gridSize = DEFAULT_GRID_SIZE,
            cellDensities = Array(DEFAULT_GRID_SIZE) { Array(DEFAULT_GRID_SIZE) { 0.0f } },
            highDensityCells = 0,
            mediumDensityCells = 0,
            lowDensityCells = DEFAULT_GRID_SIZE * DEFAULT_GRID_SIZE,
            averageCellDensity = 0.0f,
            maxCellDensity = 0.0f,
            minCellDensity = 0.0f
        )
    }
}