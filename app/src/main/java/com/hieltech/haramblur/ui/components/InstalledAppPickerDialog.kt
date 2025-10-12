package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.R
import com.hieltech.haramblur.detection.AppInfo
import kotlinx.coroutines.launch

/**
 * Dialog that allows users to pick from installed apps instead of typing package names
 * Much better UX than requiring users to know package names
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit,
    installedApps: List<AppInfo>,
    existingApps: Set<String> = emptySet(),
    conflictingApps: Set<String> = emptySet(),
    conflictMessage: String = "This app is already in the other list",
    isLoading: Boolean = false,
    onManualEntry: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter apps based on search query and exclude already added apps
    val filteredApps = remember(installedApps, searchQuery, existingApps, conflictingApps) {
        installedApps
            .filter { app ->
                // Filter by search query
                val matchesSearch = searchQuery.isBlank() || 
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
                
                matchesSearch
            }
            .sortedBy { it.appName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.select_app_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose from your installed apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_apps)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loading state
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredApps.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "Try a different search term",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Apps list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isDuplicate = existingApps.contains(app.packageName)
                            val hasConflict = conflictingApps.contains(app.packageName)
                            val isDisabled = isDuplicate || hasConflict
                            
                            InstalledAppItem(
                                app = app,
                                isDuplicate = isDuplicate,
                                hasConflict = hasConflict,
                                conflictMessage = conflictMessage,
                                onClick = {
                                    if (!isDisabled) {
                                        onAppSelected(app.packageName)
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manual entry option (if provided)
                    if (onManualEntry != null) {
                        TextButton(onClick = onManualEntry) {
                            Text("Enter manually")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Cancel button
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Individual app item in the picker
 */
@Composable
private fun InstalledAppItem(
    app: AppInfo,
    isDuplicate: Boolean,
    hasConflict: Boolean,
    conflictMessage: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDuplicate && !hasConflict, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDuplicate || hasConflict -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDuplicate || hasConflict) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isDuplicate || hasConflict) 0.4f else 0.7f
                    )
                )
                
                // Show reason why disabled
                if (isDuplicate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Already added",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (hasConflict) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conflictMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
