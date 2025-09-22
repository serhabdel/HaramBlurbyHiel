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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.LogLevel
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.RadioButtonGroup
import com.hieltech.haramblur.ui.components.SliderSetting
import com.hieltech.haramblur.ui.components.SwitchSetting

@Composable
fun ModernAdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.advanced_settings_title)) },
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
                    title = stringResource(R.string.advanced_settings_title),
                    subtitle = stringResource(R.string.nav_advanced_description),
                    modifier = Modifier.fillMaxSize()
                )
            }

            item { DeveloperLoggingSection(settings = settings, viewModel = viewModel) }

            item { AdvancedFeaturesSection(settings = settings, viewModel = viewModel) }
        }
    }
}

@Composable
private fun DeveloperLoggingSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.developer_logging_title)
    ) {
        SwitchSetting(
            title = stringResource(R.string.detailed_logging_title),
            description = stringResource(R.string.detailed_logging_description),
            checked = settings.enableDetailedLogging,
            onCheckedChange = viewModel::updateDetailedLogging
        )

        AnimatedVisibility(
            visible = settings.enableDetailedLogging,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButtonGroup(
                    title = stringResource(R.string.log_level_title),
                    options = LogLevel.values().map { level ->
                        level.displayName to level.description
                    },
                    selectedIndex = LogLevel.values().indexOf(settings.logLevel),
                    onSelectionChange = { index -> viewModel.updateLogLevel(LogLevel.values()[index]) }
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
        }

        SwitchSetting(
            title = stringResource(R.string.ultra_fast_mode_title),
            description = stringResource(R.string.ultra_fast_mode_description),
            checked = settings.ultraFastModeEnabled,
            onCheckedChange = viewModel::updateUltraFastMode
        )
    }
}

@Composable
private fun AdvancedFeaturesSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.advanced_features_title)
    ) {
        SwitchSetting(
            title = stringResource(R.string.ultra_fast_mode_title),
            description = stringResource(R.string.ultra_fast_mode_description),
            checked = settings.ultraFastModeEnabled,
            onCheckedChange = viewModel::updateUltraFastMode
        )

        SwitchSetting(
            title = stringResource(R.string.fallback_detection_title),
            description = stringResource(R.string.fallback_detection_description),
            checked = settings.enableFallbackDetection,
            onCheckedChange = viewModel::updateFallbackDetection
        )

        SwitchSetting(
            title = stringResource(R.string.user_action_logging_title),
            description = stringResource(R.string.user_action_logging_description),
            checked = settings.enableUserActionLogging,
            onCheckedChange = viewModel::updateUserActionLogging
        )
    }
}
