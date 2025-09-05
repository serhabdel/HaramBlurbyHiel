package com.hieltech.haramblur.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Simple placeholder for Islamic features theming. Hooks into Material3.
 */
@Composable
fun IslamicFeaturesTheme(content: @Composable () -> Unit) {
    // Reuse app theme for now; future: provide custom color/typography if needed
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
