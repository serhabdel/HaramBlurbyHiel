package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.ui.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToPrayer: () -> Unit = {},
    onNavigateToDhikr: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceRunning by viewModel.serviceRunning.collectAsState()

    // Welcome animation
    var showWelcome by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)
        showWelcome = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation drawer")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDebug) {
                        Icon(Icons.Default.Build, contentDescription = "Debug")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Welcome banner (fades out after 2 seconds)
            AnimatedVisibility(
                visible = showWelcome,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WelcomeBanner()
            }

            // Hero Status Card
            HeroStatusCard(
                isActive = serviceRunning,
                title = if (serviceRunning) {
                    stringResource(R.string.protection_active)
                } else {
                    stringResource(R.string.protection_inactive)
                },
                subtitle = if (serviceRunning) {
                    stringResource(R.string.monitoring_and_filtering)
                } else {
                    stringResource(R.string.enable_accessibility_service)
                },
                onClick = if (!serviceRunning) {
                    { viewModel.openAccessibilitySettings(context) }
                } else null
            )

            // Streak Card
            StreakCard(
                currentStreak = 5, // TODO: Get from viewModel
                bestStreak = 12    // TODO: Get from viewModel
            )
            
            // Prayer Times Summary
            PrayerTimesSummaryCard(
                onClick = onNavigateToPrayer
            )

            // Daily Progress
            if (serviceRunning) {
                DailyProgressCard(
                    facesBlocked = 12,
                    sitesBlocked = 8,
                    dailyGoal = 50
                )
            }

            // Quick Stats Grid
            if (serviceRunning) {
                Text(
                    text = "Live Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatGridItem(
                        icon = "👤",
                        value = "12",
                        label = "Faces\nProtected",
                        modifier = Modifier.weight(1f)
                    )
                    StatGridItem(
                        icon = "🔞",
                        value = "8",
                        label = "Sites\nBlocked",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatGridItem(
                        icon = "👁️",
                        value = "15",
                        label = "Blur\nRegions",
                        modifier = Modifier.weight(1f)
                    )
                    StatGridItem(
                        icon = "⚡",
                        value = "95%",
                        label = "Performance",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Customize protection",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                )
                QuickActionCard(
                    icon = Icons.Default.Star,
                    title = "Dhikr",
                    subtitle = "Daily remembrance",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDhikr
                )
            }

            // Setup instructions if service not running
            if (!serviceRunning) {
                SetupInstructionsCard(
                    onOpenSettings = { viewModel.openAccessibilitySettings(context) }
                )
            }

            // Feature highlights
            FeatureHighlightCard(
                emoji = "🔒",
                title = "Privacy First",
                description = "All processing happens on your device. No data leaves your phone."
            )

            FeatureHighlightCard(
                emoji = "🕌",
                title = "Islamic Features",
                description = "Prayer times, Qibla direction, and daily dhikr reminders."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeBanner() {
    UnifiedCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "👋",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize
            )
            Column {
                Text(
                    text = stringResource(R.string.welcome_to_haramblur),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.your_islamic_content_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetupInstructionsCard(
    onOpenSettings: () -> Unit
) {
    UnifiedCard(
        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.setup_required_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = stringResource(R.string.setup_instructions),
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_accessibility_settings))
            }
        }
    }
}

/**
 * Prayer Times summary card for home screen
 */
@Composable
private fun PrayerTimesSummaryCard(
    onClick: () -> Unit
) {
    val prayerViewModel: PrayerTimesViewModel = hiltViewModel()
    val prayerTimes by prayerViewModel.prayerTimes.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    
    UnifiedCard(
        onClick = onClick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🕌",
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize
                    )
                    Column {
                        Text(
                            text = "Prayer Times",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        prayerTimes?.date?.hijri?.let { hijri ->
                            Text(
                                text = "${hijri.day} ${hijri.month.en}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Prayer Times",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Next Prayer
            nextPrayer?.let { prayer ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Next Prayer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = prayer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = prayer.time,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = prayer.timeUntil,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            
            // Quick prayer times row
            prayerTimes?.timings?.let { timings ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniPrayerTime("Fajr", timings.Fajr)
                    MiniPrayerTime("Dhuhr", timings.Dhuhr)
                    MiniPrayerTime("Asr", timings.Asr)
                    MiniPrayerTime("Maghrib", timings.Maghrib)
                    MiniPrayerTime("Isha", timings.Isha)
                }
            }
        }
    }
}

@Composable
private fun MiniPrayerTime(name: String, time: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name.take(2),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
