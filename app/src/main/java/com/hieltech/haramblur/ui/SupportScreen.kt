package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.ui.components.*
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: SupportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supportState by viewModel.supportState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_help)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Help Section (without logs button - now in drawer)
            QuickHelpSection(supportState, onNavigateToSettings)

            // Developer & Community Section
            DeveloperCommunitySection(viewModel, context, scope)

            // Support the Project Section
            SupportTheProjectSection(viewModel, context, scope)

            // Troubleshooting Section
            TroubleshootingSection(supportState, viewModel, context)

            // Contact Support Section (enhanced)
            ContactSupportSection(viewModel, context, scope)

            // App Information Section
            AppInformationSection()

            // Privacy & Data Section
            PrivacyDataSection(viewModel, context, scope)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun QuickHelpSection(
    supportState: SupportState,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_help),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Service Status
            ServiceStatusIndicator(supportState.serviceRunning)

            // Quick Actions (Settings only - logs moved to drawer)
            QuickActionButton(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings),
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ServiceStatusIndicator(isRunning: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isRunning)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isRunning)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRunning) stringResource(R.string.service_active) else stringResource(R.string.service_inactive),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isRunning)
                        stringResource(R.string.haramlur_protecting)
                    else
                        stringResource(R.string.enable_accessibility_settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TroubleshootingSection(
    supportState: SupportState,
    viewModel: SupportViewModel,
    context: android.content.Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.troubleshooting),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Common Issues
            TroubleshootingItem(
                title = stringResource(R.string.content_not_blurring),
                description = stringResource(R.string.check_accessibility_enabled),
                action = {
                    viewModel.openAccessibilitySettings(context)
                },
                actionText = stringResource(R.string.open_settings)
            )

            TroubleshootingItem(
                title = stringResource(R.string.app_slow_laggy),
                description = stringResource(R.string.reduce_sensitivity),
                action = {
                    // Navigate to settings - this would need to be passed in
                },
                actionText = stringResource(R.string.adjust_settings)
            )

            TroubleshootingItem(
                title = stringResource(R.string.false_positives),
                description = stringResource(R.string.lower_confidence_threshold),
                action = {
                    // Navigate to settings
                },
                actionText = stringResource(R.string.fine_tune_detection)
            )

            // System Info
            Text(
                text = stringResource(R.string.system_information),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            SystemInfoItem(stringResource(R.string.android_version), supportState.androidVersion)
            SystemInfoItem(stringResource(R.string.device), supportState.deviceModel)
            SystemInfoItem(stringResource(R.string.app_version), supportState.appVersion)
            SystemInfoItem(stringResource(R.string.service_status), if (supportState.serviceRunning) stringResource(R.string.running) else stringResource(R.string.stopped))
        }
    }
}

@Composable
fun TroubleshootingItem(
    title: String,
    description: String,
    action: () -> Unit,
    actionText: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = action,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SystemInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ContactSupportSection(
    viewModel: SupportViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📞 Contact Support",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Need help? Choose your preferred support channel. GitHub issues are recommended for bug reports and feature requests.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Support Options
            SupportOptionCard(
                icon = Icons.Default.Email,
                title = "Email Support",
                description = "Send detailed support request with logs",
                onClick = {
                    scope.launch {
                        viewModel.sendEnhancedSupportEmail(context)
                    }
                }
            )

            SupportOptionCard(
                icon = Icons.Default.Warning,
                title = "Report Bug on GitHub",
                description = "Create issue with bug report template",
                onClick = { viewModel.openGitHubIssues(context) },
                isExternalLink = true
            )

            // Privacy Policy (kept for easy access)
            OutlinedButton(
                onClick = {
                    viewModel.openPrivacyPolicy(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Privacy Policy")
            }
        }
    }
}

@Composable
fun AppInformationSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📱 About HaramBlur",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "HaramBlur is an Islamic content filtering application that automatically detects and blurs inappropriate content across all apps on your Android device.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Key Features:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            val features = listOf(
                "🛡️ Real-time content detection",
                "🎯 Female-focused filtering",
                "⚡ GPU-accelerated processing",
                "🔒 Local processing (no data sent to servers)",
                "📊 Detailed logging for support",
                "⚙️ Customizable detection settings"
            )

            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyDataSection(
    viewModel: SupportViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔒 Privacy & Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your privacy is our priority. HaramBlur processes all content locally on your device.",
                style = MaterialTheme.typography.bodyMedium
            )

            val privacyPoints = listOf(
                "📱 No data is sent to external servers",
                "🔍 All processing happens on-device",
                "📊 Logs are stored locally for troubleshooting",
                "🗑️ Logs are automatically cleaned up",
                "🔐 No personal data collection",
                "🎯 Focused only on content filtering"
            )

            privacyPoints.forEach { point ->
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.exportDataSummary(context)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Data Summary")
                        }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Data")
                }
            }

            // Emergency section for stuck overlays
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🚨 Emergency Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use these if blur overlays get stuck or appear when they shouldn't.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.emergencyHideOverlays()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Force Hide All Overlays")
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperCommunitySection(
    viewModel: SupportViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    GitHubLinkCard(
        onRepositoryClick = { viewModel.openGitHubRepository(context) },
        onIssuesClick = { viewModel.openGitHubIssues(context) },
        onDiscussionsClick = { viewModel.openGitHubDiscussions(context) }
    )
}

@Composable
fun SupportTheProjectSection(
    viewModel: SupportViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donation Card
        DonationCard(
            onDonationClick = { viewModel.openBuyMeCoffee(context) }
        )

        // Community Section
        CommunitySection(
            onDocumentationClick = { viewModel.openDocumentation(context) },
            onShareAppClick = { viewModel.shareApp(context) }
        )
    }
}

data class SupportState(
    val serviceRunning: Boolean = false,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    val appVersion: String = "1.0.0"
)
