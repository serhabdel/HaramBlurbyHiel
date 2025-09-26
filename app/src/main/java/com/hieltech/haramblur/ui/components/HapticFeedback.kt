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
     * Light feedback for button presses and interactions - DISABLED
     */
    fun performLightFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
    }

    /**
     * Medium feedback for important actions - DISABLED
     */
    fun performMediumFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
    }

    /**
     * Heavy feedback for critical actions - DISABLED
     */
    fun performHeavyFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
    }

    /**
     * Success feedback pattern - DISABLED
     */
    fun performSuccessFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
    }

    /**
     * Error feedback pattern - DISABLED
     */
    fun performErrorFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
    }

    /**
     * Custom pattern for Islamic-themed feedback - DISABLED
     */
    fun performIslamicPatternFeedback(context: Context) {
        // Vibration disabled - no haptic feedback
        return
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
