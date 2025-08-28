package com.hieltech.haramblur.ui.components

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Centralized animation configuration system for production-ready performance optimization.
 * Defines performance-aware timing constants and provides system integration for optimal animation behavior.
 */
object AnimationConfig {

    // Performance-aware timing constants (optimized for production)
    const val FAST_DURATION = 150
    const val STANDARD_DURATION = 250
    const val SLOW_DURATION = 400
    const val MICRO_INTERACTION_DURATION = 100

    // Infinite animation durations (reduced for better performance)
    const val PULSE_DURATION = 1000
    const val SHIMMER_DURATION = 800
    const val ROTATION_DURATION = 600

    // Stagger delays (reduced for smoother experience)
    const val STAGGER_DELAY_FAST = 30
    const val STAGGER_DELAY_STANDARD = 50
    const val STAGGER_DELAY_SLOW = 80

    // Scale factors for micro-interactions
    const val MICRO_INTERACTION_SCALE = 0.98f
    const val PRESS_SCALE = 0.97f
    const val BOUNCE_SCALE = 1.1f

    // Animation performance levels based on device capabilities
    enum class AnimationPerformanceLevel {
        HIGH,      // Full animations with all effects
        MEDIUM,    // Reduced complexity animations
        LOW,       // Minimal animations only
        DISABLED   // No animations (accessibility preference)
    }

    // Optimized easing curves for different interaction types
    val MICRO_INTERACTION_EASING = FastOutSlowInEasing
    val CONTENT_TRANSITION_EASING = EaseOutCubic
    val SPRING_CONFIG = SpringSpec<Float>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )
    val FAST_SPRING_CONFIG = SpringSpec<Float>(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    /**
     * Get optimal animation duration based on performance level
     */
    fun getOptimalDuration(baseMs: Int, level: AnimationPerformanceLevel): Int {
        return when (level) {
            AnimationPerformanceLevel.HIGH -> baseMs
            AnimationPerformanceLevel.MEDIUM -> (baseMs * 0.8f).toInt()
            AnimationPerformanceLevel.LOW -> (baseMs * 0.6f).toInt()
            AnimationPerformanceLevel.DISABLED -> 0
        }.coerceAtLeast(0)
    }

    /**
     * Check if animations should be used based on system preferences
     */
    fun shouldUseAnimation(context: Context): Boolean {
        return try {
            val animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            animatorScale > 0f
        } catch (e: Exception) {
            // Default to enabled if we can't read the setting
            true
        }
    }

    /**
     * Get device performance level based on hardware capabilities
     */
    fun getDevicePerformanceLevel(context: Context): AnimationPerformanceLevel {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemoryMB = runtime.totalMemory() / (1024 * 1024)
            val availableProcessors = runtime.availableProcessors()
            val androidVersion = Build.VERSION.SDK_INT

            // Performance scoring based on device specs
            val memoryScore = when {
                totalMemoryMB >= 4096 -> 3  // 4GB+ RAM
                totalMemoryMB >= 2048 -> 2  // 2GB+ RAM
                else -> 1
            }

            val cpuScore = when {
                availableProcessors >= 8 -> 3  // Octa-core or better
                availableProcessors >= 4 -> 2  // Quad-core or better
                else -> 1
            }

            val versionScore = when {
                androidVersion >= 33 -> 3  // Android 13+
                androidVersion >= 30 -> 2  // Android 11+
                else -> 1
            }

            val totalScore = memoryScore + cpuScore + versionScore

            when {
                totalScore >= 8 -> AnimationPerformanceLevel.HIGH
                totalScore >= 5 -> AnimationPerformanceLevel.MEDIUM
                totalScore >= 3 -> AnimationPerformanceLevel.LOW
                else -> AnimationPerformanceLevel.LOW
            }
        } catch (e: Exception) {
            // Default to medium performance if detection fails
            AnimationPerformanceLevel.MEDIUM
        }
    }

    /**
     * Get optimized animation spec based on performance level
     */
    fun getOptimizedAnimationSpec(
        durationMs: Int,
        level: AnimationPerformanceLevel,
        easing: Easing = MICRO_INTERACTION_EASING
    ): AnimationSpec<Float> {
        val optimalDuration = getOptimalDuration(durationMs, level)

        return if (optimalDuration == 0) {
            snap()
        } else {
            tween(
                durationMillis = optimalDuration,
                easing = easing
            )
        }
    }

    /**
     * Get optimized spring spec based on performance level
     */
    fun getOptimizedSpringSpec(
        level: AnimationPerformanceLevel,
        dampingRatio: Float = 0.75f,
        stiffness: Float = 400f
    ): SpringSpec<Float> {
        return when (level) {
            AnimationPerformanceLevel.HIGH -> SpringSpec(
                dampingRatio = dampingRatio,
                stiffness = stiffness
            )
            AnimationPerformanceLevel.MEDIUM -> SpringSpec(
                dampingRatio = 0.6f,
                stiffness = 800f
            )
            AnimationPerformanceLevel.LOW -> SpringSpec(
                dampingRatio = 0.5f,
                stiffness = 800f
            )
            AnimationPerformanceLevel.DISABLED -> SpringSpec(
                dampingRatio = 0.75f,
                stiffness = 800f
            )
        }
    }
}

/**
 * Composable wrapper that provides performance-aware animation configuration
 */
@Composable
fun rememberAnimationConfig(): AnimationConfigData {
    val context = LocalContext.current
    return remember {
        AnimationConfigData(
            performanceLevel = AnimationConfig.getDevicePerformanceLevel(context),
            animationsEnabled = AnimationConfig.shouldUseAnimation(context)
        )
    }
}

/**
 * Data class containing animation configuration for current context
 */
data class AnimationConfigData(
    val performanceLevel: AnimationConfig.AnimationPerformanceLevel,
    val animationsEnabled: Boolean
) {

    /**
     * Get optimal duration for current performance level
     */
    fun getOptimalDuration(baseMs: Int): Int {
        return AnimationConfig.getOptimalDuration(baseMs, performanceLevel)
    }

    /**
     * Get optimized animation spec for current performance level
     */
    fun getOptimizedAnimationSpec(
        durationMs: Int,
        easing: Easing = AnimationConfig.MICRO_INTERACTION_EASING
    ): AnimationSpec<Float> {
        return AnimationConfig.getOptimizedAnimationSpec(durationMs, performanceLevel, easing)
    }

    /**
     * Get optimized spring spec for current performance level
     */
    fun getOptimizedSpringSpec(
        dampingRatio: Float = 0.75f,
        stiffness: Float = 400f
    ): SpringSpec<Float> {
        return AnimationConfig.getOptimizedSpringSpec(performanceLevel, dampingRatio, stiffness)
    }

    /**
     * Check if animations should run
     */
    fun shouldAnimate(): Boolean {
        return animationsEnabled && performanceLevel != AnimationConfig.AnimationPerformanceLevel.DISABLED
    }
}

// Debug flags for animation performance monitoring and A/B testing
object AnimationDebugFlags {
    const val ENABLE_PERFORMANCE_LOGGING = false
    const val ENABLE_ANIMATION_PROFILING = false
    const val ENABLE_MEMORY_MONITORING = false
    const val FORCE_LOW_PERFORMANCE_MODE = false
    const val FORCE_HIGH_PERFORMANCE_MODE = false
}
