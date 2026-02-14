package com.hieltech.haramblur.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.models.RecentSetting
import com.hieltech.haramblur.data.models.QuickSetting
import com.hieltech.haramblur.data.models.SystemHealth
import com.hieltech.haramblur.ui.components.HapticFeedback
import com.hieltech.haramblur.ui.components.SettingsMode
import com.hieltech.haramblur.ui.components.SettingsSearchBar
import com.hieltech.haramblur.ui.components.SettingsModeToggle
import com.hieltech.haramblur.ui.settings.components.*
import com.hieltech.haramblur.data.SettingsCategory


/**
 * Enhanced settings navigation screen with compact header, quick controls, and contextual bottom panel
 * Replaces the large header card with information-dense, contextual interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavigationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    settingsMode: SettingsMode = SettingsMode.SIMPLE,
    onSettingsModeChange: (SettingsMode) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val systemStatus by viewModel.systemStatus.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val recentSettings by viewModel.recentSettings.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = LocalContext.current
    
    // Dialog states
    var showDetailedStats by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(
                        onClick = {
                            HapticFeedback.performLightFeedback(context)
                            viewModel.refreshSystemStatus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Error state
            if (error != null) {
                item {
                    ErrorCard(
                        error = error!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
            
            // Compact status header
            item {
                CompactStatusHeader(
                    systemStatus = systemStatus,
                    onStatusClick = {
                        HapticFeedback.performMediumFeedback(context)
                        showDetailedStats = true
                    },
                    onProtectionToggle = {
                        HapticFeedback.performMediumFeedback(context)
                        viewModel.toggleQuickSetting("protection")
                    }
                )
            }

            // Search bar removed for a cleaner layout

            // Settings mode toggle
            item {
                SettingsModeToggle(
                    currentMode = settingsMode,
                    onModeChange = onSettingsModeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }


            // Enhanced quick toggle row
            item {
                QuickSettingsRow(
                    quickSettings = quickSettings,
                    onToggle = { settingKey ->
                        HapticFeedback.performLightFeedback(context)
                        viewModel.toggleQuickSetting(settingKey)
                    }
                )
            }
            
            // Settings categories grid
            item {
                SettingsCategoriesGrid(
                    onCategoryClick = { category ->
                        HapticFeedback.performLightFeedback(context)
                        onNavigateToCategory(category.name) // Convert SettingsCategory to string
                    }
                )
            }
            
            // Recent settings section
            if (recentSettings.isNotEmpty()) {
                item {
                    RecentSettingsSection(
                        recentSettings = recentSettings,
                        onSettingClick = { setting ->
                            HapticFeedback.performLightFeedback(context)
                            // Navigate to the specific setting
                        }
                    )
                }
            }

            // System health indicator
            item {
                SystemHealthCard(
                    systemHealth = systemHealth,
                    onViewDetails = {
                        HapticFeedback.performMediumFeedback(context)
                        showDetailedStats = true
                    }
                )
            }
        }
    }
    
    // Detailed statistics dialog
    if (showDetailedStats) {
        DetailedStatisticsDialog(
            systemStatus = systemStatus,
            onDismiss = { showDetailedStats = false }
        )
    }
    
    // Export settings dialog
    if (showExportDialog) {
        ExportSettingsDialog(
            onConfirm = {
                // Note: This would need to be handled in a coroutine scope
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
    
    // Reset to defaults dialog
    if (showResetDialog) {
        ResetDefaultsDialog(
            onConfirm = {
                viewModel.resetToDefaults()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
    
    // Apply preset dialog
    if (showPresetDialog) {
        ApplyPresetDialog(
            onDismiss = { showPresetDialog = false }
        )
    }
}

/**
 * Settings categories grid with optimized layout
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SettingsCategoriesGrid(
    onCategoryClick: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = NavigationCategory.values()
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Settings Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Use a non-scrollable grid (FlowRow) to avoid
        // placing a lazy grid inside a LazyColumn, which
        // causes unbounded height constraints and crashes.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            categories.forEach { category ->
                NavigationCategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category.settingsCategory) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Navigation category card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCategoryCard(
    category: NavigationCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Category-specific gradient colors
    val gradientColors = when (category.settingsCategory) {
        SettingsCategory.ISLAMIC -> listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
        SettingsCategory.DETECTION -> listOf(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
        SettingsCategory.PERFORMANCE -> listOf(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
        SettingsCategory.AI -> listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
        else -> listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            // Make categories slimmer; reduce vertical footprint
            .aspectRatio(2.2f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon
                Icon(
                    imageVector = category.icon,
                    contentDescription = stringResource(category.titleResId),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )

                // Name and description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(category.titleResId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(category.descriptionResId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Error card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * Export settings dialog
 */
@Composable
private fun ExportSettingsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Settings") },
        text = { Text("Export your current settings configuration to a file?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reset to defaults dialog
 */
@Composable
private fun ResetDefaultsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset to Defaults") },
        text = { Text("Reset all settings to their default values? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFF44336)
                )
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Apply preset dialog
 */
@Composable
private fun ApplyPresetDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apply_preset)) },
        text = { Text(stringResource(R.string.choose_preset_config)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * Quick settings row with modern toggle cards
 */
@Composable
private fun QuickSettingsRow(
    quickSettings: List<QuickSetting>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.quick_controls),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(quickSettings) { setting ->
                QuickSettingCard(
                    setting = setting,
                    onToggle = { onToggle(setting.id) }
                )
            }
        }
    }
}

/**
 * Individual quick setting card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSettingCard(
    setting: QuickSetting,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (setting.getBooleanValue()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Settings, // Use a default icon for now
                contentDescription = setting.displayName,
                tint = if (setting.getBooleanValue()) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (setting.getBooleanValue()) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Recent settings section
 */
@Composable
private fun RecentSettingsSection(
    recentSettings: List<RecentSetting>,
    onSettingClick: (RecentSetting) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Recently Modified",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentSettings.take(3).forEach { setting ->
                    RecentSettingItem(
                        setting = setting,
                        onClick = { onSettingClick(setting) }
                    )
                }
            }
        }
    }
}

/**
 * Individual recent setting item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSettingItem(
    setting: RecentSetting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.settingName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = setting.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Recent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * System health card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemHealthCard(
    systemHealth: SystemHealth?,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (systemHealth == null) return

    val healthStatus = systemHealth.getHealthStatus()
    val healthColor = when (healthStatus) {
        "Good" -> Color(0xFF4CAF50)
        "Warning" -> Color(0xFFFF9800)
        "Critical" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onViewDetails,
        colors = CardDefaults.cardColors(
            containerColor = healthColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "System Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = healthStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = healthColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
