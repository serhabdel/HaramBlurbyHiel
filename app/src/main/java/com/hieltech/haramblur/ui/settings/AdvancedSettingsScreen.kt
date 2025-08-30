package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.ui.components.*
import com.hieltech.haramblur.ui.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            // Developer & Logging Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Developer & Logging",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Detailed Logging",
                        description = "Enable detailed logging for troubleshooting",
                        checked = settings.enableDetailedLogging,
                        onCheckedChange = { viewModel.updateDetailedLogging(it) }
                    )

                    if (settings.enableDetailedLogging) {
                        RadioButtonGroup(
                            title = "Log Level",
                            options = LogLevel.values().map { it.displayName to it.description },
                            selectedIndex = LogLevel.values().indexOf(settings.logLevel),
                            onSelectionChange = { index ->
                                viewModel.updateLogLevel(LogLevel.values()[index])
                            }
                        )

                        SliderSetting(
                            title = "Log Retention",
                            description = "Days to keep logs before automatic cleanup",
                            value = settings.maxLogRetentionDays.toFloat(),
                            range = 1f..30f,
                            onValueChange = { viewModel.updateLogRetentionDays(it.toInt()) },
                            valueFormatter = { "${it.toInt()} days" }
                        )
                    }

                    SwitchSetting(
                        title = "Ultra Fast Mode",
                        description = "Maximum performance with reduced accuracy",
                        checked = settings.ultraFastModeEnabled,
                        onCheckedChange = { viewModel.updateUltraFastMode(it) }
                    )
                }
            }

            // Advanced Features Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Advanced Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Ultra Fast Mode",
                        description = "Maximum performance with reduced accuracy",
                        checked = settings.ultraFastModeEnabled,
                        onCheckedChange = { viewModel.updateUltraFastMode(it) }
                    )

                    SwitchSetting(
                        title = "Fallback Detection",
                        description = "Use backup detection methods if primary fails",
                        checked = settings.enableFallbackDetection,
                        onCheckedChange = { viewModel.updateFallbackDetection(it) }
                    )

                    SwitchSetting(
                        title = "User Action Logging",
                        description = "Log user interactions for troubleshooting",
                        checked = settings.enableUserActionLogging,
                        onCheckedChange = { viewModel.updateUserActionLogging(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}