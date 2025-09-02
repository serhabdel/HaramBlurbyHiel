package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.ui.components.*
import com.hieltech.haramblur.ui.components.ServiceControlCard
import com.hieltech.haramblur.ui.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    // State for preset confirmation
    var showPresetConfirmation by remember { mutableStateOf<PresetData?>(null) }

    // Get available presets
    val availablePresets = remember { viewModel.getAvailablePresets() }
    val currentPreset = remember(settings.currentPreset) {
        availablePresets.find { it.name == settings.currentPreset } ?: availablePresets[1]
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.general_settings_title)) },
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
            // Service Control - Most important, show prominently
            ServiceControlCard(
                isServicePaused = settings.isServicePaused,
                onTogglePause = { viewModel.toggleServicePause() }
            )

            // Language Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.language_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.app_language_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Language selection
                    RadioButtonGroup(
                        title = stringResource(R.string.select_language_title),
                        options = com.hieltech.haramblur.detection.Language.values().map {
                            it.displayName to stringResource(R.string.language_description, it.displayName)
                        },
                        selectedIndex = com.hieltech.haramblur.detection.Language.values().indexOf(settings.preferredLanguage),
                        onSelectionChange = { index ->
                            val selectedLanguage = com.hieltech.haramblur.detection.Language.values()[index]
                            viewModel.updatePreferredLanguage(selectedLanguage)
                        }
                    )

                    // Language change notice
                    if (settings.preferredLanguage != com.hieltech.haramblur.detection.Language.ENGLISH) {
                        Text(
                            text = stringResource(R.string.language_changed_restart),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Quick Presets Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quick_presets_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.quick_presets_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Preset buttons in a row for better space usage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetButton(
                            name = stringResource(R.string.maximum_protection_name),
                            description = stringResource(R.string.maximum_protection_description),
                            icon = "🛡️",
                            gradientColors = listOf(
                                Color(0xFFD32F2F),
                                Color(0xFFB71C1C)
                            ),
                            isSelected = settings.currentPreset == "Maximum Protection",
                            onClick = {
                                val preset = PresetManager.createMaximumProtectionPreset()
                                showPresetConfirmation = preset
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PresetButton(
                            name = stringResource(R.string.optimal_performance_name),
                            description = stringResource(R.string.optimal_performance_description),
                            icon = "⚡",
                            gradientColors = listOf(
                                Color(0xFF2E7D32),
                                Color(0xFF1B5E20)
                            ),
                            isSelected = settings.currentPreset == "Optimal Performance",
                            onClick = {
                                val preset = PresetManager.createOptimalPerformancePreset()
                                showPresetConfirmation = preset
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Custom preset button
                    PresetButton(
                        name = stringResource(R.string.custom_settings_name),
                        description = stringResource(R.string.custom_settings_description),
                        icon = "⚙️",
                        gradientColors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF0D47A1)
                        ),
                        isSelected = settings.currentPreset == "Custom",
                        onClick = {
                            // Custom preset - no action needed, just show it's selected
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Basic Settings Section
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
                        text = stringResource(R.string.basic_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.face_detection_title),
                        description = stringResource(R.string.face_detection_description),
                        checked = settings.enableFaceDetection,
                        onCheckedChange = { viewModel.updateFaceDetection(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.nsfw_detection_title),
                        description = stringResource(R.string.nsfw_detection_description),
                        checked = settings.enableNSFWDetection,
                        onCheckedChange = { viewModel.updateNSFWDetection(it) }
                    )

                    if (settings.enableFaceDetection) {
                        SwitchSetting(
                            title = stringResource(R.string.detect_female_faces_title),
                            description = stringResource(R.string.detect_female_faces_description),
                            checked = settings.blurFemaleFaces,
                            onCheckedChange = { viewModel.updateFemaleBlur(it) }
                        )
                    }

                    RadioButtonGroup(
                        title = stringResource(R.string.blur_intensity_title),
                        options = listOf(
                            stringResource(R.string.blur_intensity_light) to stringResource(R.string.blur_intensity_light_description),
                            stringResource(R.string.blur_intensity_medium) to stringResource(R.string.blur_intensity_medium_description),
                            stringResource(R.string.blur_intensity_strong) to stringResource(R.string.blur_intensity_strong_description),
                            stringResource(R.string.blur_intensity_maximum) to stringResource(R.string.blur_intensity_maximum_description)
                        ),
                        selectedIndex = when (settings.blurIntensity) {
                            BlurIntensity.LIGHT -> 0
                            BlurIntensity.MEDIUM -> 1
                            BlurIntensity.STRONG -> 2
                            BlurIntensity.MAXIMUM -> 3
                        },
                        onSelectionChange = { index ->
                            val intensity = when (index) {
                                0 -> BlurIntensity.LIGHT
                                1 -> BlurIntensity.MEDIUM
                                2 -> BlurIntensity.STRONG
                                else -> BlurIntensity.MAXIMUM
                            }
                            viewModel.updateBlurIntensity(intensity)
                        }
                    )

                    // Theme Selection
                    var expanded by remember { mutableStateOf(false) }
                    val themeOptions = listOf(
                        AppTheme.ISLAMIC_LIGHT to stringResource(R.string.islamic_light_theme),
                        AppTheme.ISLAMIC_DARK to stringResource(R.string.islamic_dark_theme),
                        AppTheme.MODERN_LIGHT to stringResource(R.string.modern_light_theme),
                        AppTheme.MODERN_DARK to stringResource(R.string.modern_dark_theme),
                        AppTheme.MINIMAL_LIGHT to stringResource(R.string.minimal_light_theme),
                        AppTheme.MINIMAL_DARK to stringResource(R.string.minimal_dark_theme)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = settings.appTheme.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.app_theme_title)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            themeOptions.forEach { (theme, description) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(theme.displayName)
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateAppTheme(theme)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Preset Confirmation Dialog
    showPresetConfirmation?.let { preset ->
        val settingsDiff = viewModel.getSettingsDiff(preset.settings)
        PresetConfirmationDialog(
            preset = preset,
            settingsDiff = settingsDiff,
            onConfirm = {
                viewModel.applyPresetWithBackup(preset)
                showPresetConfirmation = null
            },
            onCancel = { showPresetConfirmation = null }
        )
    }
}