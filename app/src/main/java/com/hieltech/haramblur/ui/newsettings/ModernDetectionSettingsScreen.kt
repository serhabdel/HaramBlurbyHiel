package com.hieltech.haramblur.ui.newsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.AddCustomAppDialog
import com.hieltech.haramblur.ui.components.AppCategorySelectionGrid
import com.hieltech.haramblur.ui.components.CustomAppsManager
import com.hieltech.haramblur.ui.components.SliderSetting
import com.hieltech.haramblur.ui.components.SwitchSetting
import com.hieltech.haramblur.ui.components.getAppDisplayName
import com.hieltech.haramblur.ui.newsettings.ModernSettingsHeader
import com.hieltech.haramblur.ui.newsettings.ModernSettingsSection
import com.hieltech.haramblur.ui.newsettings.EnhancedSwitchSetting
import com.hieltech.haramblur.ui.newsettings.EnhancedSliderSetting
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernDetectionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var installedApps by remember { mutableStateOf<List<com.hieltech.haramblur.detection.AppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Load installed apps when screen is first displayed
    LaunchedEffect(Unit) {
        isLoadingApps = true
        installedApps = viewModel.getInstalledApps()
        isLoadingApps = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.detection_settings_title)) },
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
                    title = stringResource(R.string.detection_settings_title),
                    subtitle = stringResource(R.string.nav_detection_description),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { DetectionSensitivitySection(settings = settings, viewModel = viewModel) }

            item { AdvancedDetectionSection(settings = settings, viewModel = viewModel) }

            item { 
                AppSpecificDetectionSection(
                    settings = settings, 
                    viewModel = viewModel,
                    installedApps = installedApps,
                    isLoadingApps = isLoadingApps
                ) 
            }
        }
    }
}

@Composable
private fun DetectionSensitivitySection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.detection_sensitivity_title),
        subtitle = stringResource(R.string.detection_sensitivity_description),
        icon = Icons.Default.Settings
    ) {
        EnhancedSliderSetting(
            title = stringResource(R.string.overall_detection_sensitivity_title),
            description = stringResource(R.string.overall_detection_sensitivity_description),
            value = settings.detectionSensitivity,
            valueRange = 0.3f..0.9f,
            onValueChange = viewModel::updateSensitivity,
            valueFormatter = { "${(it * 100).toInt()}%" },
            icon = Icons.Default.Settings
        )

        Spacer(modifier = Modifier.height(12.dp))

        EnhancedSliderSetting(
            title = stringResource(R.string.gender_confidence_threshold_title),
            description = stringResource(R.string.gender_confidence_threshold_description),
            value = settings.genderConfidenceThreshold,
            valueRange = 0.3f..0.8f,
            onValueChange = viewModel::updateGenderConfidenceThreshold,
            valueFormatter = { "${(it * 100).toInt()}%" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        EnhancedSliderSetting(
            title = stringResource(R.string.nsfw_confidence_threshold_title),
            description = stringResource(R.string.nsfw_confidence_threshold_description),
            value = settings.nsfwConfidenceThreshold,
            // Allow down to 0.30 (see DetectionThresholds.MIN_NSFW_THRESHOLD)
            valueRange = 0.3f..0.7f,
            onValueChange = viewModel::updateNSFWConfidenceThreshold,
            valueFormatter = { "${(it * 100).toInt()}%" }
        )
    }
}

@Composable
private fun AdvancedDetectionSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.advanced_detection_title),
        subtitle = "Fine-tune detection algorithms for optimal results"
    ) {
        EnhancedSwitchSetting(
            title = stringResource(R.string.performance_monitoring_title),
            description = stringResource(R.string.performance_monitoring_description),
            icon = Icons.Default.Build,
            checked = settings.enablePerformanceMonitoring,
            onCheckedChange = viewModel::updatePerformanceMonitoring,
            badge = if (settings.enablePerformanceMonitoring) "ON" else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        EnhancedSwitchSetting(
            title = stringResource(R.string.fallback_detection_title),
            description = stringResource(R.string.fallback_detection_description),
            checked = settings.enableFallbackDetection,
            onCheckedChange = viewModel::updateFallbackDetection
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppSpecificDetectionSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    installedApps: List<com.hieltech.haramblur.detection.AppInfo>,
    isLoadingApps: Boolean
) {
    ModernSettingsSection(
        title = stringResource(R.string.app_specific_detection_title),
        subtitle = stringResource(R.string.app_specific_detection_description),
        icon = Icons.Default.Check
    ) {
        EnhancedSwitchSetting(
            title = stringResource(R.string.enable_app_specific_detection_title),
            description = stringResource(R.string.enable_app_specific_detection_description),
            icon = Icons.Default.Check,
            checked = settings.enableAppSpecificDetection,
            onCheckedChange = viewModel::updateAppSpecificDetection
        )

        AnimatedVisibility(
            visible = settings.enableAppSpecificDetection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.app_categories_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                AppCategorySelectionGrid(
                    selectedCategories = settings.monitoredAppCategories,
                    onCategoryToggle = { category, isSelected ->
                        val updated = if (isSelected) {
                            settings.monitoredAppCategories + category
                        } else {
                            settings.monitoredAppCategories - category
                        }
                        viewModel.updateMonitoredAppCategories(updated)
                    }
                )

                CustomAppsManager(
                    customApps = settings.customMonitoredApps,
                    onAddApp = { packageName ->
                        if (viewModel.canAddToMonitored(packageName)) {
                            val updated = settings.customMonitoredApps + packageName
                            viewModel.updateCustomMonitoredApps(updated)
                        }
                    },
                    onRemoveApp = { packageName ->
                        val updated = settings.customMonitoredApps - packageName
                        viewModel.updateCustomMonitoredApps(updated)
                    },
                    conflictingApps = settings.excludedApps,
                    installedApps = installedApps,
                    isLoadingApps = isLoadingApps
                )

                ExcludedAppsSection(
                    settings = settings, 
                    viewModel = viewModel,
                    installedApps = installedApps,
                    isLoadingApps = isLoadingApps
                )
            }
        }
    }
}

@Composable
private fun ExcludedAppsSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    installedApps: List<com.hieltech.haramblur.detection.AppInfo>,
    isLoadingApps: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.excluded_apps_title, settings.excludedApps.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.add_app_title))
            }
        }

        if (settings.excludedApps.isNotEmpty()) {
            Column(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settings.excludedApps.forEach { packageName ->
                    Surface(
                        tonalElevation = 0.dp,
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getAppDisplayName(packageName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                viewModel.updateExcludedApps(settings.excludedApps - packageName)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.no_excluded_apps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAddDialog) {
        AddCustomAppDialog(
            onDismiss = { showAddDialog = false },
            onAddApp = { packageName ->
                if (viewModel.canAddToExcluded(packageName)) {
                    viewModel.updateExcludedApps(settings.excludedApps + packageName)
                    showAddDialog = false
                }
            },
            existingApps = settings.excludedApps,
            conflictingApps = settings.customMonitoredApps,
            conflictMessage = "This app is already in the monitored list",
            installedApps = installedApps,
            isLoadingApps = isLoadingApps
        )
    }
}
