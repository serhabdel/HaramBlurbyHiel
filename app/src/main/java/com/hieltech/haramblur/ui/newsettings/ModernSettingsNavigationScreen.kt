package com.hieltech.haramblur.ui.newsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.SettingsCategory
import com.hieltech.haramblur.data.models.QuickSetting
import com.hieltech.haramblur.data.models.RecentSetting
import com.hieltech.haramblur.data.models.StatusIndicator
import com.hieltech.haramblur.data.models.SystemHealth
import com.hieltech.haramblur.data.models.SystemStatus
import com.hieltech.haramblur.ui.components.HapticFeedback
import com.hieltech.haramblur.ui.components.SettingsMode
import com.hieltech.haramblur.ui.components.SettingsModeToggle
import com.hieltech.haramblur.ui.newsettings.EnhancedSearchBar
import com.hieltech.haramblur.ui.newsettings.ContextualHelpButton
import com.hieltech.haramblur.ui.newsettings.SettingsTipCard
import com.hieltech.haramblur.ui.settings.SettingsViewModel as NavigationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsHomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (SettingsCategory) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    settingsMode: SettingsMode,
    onSettingsModeChange: (SettingsMode) -> Unit,
    viewModel: NavigationSettingsViewModel = hiltViewModel()
) {
    val systemStatus by viewModel.systemStatus.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val recentSettings by viewModel.recentSettings.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current

    Scaffold(
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernSettingsHeader(
                        title = stringResource(R.string.settings_title),
                        subtitle = stringResource(R.string.nav_general_description),
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = {
                        HapticFeedback.performLightFeedback(context)
                        viewModel.refreshSystemStatus()
                    }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                }
            }

            // Removed search bar and settings mode toggle as they're not functional yet
            // These features can be added back when properly implemented

            item {
                StatusOverviewSection(
                    systemStatus = systemStatus,
                    systemHealth = systemHealth,
                    isLoading = isLoading,
                    errorMessage = error,
                    onRetry = viewModel::refreshSystemStatus
                )
            }

            if (quickSettings.isNotEmpty()) {
                item {
                    QuickSettingsSection(
                        quickSettings = quickSettings,
                        onToggle = { id -> viewModel.toggleQuickSetting(id) }
                    )
                }
            }

            item {
                CategoriesSection(onNavigateToCategory = onNavigateToCategory)
            }

            if (recentSettings.isNotEmpty()) {
                item {
                    RecentSettingsSection(recentSettings = recentSettings)
                }
            }
        }
    }
}

@Composable
private fun StatusOverviewSection(
    systemStatus: SystemStatus,
    systemHealth: SystemHealth,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.protection_stats),
        subtitle = stringResource(R.string.total_blocked),
        icon = Icons.Default.Settings,
        showHelp = true,
        helpTitle = "Protection Overview",
        helpDescription = "Monitor your protection status, detection sensitivity, and system health here.",
        headerContent = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    ) {
        when {
            errorMessage != null -> {
                // Simplified error display to avoid @Composable context issues
                Text(
                    text = "Error: $errorMessage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                EnhancedStatusSummaryItem(
                    label = stringResource(R.string.protection_stats),
                    value = if (systemStatus.protectionEnabled) {
                        stringResource(R.string.protection_active)
                    } else {
                        stringResource(R.string.protection_paused)
                    },
                    status = if (systemStatus.protectionEnabled) StatusIndicator.ENABLED else StatusIndicator.WARNING,
                    isLoading = isLoading
                )
                EnhancedStatusSummaryItem(
                    label = stringResource(R.string.detection_sensitivity_title),
                    value = "${systemStatus.dailyFacesDetected + systemStatus.dailySitesBlocked}",
                    status = StatusIndicator.ENABLED,
                    isLoading = isLoading
                )
                EnhancedStatusSummaryItem(
                    label = stringResource(R.string.warnings),
                    value = systemHealth.getHealthStatus(),
                    status = if (systemHealth.getIssues().isNotEmpty()) StatusIndicator.WARNING else StatusIndicator.ENABLED,
                    isLoading = isLoading
                )

                val issues = systemHealth.getIssues()
                if (issues.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.warnings),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        issues.take(3).forEach { issue ->
                            Text(
                                text = issue.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Note: Removed SettingsTipCard to avoid @Composable context issues
            }
        }
    }
}

@Composable
private fun EnhancedStatusSummaryItem(
    label: String,
    value: String,
    status: StatusIndicator = StatusIndicator.ENABLED,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 300)
    )

    val statusColor = when (status) {
        StatusIndicator.ENABLED -> MaterialTheme.colorScheme.primary
        StatusIndicator.WARNING -> MaterialTheme.colorScheme.tertiary
        StatusIndicator.ERROR -> MaterialTheme.colorScheme.error
        StatusIndicator.PENDING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (status == StatusIndicator.ERROR) 4.dp else 1.dp,
        color = if (status == StatusIndicator.ERROR)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = statusColor
                    )
                } else {
                    Icon(
                        imageVector = when (status) {
                            StatusIndicator.ENABLED -> Icons.Default.Settings
                            StatusIndicator.WARNING -> Icons.Default.Settings
                            StatusIndicator.ERROR -> Icons.Default.Settings
                            StatusIndicator.PENDING -> Icons.Default.Settings
                            else -> Icons.Default.Settings
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsSection(
    quickSettings: List<QuickSetting>,
    onToggle: (String) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.quick_actions),
        subtitle = stringResource(R.string.recent_activity),
        icon = Icons.Default.Settings
    ) {
        // Use Material 3 grid layout for quick actions
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quickSettings.take(4)) { setting ->
                Material3QuickActionCard(
                    setting = setting,
                    onToggle = { onToggle(setting.id) }
                )
            }
        }
    }
}

@Composable
private fun Material3QuickActionCard(
    setting: QuickSetting,
    onToggle: () -> Unit
) {
    val isEnabled = setting.statusIndicator == StatusIndicator.ENABLED
    
    Card(
        onClick = { if (setting.canToggle()) onToggle() },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (setting.statusIndicator) {
                StatusIndicator.ENABLED -> MaterialTheme.colorScheme.primaryContainer
                StatusIndicator.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                StatusIndicator.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = setting.iconRes),
                contentDescription = setting.displayName,
                modifier = Modifier.size(24.dp),
                tint = when (setting.statusIndicator) {
                    StatusIndicator.ENABLED -> MaterialTheme.colorScheme.primary
                    StatusIndicator.WARNING -> MaterialTheme.colorScheme.tertiary
                    StatusIndicator.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EnhancedQuickSettingChip(
    setting: QuickSetting,
    onToggle: (String) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (setting.statusIndicator == StatusIndicator.PENDING) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200)
    )

    val statusColor = when (setting.statusIndicator) {
        StatusIndicator.ENABLED -> MaterialTheme.colorScheme.primary
        StatusIndicator.WARNING -> MaterialTheme.colorScheme.tertiary
        StatusIndicator.ERROR -> MaterialTheme.colorScheme.error
        StatusIndicator.PENDING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    val backgroundColor = when (setting.statusIndicator) {
        StatusIndicator.ENABLED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        StatusIndicator.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        StatusIndicator.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        StatusIndicator.PENDING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    AssistChip(
        onClick = {
            if (setting.canToggle()) onToggle(setting.id)
        },
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = setting.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = setting.iconRes),
                    contentDescription = setting.displayName,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            leadingIconContentColor = statusColor,
            containerColor = backgroundColor
        ),
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

@Composable
private fun CategoriesSection(
    onNavigateToCategory: (SettingsCategory) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.settings),
        subtitle = stringResource(R.string.nav_detection_description),
        icon = Icons.Default.Settings
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NavigationCategory.values().forEach { category ->
                EnhancedCategoryRow(
                    category = category,
                    onClick = { onNavigateToCategory(category.category) }
                )
            }
        }
    }
}

private enum class NavigationCategory(
    val category: SettingsCategory,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    GENERAL(SettingsCategory.ESSENTIAL, R.string.nav_general_title, R.string.nav_general_description, Icons.Default.Settings),
    DETECTION(SettingsCategory.DETECTION, R.string.nav_detection_title, R.string.nav_detection_description, Icons.Default.Search),
    PERFORMANCE(SettingsCategory.PERFORMANCE, R.string.nav_performance_title, R.string.nav_performance_description, Icons.Default.Build),
    ISLAMIC(SettingsCategory.ISLAMIC, R.string.nav_islamic_title, R.string.nav_islamic_description, Icons.Default.Settings),
    ADVANCED(SettingsCategory.AI, R.string.nav_advanced_title, R.string.nav_advanced_description, Icons.Default.Settings)
}

@Composable
private fun EnhancedCategoryRow(
    category: NavigationCategory,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = tween(durationMillis = 200)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale),
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = stringResource(category.titleRes),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(category.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentSettingsSection(
    recentSettings: List<RecentSetting>
) {
    ModernSettingsSection(
        title = stringResource(R.string.recent_activity),
        subtitle = stringResource(R.string.quick_actions),
        icon = Icons.Default.Settings
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            recentSettings.take(5).forEach { setting ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = setting.settingName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = setting.getChangeDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
