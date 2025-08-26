package com.hieltech.haramblur.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Islamic-inspired Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),           // Islamic Emerald
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),

    secondary = Color(0xFF4CAF50),         // Light Islamic Emerald
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF2E7D32),

    tertiary = Color(0xFFFFD700),          // Islamic Gold
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFF5D4037),

    error = Color(0xFFD32F2F),             // Islamic Red
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    background = Color(0xFFFEFEFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFEFEFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF2E7D32),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFF8DD99F)
)

// Islamic-inspired Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8DD99F),           // Light Islamic Emerald
    onPrimary = Color(0xFF003912),
    primaryContainer = Color(0xFF145C24),
    onPrimaryContainer = Color(0xFFA5D6A7),

    secondary = Color(0xFFACCFA6),         // Muted Islamic Emerald
    onSecondary = Color(0xFF21361F),
    secondaryContainer = Color(0xFF374B35),
    onSecondaryContainer = Color(0xFFC8E6C9),

    tertiary = Color(0xFFFFD700),          // Islamic Gold (kept bright for dark mode)
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = Color(0xFFFFDAB9),

    error = Color(0xFFFFB4AB),             // Light Islamic Red
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFF8DD99F),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF2E7D32)
)

@Composable
fun HaramBlurTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}