package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive design utilities for different screen sizes
 */
enum class ScreenSize {
    COMPACT,    // Small phones
    MEDIUM,     // Large phones, small tablets
    EXPANDED    // Large tablets, foldables, desktop
}

/**
 * Get current screen size category
 */
@Composable
fun getScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth < 600 -> ScreenSize.COMPACT
        screenWidth < 840 -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
}

/**
 * Get responsive spacing based on screen size
 */
@Composable
fun responsiveSpacing(
    compact: Dp = 8.dp,
    medium: Dp = 12.dp,
    expanded: Dp = 16.dp
): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> medium
        ScreenSize.EXPANDED -> expanded
    }
}

/**
 * Get responsive padding for content
 */
@Composable
fun responsiveContentPadding(): PaddingValues {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ScreenSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ScreenSize.EXPANDED -> PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    }
}

/**
 * Get responsive grid columns
 */
@Composable
fun responsiveGridColumns(): Int {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 1
        ScreenSize.MEDIUM -> 2
        ScreenSize.EXPANDED -> 3
    }
}


/**
 * Get responsive text sizes
 */
@Composable
fun responsiveHeadlineSize(): TextUnit {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 24.sp
        ScreenSize.MEDIUM -> 28.sp
        ScreenSize.EXPANDED -> 32.sp
    }
}

/**
 * Responsive layout for feature grids
 */
@Composable
fun ResponsiveFeatureGrid(
    features: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    val columns = responsiveGridColumns()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
    ) {
        features.chunked(columns).forEach { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing())
            ) {
                rowFeatures.forEach { feature ->
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        feature()
                    }
                }
                // Add empty boxes to fill remaining space in the row
                repeat(columns - rowFeatures.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Responsive layout for action grids
 */
@Composable
fun ResponsiveActionGrid(
    actions: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    // Actions should feel compact on phones: 2 per row minimum
    val columns = when (getScreenSize()) {
        ScreenSize.COMPACT -> 2
        ScreenSize.MEDIUM -> 3
        ScreenSize.EXPANDED -> 4
    }
    val rows = (actions.size + columns - 1) / columns

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 12.dp, medium = 16.dp, expanded = 20.dp))
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing())
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < actions.size) {
                        Box(modifier = Modifier.weight(1f)) {
                            actions[index]()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get responsive emoji text size
 */
@Composable
fun responsiveEmojiSize(): TextUnit {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 32.sp
        ScreenSize.MEDIUM -> 36.sp
        ScreenSize.EXPANDED -> 40.sp
    }
}

/**
 * Get responsive card padding
 */
@Composable
fun responsiveCardPadding(): PaddingValues {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> PaddingValues(16.dp)
        ScreenSize.MEDIUM -> PaddingValues(20.dp)
        ScreenSize.EXPANDED -> PaddingValues(24.dp)
    }
}

/**
 * Get responsive icon size
 */
@Composable
fun responsiveIconSize(): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 32.dp
        ScreenSize.MEDIUM -> 36.dp
        ScreenSize.EXPANDED -> 40.dp
    }
}

/**
 * Get responsive max content width
 */
@Composable
fun responsiveMaxContentWidth(maxExpanded: Dp = 840.dp): Modifier {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> Modifier.fillMaxWidth()
        ScreenSize.MEDIUM -> Modifier.fillMaxWidth(0.9f).widthIn(max = maxExpanded)
        ScreenSize.EXPANDED -> Modifier.fillMaxWidth(0.7f).widthIn(max = maxExpanded)
    }
}

/**
 * Get responsive card elevation based on screen size
 */
@Composable
fun responsiveCardElevation(): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 4.dp
        ScreenSize.MEDIUM -> 6.dp
        ScreenSize.EXPANDED -> 8.dp
    }
}

/**
 * Get responsive corner radius for consistent rounded corners
 */
@Composable
fun responsiveCornerRadius(): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 12.dp
        ScreenSize.MEDIUM -> 16.dp
        ScreenSize.EXPANDED -> 20.dp
    }
}

/**
 * Get responsive button height for consistent button sizing
 */
@Composable
fun responsiveButtonHeight(): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> 40.dp
        ScreenSize.MEDIUM -> 44.dp
        ScreenSize.EXPANDED -> 48.dp
    }
}

/**
 * Get responsive button padding for consistent button sizing
 */
@Composable
fun responsiveButtonPadding(): PaddingValues {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ScreenSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ScreenSize.EXPANDED -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    }
}

/**
 * Get responsive grid spacing for different content types
 */
@Composable
fun responsiveGridSpacing(
    compact: Dp = 8.dp,
    medium: Dp = 12.dp,
    expanded: Dp = 16.dp
): Dp {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> medium
        ScreenSize.EXPANDED -> expanded
    }
}

/**
 * Get responsive animation durations based on screen size and performance
 */
@Composable
fun responsiveAnimationDuration(
    baseDuration: Int = 250,
    performanceMultiplier: Float = 1f
): Int {
    val screenMultiplier = when (getScreenSize()) {
        ScreenSize.COMPACT -> 0.8f
        ScreenSize.MEDIUM -> 1f
        ScreenSize.EXPANDED -> 1.2f
    }
    return (baseDuration * screenMultiplier * performanceMultiplier).toInt()
}

/**
 * Get responsive typography scale for different text types
 */
@Composable
fun responsiveTextSize(
    compact: TextUnit = 14.sp,
    medium: TextUnit = 16.sp,
    expanded: TextUnit = 18.sp
): TextUnit {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> medium
        ScreenSize.EXPANDED -> expanded
    }
}

/**
 * Get responsive layout margins for consistent page margins
 */
@Composable
fun responsiveLayoutMargins(): PaddingValues {
    return when (getScreenSize()) {
        ScreenSize.COMPACT -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ScreenSize.MEDIUM -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ScreenSize.EXPANDED -> PaddingValues(horizontal = 32.dp, vertical = 16.dp)
    }
}

/**
 * Get responsive breakpoint utilities for conditional rendering
 */
@Composable
fun isCompactScreen(): Boolean = getScreenSize() == ScreenSize.COMPACT

@Composable
fun isMediumScreen(): Boolean = getScreenSize() == ScreenSize.MEDIUM

@Composable
fun isExpandedScreen(): Boolean = getScreenSize() == ScreenSize.EXPANDED

@Composable
fun isTabletOrLarger(): Boolean = getScreenSize() != ScreenSize.COMPACT

@Composable
fun isLargeScreen(): Boolean = getScreenSize() == ScreenSize.EXPANDED
