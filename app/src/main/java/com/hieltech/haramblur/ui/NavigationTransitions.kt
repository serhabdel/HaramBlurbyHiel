package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * Smooth slide transition for screen navigation
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideTransition(
    targetState: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            // Determine direction based on indices
            val direction = if (targetState > initialState) 1 else -1
            
            slideInHorizontally(
                animationSpec = tween(300, easing = EaseOutCubic),
                initialOffsetX = { fullWidth -> direction * fullWidth }
            ) + fadeIn(
                animationSpec = tween(300)
            ) with
            slideOutHorizontally(
                animationSpec = tween(300, easing = EaseOutCubic),
                targetOffsetX = { fullWidth -> -direction * fullWidth }
            ) + fadeOut(
                animationSpec = tween(300)
            )
        },
        label = "slide_transition"
    ) { state ->
        content(state)
    }
}

/**
 * Fade scale transition for cards and content
 */
@Composable
fun FadeScaleTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(400, delayMillis = delayMillis)
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = fadeOut(
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(300)
        )
    ) {
        content()
    }
}

/**
 * Staggered list item animation
 */
@Composable
fun StaggeredListItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delay = index * 50
    
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(400, delayMillis = delay)
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(400, delayMillis = delay, easing = EaseOutCubic)
        )
    ) {
        content()
    }
}

/**
 * Page transition for onboarding/pager
 */
@Composable
fun OnboardingPageTransition(
    targetState: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            
            (slideInHorizontally(
                animationSpec = tween(400, easing = EaseOutQuart),
                initialOffsetX = { fullWidth -> direction * fullWidth }
            ) + fadeIn(animationSpec = tween(400))) togetherWith
            (slideOutHorizontally(
                animationSpec = tween(400, easing = EaseOutQuart),
                targetOffsetX = { fullWidth -> -direction * fullWidth }
            ) + fadeOut(animationSpec = tween(400)))
        },
        label = "onboarding_page"
    ) { state ->
        content(state)
    }
}

/**
 * Success/checkmark animation
 */
@Composable
fun SuccessCheckAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "success_alpha"
    )
    
    // Implementation would use these animated values
    // For actual usage, wrap content with these modifiers
}

/**
 * Pulse animation for attention
 */
@Composable
fun PulseAnimation(
    content: @Composable (scale: Float, alpha: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    content(scale, 1f)
}

// Easing curves
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
