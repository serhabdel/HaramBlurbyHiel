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
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.ui.components.*
import com.hieltech.haramblur.ui.ServiceControlCard
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
                title = { Text("General Settings") },
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
            // Service Control - Most important, show prominently
            ServiceControlCard(
                isServicePaused = settings.isServicePaused,
                onTogglePause = { viewModel.toggleServicePause() }
            )

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
                        text = "Quick Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Choose a preset to quickly configure common settings combinations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Preset buttons in a row for better space usage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetButton(
                            name = "Maximum Protection",
                            description = "Ultimate privacy with maximum content blocking",
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
                            name = "Optimal Performance",
                            description = "Balanced settings for best speed and protection",
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
                        name = "Custom Settings",
                        description = "Your personalized configuration",
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
                        text = "Basic Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Face Detection",
                        description = "Detect and blur faces in images",
                        checked = settings.enableFaceDetection,
                        onCheckedChange = { viewModel.updateFaceDetection(it) }
                    )

                    SwitchSetting(
                        title = "NSFW Content Detection",
                        description = "Detect and blur inappropriate content",
                        checked = settings.enableNSFWDetection,
                        onCheckedChange = { viewModel.updateNSFWDetection(it) }
                    )

                    if (settings.enableFaceDetection) {
                        SwitchSetting(
                            title = "Detect Female Faces",
                            description = "Automatically detect and blur female faces",
                            checked = settings.blurFemaleFaces,
                            onCheckedChange = { viewModel.updateFemaleBlur(it) }
                        )
                    }

                    RadioButtonGroup(
                        title = "Blur Intensity",
                        options = listOf(
                            "Light" to "Minimal blur, maintains some visibility",
                            "Medium" to "Balanced blur for general content",
                            "Strong" to "Heavy blur for maximum privacy",
                            "Maximum" to "Complete coverage for sensitive content"
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