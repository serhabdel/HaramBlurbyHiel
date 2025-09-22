package com.hieltech.haramblur.ui.newsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.RadioButtonGroup
import com.hieltech.haramblur.ui.components.SliderSetting
import com.hieltech.haramblur.ui.components.SwitchSetting

@Composable
fun ModernPerformanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.performance_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ModernSettingsHeader(
                    title = stringResource(R.string.performance_settings_title),
                    subtitle = stringResource(R.string.nav_performance_description),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { ProcessingSpeedSection(settings = settings, onSpeedSelected = viewModel::updateProcessingSpeed) }

            item { HardwareAccelerationSection(settings = settings, viewModel = viewModel) }

            item { ResourceManagementSection(settings = settings, viewModel = viewModel) }

            item { PerformanceMonitoringSection(settings = settings, viewModel = viewModel) }
        }
    }
}

@Composable
private fun ProcessingSpeedSection(
    settings: AppSettings,
    onSpeedSelected: (ProcessingSpeed) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.processing_speed_title),
        subtitle = stringResource(R.string.processing_speed_description)
    ) {
        RadioButtonGroup(
            title = stringResource(R.string.processing_speed_title),
            options = ProcessingSpeed.values().map { option ->
                option.displayName to option.description
            },
            selectedIndex = ProcessingSpeed.values().indexOf(settings.processingSpeed),
            onSelectionChange = { index -> onSpeedSelected(ProcessingSpeed.values()[index]) }
        )
    }
}

@Composable
private fun HardwareAccelerationSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.hardware_acceleration_title)
    ) {
        SwitchSetting(
            title = stringResource(R.string.gpu_acceleration_title),
            description = stringResource(R.string.gpu_acceleration_description),
            checked = settings.enableGPUAcceleration,
            onCheckedChange = viewModel::updateGPUAcceleration
        )

        SwitchSetting(
            title = stringResource(R.string.real_time_processing_title),
            description = stringResource(R.string.real_time_processing_description),
            checked = settings.enableRealTimeProcessing,
            onCheckedChange = viewModel::updateRealTimeProcessing
        )
    }
}

@Composable
private fun ResourceManagementSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.resource_management_title)
    ) {
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
            onValueChange = viewModel::updateImageDownscaleRatio,
            valueFormatter = { "${(it * 100).toInt()}%" }
        )
    }
}

@Composable
private fun PerformanceMonitoringSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.performance_monitoring_title_alt)
    ) {
        SwitchSetting(
            title = stringResource(R.string.performance_monitoring_title),
            description = stringResource(R.string.performance_monitoring_description),
            checked = settings.enablePerformanceMonitoring,
            onCheckedChange = viewModel::updatePerformanceMonitoring
        )

        AnimatedVisibility(
            visible = settings.enablePerformanceMonitoring,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchSetting(
                    title = stringResource(R.string.performance_logging_title),
                    description = stringResource(R.string.performance_logging_description),
                    checked = settings.enablePerformanceLogging,
                    onCheckedChange = viewModel::updatePerformanceLogging
                )

                SwitchSetting(
                    title = stringResource(R.string.error_reporting_title),
                    description = stringResource(R.string.error_reporting_description),
                    checked = settings.enableErrorReporting,
                    onCheckedChange = viewModel::updateErrorReporting
                )
            }
        }
    }
}
