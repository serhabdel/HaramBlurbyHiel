package com.hieltech.haramblur.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.hieltech.haramblur.detection.PerformanceState
import com.hieltech.haramblur.ui.StatsViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt
import com.hieltech.haramblur.data.LogRepository

/**
 * Performance banner showing current performance state with optimized animations
 */
@Composable
fun PerformanceBanner(
    state: StatsViewModel.DashboardState,
    modifier: Modifier = Modifier
) {
    val (backgroundColors, statusIcon, statusText) = when (state.performanceState) {
        PerformanceState.OPTIMAL -> Triple(
            listOf(
                Color(0xFF4CAF50).copy(alpha = 0.1f),
                Color(0xFF2E7D32).copy(alpha = 0.05f)
            ),
            "🟢",
            "Optimal Performance"
        )
        PerformanceState.DEGRADED -> Triple(
            listOf(
                Color(0xFFFF9800).copy(alpha = 0.1f),
                Color(0xFFF57C00).copy(alpha = 0.05f)
            ),
            "🟡",
            "Degraded Performance"
        )
        PerformanceState.WARNING -> Triple(
            listOf(
                Color(0xFFFF9800).copy(alpha = 0.1f),
                Color(0xFFF57C00).copy(alpha = 0.05f)
            ),
            "🟠",
            "Performance Warning"
        )
        PerformanceState.CRITICAL -> Triple(
            listOf(
                Color(0xFFD32F2F).copy(alpha = 0.1f),
                Color(0xFFB71C1C).copy(alpha = 0.05f)
            ),
            "🔴",
            "Critical Performance"
        )
    }

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = backgroundColors,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status indicator with optimized pulse
            AnimatedPulse {
                Text(
                    text = statusIcon,
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedFadeIn(
                    visible = true,
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Performance metrics with staggered animation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedFadeIn(
                        visible = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        MetricChip(
                            label = "Avg Time",
                            value = "${state.currentMetrics.avgProcessingTimeMs.toInt()}ms",
                            icon = "⚡"
                        )
                    }

                    AnimatedFadeIn(
                        visible = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        MetricChip(
                            label = "Violation Rate",
                            value = "${(state.currentMetrics.recentViolationRate * 100).toInt()}%",
                            icon = "⚠️"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metric chip for displaying individual metrics with micro-interactions and animations
 */
@Composable
fun MetricChip(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    trend: Float? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    MicroInteractionScale(
        isPressed = isPressed,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedFadeIn(
                visible = true,
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Use AnimatedCounterText for numeric values
                if (value.all { it.isDigit() || it == '.' || it == '%' || it == 'm' || it == 's' }) {
                    val numericValue = value.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
                    AnimatedCounterText(
                        targetValue = numericValue,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        formatter = { "${it.toInt()}${value.filterNot { it.isDigit() || it == '.' }}" }
                    )
                } else {
                    AnimatedFadeIn(
                        visible = true,
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                AnimatedFadeIn(
                    visible = true,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trend indicator with smooth animations
            trend?.let {
                AnimatedFadeIn(
                    visible = true,
                ) {
                    TrendIndicator(trend = it)
                }
            }
        }
    }
}

/**
 * Summary card for daily/weekly statistics with staggered entrance animations
 */
@Composable
fun SummaryCard(
    title: String,
    summary: LogRepository.DetectionSummary,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedFadeIn(
                visible = true,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Metrics grid with staggered animations
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                items(listOf(
                    Triple("Detections", summary.totalDetections.toString(), "🔍"),
                    Triple("Avg Time", "${summary.averageProcessingTime.toInt()}ms", "⏱️"),
                    Triple("Faces", summary.faceDetections.toString(), "👤"),
                    Triple("NSFW", summary.nsfwDetections.toString(), "🚫")
                )) { (label, value, icon) ->
                    AnimatedFadeIn(
                        visible = true,
                        modifier = Modifier.fillParentMaxWidth()
                    ) {
                        MetricChip(
                            label = label,
                            value = value,
                            icon = icon
                        )
                    }
                }
            }

            // Performance score with smooth animations
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedFadeIn(
                    visible = true,
                ) {
                    Text(
                        text = "Performance Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                AnimatedFadeIn(
                    visible = true,
                    modifier = Modifier.weight(1f)
                ) {
                    ProgressBarAnimation(
                        progress = summary.performanceScore,
                        color = when {
                            summary.performanceScore >= 80f -> Color(0xFF4CAF50)
                            summary.performanceScore >= 60f -> Color(0xFFFF9800)
                            else -> Color(0xFFD32F2F)
                        }
                    )
                }

                AnimatedCounterText(
                    targetValue = summary.performanceScore,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    formatter = { "${it.toInt()}%" }
                )
            }
        }
    }
}

/**
 * Timeline chart using Compose Canvas
 */
@Composable
fun TimelineChart(
    data: List<LogRepository.TimelinePoint>,
    modifier: Modifier = Modifier,
    timeRange: String = "24h"
) {
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Detection Timeline ($timeRange)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ChartCanvas(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

/**
 * Canvas-based chart implementation
 */
@Composable
private fun ChartCanvas(
    data: List<LogRepository.TimelinePoint>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        // Calculate chart bounds
        val padding = 40f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Find data ranges
        val maxDetections = data.maxOfOrNull { it.detectionCount } ?: 0
        val minTime = data.minOfOrNull { it.timestamp } ?: 0L
        val maxTime = data.maxOfOrNull { it.timestamp } ?: 0L

        if (maxDetections == 0 || maxTime == minTime) return@Canvas

        // Draw axes
        val axisColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

        // X-axis
        drawLine(
            color = axisColor,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )

        // Y-axis
        drawLine(
            color = axisColor,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )

        // Draw grid lines
        val gridColor = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        for (i in 0..5) {
            val y = padding + (chartHeight * i / 5)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw data points
        val points = mutableListOf<Offset>()

        data.forEachIndexed { index, point ->
            val x = padding + (chartWidth * (point.timestamp - minTime) / (maxTime - minTime))
            val y = height - padding - (chartHeight * point.detectionCount / maxDetections)

            points.add(Offset(x, y))

            // Draw point
            drawCircle(
                color = colorScheme.primary,
                radius = 4f,
                center = Offset(x, y)
            )
        }

        // Draw line connecting points
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = colorScheme.primary,
                style = Stroke(width = 3f)
            )
        }
    }
}

/**
 * Stats grid arranging multiple metric chips
 */
@Composable
fun StatsGrid(
    state: StatsViewModel.DashboardState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Real-time Metrics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricChip(
                label = "Total Measurements",
                value = state.currentMetrics.totalMeasurements.toString(),
                icon = "📊",
                modifier = Modifier.weight(1f)
            )

            MetricChip(
                label = "Consecutive Violations",
                value = state.currentMetrics.consecutiveViolations.toString(),
                icon = "⚠️",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricChip(
                label = "Blocked Apps",
                value = state.blockedAppsCount.toString(),
                icon = "📱",
                modifier = Modifier.weight(1f)
            )

            MetricChip(
                label = "Blocked Sites",
                value = state.blockedSitesCount.toString(),
                icon = "🌐",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Trend indicator with smooth arrow rotation and color transitions
 */
@Composable
fun TrendIndicator(
    trend: Float,
    modifier: Modifier = Modifier
) {
    val (targetRotation, color) = when {
        trend > 0f -> 0f to Color(0xFFD32F2F) // Red for increase (worse)
        trend < 0f -> 180f to Color(0xFF4CAF50) // Green for decrease (better)
        else -> 90f to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(),
        label = "trendRotation"
    )

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(500),
        label = "trendColor"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "↑",
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor,
            modifier = Modifier.rotate(rotation)
        )

        AnimatedCounterText(
            targetValue = kotlin.math.abs(trend),
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor,
            formatter = { "${it.toInt()}%" }
        )
    }
}

/**
 * Loading shimmer specifically for dashboard cards with optimized duration
 */
@Composable
fun DashboardLoadingShimmer(
    modifier: Modifier = Modifier
) {
    LoadingShimmer(
        modifier = modifier
    )
}

/**
 * Optimized chart canvas with viewport culling and progressive rendering
 */
@Composable
private fun OptimizedChartCanvas(
    data: List<LogRepository.TimelinePoint>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        // Calculate chart bounds with optimized padding
        val padding = 32f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Find data ranges
        val maxDetections = data.maxOfOrNull { it.detectionCount } ?: 0
        val minTime = data.minOfOrNull { it.timestamp } ?: 0L
        val maxTime = data.maxOfOrNull { it.timestamp } ?: 0L

        if (maxDetections == 0 || maxTime == minTime) return@Canvas

        // Progressive rendering - limit points based on performance level
        val maxPoints = 100
        val step = maxOf(1, data.size / maxPoints)
        val renderData = data.filterIndexed { index, _ -> index % step == 0 }.take(maxPoints)

        // Viewport culling - only render visible data points
        val visibleRect = Rect(0f, 0f, width, height)

        val points = renderData.mapNotNull { point ->
            val x = padding + (chartWidth * (point.timestamp - minTime) / (maxTime - minTime))
            val y = height - padding - (chartHeight * point.detectionCount / maxDetections)

            val pointOffset = Offset(x, y)
            if (visibleRect.contains(pointOffset)) pointOffset else null
        }

        // Draw axes with optimized colors

        drawLine(
            color = axisColor,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )

        drawLine(
            color = axisColor,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )

        // Draw data points with optimized rendering
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 3f,
                    center = point
                )
            }
        }
    }
}

/**
 * Progressive chart loading that renders elements in stages
 */
@Composable
fun ProgressiveChartLoading(
    data: List<LogRepository.TimelinePoint>,
    modifier: Modifier = Modifier,
    timeRange: String = "24h"
) {
    var renderStage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Stage 1: Basic structure
        renderStage = 1
        kotlinx.coroutines.delay(50)

        // Stage 2: Data points
        renderStage = 2
        kotlinx.coroutines.delay(50)

        // Stage 3: Full rendering
        renderStage = 3
    }

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedFadeIn(
                visible = renderStage >= 1,
            ) {
                Text(
                    text = "Detection Timeline ($timeRange)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (data.isEmpty()) {
                AnimatedFadeIn(
                    visible = renderStage >= 1,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AnimatedFadeIn(
                    visible = renderStage >= 2,
                ) {
                    OptimizedChartCanvas(
                        data = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }
    }
}

/**
 * Smart recomposition optimization that batches dashboard updates
 */
@Composable
fun SmartRecomposition(
    state: StatsViewModel.DashboardState,
    modifier: Modifier = Modifier,
    content: @Composable (StatsViewModel.DashboardState) -> Unit
) {
    // Use derivedStateOf to batch related state updates
    val optimizedState by remember(state) {
        derivedStateOf {
            // Only trigger recomposition when significant changes occur
            state.copy(
                currentMetrics = state.currentMetrics.copy(
                    // Round values to reduce recomposition frequency
                    avgProcessingTimeMs = (state.currentMetrics.avgProcessingTimeMs * 10).roundToInt() / 10.0,
                    recentViolationRate = (state.currentMetrics.recentViolationRate * 100).roundToInt() / 100f
                )
            )
        }
    }

    // Use animateContentSize for smooth layout changes
    Box(modifier = modifier) {
        content(optimizedState)
    }
}
