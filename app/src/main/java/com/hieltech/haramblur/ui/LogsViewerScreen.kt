package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.database.LogEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Modern color palette for logs
private val LogsGreen = Color(0xFF1B5E20)
private val LogsGreenLight = Color(0xFF4CAF50)
private val LogsGold = Color(0xFFFFD700)
private val ErrorRed = Color(0xFFD32F2F)
private val WarnAmber = Color(0xFFF57C00)
private val InfoBlue = Color(0xFF1976D2)
private val DebugGray = Color(0xFF616161)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogsViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val logs by viewModel.logs.collectAsState(initial = emptyList())
    val logStatistics by viewModel.logStatistics.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val selectedLevels by viewModel.selectedLevels.collectAsState(initial = setOf("DEBUG", "INFO", "WARN", "ERROR"))
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.application_logs),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch { viewModel.exportLogs(context) }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_logs_title))
                    }
                    IconButton(
                        onClick = {
                            scope.launch { viewModel.clearAllLogs() }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_logs))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Header Card
            item {
                LogsHeaderCard(statistics = logStatistics)
            }

            // Filter and Search Section
            item {
                ModernFilterSection(
                    selectedLevels = selectedLevels,
                    onLevelToggle = { level -> viewModel.toggleLevel(level) },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onRefresh = { viewModel.refreshLogs() }
                )
            }

            // Loading / Content
            if (isLoading) {
                items(5) { index ->
                    LogShimmerItem(index = index)
                }
            } else if (logs.isEmpty()) {
                item {
                    EmptyLogsCard()
                }
            } else {
                itemsIndexed(logs) { index, log ->
                    AnimatedLogItem(log = log, index = index)
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun LogsHeaderCard(statistics: com.hieltech.haramblur.data.database.LogStatistics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LogsGreen
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📋",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.application_logs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.logs_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (statistics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LogsStatBadge(
                        icon = "📊",
                        label = stringResource(R.string.total),
                        value = statistics.totalLogs.toString()
                    )
                    LogsStatBadge(
                        icon = "🔴",
                        label = stringResource(R.string.errors),
                        value = statistics.errorCount.toString()
                    )
                    LogsStatBadge(
                        icon = "⚠️",
                        label = stringResource(R.string.warnings),
                        value = statistics.warnCount.toString()
                    )
                    LogsStatBadge(
                        icon = "ℹ️",
                        label = stringResource(R.string.info),
                        value = statistics.infoCount.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsStatBadge(
    icon: String,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernFilterSection(
    selectedLevels: Set<String>,
    onLevelToggle: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_logs)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LogsGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Level filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val levels = listOf(
                    Triple("ERROR", ErrorRed, "🔴"),
                    Triple("WARN", WarnAmber, "⚠️"),
                    Triple("INFO", InfoBlue, "ℹ️"),
                    Triple("DEBUG", DebugGray, "🔧")
                )

                levels.forEach { (level, color, icon) ->
                    FilterChip(
                        selected = selectedLevels.contains(level),
                        onClick = { onLevelToggle(level) },
                        label = {
                            Text(
                                text = "$icon $level",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedLevels.contains(level)) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedLevels.contains(level),
                            borderColor = color.copy(alpha = 0.3f),
                            selectedBorderColor = color
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LogsGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        tint = LogsGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedLogItem(log: LogEntity, index: Int) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Staggered entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.toLong() * 30)
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "offsetY"
    )

    val levelColor = when (log.level) {
        "ERROR" -> ErrorRed
        "WARN" -> WarnAmber
        "INFO" -> InfoBlue
        else -> DebugGray
    }

    val levelIcon = when (log.level) {
        "ERROR" -> "🔴"
        "WARN" -> "⚠️"
        "INFO" -> "ℹ️"
        else -> "🔧"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = alpha,
                translationY = offsetY
            ),
        colors = CardDefaults.cardColors(
            containerColor = when (log.level) {
                "ERROR" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                "WARN" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header row: time, level badge, tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp
                Text(
                    text = dateFormatter.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = levelColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = levelColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = levelIcon, fontSize = 10.sp)
                            Text(
                                text = log.level,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = levelColor
                            )
                        }
                    }

                    // Tag badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LogsGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = log.tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Medium,
                            color = LogsGreen
                        )
                    }
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

            // Category
            if (!log.category.isNullOrBlank() && log.category != "GENERAL") {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "${stringResource(R.string.category)}: ${log.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // User action
            if (!log.userAction.isNullOrBlank()) {
                Text(
                    text = "${stringResource(R.string.user_action)}: ${log.userAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stack trace
            if (!log.stackTrace.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed.copy(alpha = 0.05f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = stringResource(R.string.stack_trace),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
    }
}

@Composable
private fun LogShimmerItem(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha * 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.2f))
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.15f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.15f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.1f))
            )
        }
    }
}

@Composable
private fun EmptyLogsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "📋", fontSize = 48.sp)
            Text(
                text = stringResource(R.string.no_logs_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.adjust_filters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
