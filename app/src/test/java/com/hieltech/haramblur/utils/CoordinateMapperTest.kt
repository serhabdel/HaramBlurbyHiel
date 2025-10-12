package com.hieltech.haramblur.utils

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateMapperTest {

    private val defaultMetrics = CoordinateMapper.ScreenMetrics(
        screenWidth = 1080,
        screenHeight = 2340,
        statusBarHeight = 0,
        navigationBarHeight = 0,
        realWidth = 1080,
        realHeight = 2340
    )

    @Test
    fun `mapBitmapRegionsToScreen preserves coordinates when dimensions match`() {
        val regions = listOf(Rect(100, 200, 300, 400))

        val mapped = CoordinateMapper.mapBitmapRegionsToScreen(
            regions = regions,
            bitmapWidth = 1080,
            bitmapHeight = 2340,
            screenMetrics = defaultMetrics,
            includeStatusBarOffset = false
        )

        assertEquals(1, mapped.size)
        assertEquals(Rect(100, 200, 300, 400), mapped.first())
    }

    @Test
    fun `mapBitmapRegionsToScreen centers when bitmap is letterboxed`() {
        val regions = listOf(Rect(0, 0, 1080, 1080))

        val mapped = CoordinateMapper.mapBitmapRegionsToScreen(
            regions = regions,
            bitmapWidth = 1080,
            bitmapHeight = 1080,
            screenMetrics = defaultMetrics,
            includeStatusBarOffset = false
        )

        assertEquals(1, mapped.size)
        val expected = Rect(0, 630, 1080, 1710)
        assertEquals(expected, mapped.first())
    }

    @Test
    fun `mapBitmapRegionsToScreen merges overlapping regions`() {
        val regions = listOf(
            Rect(100, 100, 320, 320),
            Rect(280, 260, 500, 460)
        )

        val mapped = CoordinateMapper.mapBitmapRegionsToScreen(
            regions = regions,
            bitmapWidth = 1080,
            bitmapHeight = 2340,
            screenMetrics = defaultMetrics,
            includeStatusBarOffset = false
        )

        assertEquals(1, mapped.size)
        val merged = mapped.first()
        assertEquals(100, merged.left)
        assertEquals(100, merged.top)
        assertEquals(500, merged.right)
        assertEquals(460, merged.bottom)
    }

    @Test
    fun `mapBitmapRegionsToScreen filters out tiny regions`() {
        val regions = listOf(
            Rect(0, 0, 10, 10),
            Rect(50, 50, 120, 120)
        )

        val mapped = CoordinateMapper.mapBitmapRegionsToScreen(
            regions = regions,
            bitmapWidth = 1080,
            bitmapHeight = 2340,
            screenMetrics = defaultMetrics,
            includeStatusBarOffset = false
        )

        assertEquals(1, mapped.size)
        assertTrue(mapped.first().width() >= 70)
        assertTrue(mapped.first().height() >= 70)
    }
}
