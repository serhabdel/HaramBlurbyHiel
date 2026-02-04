package com.hieltech.haramblur.performance

import android.graphics.Bitmap
import android.graphics.Rect
import java.security.MessageDigest

/**
 * Divides screen captures into a grid and detects changed regions.
 * Only re-analyzes changed regions to save processing time.
 */
class GridAnalyzer(
    private val gridRows: Int = 4,
    private val gridCols: Int = 4
) {
    private var previousGrid: Array<Array<CellData>>? = null
    
    data class CellData(
        val hash: String,
        val rect: Rect,
        val confidence: Float
    )
    
    data class Region(
        val rect: Rect,
        val changeScore: Float,
        val priority: Priority
    )
    
    enum class Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Analyze bitmap and return only changed regions.
     * On first call, returns full screen.
     */
    fun getChangedRegions(bitmap: Bitmap): List<Region> {
        val currentGrid = createGrid(bitmap)
        val previous = previousGrid
        
        previousGrid = currentGrid
        
        if (previous == null) {
            // First frame - analyze full screen
            return listOf(Region(
                rect = Rect(0, 0, bitmap.width, bitmap.height),
                changeScore = 1.0f,
                priority = Priority.CRITICAL
            ))
        }
        
        val changedCells = mutableListOf<CellData>()
        
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val current = currentGrid[row][col]
                val prev = previous[row][col]
                
                if (current.hash != prev.hash) {
                    changedCells.add(current)
                }
            }
        }
        
        return mergeAndPrioritize(changedCells, bitmap.width, bitmap.height)
    }
    
    /**
     * Clear cached grid state (e.g., after screen off)
     */
    fun reset() {
        previousGrid = null
    }
    
    private fun createGrid(bitmap: Bitmap): Array<Array<CellData>> {
        val cellWidth = bitmap.width / gridCols
        val cellHeight = bitmap.height / gridRows
        
        return Array(gridRows) { row ->
            Array(gridCols) { col ->
                val left = col * cellWidth
                val top = row * cellHeight
                val right = if (col == gridCols - 1) bitmap.width else (col + 1) * cellWidth
                val bottom = if (row == gridRows - 1) bitmap.height else (row + 1) * cellHeight
                
                val rect = Rect(left, top, right, bottom)
                val hash = computePerceptualHash(bitmap, rect)
                
                CellData(hash, rect, 0f)
            }
        }
    }
    
    /**
     * Compute a perceptual hash of the cell.
     * Uses subsampling for performance.
     */
    private fun computePerceptualHash(bitmap: Bitmap, rect: Rect): String {
        val sampleStep = 8 // Sample every 8th pixel for speed
        val rSum = mutableListOf<Int>()
        val gSum = mutableListOf<Int>()
        val bSum = mutableListOf<Int>()
        
        // Collect samples
        for (y in rect.top until rect.bottom step sampleStep) {
            for (x in rect.left until rect.right step sampleStep) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    rSum.add((pixel shr 16) and 0xFF)
                    gSum.add((pixel shr 8) and 0xFF)
                    bSum.add(pixel and 0xFF)
                }
            }
        }
        
        if (rSum.isEmpty()) return "empty"
        
        // Compute average colors
        val rAvg = rSum.average().toInt()
        val gAvg = gSum.average().toInt()
        val bAvg = bSum.average().toInt()
        
        // Create hash from above/below average
        val hashBuilder = StringBuilder()
        
        for (y in rect.top until rect.bottom step (sampleStep * 2)) {
            for (x in rect.left until rect.right step (sampleStep * 2)) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    hashBuilder.append(if (r > rAvg) '1' else '0')
                    hashBuilder.append(if (g > gAvg) '1' else '0')
                    hashBuilder.append(if (b > bAvg) '1' else '0')
                }
            }
        }
        
        return hashBuilder.toString()
    }
    
    /**
     * Merge adjacent cells and assign priorities.
     */
    private fun mergeAndPrioritize(
        cells: List<CellData>,
        screenWidth: Int,
        screenHeight: Int
    ): List<Region> {
        if (cells.isEmpty()) return emptyList()
        
        // Sort by position
        val sorted = cells.sortedBy { it.rect.top * screenWidth + it.rect.left }
        
        val regions = mutableListOf<MutableList<CellData>>()
        var currentRegion = mutableListOf(sorted.first())
        
        for (i in 1 until sorted.size) {
            val cell = sorted[i]
            val lastCell = currentRegion.last()
            
            // Check if adjacent
            if (isAdjacent(lastCell.rect, cell.rect)) {
                currentRegion.add(cell)
            } else {
                regions.add(currentRegion)
                currentRegion = mutableListOf(cell)
            }
        }
        regions.add(currentRegion)
        
        // Convert to Region objects with priorities
        return regions.map { regionCells ->
            // Merge all rects in region
            val mergedRect = regionCells.fold(regionCells.first().rect) { acc, cell ->
                Rect().apply { 
                    set(acc)
                    union(cell.rect)
                }
            }
            
            // Calculate priority based on size and position
            val area = mergedRect.width() * mergedRect.height()
            val screenArea = screenWidth * screenHeight
            val areaRatio = area.toFloat() / screenArea
            
            val priority = when {
                areaRatio > 0.5f -> Priority.CRITICAL
                areaRatio > 0.25f -> Priority.HIGH
                areaRatio > 0.1f -> Priority.MEDIUM
                else -> Priority.LOW
            }
            
            Region(
                rect = mergedRect,
                changeScore = areaRatio,
                priority = priority
            )
        }.sortedByDescending { it.priority.ordinal }
    }
    
    private fun isAdjacent(rect1: Rect, rect2: Rect): Boolean {
        // Expand first rect by 1 cell to check adjacency
        val expanded = Rect(rect1).apply { inset(-rect1.width() / 2, -rect1.height() / 2) }
        return expanded.intersect(rect2)
    }
}
