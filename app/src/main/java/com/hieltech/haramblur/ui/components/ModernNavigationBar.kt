package com.hieltech.haramblur.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState

/**
 * Modern Islamic-inspired navigation bar with smooth animations
 */
@Composable
fun ModernNavigationBar(
    currentRoute: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToBlockApps: () -> Unit = {},
    onNavigateToBlockSites: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navigationItems = listOf(
        NavigationItem(
            route = "home",
            label = "Home",
            icon = Icons.Default.CheckCircle,
            selectedIcon = Icons.Default.CheckCircle,
            description = "Home screen"
        ),
        NavigationItem(
            route = "block_apps",
            label = "Block Apps",
            icon = Icons.Default.Lock,
            selectedIcon = Icons.Default.Lock,
            description = "Block applications"
        ),
        NavigationItem(
            route = "block_sites",
            label = "Block Sites",
            icon = Icons.Default.Warning,
            selectedIcon = Icons.Default.Warning,
            description = "Block websites"
        ),
        NavigationItem(
            route = "support",
            label = "Support",
            icon = Icons.Default.Info,
            selectedIcon = Icons.Default.Info,
            description = "Get help and support"
        ),
        NavigationItem(
            route = "logs",
            label = "Logs",
            icon = Icons.Default.Search,
            selectedIcon = Icons.Default.Search,
            description = "View activity logs"
        )
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.route

            ModernNavigationBarItem(
                item = item,
                isSelected = isSelected,
                onClick = {
                    when (item.route) {
                        "home" -> onNavigateToHome()
                        "block_apps" -> onNavigateToBlockApps()
                        "block_sites" -> onNavigateToBlockSites()
                        "support" -> onNavigateToSupport()
                        "logs" -> onNavigateToLogs()
                    }
                }
            )
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
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300, easing = EaseOut),
        label = "iconColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(300, easing = EaseOut),
        label = "backgroundColor"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.description,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        label = {
            AnimatedFadeIn(visible = isSelected) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
    onNavigateToBlockApps: () -> Unit = {},
    onNavigateToBlockSites: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navigationItems = listOf(
        NavigationItem(
            route = "home",
            label = "Home",
            icon = Icons.Default.CheckCircle,
            selectedIcon = Icons.Default.CheckCircle,
            description = "Home screen"
        ),
        NavigationItem(
            route = "block_apps",
            label = "Block Apps",
            icon = Icons.Default.Lock,
            selectedIcon = Icons.Default.Lock,
            description = "Block applications"
        ),
        NavigationItem(
            route = "block_sites",
            label = "Block Sites",
            icon = Icons.Default.Warning,
            selectedIcon = Icons.Default.Warning,
            description = "Block websites"
        ),
        NavigationItem(
            route = "support",
            label = "Support",
            icon = Icons.Default.Info,
            selectedIcon = Icons.Default.Info,
            description = "Get help and support"
        ),
        NavigationItem(
            route = "logs",
            label = "Logs",
            icon = Icons.Default.Search,
            selectedIcon = Icons.Default.Search,
            description = "View activity logs"
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.route

            FloatingNavigationItem(
                item = item,
                isSelected = isSelected,
                onClick = {
                    when (item.route) {
                        "home" -> onNavigateToHome()
                        "block_apps" -> onNavigateToBlockApps()
                        "block_sites" -> onNavigateToBlockSites()
                        "support" -> onNavigateToSupport()
                        "logs" -> onNavigateToLogs()
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
            .height(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.description,
                modifier = Modifier.size(16.dp)
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
