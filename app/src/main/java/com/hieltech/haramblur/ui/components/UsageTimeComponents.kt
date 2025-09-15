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
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.ui.components.getAppDisplayName

/**
 * Component for configuring default time limits by category
 */
@Composable
fun DefaultTimeLimitsSection(
    socialMediaLimit: Int,
    messagingLimit: Int,
    onSocialMediaLimitChange: (Int) -> Unit,
    onMessagingLimitChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⏱️ Default Time Limits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Set default daily time limits for different app categories. Individual apps can have custom limits.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Social Media time limit
        TimeSliderSetting(
            title = "Social Media Apps",
            description = "Instagram, Facebook, TikTok, Twitter, Snapchat",
            value = socialMediaLimit,
            range = 15..180, // 15 minutes to 3 hours
            onValueChange = onSocialMediaLimitChange,
            icon = "📱"
        )

        // Messaging time limit
        TimeSliderSetting(
            title = "Messaging Apps",
            description = "WhatsApp, Telegram, Discord, Messenger",
            value = messagingLimit,
            range = 30..300, // 30 minutes to 5 hours
            onValueChange = onMessagingLimitChange,
            icon = "💬"
        )
    }
}

/**
 * Specialized slider for time limits with proper formatting
 */
@Composable
fun TimeSliderSetting(
    title: String,
    description: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatTimeLimit(value),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Slider
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / 15 - 1, // 15-minute increments
                modifier = Modifier.fillMaxWidth()
            )

            // Range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimeLimit(range.first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimeLimit(range.last),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Component for managing custom app time limits
 */
@Composable
fun CustomAppTimeLimitsManager(
    customLimits: Map<String, Int>,
    onAddCustomLimit: (String, Int) -> Unit,
    onRemoveCustomLimit: (String) -> Unit,
    onEditCustomLimit: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<Pair<String, Int>?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⚙️ Custom App Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${customLimits.size} custom limits set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Limit")
            }
        }

        // List of custom limits
        if (customLimits.isNotEmpty()) {
            Column(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customLimits.forEach { (packageName, limit) ->
                    CustomAppLimitItem(
                        packageName = packageName,
                        timeLimit = limit,
                        onEdit = { editingApp = packageName to limit },
                        onRemove = { onRemoveCustomLimit(packageName) }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom app limits set\nUse the button above to add specific limits for individual apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Add custom limit dialog
    if (showAddDialog) {
        AddCustomTimeLimitDialog(
            onDismiss = { showAddDialog = false },
            onAddLimit = { packageName, limit ->
                onAddCustomLimit(packageName, limit)
                showAddDialog = false
            }
        )
    }

    // Edit custom limit dialog
    editingApp?.let { (packageName, currentLimit) ->
        EditCustomTimeLimitDialog(
            packageName = packageName,
            currentLimit = currentLimit,
            onDismiss = { editingApp = null },
            onUpdateLimit = { newLimit ->
                onEditCustomLimit(packageName, newLimit)
                editingApp = null
            }
        )
    }
}

/**
 * Individual custom app limit item
 */
@Composable
fun CustomAppLimitItem(
    packageName: String,
    timeLimit: Int,
    onEdit: () -> Unit,
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getAppDisplayName(packageName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatTimeLimit(timeLimit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit limit"
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove limit",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Dialog for adding custom time limits
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomTimeLimitDialog(
    onDismiss: () -> Unit,
    onAddLimit: (String, Int) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var timeLimit by remember { mutableStateOf(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Custom Time Limit")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("App Package Name") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Time Limit: ${formatTimeLimit(timeLimit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = timeLimit.toFloat(),
                    onValueChange = { timeLimit = it.toInt() },
                    valueRange = 15f..300f, // 15 minutes to 5 hours
                    steps = 18, // 15-minute increments
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "5h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddLimit(packageName.trim(), timeLimit) },
                enabled = packageName.trim().isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for editing existing custom time limits
 */
@Composable
fun EditCustomTimeLimitDialog(
    packageName: String,
    currentLimit: Int,
    onDismiss: () -> Unit,
    onUpdateLimit: (Int) -> Unit
) {
    var timeLimit by remember { mutableStateOf(currentLimit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Time Limit")
        },
        text = {
            Column {
                Text(
                    text = "App: ${getAppDisplayName(packageName)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Time Limit: ${formatTimeLimit(timeLimit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = timeLimit.toFloat(),
                    onValueChange = { timeLimit = it.toInt() },
                    valueRange = 15f..300f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "5h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateLimit(timeLimit) }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format time limit in minutes to human-readable format
 */
fun formatTimeLimit(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

