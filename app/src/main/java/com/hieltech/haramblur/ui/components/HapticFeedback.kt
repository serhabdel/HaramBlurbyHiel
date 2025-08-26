package com.hieltech.haramblur.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Haptic feedback utilities for enhanced user experience
 */
object HapticFeedback {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Light feedback for button presses and interactions
     */
    fun performLightFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    /**
     * Medium feedback for important actions
     */
    fun performMediumFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }

    /**
     * Heavy feedback for critical actions
     */
    fun performHeavyFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }

    /**
     * Success feedback pattern
     */
    fun performSuccessFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 50, 50, 50)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        }
    }

    /**
     * Error feedback pattern
     */
    fun performErrorFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 100, 50, 100)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
        }
    }

    /**
     * Custom pattern for Islamic-themed feedback
     */
    fun performIslamicPatternFeedback(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern inspired by Islamic geometric rhythm
                val pattern = longArrayOf(0, 80, 40, 80, 40, 120)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 40, 80, 40, 120), -1)
            }
        }
    }
}

/**
 * Composable function to provide haptic feedback utilities
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return HapticFeedbackManager(context)
}

/**
 * Haptic feedback manager for use in composables
 */
class HapticFeedbackManager(private val context: Context) {

    fun light() = HapticFeedback.performLightFeedback(context)
    fun medium() = HapticFeedback.performMediumFeedback(context)
    fun heavy() = HapticFeedback.performHeavyFeedback(context)
    fun success() = HapticFeedback.performSuccessFeedback(context)
    fun error() = HapticFeedback.performErrorFeedback(context)
    fun islamic() = HapticFeedback.performIslamicPatternFeedback(context)
}

/**
 * Enhanced button with haptic feedback
 */
@Composable
fun HapticButton(
    onClick: () -> Unit,
    hapticType: HapticType = HapticType.LIGHT,
    content: @Composable () -> Unit
) {
    val hapticManager = rememberHapticFeedback()

    androidx.compose.material3.Button(
        onClick = {
            when (hapticType) {
                HapticType.LIGHT -> hapticManager.light()
                HapticType.MEDIUM -> hapticManager.medium()
                HapticType.HEAVY -> hapticManager.heavy()
                HapticType.SUCCESS -> hapticManager.success()
                HapticType.ERROR -> hapticManager.error()
                HapticType.ISLAMIC -> hapticManager.islamic()
            }
            onClick()
        }
    ) {
        content()
    }
}

/**
 * Enhanced card with haptic feedback
 */
@Composable
fun HapticCard(
    onClick: (() -> Unit)? = null,
    hapticType: HapticType = HapticType.LIGHT,
    content: @Composable () -> Unit
) {
    val hapticManager = rememberHapticFeedback()

    ModernCard(
        onClick = if (onClick != null) {
            {
                when (hapticType) {
                    HapticType.LIGHT -> hapticManager.light()
                    HapticType.MEDIUM -> hapticManager.medium()
                    HapticType.HEAVY -> hapticManager.heavy()
                    HapticType.SUCCESS -> hapticManager.success()
                    HapticType.ERROR -> hapticManager.error()
                    HapticType.ISLAMIC -> hapticManager.islamic()
                }
                onClick()
            }
        } else null
    ) {
        content()
    }
}

enum class HapticType {
    LIGHT, MEDIUM, HEAVY, SUCCESS, ERROR, ISLAMIC
}
