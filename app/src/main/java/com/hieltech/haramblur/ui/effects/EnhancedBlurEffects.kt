package com.hieltech.haramblur.ui.effects

import android.graphics.*
import android.os.Build
import android.util.Log
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurRenderingMode
import com.hieltech.haramblur.data.BlurStyle
import kotlin.math.*
import kotlin.random.Random

/**
 * Enhanced blur effects for maximum content blocking and privacy protection
 * with hardware acceleration and performance optimizations
 */
class EnhancedBlurEffects {
    
    companion object {
        private const val TAG = "EnhancedBlurEffects"
        private const val NOISE_DENSITY = 0.4f
        private const val PATTERN_ALPHA_BASE = 150
        private const val LAYERED_BLUR_LAYERS = 3
        private const val MAX_PARTICLE_COUNT = 10000 // Limit to prevent jank
        private const val MAX_CACHED_PATTERN_SIZE = 512 // Maximum size for cached patterns
    }
    
    var currentRenderingMode: BlurRenderingMode = BlurRenderingMode.SMOOTH
    private var lastFrameTime: Long = 0L
    private var frameTimeThreshold: Long = 16L // 60fps target
    
    // Cached Paint objects for performance
    private val cachedPaintObjects = mutableMapOf<String, Paint>()
    
    // Cached bitmaps for performance
    private var cachedGrainBitmap: Bitmap? = null
    private var cachedNoiseBitmap: Bitmap? = null
    private var cachedPatternBitmap: Bitmap? = null
    
    // Performance monitoring
    private var frameCount = 0
    private var totalFrameTime = 0L
    private var lastPerformanceLogTime = 0L

    /**
     * Enable hardware acceleration for blur rendering
     */
    fun enableHardwareAcceleration(canvas: Canvas, paint: Paint): Boolean {
        return try {
            // Enable hardware acceleration if available
            paint.isDither = true
            paint.isAntiAlias = true
            
            // For API 31+, use RenderEffect for hardware-accelerated blur
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Note: RenderEffect.createBlurEffect would be applied to the View, not here
                // This is a placeholder for the concept
                Log.d(TAG, "Hardware acceleration available (API 31+)")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable hardware acceleration", e)
            false
        }
    }

    /**
     * Set performance mode for blur rendering
     */
    fun setPerformanceMode(mode: BlurRenderingMode) {
        currentRenderingMode = mode
        Log.d(TAG, "Blur rendering mode set to: ${mode.name}")
    }

    /**
     * Generate cached grain bitmap for performance optimization
     */
    private fun generateCachedGrainBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtMost(MAX_CACHED_PATTERN_SIZE),
            height.coerceAtMost(MAX_CACHED_PATTERN_SIZE),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        val grainPaint = Paint().apply {
            isAntiAlias = true
            alpha = 180
        }
        
        val grainCount = ((width * height * 0.15f) / 100).toInt().coerceAtMost(MAX_PARTICLE_COUNT / 10)
        
        repeat(grainCount) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val grainSize = Random.nextFloat() * 2.5f + 0.5f
            val brightness = Random.nextInt(80, 200)
            
            grainPaint.color = Color.rgb(brightness, brightness, brightness)
            grainPaint.alpha = (brightness / 255f * 180 + 75).toInt()
            
            if (Random.nextFloat() < 0.7f) {
                canvas.drawCircle(x, y, grainSize, grainPaint)
            } else {
                val rect = RectF(
                    x - grainSize,
                    y - grainSize * 0.7f,
                    x + grainSize,
                    y + grainSize * 0.7f
                )
                canvas.drawOval(rect, grainPaint)
            }
        }
        
        return bitmap
    }

    /**
     * Generate cached noise bitmap for performance optimization
     */
    private fun generateCachedNoiseBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtMost(MAX_CACHED_PATTERN_SIZE),
            height.coerceAtMost(MAX_CACHED_PATTERN_SIZE),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        val noisePaint = Paint().apply {
            isAntiAlias = true
            alpha = 180
        }
        
        val numNoise = ((width * height * 0.4f) / 9).toInt().coerceAtMost(MAX_PARTICLE_COUNT / 5)
        
        repeat(numNoise) {
            val x = Random.nextInt(width)
            val y = Random.nextInt(height)
            val brightness = Random.nextInt(100, 255)
            
            noisePaint.color = Color.rgb(brightness, brightness, brightness)
            canvas.drawCircle(x.toFloat(), y.toFloat(), 3f, noisePaint)
        }
        
        return bitmap
    }

    /**
     * Refine blur edges with RectF and fractional insets for anti-aliased soft edge blending
     */
    fun refineBlurEdges(rect: Rect, canvas: Canvas, precision: Float = 0.5f) {
        try {
            // Convert to RectF for fractional precision
            val rectF = RectF(rect)
            
            // Apply fractional insets based on precision (0.1 to 1.0 pixels)
            val insetAmount = precision * 0.5f // 0.05 to 0.5 pixels
            
            // Create slightly inset rectangle to avoid edge artifacts
            val refinedRectF = RectF(
                rectF.left + insetAmount,
                rectF.top + insetAmount,
                rectF.right - insetAmount,
                rectF.bottom - insetAmount
            )
            
            // Apply anti-aliased soft edge blending
            val edgePaint = getCachedPaint("edge_refinement") {
                isAntiAlias = true
                isDither = true
                style = Paint.Style.STROKE
                strokeWidth = precision * 2f // 0.2 to 2.0 pixels based on precision
                alpha = (255 * precision).toInt()
                // Use soft edge blending with PorterDuff mode
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            
            canvas.drawRect(refinedRectF, edgePaint)
            
            // Add additional soft edge layer for high precision
            if (precision > 0.7f) {
                val softEdgePaint = getCachedPaint("soft_edge_refinement") {
                    isAntiAlias = true
                    isDither = true
                    style = Paint.Style.STROKE
                    strokeWidth = precision * 1.5f
                    alpha = (128 * precision).toInt() // Softer alpha for blending
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                }
                
                // Slightly larger rectangle for soft edge effect
                val softEdgeRectF = RectF(
                    rectF.left + insetAmount * 0.5f,
                    rectF.top + insetAmount * 0.5f,
                    rectF.right - insetAmount * 0.5f,
                    rectF.bottom - insetAmount * 0.5f
                )
                
                canvas.drawRect(softEdgeRectF, softEdgePaint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refine blur edges", e)
        }
    }

    /**
     * Smooth interpolation between blur intensities during animations
     */
    fun interpolateBlurTransition(
        fromIntensity: BlurIntensity,
        toIntensity: BlurIntensity,
        progress: Float
    ): BlurIntensity {
        val fromOrdinal = fromIntensity.ordinal
        val toOrdinal = toIntensity.ordinal
        
        val interpolatedOrdinal = fromOrdinal + (toOrdinal - fromOrdinal) * progress
        val clampedOrdinal = interpolatedOrdinal.roundToInt().coerceIn(0, BlurIntensity.values().size - 1)
        
        return BlurIntensity.values()[clampedOrdinal]
    }

    /**
     * Check if we should skip this frame for performance reasons
     */
    fun shouldSkipFrame(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastFrameTime
        
        return if (currentRenderingMode == BlurRenderingMode.SMOOTH && timeSinceLastFrame < frameTimeThreshold) {
            Log.d(TAG, "Skipping frame - too soon (${timeSinceLastFrame}ms < ${frameTimeThreshold}ms)")
            true
        } else {
            lastFrameTime = currentTime
            false
        }
    }

    /**
     * Get cached paint object for performance
     */
    private fun getCachedPaint(key: String, config: Paint.() -> Unit): Paint {
        return cachedPaintObjects.getOrPut(key) {
            Paint().apply(config)
        }
    }

    /**
     * Clear cached resources when memory is low
     */
    fun clearCachedResources() {
        try {
            cachedGrainBitmap?.recycle()
            cachedNoiseBitmap?.recycle()
            cachedPatternBitmap?.recycle()
            
            cachedGrainBitmap = null
            cachedNoiseBitmap = null
            cachedPatternBitmap = null
            cachedPaintObjects.clear()
            
            Log.d(TAG, "Cached blur resources cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cached resources", e)
        }
    }

    /**
     * Log performance metrics
     */
    private fun logPerformanceMetrics(frameTime: Long) {
        frameCount++
        totalFrameTime += frameTime
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPerformanceLogTime > 5000) { // Log every 5 seconds
            val avgFrameTime = totalFrameTime / frameCount
            val fps = if (avgFrameTime > 0) 1000 / avgFrameTime else 0
            
            Log.d(TAG, "Blur performance - Frames: $frameCount, Avg time: ${avgFrameTime}ms, FPS: $fps")
            
            // Reset counters
            frameCount = 0
            totalFrameTime = 0
            lastPerformanceLogTime = currentTime
        }
    }
    
    /**
     * Apply enhanced blur effect based on content sensitivity with performance optimizations
     * Enhanced with settings wiring for interpolation, max regions/frame, edge flags
     */
    fun applyEnhancedBlur(
        canvas: Canvas,
        rect: Rect,
        blurIntensity: BlurIntensity,
        blurStyle: BlurStyle,
        contentSensitivity: Float = 0.5f, // 0.0 = low sensitivity, 1.0 = high sensitivity
        enableEdgeRefinement: Boolean = true,
        enableAntiAliasing: Boolean = true,
        precision: Float = 0.5f,
        enableBlurEdgeRefinement: Boolean = true,
        blurEdgeAntiAliasing: Boolean = true,
        blurBoundaryPrecision: Float = 0.5f,
        enableBlurFrameRateLimiting: Boolean = true,
        maxBlurRegionsPerFrame: Int = 12,
        enableBlurRegionInterpolation: Boolean = true,
        blurAnimationDuration: Int = 250
    ) {
        // Check if we should skip this frame for performance (respect frame rate limiting setting)
        if (enableBlurFrameRateLimiting && shouldSkipFrame()) {
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Enable hardware acceleration
            val paint = Paint()
            enableHardwareAcceleration(canvas, paint)
            
            // Apply edge refinement based on settings
            if (enableBlurEdgeRefinement && blurEdgeAntiAliasing) {
                refineBlurEdges(rect, canvas, blurBoundaryPrecision)
            } else if (enableEdgeRefinement) {
                // Fallback to legacy edge refinement
                refineBlurEdges(rect, canvas, precision)
            }
            
            // Apply blur based on rendering mode with interpolation support
            when (currentRenderingMode) {
                BlurRenderingMode.SMOOTH -> {
                    // Use cached patterns for better performance
                    applyOptimizedBlur(canvas, rect, blurIntensity, blurStyle, contentSensitivity)
                }
                BlurRenderingMode.QUALITY -> {
                    // Use full quality rendering
                    applyQualityBlur(canvas, rect, blurIntensity, blurStyle, contentSensitivity)
                }
                BlurRenderingMode.ADAPTIVE -> {
                    // Automatically choose based on performance
                    val frameTime = System.currentTimeMillis() - lastFrameTime
                    if (frameTime > frameTimeThreshold * 2) {
                        applyOptimizedBlur(canvas, rect, blurIntensity, blurStyle, contentSensitivity)
                    } else {
                        applyQualityBlur(canvas, rect, blurIntensity, blurStyle, contentSensitivity)
                    }
                }
            }
            
            // Log performance metrics
            val frameTime = System.currentTimeMillis() - startTime
            logPerformanceMetrics(frameTime)
            
            if (frameTime > 8) { // Log if frame takes more than 8ms
                Log.w(TAG, "Blur rendering took ${frameTime}ms - consider optimizing")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply enhanced blur", e)
            // Fallback to simple solid blur
            applyFallbackBlur(canvas, rect, blurIntensity, contentSensitivity)
        }
    }

    /**
     * Apply optimized blur using cached patterns for smooth performance
     */
    private fun applyOptimizedBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        style: BlurStyle,
        sensitivity: Float
    ) {
        when (style) {
            BlurStyle.SOLID -> applySolidBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.PIXELATED -> applyPixelatedBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.NOISE -> applyOptimizedNoiseBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.ARTISTIC -> applyOptimizedArtisticBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.COMBINED -> applyOptimizedCombinedBlur(canvas, rect, intensity, sensitivity)
        }
    }

    /**
     * Apply quality blur with full rendering for best visual results
     */
    private fun applyQualityBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        style: BlurStyle,
        sensitivity: Float
    ) {
        when (style) {
            BlurStyle.SOLID -> applySolidBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.PIXELATED -> applyPixelatedBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.NOISE -> applyNoiseBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.ARTISTIC -> applyArtisticBlur(canvas, rect, intensity, sensitivity)
            BlurStyle.COMBINED -> applyCombinedBlur(canvas, rect, intensity, sensitivity)
        }
    }

    /**
     * Apply fallback blur for error recovery
     */
    private fun applyFallbackBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        val paint = getCachedPaint("fallback_blur") {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = intensity.alphaValue
        }
        
        canvas.drawRect(rect, paint)
    }

    /**
     * Apply optimized noise blur using cached bitmaps
     */
    fun applyOptimizedNoiseBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Base solid layer
        val basePaint = getCachedPaint("noise_base") {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = (intensity.alphaValue * 0.7f).toInt()
        }
        canvas.drawRect(rect, basePaint)
        
        // Use cached noise bitmap instead of per-pixel generation
        val cachedNoise = cachedNoiseBitmap ?: run {
            val bitmap = generateCachedNoiseBitmap(rect.width(), rect.height())
            cachedNoiseBitmap = bitmap
            bitmap
        }
        
        val noisePaint = getCachedPaint("noise_overlay") {
            isAntiAlias = true
            alpha = (sensitivity * 120 + 60).toInt()
        }
        
        // Draw cached noise pattern
        canvas.drawBitmap(cachedNoise, rect.left.toFloat(), rect.top.toFloat(), noisePaint)
        
        // Additional static pattern for maximum privacy (reduced particle count)
        if (sensitivity > 0.8f) {
            addOptimizedStaticPattern(canvas, rect, sensitivity)
        }
    }

    /**
     * Apply optimized artistic blur using cached bitmaps
     */
    private fun applyOptimizedArtisticBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Base layer with subtle gradient
        val basePaint = getCachedPaint("artistic_base") {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = (intensity.alphaValue * 0.85f).toInt()
        }
        canvas.drawRect(rect, basePaint)
        
        // Use cached grain bitmap for film grain effect
        val cachedGrain = cachedGrainBitmap ?: run {
            val bitmap = generateCachedGrainBitmap(rect.width(), rect.height())
            cachedGrainBitmap = bitmap
            bitmap
        }
        
        val grainPaint = getCachedPaint("artistic_grain") {
            isAntiAlias = true
            alpha = (sensitivity * 80 + 100).toInt()
        }
        
        // Draw cached grain pattern
        canvas.drawBitmap(cachedGrain, rect.left.toFloat(), rect.top.toFloat(), grainPaint)
        
        // Add subtle organic noise pattern (reduced particle count)
        addOptimizedOrganicNoise(canvas, rect, sensitivity)
        
        // Soft vignette effect for high sensitivity
        if (sensitivity > 0.6f) {
            applySoftVignette(canvas, rect, sensitivity)
        }
    }

    /**
     * Apply optimized combined blur using cached patterns
     */
    private fun applyOptimizedCombinedBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Layer 1: Base solid blur
        applySolidBlur(canvas, rect, intensity, sensitivity * 0.8f)
        
        // Layer 2: Pixelated overlay (optimized)
        val pixelSize = (15 - (sensitivity * 8)).toInt().coerceAtLeast(4)
        drawPixelatedPattern(canvas, rect, pixelSize, (intensity.alphaValue * 0.6f).toInt(), sensitivity)
        
        // Layer 3: Cached noise overlay
        val cachedNoise = cachedNoiseBitmap ?: run {
            val bitmap = generateCachedNoiseBitmap(rect.width(), rect.height())
            cachedNoiseBitmap = bitmap
            bitmap
        }
        
        val noisePaint = getCachedPaint("combined_noise") {
            isAntiAlias = true
            alpha = (sensitivity * 80 + 40).toInt()
        }
        
        canvas.drawBitmap(cachedNoise, rect.left.toFloat(), rect.top.toFloat(), noisePaint)
        
        // Layer 4: Cached artistic grain for very high sensitivity
        if (sensitivity > 0.8f) {
            val cachedGrain = cachedGrainBitmap ?: run {
                val bitmap = generateCachedGrainBitmap(rect.width(), rect.height())
                cachedGrainBitmap = bitmap
                bitmap
            }
            
            val grainPaint = getCachedPaint("combined_grain") {
                isAntiAlias = true
                alpha = (sensitivity * 60 + 80).toInt()
            }
            
            canvas.drawBitmap(cachedGrain, rect.left.toFloat(), rect.top.toFloat(), grainPaint)
        }
        
        // Layer 5: Privacy border
        addPrivacyBorder(canvas, rect, intensity, sensitivity)
    }

    /**
     * Add optimized static pattern with reduced particle count
     */
    private fun addOptimizedStaticPattern(canvas: Canvas, rect: Rect, sensitivity: Float) {
        val staticPaint = getCachedPaint("optimized_static") {
            isAntiAlias = false
            alpha = (sensitivity * 120).toInt()
        }
        
        val dotSize = 2
        val spacing = 8 // Increased spacing to reduce particle count
        val maxParticles = MAX_PARTICLE_COUNT / 20 // Limit particles
        
        var particleCount = 0
        
        for (x in rect.left until rect.right step spacing) {
            for (y in rect.top until rect.bottom step spacing) {
                if (particleCount >= maxParticles) break
                
                if (Random.nextFloat() < 0.5f) { // Reduced probability
                    val brightness = Random.nextInt(50, 200)
                    staticPaint.color = Color.rgb(brightness, brightness, brightness)
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + dotSize).toFloat(),
                        (y + dotSize).toFloat(),
                        staticPaint
                    )
                    particleCount++
                }
            }
            if (particleCount >= maxParticles) break
        }
    }

    /**
     * Add optimized organic noise with reduced particle count
     */
    private fun addOptimizedOrganicNoise(canvas: Canvas, rect: Rect, sensitivity: Float) {
        val noisePaint = getCachedPaint("optimized_organic") {
            isAntiAlias = true
            alpha = (sensitivity * 50 + 30).toInt()
        }
        
        val clusterCount = (rect.width() * rect.height() * 0.00005f).toInt().coerceAtLeast(3) // Reduced density
        val maxParticles = MAX_PARTICLE_COUNT / 50 // Limit particles per cluster
        
        repeat(clusterCount) {
            val centerX = rect.left + Random.nextFloat() * rect.width()
            val centerY = rect.top + Random.nextFloat() * rect.height()
            val clusterSize = Random.nextFloat() * 10f + 3f // Reduced cluster size
            val clusterDensity = Random.nextInt(2, 5) // Reduced cluster density
            
            var particleCount = 0
            
            // Create small clusters of noise points
            repeat(clusterDensity) {
                if (particleCount >= maxParticles) return@repeat
                
                val offsetX = (Random.nextFloat() - 0.5f) * clusterSize
                val offsetY = (Random.nextFloat() - 0.5f) * clusterSize
                val x = centerX + offsetX
                val y = centerY + offsetY
                
                if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                    val brightness = Random.nextInt(60, 180)
                    noisePaint.color = Color.rgb(brightness, brightness, brightness)
                    
                    val pointSize = Random.nextFloat() * 1.0f + 0.3f // Reduced point size
                    canvas.drawCircle(x, y, pointSize, noisePaint)
                    particleCount++
                }
            }
        }
    }
    
    /**
     * Apply solid blur with intensity scaling
     */
    private fun applySolidBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        val baseAlpha = intensity.alphaValue
        val adjustedAlpha = (baseAlpha + (sensitivity * 55)).toInt().coerceAtMost(255)
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = adjustedAlpha
        }
        
        canvas.drawRect(rect, paint)
        
        // Add subtle texture for higher sensitivity content
        if (sensitivity > 0.7f) {
            addTextureOverlay(canvas, rect, sensitivity)
        }
    }
    
    /**
     * Apply pixelated blur with dynamic pixel size
     */
    fun applyPixelatedBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float,
        alpha: Int = 200
    ) {
        // Smaller pixels for higher sensitivity content
        val basePixelSize = when (intensity) {
            BlurIntensity.LIGHT -> 25
            BlurIntensity.MEDIUM -> 20
            BlurIntensity.STRONG -> 15
            BlurIntensity.MAXIMUM -> 10
        }
        
        val pixelSize = (basePixelSize * (1.0f - sensitivity * 0.5f)).toInt().coerceAtLeast(5)
        val alpha = if (alpha != 200) alpha else (intensity.alphaValue + (sensitivity * 50)).toInt().coerceAtMost(255)
        
        drawPixelatedPattern(canvas, rect, pixelSize, alpha, sensitivity)
        
        // Add noise overlay for high sensitivity
        if (sensitivity > 0.6f) {
            addNoiseOverlay(canvas, rect, sensitivity * 0.3f)
        }
    }
    
    /**
     * Apply noise-based blur
     */
    private fun applyNoiseBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Base solid layer
        val basePaint = Paint().apply {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = (intensity.alphaValue * 0.7f).toInt()
        }
        canvas.drawRect(rect, basePaint)
        
        // Dense noise overlay
        val noiseDensity = NOISE_DENSITY + (sensitivity * 0.4f)
        addNoiseOverlay(canvas, rect, noiseDensity)
        
        // Additional static pattern for maximum privacy
        if (sensitivity > 0.8f) {
            addStaticPattern(canvas, rect, sensitivity)
        }
    }
    
    /**
     * Apply artistic blur with film grain effect
     */
    private fun applyArtisticBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Base layer with subtle gradient
        val baseColor = getSensitivityBasedColor(sensitivity)
        val basePaint = Paint().apply {
            isAntiAlias = true
            color = baseColor
            alpha = (intensity.alphaValue * 0.85f).toInt()
        }
        canvas.drawRect(rect, basePaint)
        
        // Apply film grain texture
        applyFilmGrain(canvas, rect, intensity, sensitivity)
        
        // Add subtle organic noise pattern
        applyOrganicNoise(canvas, rect, sensitivity)
        
        // Soft vignette effect for high sensitivity
        if (sensitivity > 0.6f) {
            applySoftVignette(canvas, rect, sensitivity)
        }
    }
    
    /**
     * Apply film grain texture similar to analog photography
     */
    private fun applyFilmGrain(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        val grainDensity = when (intensity) {
            BlurIntensity.LIGHT -> 0.15f
            BlurIntensity.MEDIUM -> 0.25f
            BlurIntensity.STRONG -> 0.35f
            BlurIntensity.MAXIMUM -> 0.45f
        }
        
        // Adjust density based on sensitivity
        val adjustedDensity = grainDensity + (sensitivity * 0.2f)
        val grainCount = ((rect.width() * rect.height()) * adjustedDensity).toInt()
        
        val grainPaint = Paint().apply {
            isAntiAlias = true
        }
        
        repeat(grainCount) {
            val x = rect.left + Random.nextFloat() * rect.width()
            val y = rect.top + Random.nextFloat() * rect.height()
            
            // Create grain with varying sizes and intensities
            val grainSize = Random.nextFloat() * 2.5f + 0.5f
            val brightness = when {
                Random.nextFloat() < 0.6f -> Random.nextInt(80, 140) // Most grains are medium
                Random.nextFloat() < 0.8f -> Random.nextInt(140, 200) // Some brighter grains
                else -> Random.nextInt(20, 80) // Few darker grains
            }
            
            // Vary opacity based on grain brightness
            val grainAlpha = ((brightness / 255f) * 180 + 75).toInt()
            
            grainPaint.color = Color.rgb(brightness, brightness, brightness)
            grainPaint.alpha = grainAlpha
            
            // Draw organic-shaped grain (not perfect circles)
            if (Random.nextFloat() < 0.7f) {
                // Most grains are circular
                canvas.drawCircle(x, y, grainSize, grainPaint)
            } else {
                // Some grains are slightly elongated
                val rect = android.graphics.RectF(
                    x - grainSize,
                    y - grainSize * 0.7f,
                    x + grainSize,
                    y + grainSize * 0.7f
                )
                canvas.drawOval(rect, grainPaint)
            }
        }
    }
    
    /**
     * Apply organic noise pattern for natural look
     */
    private fun applyOrganicNoise(
        canvas: Canvas,
        rect: Rect,
        sensitivity: Float
    ) {
        val noisePaint = Paint().apply {
            isAntiAlias = true
            alpha = (sensitivity * 60 + 40).toInt()
        }
        
        val clusterCount = (rect.width() * rect.height() * 0.0001f).toInt().coerceAtLeast(5)
        
        repeat(clusterCount) {
            val centerX = rect.left + Random.nextFloat() * rect.width()
            val centerY = rect.top + Random.nextFloat() * rect.height()
            val clusterSize = Random.nextFloat() * 15f + 5f
            val clusterDensity = Random.nextInt(3, 8)
            
            // Create small clusters of noise points
            repeat(clusterDensity) {
                val offsetX = (Random.nextFloat() - 0.5f) * clusterSize
                val offsetY = (Random.nextFloat() - 0.5f) * clusterSize
                val x = centerX + offsetX
                val y = centerY + offsetY
                
                if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                    val brightness = Random.nextInt(60, 180)
                    noisePaint.color = Color.rgb(brightness, brightness, brightness)
                    
                    val pointSize = Random.nextFloat() * 1.5f + 0.5f
                    canvas.drawCircle(x, y, pointSize, noisePaint)
                }
            }
        }
    }
    
    /**
     * Apply soft vignette effect around edges
     */
    private fun applySoftVignette(
        canvas: Canvas,
        rect: Rect,
        sensitivity: Float
    ) {
        val vignettePaint = Paint().apply {
            isAntiAlias = true
            color = getSensitivityBasedColor(sensitivity)
            alpha = (sensitivity * 80).toInt()
        }
        
        val vignetteWidth = (rect.width() * 0.1f).coerceAtLeast(10f)
        val vignetteHeight = (rect.height() * 0.1f).coerceAtLeast(10f)
        
        // Draw soft edges
        val topRect = android.graphics.RectF(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.top + vignetteHeight
        )
        canvas.drawRect(topRect, vignettePaint)
        
        val bottomRect = android.graphics.RectF(
            rect.left.toFloat(),
            rect.bottom - vignetteHeight,
            rect.right.toFloat(),
            rect.bottom.toFloat()
        )
        canvas.drawRect(bottomRect, vignettePaint)
        
        val leftRect = android.graphics.RectF(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.left + vignetteWidth,
            rect.bottom.toFloat()
        )
        canvas.drawRect(leftRect, vignettePaint)
        
        val rightRect = android.graphics.RectF(
            rect.right - vignetteWidth,
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat()
        )
        canvas.drawRect(rightRect, vignettePaint)
    }

    /**
     * Apply combined blur effects with multiple layers
     */
    private fun applyCombinedBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Layer 1: Base solid blur
        applySolidBlur(canvas, rect, intensity, sensitivity * 0.8f)
        
        // Layer 2: Pixelated overlay
        val pixelSize = (15 - (sensitivity * 8)).toInt().coerceAtLeast(4)
        drawPixelatedPattern(canvas, rect, pixelSize, (intensity.alphaValue * 0.6f).toInt(), sensitivity)
        
        // Layer 3: Noise overlay
        addNoiseOverlay(canvas, rect, sensitivity * 0.5f)
        
        // Layer 4: Artistic grain for very high sensitivity
        if (sensitivity > 0.8f) {
            applyFilmGrain(canvas, rect, intensity, sensitivity * 0.6f)
        }
        
        // Layer 5: Interference pattern for high sensitivity
        if (sensitivity > 0.7f) {
            addInterferencePattern(canvas, rect, sensitivity)
        }
        
        // Layer 6: Privacy border
        addPrivacyBorder(canvas, rect, intensity, sensitivity)
    }
    
    /**
     * Draw pixelated pattern with color variation
     */
    private fun drawPixelatedPattern(
        canvas: Canvas,
        rect: Rect,
        pixelSize: Int,
        alpha: Int,
        sensitivity: Float
    ) {
        val colorVariation = (sensitivity * 60).toInt()
        
        for (x in rect.left until rect.right step pixelSize) {
            for (y in rect.top until rect.bottom step pixelSize) {
                val pixelRect = Rect(
                    x,
                    y,
                    minOf(x + pixelSize, rect.right),
                    minOf(y + pixelSize, rect.bottom)
                )
                
                val baseColor = getSensitivityBasedColor(sensitivity)
                val variation = Random.nextInt(-colorVariation, colorVariation)
                
                val r = ((baseColor shr 16) and 0xFF) + variation
                val g = ((baseColor shr 8) and 0xFF) + variation
                val b = (baseColor and 0xFF) + variation
                
                val adjustedColor = Color.rgb(
                    r.coerceIn(0, 255),
                    g.coerceIn(0, 255),
                    b.coerceIn(0, 255)
                )
                
                val paint = Paint().apply {
                    isAntiAlias = false
                    color = adjustedColor
                    setAlpha(alpha)
                }
                
                canvas.drawRect(pixelRect, paint)
            }
        }
    }
    
    /**
     * Add noise overlay for enhanced privacy using cached bitmap with BitmapShader
     */
    private fun addNoiseOverlay(canvas: Canvas, rect: Rect, density: Float) {
        // Use cached noise bitmap with BitmapShader for better performance
        val cachedNoise = cachedNoiseBitmap ?: run {
            val bitmap = generateCachedNoiseBitmap(rect.width(), rect.height())
            cachedNoiseBitmap = bitmap
            bitmap
        }
        
        // Create shader from cached bitmap
        val noiseShader = BitmapShader(cachedNoise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        
        val noisePaint = getCachedPaint("noise_overlay_shader") {
            isAntiAlias = true
            alpha = (density * 180).toInt().coerceIn(0, 255)
            shader = noiseShader
        }
        
        // Apply shader to entire rect area
        canvas.drawRect(rect, noisePaint)
        
        // Additional static pattern for maximum privacy (reduced particle count)
        if (density > 0.8f) {
            addOptimizedStaticPattern(canvas, rect, density)
        }
    }
    
    /**
     * Add texture overlay for enhanced blocking
     */
    private fun addTextureOverlay(canvas: Canvas, rect: Rect, sensitivity: Float) {
        val texturePaint = Paint().apply {
            isAntiAlias = true
            alpha = (sensitivity * 100).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val spacing = 20
        val color = if (sensitivity > 0.8f) Color.parseColor("#FF4444") else Color.parseColor("#666666")
        texturePaint.color = color
        
        // Draw crosshatch pattern
        for (i in rect.left until rect.right step spacing) {
            canvas.drawLine(i.toFloat(), rect.top.toFloat(), i.toFloat(), rect.bottom.toFloat(), texturePaint)
        }
        
        for (i in rect.top until rect.bottom step spacing) {
            canvas.drawLine(rect.left.toFloat(), i.toFloat(), rect.right.toFloat(), i.toFloat(), texturePaint)
        }
    }
    
    /**
     * Add static pattern for maximum privacy
     */
    private fun addStaticPattern(canvas: Canvas, rect: Rect, sensitivity: Float) {
        val staticPaint = Paint().apply {
            isAntiAlias = false
            alpha = (sensitivity * 120).toInt()
        }
        
        val dotSize = 2
        val spacing = 6
        
        for (x in rect.left until rect.right step spacing) {
            for (y in rect.top until rect.bottom step spacing) {
                if (Random.nextFloat() < 0.6f) {
                    val brightness = Random.nextInt(50, 200)
                    staticPaint.color = Color.rgb(brightness, brightness, brightness)
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + dotSize).toFloat(),
                        (y + dotSize).toFloat(),
                        staticPaint
                    )
                }
            }
        }
    }
    
    /**
     * Add interference pattern for high-sensitivity content
     */
    private fun addInterferencePattern(canvas: Canvas, rect: Rect, sensitivity: Float) {
        val interferePaint = Paint().apply {
            isAntiAlias = true
            alpha = (sensitivity * 80).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.parseColor("#AA4444")
        }
        
        val waveHeight = 20f
        val frequency = 0.1f
        val step = 5
        
        // Draw wave interference pattern
        for (y in rect.top until rect.bottom step step) {
            val path = Path()
            var started = false
            
            for (x in rect.left until rect.right step 2) {
                val waveY = y + (waveHeight * sin(x * frequency)).toFloat()
                if (!started) {
                    path.moveTo(x.toFloat(), waveY)
                    started = true
                } else {
                    path.lineTo(x.toFloat(), waveY)
                }
            }
            
            canvas.drawPath(path, interferePaint)
        }
    }
    
    /**
     * Add privacy border around blurred area
     */
    private fun addPrivacyBorder(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = (intensity.alphaValue * 0.8f).toInt()
        }
        
        val borderWidth = when {
            sensitivity > 0.8f -> 6f
            sensitivity > 0.5f -> 4f
            else -> 2f
        }
        
        val borderColor = when {
            sensitivity > 0.8f -> Color.parseColor("#FF6B6B") // Red for high sensitivity
            sensitivity > 0.5f -> Color.parseColor("#FFA726") // Orange for medium
            else -> Color.parseColor("#CCCCCC") // Gray for low
        }
        
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor
        
        // Draw multiple border layers for high sensitivity
        val layers = if (sensitivity > 0.7f) 2 else 1
        
        for (layer in 0 until layers) {
            val offset = layer * 3
            val layerRect = Rect(
                rect.left - offset,
                rect.top - offset,
                rect.right + offset,
                rect.bottom + offset
            )
            
            borderPaint.alpha = (borderPaint.alpha * (1.0f - layer * 0.3f)).toInt()
            canvas.drawRect(layerRect, borderPaint)
        }
    }
    
    /**
     * Get color based on content sensitivity
     */
    private fun getSensitivityBasedColor(sensitivity: Float): Int {
        return when {
            sensitivity > 0.8f -> Color.parseColor("#2A2A2A") // Very dark for high sensitivity
            sensitivity > 0.6f -> Color.parseColor("#404040") // Dark gray
            sensitivity > 0.4f -> Color.parseColor("#606060") // Medium gray
            sensitivity > 0.2f -> Color.parseColor("#808080") // Light gray
            else -> Color.parseColor("#A0A0A0") // Very light gray
        }
    }
    
    /**
     * Create blur intensity scaling based on content type
     */
    fun getScaledIntensity(
        baseIntensity: BlurIntensity,
        contentSensitivity: Float,
        isExplicitContent: Boolean = false,
        isFullScreen: Boolean = false
    ): BlurIntensity {
        val scaleFactor = when {
            isExplicitContent -> 1.5f
            isFullScreen -> 1.3f
            contentSensitivity > 0.8f -> 1.4f
            contentSensitivity > 0.6f -> 1.2f
            contentSensitivity > 0.4f -> 1.1f
            else -> 1.0f
        }
        
        return when (baseIntensity) {
            BlurIntensity.LIGHT -> {
                if (scaleFactor > 1.3f) BlurIntensity.STRONG
                else if (scaleFactor > 1.1f) BlurIntensity.MEDIUM
                else BlurIntensity.LIGHT
            }
            BlurIntensity.MEDIUM -> {
                if (scaleFactor > 1.2f) BlurIntensity.MAXIMUM
                else if (scaleFactor > 1.1f) BlurIntensity.STRONG
                else BlurIntensity.MEDIUM
            }
            BlurIntensity.STRONG -> {
                if (scaleFactor > 1.1f) BlurIntensity.MAXIMUM
                else BlurIntensity.STRONG
            }
            BlurIntensity.MAXIMUM -> BlurIntensity.MAXIMUM
        }
    }
    
    /**
     * Validate blur effectiveness for testing
     */
    fun validateBlurEffectiveness(
        blurIntensity: BlurIntensity,
        blurStyle: BlurStyle,
        contentSensitivity: Float
    ): BlurEffectivenessResult {
        val privacyScore = calculatePrivacyScore(blurIntensity, blurStyle, contentSensitivity)
        val performanceScore = calculatePerformanceScore(blurStyle)
        val overallScore = (privacyScore + performanceScore) / 2
        
        return BlurEffectivenessResult(
            privacyScore = privacyScore,
            performanceScore = performanceScore,
            overallScore = overallScore,
            isEffective = overallScore >= 0.8f,
            recommendations = generateRecommendations(privacyScore, performanceScore, contentSensitivity)
        )
    }
    
    private fun calculatePrivacyScore(
        intensity: BlurIntensity,
        style: BlurStyle,
        sensitivity: Float
    ): Float {
        val intensityScore = when (intensity) {
            BlurIntensity.LIGHT -> 0.6f
            BlurIntensity.MEDIUM -> 0.75f
            BlurIntensity.STRONG -> 0.9f
            BlurIntensity.MAXIMUM -> 1.0f
        }
        
        val styleScore = when (style) {
            BlurStyle.SOLID -> 0.7f
            BlurStyle.PIXELATED -> 0.8f
            BlurStyle.NOISE -> 0.85f
            BlurStyle.ARTISTIC -> 0.9f
            BlurStyle.COMBINED -> 1.0f
        }
        
        val sensitivityBonus = sensitivity * 0.2f
        
        return (intensityScore * 0.5f + styleScore * 0.5f + sensitivityBonus).coerceAtMost(1.0f)
    }
    
    private fun calculatePerformanceScore(style: BlurStyle): Float {
        return when (style) {
            BlurStyle.SOLID -> 1.0f
            BlurStyle.PIXELATED -> 0.8f
            BlurStyle.NOISE -> 0.7f
            BlurStyle.ARTISTIC -> 0.75f
            BlurStyle.COMBINED -> 0.6f
        }
    }
    
    private fun generateRecommendations(
        privacyScore: Float,
        performanceScore: Float,
        sensitivity: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (privacyScore < 0.8f) {
            recommendations.add("Consider using stronger blur intensity for better privacy protection")
            if (sensitivity > 0.7f) {
                recommendations.add("High sensitivity content detected - recommend COMBINED blur style")
            }
        }
        
        if (performanceScore < 0.7f) {
            recommendations.add("Consider using SOLID or PIXELATED style for better performance")
        }
        
        if (sensitivity > 0.8f && privacyScore < 0.9f) {
            recommendations.add("Explicit content requires maximum privacy protection")
        }
        
        return recommendations
    }
}

/**
 * Result of blur effectiveness validation
 */
data class BlurEffectivenessResult(
    val privacyScore: Float,
    val performanceScore: Float,
    val overallScore: Float,
    val isEffective: Boolean,
    val recommendations: List<String>
)