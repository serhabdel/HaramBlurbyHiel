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
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.performance_settings_title)) },
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
            // Processing Speed Section
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
                        text = stringResource(R.string.processing_speed_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.processing_speed_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RadioButtonGroup(
                        title = stringResource(R.string.processing_speed_title),
                        options = ProcessingSpeed.values().map { it.name to it.description },
                        selectedIndex = ProcessingSpeed.values().indexOf(settings.processingSpeed),
                        onSelectionChange = { index ->
                            viewModel.updateProcessingSpeed(ProcessingSpeed.values()[index])
                        }
                    )
                }
            }

            // Hardware Acceleration Section
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
                        text = stringResource(R.string.hardware_acceleration_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.gpu_acceleration_title),
                        description = stringResource(R.string.gpu_acceleration_description),
                        checked = settings.enableGPUAcceleration,
                        onCheckedChange = { viewModel.updateGPUAcceleration(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.real_time_processing_title),
                        description = stringResource(R.string.real_time_processing_description),
                        checked = settings.enableRealTimeProcessing,
                        onCheckedChange = { viewModel.updateRealTimeProcessing(it) }
                    )
                }
            }

            // Resource Management Section
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
                        text = stringResource(R.string.resource_management_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SliderSetting(
                        title = stringResource(R.string.max_processing_time_title),
                        description = stringResource(R.string.max_processing_time_description),
                        value = settings.maxProcessingTimeMs.toFloat(),
                        range = 25f..200f,
                        onValueChange = { viewModel.updateMaxProcessingTime(it.toLong()) },
                        valueFormatter = { "${it.toInt()}ms" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.frame_skip_threshold_title),
                        description = stringResource(R.string.frame_skip_threshold_description),
                        value = settings.frameSkipThreshold.toFloat(),
                        range = 1f..10f,
                        onValueChange = { viewModel.updateFrameSkipThreshold(it.toInt()) },
                        valueFormatter = { "${it.toInt()} frames" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.image_downscale_ratio_title),
                        description = stringResource(R.string.image_downscale_ratio_description),
                        value = settings.imageDownscaleRatio,
                        range = 0.3f..1.0f,
                        onValueChange = { viewModel.updateImageDownscaleRatio(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )
                }
            }

            // Performance Monitoring Section
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
                        text = stringResource(R.string.performance_monitoring_title_alt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.performance_monitoring_title),
                        description = stringResource(R.string.performance_monitoring_description),
                        checked = settings.enablePerformanceMonitoring,
                        onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                    )

                    if (settings.enablePerformanceMonitoring) {
                        SwitchSetting(
                            title = stringResource(R.string.performance_logging_title),
                            description = stringResource(R.string.performance_logging_description),
                            checked = settings.enablePerformanceLogging,
                            onCheckedChange = { viewModel.updatePerformanceLogging(it) }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.error_reporting_title),
                            description = stringResource(R.string.error_reporting_description),
                            checked = settings.enableErrorReporting,
                            onCheckedChange = { viewModel.updateErrorReporting(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}