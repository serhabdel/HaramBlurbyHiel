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
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Settings") },
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
                        text = "Processing Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Balance between speed and accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RadioButtonGroup(
                        title = "Processing Speed",
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
                        text = "Hardware Acceleration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "GPU Acceleration",
                        description = "Use GPU for faster detection (recommended)",
                        checked = settings.enableGPUAcceleration,
                        onCheckedChange = { viewModel.updateGPUAcceleration(it) }
                    )

                    SwitchSetting(
                        title = "Real-time Processing",
                        description = "Process content instantly as it appears",
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
                        text = "Resource Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SliderSetting(
                        title = "Max Processing Time",
                        description = "Maximum time allowed for detection per frame",
                        value = settings.maxProcessingTimeMs.toFloat(),
                        range = 25f..200f,
                        onValueChange = { viewModel.updateMaxProcessingTime(it.toLong()) },
                        valueFormatter = { "${it.toInt()}ms" }
                    )

                    SliderSetting(
                        title = "Frame Skip Threshold",
                        description = "Skip frames when processing is slow",
                        value = settings.frameSkipThreshold.toFloat(),
                        range = 1f..10f,
                        onValueChange = { viewModel.updateFrameSkipThreshold(it.toInt()) },
                        valueFormatter = { "${it.toInt()} frames" }
                    )

                    SliderSetting(
                        title = "Image Downscale Ratio",
                        description = "Reduce image size for faster processing",
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
                        text = "Performance Monitoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Performance Monitoring",
                        description = "Log performance metrics and timing information",
                        checked = settings.enablePerformanceMonitoring,
                        onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                    )

                    if (settings.enablePerformanceMonitoring) {
                        SwitchSetting(
                            title = "Performance Logging",
                            description = "Log performance metrics and timing information",
                            checked = settings.enablePerformanceLogging,
                            onCheckedChange = { viewModel.updatePerformanceLogging(it) }
                        )

                        SwitchSetting(
                            title = "Error Reporting",
                            description = "Log errors and crashes for debugging",
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