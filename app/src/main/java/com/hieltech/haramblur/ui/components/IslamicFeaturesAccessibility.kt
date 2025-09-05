package com.hieltech.haramblur.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_ANNOUNCEMENT

/**
 * Accessibility helpers for Islamic features (stub for future expansion).
 */
object IslamicFeaturesAccessibility {
    @Composable
    fun announce(message: String) {
        val view = LocalView.current
        view.announceForAccessibility(message)
    }

    fun contentDescriptionForQibla(angleTo: Float, aligned: Boolean): String {
        return if (aligned) "Qibla aligned" else "Qibla angle ${"%.0f".format(angleTo)} degrees"
    }
}
