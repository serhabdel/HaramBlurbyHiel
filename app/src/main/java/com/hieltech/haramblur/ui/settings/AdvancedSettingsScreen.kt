package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
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
                title = { Text(stringResource(R.string.advanced_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
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
                        text = stringResource(R.string.developer_logging_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.detailed_logging_title),
                        description = stringResource(R.string.detailed_logging_description),
                        checked = settings.enableDetailedLogging,
                        onCheckedChange = { viewModel.updateDetailedLogging(it) }
                    )

                    if (settings.enableDetailedLogging) {
                        RadioButtonGroup(
                            title = stringResource(R.string.log_level_title),
                            options = LogLevel.values().map { it.displayName to it.description },
                            selectedIndex = LogLevel.values().indexOf(settings.logLevel),
                            onSelectionChange = { index ->
                                viewModel.updateLogLevel(LogLevel.values()[index])
                            }
                        )

                        SliderSetting(
                            title = stringResource(R.string.log_retention_title),
                            description = stringResource(R.string.log_retention_description),
                            value = settings.maxLogRetentionDays.toFloat(),
                            range = 1f..30f,
                            onValueChange = { viewModel.updateLogRetentionDays(it.toInt()) },
                            valueFormatter = { "${it.toInt()} days" }
                        )
                    }

                    SwitchSetting(
                        title = stringResource(R.string.ultra_fast_mode_title),
                        description = stringResource(R.string.ultra_fast_mode_description),
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
                        text = stringResource(R.string.advanced_features_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.ultra_fast_mode_title),
                        description = stringResource(R.string.ultra_fast_mode_description),
                        checked = settings.ultraFastModeEnabled,
                        onCheckedChange = { viewModel.updateUltraFastMode(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.fallback_detection_title),
                        description = stringResource(R.string.fallback_detection_description),
                        checked = settings.enableFallbackDetection,
                        onCheckedChange = { viewModel.updateFallbackDetection(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.user_action_logging_title),
                        description = stringResource(R.string.user_action_logging_description),
                        checked = settings.enableUserActionLogging,
                        onCheckedChange = { viewModel.updateUserActionLogging(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}