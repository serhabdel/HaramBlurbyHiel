package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

@Composable
fun IslamicFeaturesShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shiftAnim"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(shift - 1000f, 0f),
        end = androidx.compose.ui.geometry.Offset(shift, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun PrayerTimesLoadingSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().height(20.dp))
            Row(Modifier.padding(top = 12.dp)) {
                IslamicFeaturesShimmer(Modifier.weight(1f).height(60.dp))
                IslamicFeaturesShimmer(Modifier.weight(1f).padding(start = 8.dp).height(60.dp))
            }
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().padding(top = 12.dp).height(140.dp))
        }
    }
}

@Composable
fun QiblaCompassLoadingSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IslamicFeaturesShimmer(Modifier.height(24.dp).fillMaxWidth())
            IslamicFeaturesShimmer(Modifier.padding(top = 12.dp).height(200.dp).fillMaxWidth())
        }
    }
}

@Composable
fun IslamicCalendarLoadingSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().height(20.dp))
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().padding(top = 12.dp).height(40.dp))
        }
    }
}

@Composable
fun LocationStatusLoadingSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().height(20.dp))
            IslamicFeaturesShimmer(Modifier.fillMaxWidth().padding(top = 8.dp).height(16.dp))
        }
    }
}
