package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.hieltech.haramblur.ui.components.*
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToBlockApps: () -> Unit = {},
    onNavigateToBlockSites: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    appBlockingManager: AppBlockingManager? = null,
    siteBlockingManager: EnhancedSiteBlockingManager? = null
) {
    val context = LocalContext.current
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    var blockedAppsCount by remember { mutableIntStateOf(0) }
    var blockedSitesCount by remember { mutableIntStateOf(0) }

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

    Scaffold(
        topBar = {
            ModernTopAppBar(
                onNavigateToSupport = onNavigateToSupport,
                onNavigateToLogs = onNavigateToLogs,
                onNavigateToDebug = onNavigateToDebug,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            ModernNavigationBar(
                currentRoute = "home",
                onNavigateToHome = {},
                onNavigateToBlockApps = onNavigateToBlockApps,
                onNavigateToBlockSites = onNavigateToBlockSites,
                onNavigateToSupport = onNavigateToSupport,
                onNavigateToLogs = onNavigateToLogs
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Animated Welcome Section
            AnimatedFadeIn(visible = showWelcome, durationMillis = 800) {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "Your Islamic content filter is ready to protect your digital space",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Service Status with Modern Design
            AnimatedSlideInFromBottom(visible = !showWelcome, durationMillis = 600) {
                StatusCard(
                    title = if (serviceRunning) "🟢 Service Active" else "🔴 Service Inactive",
                    subtitle = if (serviceRunning) "Content filtering is enabled" else "Enable accessibility service to start",
                    status = if (serviceRunning) StatusType.SUCCESS else StatusType.WARNING,
                    icon = {
                        if (serviceRunning) {
                            AnimatedPulse {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Service Status",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Service Status",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )
            }

            // Feature Grid (only show when service is running and welcome is hidden)
            if (serviceRunning && !showWelcome) {
                AnimatedFadeIn(visible = showFeatures, durationMillis = 800) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Key Features",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureCard(
                                title = "Smart Detection",
                                description = "AI-powered content recognition with GPU acceleration",
                                icon = "🤖",
                                modifier = Modifier.weight(1f)
                            )

                            FeatureCard(
                                title = "Privacy First",
                                description = "All processing happens locally on your device",
                                icon = "🔒",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureCard(
                                title = "Islamic Values",
                                description = "Designed to help maintain Islamic principles online",
                                icon = "☪️",
                                modifier = Modifier.weight(1f)
                            )

                            FeatureCard(
                                title = "Real-time",
                                description = "Instant content filtering across all apps",
                                icon = "⚡",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Real-time Stats (only when service is running)
            if (serviceRunning && !showWelcome) {
                AnimatedSlideInFromBottom(visible = showFeatures, durationMillis = 600) {
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

            // Quick Actions Grid
            if (serviceRunning && !showWelcome) {
                AnimatedFadeIn(visible = showFeatures, durationMillis = 1000) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Block Apps",
                                subtitle = "$blockedAppsCount blocked",
                                icon = "📱",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToBlockApps
                            )

                            QuickActionCard(
                                title = "Block Sites",
                                subtitle = "$blockedSitesCount custom",
                                icon = "🌐",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToBlockSites
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "View Logs",
                                subtitle = "Activity history",
                                icon = "📋",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToLogs
                            )

                            QuickActionCard(
                                title = "Support",
                                subtitle = "Get help",
                                icon = "🆘",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToSupport
                            )
                        }
                    }
                }
            }

            // Setup Instructions (only when service is not running)
            if (!serviceRunning && !showWelcome) {
                AnimatedSlideInFromBottom(visible = true, durationMillis = 600) {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Setup Required",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Setup Required",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "To use HaramBlur, you need to enable the accessibility service:",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SetupStep(number = "1", text = "Tap 'Enable Service' below")
                                SetupStep(number = "2", text = "Find 'HaramBlur' in the list")
                                SetupStep(number = "3", text = "Toggle it ON")
                                SetupStep(number = "4", text = "Confirm by tapping 'Allow'")
                            }

                            Button(
                                onClick = { viewModel.openAccessibilitySettings(context) },
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
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    Text("Enable Accessibility Service")
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            if (!showWelcome) {
                AnimatedFadeIn(visible = !showWelcome, durationMillis = 1000) {
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
}

// Old components removed - using new component library instead
