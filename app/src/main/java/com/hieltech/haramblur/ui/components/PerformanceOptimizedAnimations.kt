package com.hieltech.haramblur.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedPulse(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(modifier = Modifier.alpha(scale)) {
        content()
    }
}

@Composable
fun AnimatedCounterText(
    targetValue: Float,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    color: Color = Color.Unspecified,
    durationMillis: Int = 1000,
    formatter: (Float) -> String = { it.toString() }
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis)
    )

    Text(
        text = formatter(animatedValue),
        modifier = modifier,
        style = style,
        color = color
    )
}

@Composable
fun ProgressBarAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000)
    )

    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier,
        color = color
    )
}

@Composable
fun AnimatedSlideInFromBottom(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }) + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}
