package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.R
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.ui.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.hieltech.haramblur.ui.components.*
import androidx.compose.material3.Icon
import com.hieltech.haramblur.ui.StatsViewModel
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.derivedStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToBlockApps: () -> Unit = {},
    onNavigateToBlockSites: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToPermissionWizard: (() -> Unit)? = null,
    viewModel: MainViewModel = hiltViewModel(),
    statsViewModel: StatsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager? = null,
    siteBlockingManager: EnhancedSiteBlockingManager? = null
) {
    val context = LocalContext.current
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val dashboardState by statsViewModel.dashboardState.collectAsState()
    val selectedTimeRange by statsViewModel.selectedTimeRange.collectAsState()
    val selectedTimelineType by statsViewModel.selectedTimelineType.collectAsState()
    val permissionStatus by permissionHelper.permissionStatusFlow.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val isServicePaused = settings.isServicePaused
    var blockedAppsCount by remember { mutableIntStateOf(0) }
    var blockedSitesCount by remember { mutableIntStateOf(0) }

    // Check if all required permissions are granted
    val enhancedPermissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()
    val hasRequiredPermissions = enhancedPermissionStatus.isComplete
    val accessibilityGranted = enhancedPermissionStatus.accessibilityServiceGranted

    // Collect blocking counts with proper error handling
    LaunchedEffect(appBlockingManager) {
        if (appBlockingManager != null) {
            try {
                appBlockingManager.getBlockedAppsCount()?.collectLatest { count ->
                    blockedAppsCount = count
                    Log.d("HomeScreen", "Blocked apps count updated: $count")
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error collecting blocked apps count", e)
            }
        }
    }

    LaunchedEffect(siteBlockingManager) {
        if (siteBlockingManager != null) {
            try {
                siteBlockingManager.getCustomBlockedWebsitesCount()?.collectLatest { count ->
                    blockedSitesCount = count
                    Log.d("HomeScreen", "Blocked sites count updated: $count")
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error collecting blocked sites count", e)
            }
        }
    }

    // Animation states
    var showWelcome by remember { mutableStateOf(true) }
    var showFeatures by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showWelcome = false
        delay(300)
        showFeatures = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Optimized Welcome Section with SmartAnimationVisibility
            AnimatedVisibility(
                visible = showWelcome
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedFadeIn(
                            visible = true,
                        ) {
                            Text(
                                text = "🛡️",
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                        AnimatedFadeIn(
                            visible = true,
                        ) {
                            Text(
                                text = "Welcome to HaramBlur",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedFadeIn(
                            visible = true,
                        ) {
                            Text(
                                text = "Your Islamic content filter is ready to protect your digital space",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Optimized Service Status with PerformanceAwareAnimation
            AnimatedVisibility(
                visible = !showWelcome
            ) {
                StatusCard(
                    title = if (accessibilityGranted && serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                    subtitle = when {
                        accessibilityGranted && serviceRunning -> "Content filtering is enabled with all permissions"
                        accessibilityGranted && !serviceRunning -> "Accessibility service enabled, but service not running"
                        else -> "Complete setup to enable content filtering"
                    },
                    status = when {
                        accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                        accessibilityGranted && !serviceRunning -> StatusType.WARNING
                        else -> StatusType.ERROR
                    },
                    icon = {
                        when {
                            accessibilityGranted && serviceRunning -> {
                                AnimatedPulse {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Service Status",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            accessibilityGranted && !serviceRunning -> {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Service Status",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Service Status",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                )
            }

            // Optimized Dashboard Section with staggered entrance animations
            if (hasRequiredPermissions && serviceRunning && !showWelcome) {
                AnimatedVisibility(
                    visible = showFeatures,
                ) {
                    Box {
                        DashboardSection(
                            dashboardState,
                            selectedTimeRange,
                            selectedTimelineType,
                            statsViewModel::setSelectedTimeRange,
                            statsViewModel::setSelectedTimelineType,
                            statsViewModel::refreshData
                        )
                    }
                }
            }

            // Optimized Feature Grid with AnimatedList and performance-aware item limits
            if (hasRequiredPermissions && serviceRunning && !showWelcome) {
                AnimatedVisibility(visible = showFeatures) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedFadeIn(
                            visible = true,
                        ) {
                            Text(
                                text = "Key Features",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Horizontal grid layout for features
                        val features = listOf(
                            Triple("Smart Detection", "AI-powered content recognition with GPU acceleration", "🤖"),
                            Triple("Privacy First", "All processing happens locally on your device", "🔒"),
                            Triple("Islamic Values", "Designed to help maintain Islamic principles online", "☪️"),
                            Triple("Real-time", "Instant content filtering across all apps", "⚡")
                        )

                        // First row with 2 features - full width utilization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            features.take(2).forEach { (title, description, icon) ->
                                AnimatedFadeIn(
                                    visible = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CompactFeatureCard(
                                        title = title,
                                        description = description,
                                        icon = icon
                                    )
                                }
                            }
                        }

                        // Second row with remaining 2 features - full width utilization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            features.drop(2).forEach { (title, description, icon) ->
                                AnimatedFadeIn(
                                    visible = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CompactFeatureCard(
                                        title = title,
                                        description = description,
                                        icon = icon
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Optimized Real-time Stats with AnimatedContentTransition
            if (hasRequiredPermissions && serviceRunning && !showWelcome) {
                Box(
                ) {
                    StatusCard(
                        title = "🛡️ Protection Active",
                        subtitle = "$blockedAppsCount apps blocked • $blockedSitesCount sites blocked",
                        status = StatusType.SUCCESS,
                        icon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Protection Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )
                }
            }

            // Optimized Quick Actions Grid with MicroInteractionScale
            if (hasRequiredPermissions && serviceRunning && !showWelcome) {
                AnimatedVisibility(
                    visible = showFeatures,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedFadeIn(
                            visible = true,
                        ) {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Horizontal scroll for Quick Actions
                        val quickActions = listOf(
                            Quadruple(
                                if (isServicePaused) "Resume Services" else "Pause Services", 
                                if (isServicePaused) "Service paused" else "All services active", 
                                if (isServicePaused) "▶️" else "⏸️", 
                                { settingsViewModel.toggleServicePause() }
                            ),
                            Quadruple("Block Apps", "$blockedAppsCount blocked", "📱", onNavigateToBlockApps),
                            Quadruple("Block Sites", "$blockedSitesCount custom", "🌐", onNavigateToBlockSites),
                            Quadruple("View Logs", "Activity history", "📋", onNavigateToLogs),
                            Quadruple("Support", "Get help", "🆘", onNavigateToSupport)
                        )

                        // First row with 3 cards - full width utilization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickActions.take(3).forEach { (title, subtitle, icon, onClick) ->
                                var isPressed by remember { mutableStateOf(false) }

                                MicroInteractionScale(
                                    isPressed = isPressed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CompactQuickActionCard(
                                        title = title,
                                        subtitle = subtitle,
                                        icon = icon,
                                        onClick = {
                                            isPressed = true
                                            onClick()
                                            GlobalScope.launch {
                                                delay(200)
                                                isPressed = false
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Second row with remaining cards - full width utilization
                        if (quickActions.size > 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickActions.drop(3).forEach { (title, subtitle, icon, onClick) ->
                                    var isPressed by remember { mutableStateOf(false) }

                                    MicroInteractionScale(
                                        isPressed = isPressed,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CompactQuickActionCard(
                                            title = title,
                                            subtitle = subtitle,
                                            icon = icon,
                                            onClick = {
                                                isPressed = true
                                                onClick()
                                                GlobalScope.launch {
                                                    delay(200)
                                                    isPressed = false
                                                }
                                            }
                                        )
                                    }
                                }
                                // Add spacers for remaining slots in the second row
                                repeat(3 - quickActions.drop(3).size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Permission Setup Status (minimal fallback)
            if (!hasRequiredPermissions && !showWelcome) {
                AnimatedSlideInFromBottom(visible = true, durationMillis = 600) {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Setup Incomplete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Setup Incomplete",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "Some permissions are missing for optimal protection. Complete the setup to enable all features.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (onNavigateToPermissionWizard != null) {
                                Button(
                                    onClick = onNavigateToPermissionWizard,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Complete Setup")
                                        Text("Complete Setup")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            if (!showWelcome) {
                AnimatedFadeIn(visible = !showWelcome) {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "About HaramBlur",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "HaramBlur automatically detects and blurs inappropriate content across all apps on your device, helping you maintain Islamic values while using technology. All processing happens locally on your device for complete privacy and security.",
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = androidx.compose.ui.unit.TextUnit(1.6f, androidx.compose.ui.unit.TextUnitType.Em)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                FeatureChip(text = "🔒 Privacy First")
                                FeatureChip(text = "⚡ Real-time")
                                FeatureChip(text = "🛡️ Islamic Values")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
}

/**
 * Data class for holding quadruple values
 */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Comprehensive dashboard section displaying real-time stats and analytics
 */
@Composable
fun DashboardSection(
    dashboardState: StatsViewModel.DashboardState,
    selectedTimeRange: StatsViewModel.TimeRange,
    selectedTimelineType: StatsViewModel.TimelineType,
    onTimeRangeSelected: (StatsViewModel.TimeRange) -> Unit,
    onTimelineTypeSelected: (StatsViewModel.TimelineType) -> Unit,
    onRefreshData: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Dashboard Header with Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Performance Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(
                onClick = onRefreshData,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh Dashboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Performance Banner
        PerformanceBanner(state = dashboardState)

        // Stats Grid
        StatsGrid(state = dashboardState)

        // Daily & Weekly Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Today",
                summary = dashboardState.dailySummary,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "This Week",
                summary = dashboardState.weeklySummary,
                modifier = Modifier.weight(1f)
            )
        }

        // Timeline Chart with Time Range and Type Selection
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time Range Segmented Control
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f)
                ) {
                    StatsViewModel.TimeRange.values().forEachIndexed { index, timeRange ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = StatsViewModel.TimeRange.values().size
                            ),
                            onClick = { onTimeRangeSelected(timeRange) },
                            selected = timeRange == selectedTimeRange
                        ) {
                            Text(text = timeRange.displayName)
                        }
                    }
                }

                // Timeline Type Toggle
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f)
                ) {
                    StatsViewModel.TimelineType.values().forEachIndexed { index, timelineType ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = StatsViewModel.TimelineType.values().size
                            ),
                            onClick = { onTimelineTypeSelected(timelineType) },
                            selected = timelineType == selectedTimelineType
                        ) {
                            Text(text = timelineType.displayName)
                        }
                    }
                }
            }

            // Timeline Chart
            TimelineChart(
                data = dashboardState.timelineData,
                timeRange = "${selectedTimelineType.displayName} - ${selectedTimeRange.displayName}",
                modifier = Modifier
            )
        }

        // Key Performance Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricChip(
                label = "Detection Rate",
                value = "${dashboardState.dailySummary.totalDetections}/hour",
                icon = "🎯",
                modifier = Modifier.weight(1f)
            )

            MetricChip(
                label = "Success Rate",
                value = "${(dashboardState.dailySummary.performanceScore).toInt()}%",
                icon = "✅",
                modifier = Modifier.weight(1f)
            )

            MetricChip(
                label = "Trend",
                value = when (dashboardState.performanceTrends.trendDirection) {
                    StatsViewModel.PerformanceTrends.TrendDirection.IMPROVING -> "Improving"
                    StatsViewModel.PerformanceTrends.TrendDirection.DECLINING -> "Declining"
                    else -> "Stable"
                },
                icon = when (dashboardState.performanceTrends.trendDirection) {
                    StatsViewModel.PerformanceTrends.TrendDirection.IMPROVING -> "📈"
                    StatsViewModel.PerformanceTrends.TrendDirection.DECLINING -> "📉"
                    else -> "➡️"
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Error State
        dashboardState.error?.let { error ->
            ModernCard(
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Loading State
        if (dashboardState.isLoading) {
            ModernCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading dashboard data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
