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
fun DetectionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection Settings") },
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
            // Detection Sensitivity Section
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
                        text = "Detection Sensitivity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Adjust how sensitive the detection should be. Higher values detect more content but may blur normal images.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SliderSetting(
                        title = "Overall Detection Sensitivity",
                        description = "General sensitivity for all detection types",
                        value = settings.detectionSensitivity,
                        range = 0.3f..0.9f,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = "Detection Sensitivity",
                        description = "Overall sensitivity for content detection",
                        value = settings.detectionSensitivity,
                        range = 0.3f..0.9f,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = "Gender Confidence Threshold",
                        description = "Minimum confidence for gender classification",
                        value = settings.genderConfidenceThreshold,
                        range = 0.3f..0.8f,
                        onValueChange = { viewModel.updateGenderConfidenceThreshold(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = "NSFW Confidence Threshold",
                        description = "Minimum confidence for NSFW content detection",
                        value = settings.nsfwConfidenceThreshold,
                        range = 0.4f..0.7f,
                        onValueChange = { viewModel.updateNSFWConfidenceThreshold(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )
                }
            }

            // Advanced Detection Options
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
                        text = "Advanced Detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Performance Monitoring",
                        description = "Log performance metrics and timing information",
                        checked = settings.enablePerformanceMonitoring,
                        onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                    )

                    SwitchSetting(
                        title = "Fallback Detection",
                        description = "Use fallback detection methods if primary fails",
                        checked = settings.enableFallbackDetection,
                        onCheckedChange = { viewModel.updateFallbackDetection(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}