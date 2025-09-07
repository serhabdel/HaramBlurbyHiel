package com.hieltech.haramblur.ui.components

import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R

/**
 * Utility functions for app UI components
 */

/**
 * Get display name for app package
 */
fun getAppDisplayName(packageName: String): String {
    return when {
        packageName.contains("instagram") -> "Instagram"
        packageName.contains("facebook") -> "Facebook"
        packageName.contains("tiktok") || packageName.contains("musically") -> "TikTok"
        packageName.contains("twitter") -> "Twitter"
        packageName.contains("whatsapp") -> "WhatsApp"
        packageName.contains("chrome") -> "Chrome"
        packageName.contains("firefox") -> "Firefox"
        packageName.contains("youtube") -> "YouTube"
        packageName.contains("netflix") -> "Netflix"
        else -> packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
    }
}
