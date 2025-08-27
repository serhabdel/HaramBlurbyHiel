package com.hieltech.haramblur.ui.effects

import android.graphics.*
import android.util.Log
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import kotlin.math.*
import kotlin.random.Random

/**
 * Enhanced blur effects for maximum content blocking and privacy protection
 */
class EnhancedBlurEffects {
    
    companion object {
        private const val TAG = "EnhancedBlurEffects"
        private const val NOISE_DENSITY = 0.4f
        private const val PATTERN_ALPHA_BASE = 150
        private const val LAYERED_BLUR_LAYERS = 3
    }
    
    /**
     * Apply enhanced blur effect based on content sensitivity
     */
    fun applyEnhancedBlur(
        canvas: Canvas,
        rect: Rect,
        blurIntensity: BlurIntensity,
        blurStyle: BlurStyle,
        contentSensitivity: Float = 0.5f // 0.0 = low sensitivity, 1.0 = high sensitivity
    ) {
        when (blurStyle) {
            BlurStyle.SOLID -> applySolidBlur(canvas, rect, blurIntensity, contentSensitivity)
            BlurStyle.PIXELATED -> applyPixelatedBlur(canvas, rect, blurIntensity, contentSensitivity)
            BlurStyle.NOISE -> applyNoiseBlur(canvas, rect, blurIntensity, contentSensitivity)
            BlurStyle.ARTISTIC -> applyArtisticBlur(canvas, rect, blurIntensity, contentSensitivity)
            BlurStyle.COMBINED -> applyCombinedBlur(canvas, rect, blurIntensity, contentSensitivity)
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
    private fun applyPixelatedBlur(
        canvas: Canvas,
        rect: Rect,
        intensity: BlurIntensity,
        sensitivity: Float
    ) {
        // Smaller pixels for higher sensitivity content
        val basePixelSize = when (intensity) {
            BlurIntensity.LIGHT -> 25
            BlurIntensity.MEDIUM -> 20
            BlurIntensity.STRONG -> 15
            BlurIntensity.MAXIMUM -> 10
        }
        
        val pixelSize = (basePixelSize * (1.0f - sensitivity * 0.5f)).toInt().coerceAtLeast(5)
        val alpha = (intensity.alphaValue + (sensitivity * 50)).toInt().coerceAtMost(255)
        
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
     * Add noise overlay for enhanced privacy
     */
    private fun addNoiseOverlay(canvas: Canvas, rect: Rect, density: Float) {
        val noiseSize = 3
        val numNoise = ((rect.width() * rect.height() * density) / (noiseSize * noiseSize)).toInt()
        
        val noisePaint = Paint().apply {
            isAntiAlias = true
            alpha = 180
        }
        
        repeat(numNoise) {
            val x = rect.left + Random.nextInt(rect.width())
            val y = rect.top + Random.nextInt(rect.height())
            val brightness = Random.nextInt(100, 255)
            
            noisePaint.color = Color.rgb(brightness, brightness, brightness)
            canvas.drawCircle(x.toFloat(), y.toFloat(), noiseSize.toFloat(), noisePaint)
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