package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import com.hieltech.haramblur.ui.NavRoutes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Modern navigation drawer content for secondary screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNavigationDrawerContent(
    currentRoute: String? = null,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onCloseDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerItems = listOf(
        DrawerItem(
            route = NavRoutes.SUPPORT,
            label = "Support",
            icon = Icons.Default.Info,
            selectedIcon = Icons.Default.Info,
            description = "Get help and support"
        ),
        DrawerItem(
            route = NavRoutes.LOGS,
            label = "Logs",
            icon = Icons.Default.Search,
            selectedIcon = Icons.Default.Search,
            description = "Application logs & diagnostics"
        ),
        DrawerItem(
            route = NavRoutes.DEBUG,
            label = "Debug",
            icon = Icons.Default.Build,
            selectedIcon = Icons.Default.Build,
            description = "Debug tools and information"
        )
    )

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header section
            DrawerHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation items
            drawerItems.forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.description,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        when (item.route) {
                            "logs" -> onNavigateToLogs()
                            "debug" -> onNavigateToDebug()
                            "support" -> onNavigateToSupport()
                        }
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        unselectedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

/**
 * Drawer header with app branding
 */
@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            )
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Islamic geometric shield icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "HaramBlur",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tools & Support",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data class for drawer navigation items
 */
private data class DrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val description: String
)
