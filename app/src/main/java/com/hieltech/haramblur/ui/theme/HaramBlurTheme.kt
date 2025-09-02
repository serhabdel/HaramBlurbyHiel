package com.hieltech.haramblur.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hieltech.haramblur.data.AppTheme
import com.hieltech.haramblur.detection.Language

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

// Modern Light Color Scheme
private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF1D192B),

    secondary = Color(0xFF03DAC6),
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF9DF7E8),
    onSecondaryContainer = Color(0xFF6200EE),

    tertiary = Color(0xFF018786),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9EF2F0),
    onTertiaryContainer = Color(0xFF00201E),

    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF37000B),

    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF6200EE),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF)
)

// Modern Dark Color Scheme
private val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9EF2F0),

    tertiary = Color(0xFF4DD0E1),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF9EF2F0),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFCD8DF),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFD0BCFF),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6200EE)
)

// Minimal Light Color Scheme
private val MinimalLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF1C1B1F),

    secondary = Color(0xFF404040),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF000000),

    tertiary = Color(0xFF606060),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5F5F5),
    onTertiaryContainer = Color(0xFF1C1B1F),

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4EC),
    onErrorContainer = Color(0xFF1C1B1F),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF404040),
    surfaceTint = Color(0xFF000000),

    outline = Color(0xFF909090),
    outlineVariant = Color(0xFFD0D0D0),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1C1B1F),
    inverseOnSurface = Color(0xFFE0E0E0),
    inversePrimary = Color(0xFF606060)
)

// Minimal Dark Color Scheme
private val MinimalDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF1C1B1F),
    primaryContainer = Color(0xFF404040),
    onPrimaryContainer = Color(0xFFE0E0E0),

    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF1C1B1F),
    secondaryContainer = Color(0xFF606060),
    onSecondaryContainer = Color(0xFFE0E0E0),

    tertiary = Color(0xFF909090),
    onTertiary = Color(0xFF1C1B1F),
    tertiaryContainer = Color(0xFF303030),
    onTertiaryContainer = Color(0xFFE0E0E0),

    error = Color(0xFFEF5350),
    onError = Color(0xFF1C1B1F),
    errorContainer = Color(0xFF5D1A1A),
    onErrorContainer = Color(0xFFE0E0E0),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceTint = Color(0xFFFFFFFF),

    outline = Color(0xFF606060),
    outlineVariant = Color(0xFF404040),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF909090)
)

@Composable
fun HaramBlurTheme(
    appTheme: AppTheme = AppTheme.ISLAMIC_LIGHT,
    preferredLanguage: Language = Language.ENGLISH,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.ISLAMIC_LIGHT -> LightColorScheme
        AppTheme.ISLAMIC_DARK -> DarkColorScheme
        AppTheme.MODERN_LIGHT -> ModernLightColorScheme
        AppTheme.MODERN_DARK -> ModernDarkColorScheme
        AppTheme.MINIMAL_LIGHT -> MinimalLightColorScheme
        AppTheme.MINIMAL_DARK -> MinimalDarkColorScheme
    }

    // Determine layout direction based on language
    val layoutDirection = if (preferredLanguage.isRTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}