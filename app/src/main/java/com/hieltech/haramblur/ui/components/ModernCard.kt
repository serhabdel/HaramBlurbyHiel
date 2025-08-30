package com.hieltech.haramblur.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Status types for status cards
 */
enum class StatusType {
    SUCCESS, WARNING, ERROR, INFO, NORMAL
}

/**
 * Modern Islamic-inspired card with gradient backgrounds and smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    gradientColors: List<Color>? = null,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val config = rememberAnimationConfig()
    val hapticManager = rememberHapticFeedback()
    var isPressed by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) AnimationConfig.PRESS_SCALE else 1f,
        animationSpec = spring(),
        label = "cardScale"
    )

    val elevationAnimated by animateDpAsState(
        targetValue = when {
            isPressed -> elevation * 0.5f
            isHovered -> elevation * 1.2f
            else -> elevation
        },
        animationSpec = spring(),
        label = "cardElevation"
    )

    val animatedGradientColors by remember(gradientColors, isHovered) {
        derivedStateOf {
            gradientColors?.map { color ->
                if (isHovered) {
                    color.copy(alpha = (color.alpha + 0.1f).coerceAtMost(1f))
                } else {
                    color
                }
            }
        }
    }

    val cardModifier = modifier
        .shadow(
            elevation = elevationAnimated,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .scale(scale)
        .then(
            when {
                animatedGradientColors != null -> Modifier.background(
                    Brush.verticalGradient(animatedGradientColors!!)
                )
                backgroundColor != null -> Modifier.background(backgroundColor)
                else -> Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
            }
        )
        .then(
            if (onClick != null) {
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                    isPressed = true
                                    if (config.shouldAnimate()) {
                                        hapticManager.light()
                                    }
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                    isPressed = false
                                    onClick()
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Exit -> {
                                    isPressed = false
                                    isHovered = false
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Enter -> {
                                    isHovered = true
                                }
                            }
                        }
                    }
                }
            } else {
                Modifier
            }
        )
        .then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}


/**
 * Feature card with Islamic geometric styling
 */
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Quick action button card
 */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Compact quick action card for horizontal layouts
 */
@Composable
fun CompactQuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact feature card for horizontal layouts
 */
@Composable
fun CompactFeatureCard(
    title: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 3
            )
        }
    }
}



/**
 * AnimatedCard variant with entrance animations using optimized timing
 */
@Composable
fun AnimatedCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    gradientColors: List<Color>? = null,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    AnimatedFadeIn(
        visible = visible,
        modifier = modifier,
        durationMillis = AnimationConfig.STANDARD_DURATION
    ) {
        ModernCard(
            onClick = onClick,
            elevation = elevation,
            cornerRadius = cornerRadius,
            gradientColors = gradientColors,
            backgroundColor = backgroundColor,
            contentPadding = contentPadding,
            contentDescription = contentDescription,
            content = content
        )
    }
}

/**
 * ExpandableCard with smooth expand/collapse animations using performance-aware timing
 */
@Composable
fun ExpandableCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    gradientColors: List<Color>? = null,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    headerContent: @Composable () -> Unit,
    expandableContent: @Composable () -> Unit
) {
    ModernCard(
        modifier = modifier,
        onClick = onToggle,
        elevation = elevation,
        cornerRadius = cornerRadius,
        gradientColors = gradientColors,
        backgroundColor = backgroundColor,
        contentPadding = contentPadding
    ) {
        Column {
            headerContent()
            AnimatedExpandableContent(expanded = expanded) {
                expandableContent()
            }
        }
    }
}

/**
 * LoadingCard variant with optimized shimmer effect using reduced animation duration
 */
@Composable
fun LoadingCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    contentHeight: Dp = 120.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    ModernCard(
        modifier = modifier.height(contentHeight),
        elevation = elevation,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding
    ) {
        LoadingShimmer(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Enhanced StatusCard with smoother status transitions and icon animations
 */
@Composable
fun StatusCard(
    title: String,
    subtitle: String? = null,
    status: StatusType = StatusType.NORMAL,
    icon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val config = rememberAnimationConfig()

    val statusColors = when (status) {
        StatusType.SUCCESS -> listOf(
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF2E7D32).copy(alpha = 0.05f)
        )
        StatusType.WARNING -> listOf(
            Color(0xFFFF9800).copy(alpha = 0.1f),
            Color(0xFFF57C00).copy(alpha = 0.05f)
        )
        StatusType.ERROR -> listOf(
            Color(0xFFD32F2F).copy(alpha = 0.1f),
            Color(0xFFB71C1C).copy(alpha = 0.05f)
        )
        StatusType.INFO -> listOf(
            Color(0xFF1976D2).copy(alpha = 0.1f),
            Color(0xFF1565C0).copy(alpha = 0.05f)
        )
        StatusType.NORMAL -> listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(),
        label = "statusScale"
    )

    ModernCard(
        modifier = modifier.fillMaxWidth().scale(scale),
        gradientColors = statusColors,
        onClick = onClick,
        contentPadding = PaddingValues(20.dp),
        contentDescription = "$title status: $status"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                AnimatedFadeIn(
                    visible = true,
                    durationMillis = AnimationConfig.FAST_DURATION
                ) {
                    icon()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedFadeIn(
                    visible = true,
                    durationMillis = AnimationConfig.STANDARD_DURATION
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (subtitle != null) {
                    AnimatedFadeIn(
                        visible = true,
                        durationMillis = AnimationConfig.STANDARD_DURATION
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * NeumorphismCard variant with subtle shadow animations for modern UI feel
 */
@Composable
fun NeumorphismCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val config = rememberAnimationConfig()
    val hapticManager = rememberHapticFeedback()
    var isPressed by remember { mutableStateOf(false) }

    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(),
        label = "neumorphismShadow"
    )

    val cardModifier = modifier
        .shadow(
            elevation = shadowOffset,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = Color.Black.copy(alpha = 0.15f),
            ambientColor = Color.White.copy(alpha = 0.1f)
        )
        .background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(cornerRadius)
        )
        .then(
            if (onClick != null) {
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                    isPressed = true
                                    if (config.shouldAnimate()) {
                                        hapticManager.light()
                                    }
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                    isPressed = false
                                    onClick()
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Exit -> {
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
            } else {
                Modifier
            }
        )
        .then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            }
        )

    Box(modifier = cardModifier) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
