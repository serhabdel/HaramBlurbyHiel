package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.models.AppCategory

/**
 * Grid component for selecting app categories to monitor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategorySelectionGrid(
    selectedCategories: Set<AppCategory>,
    onCategoryToggle: (AppCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Create a list of categories to avoid potential issues with enum iteration
    val categories = remember {
        listOf(
            AppCategory.SOCIAL_MEDIA,
            AppCategory.BROWSERS,
            AppCategory.DATING,
            AppCategory.MESSAGING,
            AppCategory.ENTERTAINMENT
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            AppCategoryCard(
                category = category,
                isSelected = selectedCategories.contains(category),
                onToggle = { onCategoryToggle(category, it) }
            )
        }
    }
}

/**
 * Individual app category card with selection state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategoryCard(
    category: AppCategory,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp), // Reduced height since we're in a vertical layout
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = getCategoryIcon(category),
                    style = MaterialTheme.typography.headlineSmall
                )
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggle
                )
            }

            // Category info
            Column {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category.defaultApps.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Component for managing custom monitored apps
 */
@Composable
fun CustomAppsManager(
    customApps: Set<String>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    conflictingApps: Set<String> = emptySet(),
    installedApps: List<com.hieltech.haramblur.detection.AppInfo> = emptyList(),
    isLoadingApps: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.custom_apps_title, customApps.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add_app_title))
            }
        }

        // List of custom apps
        if (customApps.isNotEmpty()) {
            Column(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                customApps.forEach { packageName ->
                    CustomAppItem(
                        packageName = packageName,
                        onRemove = { onRemoveApp(packageName) }
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.no_custom_apps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Add app dialog
    if (showAddDialog) {
        AddCustomAppDialog(
            onDismiss = { showAddDialog = false },
            onAddApp = { packageName ->
                onAddApp(packageName)
                showAddDialog = false
            },
            existingApps = customApps,
            conflictingApps = conflictingApps,
            conflictMessage = "This app is already in the excluded list",
            installedApps = installedApps,
            isLoadingApps = isLoadingApps
        )
    }
}

/**
 * Individual custom app item with remove button
 */
@Composable
fun CustomAppItem(
    packageName: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getAppDisplayName(packageName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove app",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog for adding custom apps - now with app picker for better UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomAppDialog(
    onDismiss: () -> Unit,
    onAddApp: (String) -> Unit,
    existingApps: Set<String> = emptySet(),
    conflictingApps: Set<String> = emptySet(),
    conflictMessage: String = "This app is already in the other list",
    installedApps: List<com.hieltech.haramblur.detection.AppInfo> = emptyList(),
    isLoadingApps: Boolean = false
) {
    var showManualEntry by remember { mutableStateOf(false) }
    var packageName by remember { mutableStateOf("") }
    val trimmedPackageName = packageName.trim()
    val isDuplicate = trimmedPackageName.isNotEmpty() && existingApps.contains(trimmedPackageName)
    val hasConflict = trimmedPackageName.isNotEmpty() && conflictingApps.contains(trimmedPackageName)
    val isValid = trimmedPackageName.isNotEmpty() && !isDuplicate && !hasConflict

    if (showManualEntry) {
        // Manual package name entry (fallback for advanced users)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.add_custom_app_title))
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.package_name_title),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text(stringResource(R.string.package_name_title)) },
                        placeholder = { Text(stringResource(R.string.package_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.package_name_example),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isDuplicate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This app is already in the list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (hasConflict) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = conflictMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showManualEntry = false }) {
                        Text("← Back to app picker")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAddApp(trimmedPackageName) },
                    enabled = isValid
                ) {
                    Text(stringResource(R.string.add_title))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    } else {
        // Show app picker directly (not nested in another dialog)
        InstalledAppPickerDialog(
            onDismiss = onDismiss,
            onAppSelected = { selectedPackage ->
                onAddApp(selectedPackage)
            },
            installedApps = installedApps,
            existingApps = existingApps,
            conflictingApps = conflictingApps,
            conflictMessage = conflictMessage,
            isLoading = isLoadingApps,
            onManualEntry = { showManualEntry = true }
        )
    }
}

/**
 * Get icon for app category
 */
private fun getCategoryIcon(category: AppCategory): String {
    return when (category) {
        AppCategory.SOCIAL_MEDIA -> "📱"
        AppCategory.BROWSERS -> "🌐"
        AppCategory.DATING -> "💕"
        AppCategory.MESSAGING -> "💬"
        AppCategory.ENTERTAINMENT -> "🎬"
    }
}

