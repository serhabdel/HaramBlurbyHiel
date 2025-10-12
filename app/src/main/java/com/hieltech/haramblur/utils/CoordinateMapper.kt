package com.hieltech.haramblur.utils

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Utility class for mapping coordinates between different coordinate spaces
 * Fixes precision issues when mapping blur regions from detection space to screen space
 */
object CoordinateMapper {
    private const val TAG = "CoordinateMapper"
    
    data class ScreenMetrics(
        val screenWidth: Int,
        val screenHeight: Int,
        val statusBarHeight: Int,
        val navigationBarHeight: Int,
        val realWidth: Int,
        val realHeight: Int
    )
    
    /**
     * Get screen metrics including system UI dimensions
     */
    fun getScreenMetrics(context: Context): ScreenMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            
            // Get the real screen dimensions
            val realWidth = bounds.width()
            val realHeight = bounds.height()
            
            // Calculate system UI insets
            val windowInsets = windowMetrics.windowInsets
            val systemBarsInsets = windowInsets.getInsets(
                android.view.WindowInsets.Type.systemBars()
            )
            
            val statusBarHeight = systemBarsInsets.top
            val navigationBarHeight = systemBarsInsets.bottom
            
            // Available screen dimensions (excluding system bars)
            val screenWidth = realWidth
            val screenHeight = realHeight - statusBarHeight - navigationBarHeight
            
            return ScreenMetrics(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                statusBarHeight = statusBarHeight,
                navigationBarHeight = navigationBarHeight,
                realWidth = realWidth,
                realHeight = realHeight
            )
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            // Estimate status bar height
            val statusBarHeight = getStatusBarHeight(context)
            
            // Estimate navigation bar height
            val navigationBarHeight = getNavigationBarHeight(context)
            
            return ScreenMetrics(
                screenWidth = screenWidth,
                screenHeight = screenHeight - statusBarHeight - navigationBarHeight,
                statusBarHeight = statusBarHeight,
                navigationBarHeight = navigationBarHeight,
                realWidth = screenWidth,
                realHeight = screenHeight
            )
        }
    }
    
    /**
     * Get status bar height for older Android versions
     */
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * Get navigation bar height for older Android versions
     */
    private fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * Map blur regions from detection/bitmap space to screen space with proper offset adjustment
     */
    fun mapBitmapRegionsToScreen(
        regions: List<Rect>,
        bitmapWidth: Int,
        bitmapHeight: Int,
        screenMetrics: ScreenMetrics,
        includeStatusBarOffset: Boolean = true
    ): List<Rect> {
        if (regions.isEmpty() || bitmapWidth <= 0 || bitmapHeight <= 0) return emptyList()

        val overlayBounds = buildOverlayBounds(screenMetrics, includeStatusBarOffset)
        val orientationCandidates = resolveOrientationCandidates(bitmapWidth, bitmapHeight, screenMetrics)

        Log.d(TAG, "=== COORDINATE MAPPING DEBUG ===")
        Log.d(TAG, "Bitmap: ${bitmapWidth}x${bitmapHeight}")
        Log.d(TAG, "Screen (real): ${screenMetrics.realWidth}x${screenMetrics.realHeight}")
        Log.d(TAG, "Overlay bounds: ${overlayBounds.toShortString()}")

        var bestOrientation = OrientationAdjustment.NONE
        var bestRegions: List<RectF> = emptyList()
        var maxCoveredArea = 0f

        for (orientation in orientationCandidates) {
            val sourceWidth = orientation.transformedWidth(bitmapWidth, bitmapHeight)
            val sourceHeight = orientation.transformedHeight(bitmapWidth, bitmapHeight)
            val transform = buildTransformMatrix(sourceWidth, sourceHeight, overlayBounds)

            val mapped = regions.mapIndexedNotNull { index, region ->
                val orientedRect = orientation.mapRect(region, bitmapWidth, bitmapHeight)
                transform.mapRect(orientedRect)
                val clamped = clampToBounds(orientedRect, overlayBounds)

                if (clamped.width() < MIN_REGION_SIZE || clamped.height() < MIN_REGION_SIZE) {
                    Log.d(TAG, "Orientation ${orientation.name}: region $index dropped (too small) ${clamped.toShortString()}")
                    null
                } else {
                    Log.d(TAG, "Orientation ${orientation.name}: region $index -> ${clamped.toShortString()}")
                    clamped
                }
            }

            val normalized = normalizeRegions(mapped, overlayBounds)
            val coveredArea = normalized.sumOf { (it.width() * it.height()).toDouble() }.toFloat()

            Log.d(TAG, "Orientation ${orientation.name}: ${normalized.size} normalized regions, covered area=$coveredArea")

            if (coveredArea > maxCoveredArea) {
                maxCoveredArea = coveredArea
                bestRegions = normalized
                bestOrientation = orientation
            }
        }

        Log.d(TAG, "Selected orientation: ${bestOrientation.name}, regions=${bestRegions.size}")

        return bestRegions.mapIndexed { index, rectF ->
            val mappedRegion = Rect(
                rectF.left.roundToInt(),
                rectF.top.roundToInt(),
                rectF.right.roundToInt(),
                rectF.bottom.roundToInt()
            )

            Log.d(TAG, "Region $index final: [${mappedRegion.left},${mappedRegion.top}] -> [${mappedRegion.right},${mappedRegion.bottom}]")

            mappedRegion
        }
    }

    private fun buildOverlayBounds(
        screenMetrics: ScreenMetrics,
        includeStatusBarOffset: Boolean
    ): RectF {
        val topInset = if (includeStatusBarOffset) screenMetrics.statusBarHeight.toFloat() else 0f
        val bottomInset = if (includeStatusBarOffset) screenMetrics.navigationBarHeight.toFloat() else 0f

        val top = topInset
        val bottom = max(top, screenMetrics.realHeight.toFloat() - bottomInset)

        return RectF(
            0f,
            top,
            screenMetrics.realWidth.toFloat(),
            bottom
        )
    }

    private fun buildTransformMatrix(
        sourceWidth: Int,
        sourceHeight: Int,
        overlayBounds: RectF
    ): Matrix {
        val matrix = Matrix()

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return matrix
        }

        val srcRect = RectF(0f, 0f, sourceWidth.toFloat(), sourceHeight.toFloat())
        val scaleX = overlayBounds.width() / srcRect.width()
        val scaleY = overlayBounds.height() / srcRect.height()
        val scaleDifference = abs(scaleX - scaleY)
        val aspectTolerance = 0.015f

        if (scaleDifference <= aspectTolerance) {
            val scale = (scaleX + scaleY) / 2f
            val scaledWidth = srcRect.width() * scale
            val scaledHeight = srcRect.height() * scale
            val dx = overlayBounds.left + (overlayBounds.width() - scaledWidth) / 2f
            val dy = overlayBounds.top + (overlayBounds.height() - scaledHeight) / 2f

            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)
        } else {
            val scale: Float
            val dx: Float
            val dy: Float

            if (scaleX < scaleY) {
                scale = scaleX
                val scaledHeight = srcRect.height() * scale
                dx = overlayBounds.left
                dy = overlayBounds.top + (overlayBounds.height() - scaledHeight) / 2f
            } else {
                scale = scaleY
                val scaledWidth = srcRect.width() * scale
                dx = overlayBounds.left + (overlayBounds.width() - scaledWidth) / 2f
                dy = overlayBounds.top
            }

            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)
        }

        Log.d(
            TAG,
            "Transform built: src=${srcRect.toShortString()}, dst=${overlayBounds.toShortString()}, scaleX=$scaleX, scaleY=$scaleY"
        )

        return matrix
    }

    private fun clampToBounds(rect: RectF, bounds: RectF): RectF {
        rect.left = rect.left.coerceIn(bounds.left, bounds.right)
        rect.top = rect.top.coerceIn(bounds.top, bounds.bottom)
        rect.right = rect.right.coerceIn(bounds.left, bounds.right)
        rect.bottom = rect.bottom.coerceIn(bounds.top, bounds.bottom)

        if (rect.left > rect.right) {
            val mid = (rect.left + rect.right) / 2f
            rect.left = mid
            rect.right = mid
        }

        if (rect.top > rect.bottom) {
            val mid = (rect.top + rect.bottom) / 2f
            rect.top = mid
            rect.bottom = mid
        }

        return rect
    }

    private fun normalizeRegions(regions: List<RectF>, bounds: RectF): List<RectF> {
        if (regions.isEmpty()) return emptyList()

        val filtered = regions.filter { it.width() >= MIN_REGION_SIZE && it.height() >= MIN_REGION_SIZE }
        if (filtered.isEmpty()) return emptyList()

        val merged = mergeRegions(filtered, bounds)
        val unique = removeContainedRegions(merged)

        return unique.sortedWith(compareBy({ it.top }, { it.left }))
    }

    private fun mergeRegions(regions: List<RectF>, bounds: RectF): MutableList<RectF> {
        if (regions.isEmpty()) return mutableListOf()

        val working = mutableListOf<RectF>()
        val dynamicGap = max(MERGE_GAP_MIN, min(bounds.width(), bounds.height()) * MERGE_GAP_RATIO)

        regions.sortedBy { it.left }.forEach { candidate ->
            var merged = false
            for (existing in working) {
                if (shouldMerge(existing, candidate, dynamicGap)) {
                    existing.union(candidate)
                    clampToBounds(existing, bounds)
                    merged = true
                    break
                }
            }

            if (!merged) {
                working.add(RectF(candidate))
            }
        }

        return working
    }

    private fun shouldMerge(a: RectF, b: RectF, gap: Float): Boolean {
        val expandedA = RectF(
            a.left - gap,
            a.top - gap,
            a.right + gap,
            a.bottom + gap
        )
        val expandedB = RectF(
            b.left - gap,
            b.top - gap,
            b.right + gap,
            b.bottom + gap
        )

        return RectF.intersects(expandedA, expandedB) || expandedA.contains(b) || expandedB.contains(a)
    }

    private fun removeContainedRegions(regions: MutableList<RectF>): List<RectF> {
        if (regions.size <= 1) return regions

        val result = mutableListOf<RectF>()

        regions.forEachIndexed { index, rect ->
            val isContained = regions.any { other ->
                other !== rect && other.contains(rect)
            }

            if (!isContained) {
                result.add(RectF(rect))
            } else {
                Log.d(TAG, "Region ${rect.toShortString()} removed (contained in larger region)")
            }
        }

        return result
    }

    private fun resolveOrientationCandidates(
        bitmapWidth: Int,
        bitmapHeight: Int,
        screenMetrics: ScreenMetrics
    ): List<OrientationAdjustment> {
        val candidates = mutableListOf(OrientationAdjustment.NONE)
        val width = screenMetrics.realWidth
        val height = screenMetrics.realHeight
        val tolerance = 8

        val widthMatches = approxEquals(bitmapWidth, width, tolerance)
        val heightMatches = approxEquals(bitmapHeight, height, tolerance)

        if (!widthMatches || !heightMatches) {
            val swappedWidthMatches = approxEquals(bitmapWidth, height, tolerance)
            val swappedHeightMatches = approxEquals(bitmapHeight, width, tolerance)

            if (swappedWidthMatches && swappedHeightMatches) {
                candidates.add(OrientationAdjustment.ROTATED_90_CW)
                candidates.add(OrientationAdjustment.ROTATED_90_CCW)
            }
        }

        return candidates.distinct()
    }

    private fun approxEquals(a: Int, b: Int, tolerance: Int): Boolean {
        return abs(a - b) <= tolerance
    }

    private enum class OrientationAdjustment {
        NONE {
            override fun transformedWidth(bitmapWidth: Int, bitmapHeight: Int) = bitmapWidth
            override fun transformedHeight(bitmapWidth: Int, bitmapHeight: Int) = bitmapHeight
            override fun mapRect(rect: Rect, bitmapWidth: Int, bitmapHeight: Int) = RectF(rect)
        },
        ROTATED_90_CW {
            override fun transformedWidth(bitmapWidth: Int, bitmapHeight: Int) = bitmapHeight
            override fun transformedHeight(bitmapWidth: Int, bitmapHeight: Int) = bitmapWidth
            override fun mapRect(rect: Rect, bitmapWidth: Int, bitmapHeight: Int): RectF {
                val left = bitmapHeight - rect.bottom
                val top = rect.left
                val right = bitmapHeight - rect.top
                val bottom = rect.right
                return RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            }
        },
        ROTATED_90_CCW {
            override fun transformedWidth(bitmapWidth: Int, bitmapHeight: Int) = bitmapHeight
            override fun transformedHeight(bitmapWidth: Int, bitmapHeight: Int) = bitmapWidth
            override fun mapRect(rect: Rect, bitmapWidth: Int, bitmapHeight: Int): RectF {
                val left = rect.top
                val top = bitmapWidth - rect.right
                val right = rect.bottom
                val bottom = bitmapWidth - rect.left
                return RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            }
        };

        abstract fun transformedWidth(bitmapWidth: Int, bitmapHeight: Int): Int
        abstract fun transformedHeight(bitmapWidth: Int, bitmapHeight: Int): Int
        abstract fun mapRect(rect: Rect, bitmapWidth: Int, bitmapHeight: Int): RectF
    }

    private const val MIN_REGION_SIZE = 18f
    private const val MERGE_GAP_MIN = 16f
    private const val MERGE_GAP_RATIO = 0.012f
    
    /**
     * Adjust blur regions for accessibility overlay positioning
     */
    fun adjustForAccessibilityOverlay(
        regions: List<Rect>,
        context: Context
    ): List<Rect> {
        val screenMetrics = getScreenMetrics(context)
        
        // For accessibility overlays, we need to position relative to the full screen
        // including system UI areas
        return regions.map { region ->
            // The accessibility overlay covers the entire screen including system bars
            // So we don't need to add offsets, but we need to ensure regions fit
            Rect(
                maxOf(0, region.left),
                maxOf(0, region.top),
                minOf(screenMetrics.realWidth, region.right),
                minOf(screenMetrics.realHeight, region.bottom)
            )
        }
    }
    
    /**
     * Debug function to validate region positioning
     */
    fun debugRegionMapping(
        originalRegion: Rect,
        mappedRegion: Rect,
        context: Context
    ) {
        val screenMetrics = getScreenMetrics(context)
        
        Log.w(TAG, "===== BLUR REGION DEBUG =====")
        Log.w(TAG, "Screen: ${screenMetrics.realWidth}x${screenMetrics.realHeight}")
        Log.w(TAG, "Status Bar: ${screenMetrics.statusBarHeight}px")
        Log.w(TAG, "Nav Bar: ${screenMetrics.navigationBarHeight}px")
        Log.w(TAG, "Original Region: [${originalRegion.left},${originalRegion.top}] -> [${originalRegion.right},${originalRegion.bottom}]")
        Log.w(TAG, "Mapped Region: [${mappedRegion.left},${mappedRegion.top}] -> [${mappedRegion.right},${mappedRegion.bottom}]")
        Log.w(TAG, "Region Size: ${mappedRegion.width()}x${mappedRegion.height()}")
        Log.w(TAG, "=============================")
    }
}
