package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.database.LogEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogsViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Add error handling for state collection
    val logs by viewModel.logs.collectAsState(initial = emptyList())
    val logStatistics by viewModel.logStatistics.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val selectedLevels by viewModel.selectedLevels.collectAsState(initial = setOf("DEBUG", "INFO", "WARN", "ERROR"))
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Application Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export button
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.exportLogs(context)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Logs")
                    }

                    // Clear logs button
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.clearAllLogs()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter and search controls
            LogsFilterSection(
                selectedLevels = selectedLevels,
                onLevelToggle = { level -> viewModel.toggleLevel(level) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                statistics = logStatistics,
                onRefresh = { viewModel.refreshLogs() }
            )

            // Logs list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LogsList(
                    logs = logs,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsFilterSection(
    selectedLevels: Set<String>,
    onLevelToggle: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statistics: com.hieltech.haramblur.data.database.LogStatistics?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statistics row
            if (statistics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip("Total", statistics.totalLogs.toString(), Icons.Default.List)
                    StatChip("Errors", statistics.errorCount.toString(), Icons.Default.Warning, MaterialTheme.colorScheme.error)
                    StatChip("Warnings", statistics.warnCount.toString(), Icons.Default.Warning, MaterialTheme.colorScheme.tertiary)
                    StatChip("Info", statistics.infoCount.toString(), Icons.Default.Info, MaterialTheme.colorScheme.secondary)
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            // Level filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
                levels.forEach { level ->
                    FilterChip(
                        selected = selectedLevels.contains(level),
                        onClick = { onLevelToggle(level) },
                        label = { Text(level) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (level) {
                                "ERROR" -> MaterialTheme.colorScheme.error
                                "WARN" -> MaterialTheme.colorScheme.tertiary
                                "INFO" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }

                // Refresh button
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun LogsList(
    logs: List<LogEntity>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No logs found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Try adjusting your filters or search query",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntity) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (log.level) {
                "ERROR" -> MaterialTheme.colorScheme.errorContainer
                "WARN" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header with timestamp, level, and tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (log.level) {
                            "ERROR" -> MaterialTheme.colorScheme.error
                            "WARN" -> MaterialTheme.colorScheme.tertiary
                            "INFO" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = log.level,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            color = when (log.level) {
                                "ERROR" -> MaterialTheme.colorScheme.error
                                "WARN" -> MaterialTheme.colorScheme.tertiary
                                "INFO" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Message
            SelectionContainer {
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Additional details (if available)
            if (!log.category.isNullOrBlank() && log.category != "GENERAL") {
                Text(
                    text = "Category: ${log.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.userAction.isNullOrBlank()) {
                Text(
                    text = "User Action: ${log.userAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.stackTrace.isNullOrBlank()) {
                Text(
                    text = "Stack Trace:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectionContainer {
                    Text(
                        text = log.stackTrace!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
