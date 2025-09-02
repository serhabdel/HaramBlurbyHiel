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
fun DetectionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detection_settings_title)) },
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
                        text = stringResource(R.string.detection_sensitivity_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.detection_sensitivity_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SliderSetting(
                        title = stringResource(R.string.overall_detection_sensitivity_title),
                        description = stringResource(R.string.overall_detection_sensitivity_description),
                        value = settings.detectionSensitivity,
                        range = 0.3f..0.9f,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.detection_sensitivity_title_alt),
                        description = stringResource(R.string.detection_sensitivity_description_alt),
                        value = settings.detectionSensitivity,
                        range = 0.3f..0.9f,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.gender_confidence_threshold_title),
                        description = stringResource(R.string.gender_confidence_threshold_description),
                        value = settings.genderConfidenceThreshold,
                        range = 0.3f..0.8f,
                        onValueChange = { viewModel.updateGenderConfidenceThreshold(it) },
                        valueFormatter = { "${(it * 100).toInt()}%" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.nsfw_confidence_threshold_title),
                        description = stringResource(R.string.nsfw_confidence_threshold_description),
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
                        text = stringResource(R.string.advanced_detection_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.performance_monitoring_title),
                        description = stringResource(R.string.performance_monitoring_description),
                        checked = settings.enablePerformanceMonitoring,
                        onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.fallback_detection_title),
                        description = stringResource(R.string.fallback_detection_description),
                        checked = settings.enableFallbackDetection,
                        onCheckedChange = { viewModel.updateFallbackDetection(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}