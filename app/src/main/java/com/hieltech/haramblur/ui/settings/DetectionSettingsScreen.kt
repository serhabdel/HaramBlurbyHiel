package com.hieltech.haramblur.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.ui.components.*
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.getAppDisplayName

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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

            // App-Specific Detection Settings
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_specific_detection_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.app_specific_detection_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Master toggle
                    SwitchSetting(
                        title = stringResource(R.string.enable_app_specific_detection_title),
                        description = stringResource(R.string.enable_app_specific_detection_description),
                        checked = settings.enableAppSpecificDetection,
                        onCheckedChange = { viewModel.updateAppSpecificDetection(it) }
                    )

                    // App Category Selection (shown when app-specific detection is enabled)
                    AnimatedVisibility(
                        visible = settings.enableAppSpecificDetection,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_categories_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            AppCategorySelectionGrid(
                                selectedCategories = settings.monitoredAppCategories,
                                onCategoryToggle = { category, isSelected ->
                                    val updatedCategories = if (isSelected) {
                                        settings.monitoredAppCategories + category
                                    } else {
                                        settings.monitoredAppCategories - category
                                    }
                                    viewModel.updateMonitoredAppCategories(updatedCategories)
                                }
                            )
                        }
                    }

                    // Custom Apps Section (shown when app-specific detection is enabled)
                    AnimatedVisibility(
                        visible = settings.enableAppSpecificDetection,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CustomAppsManager(
                                customApps = settings.customMonitoredApps,
                                onAddApp = { packageName ->
                                    if (viewModel.canAddToMonitored(packageName)) {
                                        val updatedApps = settings.customMonitoredApps + packageName
                                        viewModel.updateCustomMonitoredApps(updatedApps)
                                    }
                                },
                                onRemoveApp = { packageName ->
                                    val updatedApps = settings.customMonitoredApps - packageName
                                    viewModel.updateCustomMonitoredApps(updatedApps)
                                },
                                conflictingApps = settings.excludedApps
                            )
                        }
                    }

                    // Excluded Apps Section (shown when app-specific detection is enabled)
                    AnimatedVisibility(
                        visible = settings.enableAppSpecificDetection,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        var showAddExcludedDialog by remember { mutableStateOf(false) }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.excluded_apps_title, settings.excludedApps.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedButton(
                                    onClick = { showAddExcludedDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.add_app_title))
                                }
                            }

                            // List of excluded apps
                            if (settings.excludedApps.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.heightIn(max = 150.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    settings.excludedApps.forEach { packageName ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
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
                                                IconButton(
                                                    onClick = {
                                                        val updatedApps = settings.excludedApps - packageName
                                                        viewModel.updateExcludedApps(updatedApps)
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove",
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

                        // Add excluded app dialog
                        if (showAddExcludedDialog) {
                            AddCustomAppDialog(
                                onDismiss = { showAddExcludedDialog = false },
                                onAddApp = { packageName ->
                                    if (viewModel.canAddToExcluded(packageName)) {
                                        val updatedApps = settings.excludedApps + packageName
                                        viewModel.updateExcludedApps(updatedApps)
                                        showAddExcludedDialog = false
                                    }
                                },
                                existingApps = settings.excludedApps,
                                conflictingApps = settings.customMonitoredApps,
                                conflictMessage = "This app is already in the monitored list"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
