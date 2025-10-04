package com.hieltech.haramblur.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.compass.SensorAccuracy
import com.hieltech.haramblur.data.compass.CompassSize
import com.hieltech.haramblur.data.compass.QiblaCompassData
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hieltech.haramblur.data.LocationMethod
import com.hieltech.haramblur.data.LocationPermissionStatus
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.responsiveSpacing
import com.hieltech.haramblur.ui.components.responsiveCardPadding
import com.hieltech.haramblur.ui.components.getScreenSize
import com.hieltech.haramblur.ui.components.ScreenSize

/**
 * Interactive Qibla Compass Widget
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassWidget(
    modifier: Modifier = Modifier,
    // Settings-driven behavior
    showDegreeMarkings: Boolean = true,
    hapticOnAligned: Boolean = false,
    alignmentToleranceDeg: Float = 5f,
    animationSpeed: Float = 1.0f,
    preferredSize: CompassSize = CompassSize.MEDIUM,
    viewModel: QiblaCompassViewModel = hiltViewModel(),
    // Enhancements
    showLoading: Boolean = false,
    error: IslamicErrorState = IslamicErrorState.NoError,
    onRetry: (() -> Unit)? = null
) {
    val isPreview = LocalInspectionMode.current

    // Permission/state guard using SettingsViewModel
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val permissionGranted = settings.locationPermissionStatus == LocationPermissionStatus.GRANTED || settings.locationMethod == LocationMethod.MANUAL_CITY

    // Start/stop compass based on lifecycle when permitted
    LaunchedEffect(permissionGranted) {
        if (!isPreview && permissionGranted) viewModel.startCompass()
    }
    DisposableEffect(Unit) {
        onDispose { if (!isPreview) viewModel.stopCompass() }
    }

    val state by viewModel.state.collectAsState()

    // Compose effective error considering permissions
    val effectiveError = when {
        !permissionGranted -> IslamicErrorState.PermissionError("Location permission required for compass.")
        else -> error
    }

    IslamicFeaturesErrorBoundary(errorState = effectiveError, onRetry = {
        settingsViewModel.syncLocationPermissionStatus()
        onRetry?.invoke()
    }) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                // Enhanced header with gradient background
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🧭",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(id = R.string.qibla_direction_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showLoading) {
                    QiblaCompassLoadingSkeleton(modifier = Modifier.fillMaxWidth())
                } else {
                    CompassDial(
                        state = state,
                        showDegreeMarkings = showDegreeMarkings,
                        hapticOnAligned = hapticOnAligned,
                        alignmentToleranceDeg = alignmentToleranceDeg,
                        animationSpeed = animationSpeed,
                        dialSize = getResponsiveCompassSize(preferredSize)
                    )
                }

                // Sensor accuracy hint - Enhanced display
                val accText = when (state.compassState.sensorAccuracy) {
                    SensorAccuracy.UNRELIABLE -> stringResource(R.string.qibla_accuracy_unreliable)
                    SensorAccuracy.LOW -> stringResource(R.string.qibla_accuracy_low)
                    SensorAccuracy.MEDIUM -> stringResource(R.string.qibla_accuracy_medium)
                    SensorAccuracy.HIGH -> stringResource(R.string.qibla_accuracy_high)
                    SensorAccuracy.UNAVAILABLE -> stringResource(R.string.qibla_accuracy_unavailable)
                }
                val accColor = when (state.compassState.sensorAccuracy) {
                    SensorAccuracy.HIGH -> MaterialTheme.colorScheme.primary
                    SensorAccuracy.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    SensorAccuracy.LOW -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.error
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "📡",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accuracy: $accText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = accColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompassDial(
    state: QiblaCompassData,
    showDegreeMarkings: Boolean,
    hapticOnAligned: Boolean,
    alignmentToleranceDeg: Float,
    animationSpeed: Float,
    dialSize: Dp
) {
    val haptic = LocalHapticFeedback.current

    // Animated rotation of the needle pointing to Qibla relative to device heading
    val angleTo = state.compassState.angleToQibla
    val animatedAngle by animateFloatAsState(
        targetValue = angleTo,
        animationSpec = tween(
            durationMillis = (300 / animationSpeed.coerceAtLeast(0.1f)).toInt(),
            easing = LinearEasing
        ), label = "angleAnim"
    )

    // Haptic feedback disabled - no vibration feedback
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(angleTo, alignmentToleranceDeg) {
        val aligned = kotlin.math.abs(angleTo) <= alignmentToleranceDeg
        // Haptic feedback removed to disable vibrations
        wasAligned = aligned
    }

    val strokeColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val aligned = kotlin.math.abs(state.compassState.angleToQibla) <= alignmentToleranceDeg

    Card(
        modifier = Modifier.size(dialSize),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .semantics {
                    contentDescription = IslamicFeaturesAccessibility.contentDescriptionForQibla(
                        angleTo = state.compassState.angleToQibla,
                        aligned = aligned
                    )
                },
            contentAlignment = Alignment.Center
        ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val drawWidth = size.width
            val drawHeight = size.height
            val radius = (kotlin.math.min(drawWidth, drawHeight) / 2f) - 24.dp.toPx()
            val center = Offset(drawWidth / 2f, drawHeight / 2f)

            // Outer circle
            drawCircle(
                color = outlineColor,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Degree ticks
            if (showDegreeMarkings) {
                val longTick = 14.dp.toPx()
                val shortTick = 7.dp.toPx()
                for (deg in 0 until 360 step 5) {
                    val rad = Math.toRadians(deg.toDouble()).toFloat()
                    val isCardinal = deg % 90 == 0
                    val isMajor = deg % 30 == 0
                    val tickLen = when {
                        isCardinal -> longTick
                        isMajor -> (longTick * 0.75f)
                        else -> shortTick
                    }
                    val outer = Offset(
                        x = center.x + radius * kotlin.math.cos(rad.toDouble()).toFloat(),
                        y = center.y + radius * kotlin.math.sin(rad.toDouble()).toFloat()
                    )
                    val inner = Offset(
                        x = center.x + (radius - tickLen) * kotlin.math.cos(rad.toDouble()).toFloat(),
                        y = center.y + (radius - tickLen) * kotlin.math.sin(rad.toDouble()).toFloat()
                    )
                    drawLine(
                        color = tickColor,
                        start = inner,
                        end = outer,
                        strokeWidth = if (isMajor || isCardinal) 2f else 1f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Qibla needle: draw a triangle-like pointer; angleTo<0 => rotate right
            val needleLen = radius * 0.85f
            val angleRad = Math.toRadians(animatedAngle.toDouble() - 90).toFloat() // -90 to make 0 deg point up
            val tip = Offset(
                x = center.x + needleLen * kotlin.math.cos(angleRad.toDouble()).toFloat(),
                y = center.y + needleLen * kotlin.math.sin(angleRad.toDouble()).toFloat()
            )
            // Tail
            val tail = Offset(
                x = center.x - needleLen * 0.25f * kotlin.math.cos(angleRad.toDouble()).toFloat(),
                y = center.y - needleLen * 0.25f * kotlin.math.sin(angleRad.toDouble()).toFloat()
            )
            // Mid offsets for width
            val perp = angleRad + (Math.PI / 2).toFloat()
            val halfWidth = 8.dp.toPx()
            val left = Offset(
                x = tail.x + halfWidth * kotlin.math.cos(perp.toDouble()).toFloat(),
                y = tail.y + halfWidth * kotlin.math.sin(perp.toDouble()).toFloat()
            )
            val right = Offset(
                x = tail.x - halfWidth * kotlin.math.cos(perp.toDouble()).toFloat(),
                y = tail.y - halfWidth * kotlin.math.sin(perp.toDouble()).toFloat()
            )

            // Needle outline
            drawLine(color = strokeColor, start = center, end = tip, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = Color.Red, start = left, end = tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = Color.Red, start = right, end = tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }

            // Alignment banner - Enhanced design
            if (aligned) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.qibla_aligned),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Accessibility announcement when aligned
                IslamicFeaturesAccessibility.announce(stringResource(R.string.qibla_aligned))
            }
        }
    }
}

/**
 * Get responsive compass size based on screen size and user preference
 */
@Composable
private fun getResponsiveCompassSize(preferredSize: CompassSize): Dp {
    val screenSize = getScreenSize()

    return when (preferredSize) {
        CompassSize.SMALL -> when (screenSize) {
            ScreenSize.COMPACT -> 140.dp
            ScreenSize.MEDIUM -> 160.dp
            ScreenSize.EXPANDED -> 180.dp
        }
        CompassSize.MEDIUM -> when (screenSize) {
            ScreenSize.COMPACT -> 180.dp
            ScreenSize.MEDIUM -> 220.dp
            ScreenSize.EXPANDED -> 260.dp
        }
        CompassSize.LARGE -> when (screenSize) {
            ScreenSize.COMPACT -> 220.dp
            ScreenSize.MEDIUM -> 280.dp
            ScreenSize.EXPANDED -> 340.dp
        }
    }
}
