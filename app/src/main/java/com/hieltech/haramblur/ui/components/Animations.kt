package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Smooth enter/exit animations for content
 */
@Composable
fun AnimatedFadeIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis, easing = EaseOut)),
        exit = fadeOut(animationSpec = tween(durationMillis, easing = EaseIn)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Slide in from bottom with fade
 */
@Composable
fun AnimatedSlideInFromBottom(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 400,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis, easing = EaseOut)
        ) + fadeIn(animationSpec = tween(durationMillis, easing = EaseOut)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis, easing = EaseIn)
        ) + fadeOut(animationSpec = tween(durationMillis, easing = EaseIn)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Scale and fade animation for interactive elements
 */
@Composable
fun AnimatedScaleOnClick(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .animateContentSize()
    ) {
        content()
    }
}

/**
 * Staggered animation for lists
 */
@Composable
fun <T> AnimatedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Int) -> Unit
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val delay = index * 100L // 100ms delay between items

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = delay.toInt(),
                        easing = EaseOut
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = delay.toInt(),
                        easing = EaseOut
                    )
                )
            ) {
                itemContent(item, index)
            }
        }
    }
}

/**
 * Pulse animation for status indicators
 */
@Composable
fun AnimatedPulse(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = modifier.alpha(alpha)) {
        content()
    }
}

/**
 * Shimmer loading effect
 */
@Composable
fun AnimatedShimmer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    Box(modifier = modifier) {
        content()
        // Add shimmer overlay effect
    }
}

/**
 * Bounce animation for success states
 */
@Composable
fun AnimatedBounceIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (visible) 1f else 0f)
    ) {
        content()
    }
}

/**
 * Rotation animation for loading states
 */
@Composable
fun AnimatedRotation(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = if (isLoading) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(300, easing = EaseOut)
        },
        label = "rotation"
    )

    Box(modifier = modifier.animateContentSize()) {
        content()
    }
}

/**
 * Expandable content animation
 */
@Composable
fun AnimatedExpandableContent(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = tween(300, easing = EaseOut),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(300, easing = EaseOut)),
        exit = shrinkVertically(
            animationSpec = tween(200, easing = EaseIn),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(200, easing = EaseIn)),
        modifier = modifier
    ) {
        content()
    }
}
