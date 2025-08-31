package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.DhikrDataSource
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
 * Responsive HomeScreen that adapts to different screen sizes using Window Size Classes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactHomeScreen(
    isLandscape: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)?,
    onTriggerDhikr: (() -> Unit)? = null,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val dashboardState by statsViewModel.dashboardState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val permissionStatus by permissionHelper.permissionStatusFlow.collectAsState()

    val enhancedPermissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()
    val hasRequiredPermissions = enhancedPermissionStatus.isComplete
    val accessibilityGranted = enhancedPermissionStatus.accessibilityServiceGranted
    val isServicePaused = settings.isServicePaused

    var blockedAppsCount by remember { mutableIntStateOf(0) }
    var blockedSitesCount by remember { mutableIntStateOf(0) }

    // Animation states
    var showWelcome by remember { mutableStateOf(true) }
    var showFeatures by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showWelcome = false
        delay(300)
        showFeatures = true
    }

    // Collect blocking counts
    LaunchedEffect(appBlockingManager) {
        appBlockingManager?.getBlockedAppsCount()?.collectLatest { count ->
            blockedAppsCount = count
        }
    }

    LaunchedEffect(siteBlockingManager) {
        siteBlockingManager?.getCustomBlockedWebsitesCount()?.collectLatest { count ->
            blockedSitesCount = count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Welcome Section
        AnimatedVisibility(visible = showWelcome) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    AnimatedFadeIn(visible = true) {
                        Text(
                            text = "🛡️",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                    AnimatedFadeIn(visible = true) {
                        Text(
                            text = "Welcome to HaramBlur",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedFadeIn(visible = true) {
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

        // Service Status
        AnimatedVisibility(visible = !showWelcome) {
            StatusCard(
                title = if (accessibilityGranted && serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                subtitle = when {
                    accessibilityGranted && serviceRunning -> "Content filtering is enabled"
                    accessibilityGranted && !serviceRunning -> "Accessibility enabled, service not running"
                    else -> "Complete setup to enable filtering"
                },
                status = when {
                    accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                    accessibilityGranted && !serviceRunning -> StatusType.WARNING
                    else -> StatusType.ERROR
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Service Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )
        }

        // Dhikr Bar (only if dhikr is enabled)
        if (settings.dhikrEnabled && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                DhikrBar(settings = settings)
            }
        }

        // Dashboard (only if permissions granted and service running)
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                CompactDashboardSection(
                    dashboardState = dashboardState,
                    selectedTimeRange = statsViewModel.selectedTimeRange.collectAsState().value,
                    selectedTimelineType = statsViewModel.selectedTimelineType.collectAsState().value,
                    onTimeRangeSelected = statsViewModel::setSelectedTimeRange,
                    onTimelineTypeSelected = statsViewModel::setSelectedTimelineType,
                    onRefreshData = statsViewModel::refreshData
                )
            }
        }

        // Islamic Features Section
        if (settings.enablePrayerTimes && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Prayer Times Widget
                    PrayerTimesWidget(
                        prayerData = dashboardState.prayerTimes,
                        nextPrayer = dashboardState.nextPrayer,
                        onLocationSettingsClick = {
                            // Navigate to Islamic settings
                            onNavigateToSettings()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Islamic Calendar Widget
                    if (settings.enableIslamicCalendar) {
                        IslamicCalendarWidget(
                            hijriDate = dashboardState.hijriDate,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Qibla Direction Widget
                    if (settings.enableQiblaDirection) {
                        QiblaDirectionWidget(
                            qiblaDirection = dashboardState.qiblaDirection,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Protection Status
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            StatusCard(
                title = "🛡️ Protection Active",
                subtitle = "$blockedAppsCount apps • $blockedSitesCount sites blocked",
                status = StatusType.SUCCESS,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Protection Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            )
        }

        // Dhikr Status (if enabled)
        if (settings.dhikrEnabled && hasRequiredPermissions && serviceRunning && !showWelcome) {
            DhikrStatusCard(
                settings = settings,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Actions (2 per row for compact)
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

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

                    // Add Dhikr trigger if enabled
                    val dhikrActions = if (settings.dhikrEnabled && onTriggerDhikr != null) {
                        listOf(
                            Quadruple(
                                "Show Dhikr",
                                "Islamic remembrance",
                                "🕌",
                                onTriggerDhikr
                            )
                        )
                    } else emptyList()

                    val allActions = quickActions + dhikrActions

                    // Group actions in pairs
                    allActions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (title, subtitle, icon, onClick) ->
                                CompactQuickActionCard(
                                    title = title,
                                    subtitle = subtitle,
                                    icon = icon,
                                    onClick = onClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if odd number
                            repeat(2 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Main responsive HomeScreen composable that adapts to different screen sizes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenResponsive(
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)? = null,
    onTriggerDhikr: (() -> Unit)? = null,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    // Determine window size class
    val windowSizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    when (windowSizeClass) {
        "Compact" -> CompactHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            onTriggerDhikr = onTriggerDhikr,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Medium" -> MediumHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Expanded" -> ExpandedHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        }
    }
}

/**
 * Main responsive HomeScreen composable that adapts to different screen sizes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenResponsive(
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)? = null,
    onTriggerDhikr: (() -> Unit)? = null,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    // Determine window size class
    val windowSizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    when (windowSizeClass) {
        "Compact" -> CompactHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            onTriggerDhikr = onTriggerDhikr,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Medium" -> MediumHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Expanded" -> ExpandedHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        }
    }
}

/**
 * Main responsive HomeScreen composable that adapts to different screen sizes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenResponsive(
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)? = null,
    onTriggerDhikr: (() -> Unit)? = null,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    // Determine window size class
    val windowSizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    when (windowSizeClass) {
        "Compact" -> CompactHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            onTriggerDhikr = onTriggerDhikr,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Medium" -> MediumHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Expanded" -> ExpandedHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        }
    }
}

// Test comment

        // Permission Setup (if needed)
        if (!hasRequiredPermissions && !showWelcome) {
            ModernCard(
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Setup Incomplete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "Complete setup to enable content filtering",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (onNavigateToPermissionWizard != null) {
                        Button(
                            onClick = onNavigateToPermissionWizard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Complete Setup")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Medium layout for tablets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumHomeScreen(
    isLandscape: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)?,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val dashboardState by statsViewModel.dashboardState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val enhancedPermissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()
    val hasRequiredPermissions = enhancedPermissionStatus.isComplete
    val accessibilityGranted = enhancedPermissionStatus.accessibilityServiceGranted
    val isServicePaused = settings.isServicePaused

    var blockedAppsCount by remember { mutableIntStateOf(0) }
    var blockedSitesCount by remember { mutableIntStateOf(0) }

    // Animation states
    var showWelcome by remember { mutableStateOf(true) }
    var showFeatures by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showWelcome = false
        delay(300)
        showFeatures = true
    }

    // Collect blocking counts
    LaunchedEffect(appBlockingManager) {
        appBlockingManager?.getBlockedAppsCount()?.collectLatest { count ->
            blockedAppsCount = count
        }
    }

    LaunchedEffect(siteBlockingManager) {
        siteBlockingManager?.getCustomBlockedWebsitesCount()?.collectLatest { count ->
            blockedSitesCount = count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Row: Welcome + Service Status
        if (showWelcome) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "🛡️",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "Welcome to HaramBlur",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your Islamic content filter is ready",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusCard(
                    title = if (accessibilityGranted && serviceRunning) "🟢 Active" else "🔴 Inactive",
                    subtitle = when {
                        accessibilityGranted && serviceRunning -> "Filtering enabled"
                        else -> "Setup needed"
                    },
                    status = when {
                        accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                        else -> StatusType.ERROR
                    },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Service Status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        } else {
            // Service Status only when welcome is hidden
            StatusCard(
                title = if (accessibilityGranted && serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                subtitle = when {
                    accessibilityGranted && serviceRunning -> "Content filtering is enabled"
                    else -> "Complete setup to enable filtering"
                },
                status = when {
                    accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                    else -> StatusType.ERROR
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Service Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )
        }

        // Dhikr Bar (only if dhikr is enabled)
        if (settings.dhikrEnabled && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                DhikrBar(settings = settings)
            }
        }

        // Dashboard
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                MediumDashboardSection(
                    dashboardState = dashboardState,
                    selectedTimeRange = statsViewModel.selectedTimeRange.collectAsState().value,
                    selectedTimelineType = statsViewModel.selectedTimelineType.collectAsState().value,
                    onTimeRangeSelected = statsViewModel::setSelectedTimeRange,
                    onTimelineTypeSelected = statsViewModel::setSelectedTimelineType,
                    onRefreshData = statsViewModel::refreshData
                )
            }
        }

        // Quick Actions
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Actions Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val quickActions = listOf(
                            Quadruple(
                                if (isServicePaused) "Resume" else "Pause",
                                if (isServicePaused) "Paused" else "Active",
                                if (isServicePaused) "▶️" else "⏸️",
                                { settingsViewModel.toggleServicePause() }
                            ),
                            Quadruple("Block Apps", "$blockedAppsCount blocked", "📱", onNavigateToBlockApps),
                            Quadruple("Block Sites", "$blockedSitesCount sites", "🌐", onNavigateToBlockSites),
                            Quadruple("View Logs", "History", "📋", onNavigateToLogs),
                            Quadruple("Support", "Help", "🆘", onNavigateToSupport)
                        )

                        quickActions.forEach { (title, subtitle, icon, onClick) ->
                            MediumQuickActionCard(title, subtitle, icon, onClick)
                        }
                    }
                }
            }
        }

        // Islamic Features Section
        if (settings.enablePrayerTimes && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prayer Times Widget
                    PrayerTimesWidget(
                        onLocationSettingsClick = {
                            // Navigate to Islamic settings
                            onNavigateToSettings()
                        },
                        prayerData = dashboardState.prayerTimes,
                        nextPrayer = dashboardState.nextPrayer,
                        modifier = Modifier.weight(1f)
                    )

                    // Islamic Calendar Widget
                    if (settings.enableIslamicCalendar) {
                        IslamicCalendarWidget(
                            hijriDate = dashboardState.hijriDate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Qibla Direction Widget (full width)
                if (settings.enableQiblaDirection) {
                    QiblaDirectionWidget(
                        qiblaDirection = dashboardState.qiblaDirection,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Protection Status
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            StatusCard(
                title = "🛡️ Protection Active",
                subtitle = "$blockedAppsCount apps blocked • $blockedSitesCount sites blocked",
                status = StatusType.SUCCESS,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Protection Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            )
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "About HaramBlur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Automatically detects and blurs inappropriate content across all apps on your device, helping you maintain Islamic values while using technology. All processing happens locally for complete privacy.",
                            style = MaterialTheme.typography.bodyLarge
                        )

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
 * Expanded layout for large tablets and desktop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedHomeScreen(
    isLandscape: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)?,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val dashboardState by statsViewModel.dashboardState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val enhancedPermissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()
    val hasRequiredPermissions = enhancedPermissionStatus.isComplete
    val accessibilityGranted = enhancedPermissionStatus.accessibilityServiceGranted
    val isServicePaused = settings.isServicePaused

    var blockedAppsCount by remember { mutableIntStateOf(0) }
    var blockedSitesCount by remember { mutableIntStateOf(0) }

    // Animation states
    var showWelcome by remember { mutableStateOf(true) }
    var showFeatures by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showWelcome = false
        delay(300)
        showFeatures = true
    }

    // Collect blocking counts
    LaunchedEffect(appBlockingManager) {
        appBlockingManager?.getBlockedAppsCount()?.collectLatest { count ->
            blockedAppsCount = count
        }
    }

    LaunchedEffect(siteBlockingManager) {
        siteBlockingManager?.getCustomBlockedWebsitesCount()?.collectLatest { count ->
            blockedSitesCount = count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Row: Welcome + Service Status + Dashboard Preview
        if (showWelcome) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🛡️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Welcome to HaramBlur",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your Islamic content filter is ready to protect your digital space",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Service Status
                StatusCard(
                    title = if (accessibilityGranted && serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                    subtitle = when {
                        accessibilityGranted && serviceRunning -> "Content filtering enabled"
                        else -> "Complete setup"
                    },
                    status = when {
                        accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                        else -> StatusType.ERROR
                    },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Service Status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )

                // Dashboard Preview
                if (hasRequiredPermissions && serviceRunning) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📊 Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${dashboardState.dailySummary.totalDetections} detections today",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(dashboardState.dailySummary.performanceScore).toInt()}% success rate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            // Service Status + Dashboard when welcome is hidden
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusCard(
                    title = if (accessibilityGranted && serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                    subtitle = when {
                        accessibilityGranted && serviceRunning -> "Content filtering is enabled"
                        else -> "Complete setup to enable filtering"
                    },
                    status = when {
                        accessibilityGranted && serviceRunning -> StatusType.SUCCESS
                        else -> StatusType.ERROR
                    },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Service Status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )

                if (hasRequiredPermissions && serviceRunning) {
                    Card(modifier = Modifier.weight(2f)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📊 Performance Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${dashboardState.dailySummary.totalDetections} detections • ${(dashboardState.dailySummary.performanceScore).toInt()}% success rate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Dhikr Bar (only if dhikr is enabled)
        if (settings.dhikrEnabled && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                DhikrBar(settings = settings)
            }
        }

        // Main Content Grid: Quick Actions + Analytics
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Actions Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

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

                        quickActions.forEach { (title, subtitle, icon, onClick) ->
                            ExpandedQuickActionCard(title, subtitle, icon, onClick)
                        }
                    }

                    // Analytics Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Key metrics cards
                        MetricCard("Detection Rate", "${dashboardState.dailySummary.totalDetections}/hour", "🎯")
                        MetricCard("Success Rate", "${(dashboardState.dailySummary.performanceScore).toInt()}%", "✅")
                        MetricCard("Trend", "Improving", "📈")
                    }
                }
            }
        }

        // Islamic Features Section
        if (settings.enablePrayerTimes && !showWelcome) {
            AnimatedVisibility(visible = showFeatures) {
                // Islamic widgets in a row layout for expanded screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prayer Times Widget
                    PrayerTimesWidget(
                        prayerData = dashboardState.prayerTimes,
                        nextPrayer = dashboardState.nextPrayer,
                        onLocationSettingsClick = {
                            // Navigate to Islamic settings
                            onNavigateToSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Islamic Calendar Widget
                    if (settings.enableIslamicCalendar) {
                        IslamicCalendarWidget(
                            hijriDate = dashboardState.hijriDate,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Qibla Direction Widget
                    if (settings.enableQiblaDirection) {
                        QiblaDirectionWidget(
                            qiblaDirection = dashboardState.qiblaDirection,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Protection Status
        if (hasRequiredPermissions && serviceRunning && !showWelcome) {
            StatusCard(
                title = "🛡️ Protection Active",
                subtitle = "$blockedAppsCount apps blocked • $blockedSitesCount sites blocked • ${(dashboardState.dailySummary.performanceScore).toInt()}% success rate",
                status = StatusType.SUCCESS,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Protection Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Compact versions of components for smaller screens
@Composable
private fun CompactFeatureCard(title: String, description: String, icon: String) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactQuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Medium versions for tablets
@Composable
private fun MediumFeatureCard(title: String, description: String, icon: String) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MediumQuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Expanded versions for large screens
@Composable
private fun ExpandedFeatureCard(title: String, description: String, icon: String) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.displaySmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandedQuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: String) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Compact dashboard for smaller screens
@Composable
private fun CompactDashboardSection(
    dashboardState: StatsViewModel.DashboardState,
    selectedTimeRange: StatsViewModel.TimeRange,
    selectedTimelineType: StatsViewModel.TimelineType,
    onTimeRangeSelected: (StatsViewModel.TimeRange) -> Unit,
    onTimelineTypeSelected: (StatsViewModel.TimelineType) -> Unit,
    onRefreshData: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRefreshData) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        // Key metrics in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricChip(
                label = "Today",
                value = "${dashboardState.dailySummary.totalDetections}",
                icon = "🎯",
                modifier = Modifier.weight(1f)
            )
            MetricChip(
                label = "Success",
                value = "${(dashboardState.dailySummary.performanceScore).toInt()}%",
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Medium dashboard for tablets
@Composable
private fun MediumDashboardSection(
    dashboardState: StatsViewModel.DashboardState,
    selectedTimeRange: StatsViewModel.TimeRange,
    selectedTimelineType: StatsViewModel.TimelineType,
    onTimeRangeSelected: (StatsViewModel.TimeRange) -> Unit,
    onTimelineTypeSelected: (StatsViewModel.TimelineType) -> Unit,
    onRefreshData: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
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
            IconButton(onClick = onRefreshData) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        // Summary cards
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

        // Key metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                value = "Improving",
                icon = "📈",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
// Test comment added via bash

/**
 * Main responsive HomeScreen composable that adapts to different screen sizes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenResponsive(
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToBlockApps: () -> Unit,
    onNavigateToBlockSites: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPermissionWizard: (() -> Unit)? = null,
    onTriggerDhikr: (() -> Unit)? = null,
    viewModel: MainViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    permissionHelper: PermissionHelper,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    // Determine window size class
    val windowSizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    when (windowSizeClass) {
        "Compact" -> CompactHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            onTriggerDhikr = onTriggerDhikr,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Medium" -> MediumHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
        "Expanded" -> ExpandedHomeScreen(
            isLandscape = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDebug = onNavigateToDebug,
            onNavigateToBlockApps = onNavigateToBlockApps,
            onNavigateToBlockSites = onNavigateToBlockSites,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLogs = onNavigateToLogs,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPermissionWizard = onNavigateToPermissionWizard,
            viewModel = viewModel,
            statsViewModel = statsViewModel,
            settingsViewModel = settingsViewModel,
            permissionHelper = permissionHelper,
            appBlockingManager = appBlockingManager,
            siteBlockingManager = siteBlockingManager
        )
    }
}

/**
 * Animated horizontal dhikr bar that displays Islamic remembrance phrases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DhikrBar(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    // Only show if dhikr is enabled
    if (!settings.dhikrEnabled) return

    val dhikrList = remember { DhikrDataSource.getAllDhikr() }
    var currentDhikrIndex by remember { mutableIntStateOf(0) }

    // Auto-cycle through dhikrs with configurable delay
    val displayDuration = (settings.dhikrDisplayDuration * 1000L).coerceAtLeast(3000L) // Minimum 3 seconds

    LaunchedEffect(Unit) {
        while (true) {
            delay(displayDuration)
            currentDhikrIndex = (currentDhikrIndex + 1) % dhikrList.size
        }
    }

    val currentDhikr = dhikrList.getOrNull(currentDhikrIndex) ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated text container with horizontal slide
            AnimatedContent(
                targetState = currentDhikr,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
                        initialOffsetX = { width -> width }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
                        targetOffsetX = { width -> -width }
                    )
                },
                label = "DhikrTransition"
            ) { dhikr ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Arabic text
                    Text(
                        text = dhikr.arabicText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Show transliteration if enabled
                    if (settings.dhikrShowTransliteration) {
                        Text(
                            text = dhikr.transliteration,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Show translation if enabled
                    if (settings.dhikrShowTranslation) {
                        Text(
                            text = dhikr.englishTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
