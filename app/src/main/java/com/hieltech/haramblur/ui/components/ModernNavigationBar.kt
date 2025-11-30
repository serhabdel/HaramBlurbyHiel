package com.hieltech.haramblur.ui.components

import androidx.compose.animation.core.*
import com.hieltech.haramblur.ui.NavRoutes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Modern Islamic-inspired navigation bar with smooth animations
 */
@Composable
fun ModernNavigationBar(
    currentRoute: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToBlockAppsSites: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val showLabels = true // Always show labels for better UX

    val navigationItems = listOf(
        NavigationItem(
            route = NavRoutes.HOME,
            label = "Home",
            icon = Icons.Default.Home,
            selectedIcon = Icons.Default.Home,
            description = "Protection overview and dashboard"
        ),
        NavigationItem(
            route = NavRoutes.BLOCK_APPS_SITES,
            label = "Protection",
            icon = Icons.Default.Lock,
            selectedIcon = Icons.Default.Lock,
            description = "Content blocking and filtering"
        ),
        NavigationItem(
            route = NavRoutes.INSIGHTS,
            label = "Insights",
            icon = Icons.Default.Info,
            selectedIcon = Icons.Default.Info,
            description = "Stats and feature status"
        ),
        NavigationItem(
            route = NavRoutes.SETTINGS,
            label = "Settings",
            icon = Icons.Default.Settings,
            selectedIcon = Icons.Default.Settings,
            description = "App configuration and preferences"
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            navigationItems.forEach { item ->
                val isSelected = currentRoute == item.route

                ModernNavigationBarItem(
                    item = item,
                    isSelected = isSelected,
                    showLabel = showLabels,
                    onClick = {
                        when (item.route) {
                            NavRoutes.HOME -> onNavigateToHome()
                            NavRoutes.BLOCK_APPS_SITES -> onNavigateToBlockAppsSites()
                            NavRoutes.INSIGHTS -> onNavigateToInsights()
                            NavRoutes.SETTINGS -> onNavigateToSettings()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual navigation bar item with animations
 */
@Composable
private fun RowScope.ModernNavigationBarItem(
    item: NavigationItem,
    isSelected: Boolean,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "iconColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp) // Even larger touch target for accessibility
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(20.dp)) // Smoother rounded corners
                        .background(
                            if (isSelected) {
                                Brush.radialGradient(
                                    listOf(
                                        backgroundColor,
                                        backgroundColor.copy(alpha = 0.1f)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.description,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp) // Optimal size for visibility
                    )
                }
            }
        },
        label = {
            if (showLabel) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    maxLines = 1
                )
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Transparent, // Handled by our custom icon
            unselectedIconColor = Color.Transparent, // Handled by our custom icon
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent // No default indicator
        )
    )
}

/**
 * Floating action button style navigation for main actions
 */
@Composable
fun FloatingNavigationBar(
    currentRoute: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToBlockAppsSites: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navigationItems = listOf(
        NavigationItem(
            route = NavRoutes.HOME,
            label = "Home",
            icon = Icons.Default.Home,
            selectedIcon = Icons.Default.Home,
            description = "Protection overview and dashboard"
        ),
        NavigationItem(
            route = NavRoutes.BLOCK_APPS_SITES,
            label = "Protection",
            icon = Icons.Default.Lock,
            selectedIcon = Icons.Default.Lock,
            description = "Content blocking and filtering"
        ),
        NavigationItem(
            route = NavRoutes.SETTINGS,
            label = "Settings",
            icon = Icons.Default.Settings,
            selectedIcon = Icons.Default.Settings,
            description = "App configuration and preferences"
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.route

            FloatingNavigationItem(
                item = item,
                isSelected = isSelected,
                onClick = {
                    when (item.route) {
                        NavRoutes.HOME -> onNavigateToHome()
                        NavRoutes.BLOCK_APPS_SITES -> onNavigateToBlockAppsSites()
                        NavRoutes.SETTINGS -> onNavigateToSettings()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Floating navigation item with modern styling
 */
@Composable
private fun FloatingNavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp) // Better height for floating navigation
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.description,
                modifier = Modifier.size(20.dp) // Better size for floating navigation
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Data class for navigation items
 */
private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val description: String
)