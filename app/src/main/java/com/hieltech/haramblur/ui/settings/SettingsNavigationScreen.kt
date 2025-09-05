package com.hieltech.haramblur.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.models.RecentSetting
import com.hieltech.haramblur.ui.components.HapticFeedback
import com.hieltech.haramblur.ui.settings.components.*

/**
 * Enhanced settings navigation screen with compact header, quick controls, and contextual bottom panel
 * Replaces the large header card with information-dense, contextual interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavigationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
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
                title = { 
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
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
            
            // Enhanced quick toggle row
            item {
                EnhancedQuickToggleRow(
                    quickSettings = quickSettings,
                    onSettingToggle = { settingId ->
                        viewModel.toggleQuickSetting(settingId)
                    },
                    onSettingUpdate = { settingId, newValue ->
                        viewModel.updateQuickSetting(settingId, newValue)
                    }
                )
            }
            
            // Settings categories grid
            item {
                SettingsCategoriesGrid(
                    onCategoryClick = { category ->
                        HapticFeedback.performLightFeedback(context)
                        onNavigateToCategory(category)
                    }
                )
            }
            
            // Contextual bottom panel
            item {
                ContextualBottomPanel(
                    recentSettings = recentSettings,
                    systemHealth = systemHealth,
                    onExportSettings = { showExportDialog = true },
                    onResetDefaults = { showResetDialog = true },
                    onApplyPreset = { showPresetDialog = true },
                    onRecentSettingClick = { setting ->
                        // Navigate to the specific setting
                        onNavigateToCategory(setting.category)
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
private fun SettingsCategoriesGrid(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        SettingsCategory(
            id = "islamic",
            name = "Islamic Features",
            icon = Icons.Default.Home,
            color = Color(0xFF4CAF50),
            settingCount = 12,
            description = "Prayer times, Qibla, Dhikr"
        ),
        SettingsCategory(
            id = "protection",
            name = "Protection",
            icon = Icons.Default.Security,
            color = Color(0xFF2196F3),
            settingCount = 8,
            description = "Face detection, content blocking"
        ),
        SettingsCategory(
            id = "privacy",
            name = "Privacy",
            icon = Icons.Default.Lock,
            color = Color(0xFF9C27B0),
            settingCount = 6,
            description = "Data protection, permissions"
        ),
        SettingsCategory(
            id = "appearance",
            name = "Appearance",
            icon = Icons.Default.ColorLens,
            color = Color(0xFFFF9800),
            settingCount = 4,
            description = "Theme, layout, customization"
        ),
        SettingsCategory(
            id = "notifications",
            name = "Notifications",
            icon = Icons.Default.Notifications,
            color = Color(0xFFF44336),
            settingCount = 5,
            description = "Alerts, reminders, sounds"
        ),
        SettingsCategory(
            id = "advanced",
            name = "Advanced",
            icon = Icons.Default.Settings,
            color = Color(0xFF607D8B),
            settingCount = 10,
            description = "Debug, logs, performance"
        )
    )
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Settings Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(categories) { category ->
                SettingsCategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

/**
 * Settings category card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = category.color,
                modifier = Modifier.size(32.dp)
            )
            
            // Name and count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${category.settingCount} settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                imageVector = Icons.Default.Error,
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
        title = { Text("Apply Preset") },
        text = { Text("Choose a preset configuration to apply to your settings.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * Settings category data class
 */
data class SettingsCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val settingCount: Int,
    val description: String
)
