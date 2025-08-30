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
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
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
            // Blur Style Section
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
                        text = "Blur Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Choose how detected content should be blurred",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RadioButtonGroup(
                        title = "Blur Style",
                        options = listOf(
                            "Artistic" to "Film grain style blur effect (Recommended)",
                            "Solid" to "Simple gray overlay",
                            "Pixelated" to "Mosaic-style blur effect",
                            "Noise" to "Random pattern blur",
                            "Combined" to "Multiple blur effects layered"
                        ),
                        selectedIndex = when (settings.blurStyle) {
                            BlurStyle.ARTISTIC -> 0
                            BlurStyle.SOLID -> 1
                            BlurStyle.PIXELATED -> 2
                            BlurStyle.NOISE -> 3
                            BlurStyle.COMBINED -> 4
                        },
                        onSelectionChange = { index ->
                            val style = when (index) {
                                0 -> BlurStyle.ARTISTIC
                                1 -> BlurStyle.SOLID
                                2 -> BlurStyle.PIXELATED
                                3 -> BlurStyle.NOISE
                                else -> BlurStyle.COMBINED
                            }
                            viewModel.updateBlurStyle(style)
                        }
                    )
                }
            }

            // Blur Customization Section
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
                        text = "Blur Customization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SliderSetting(
                        title = "Blur Area Expansion",
                        description = "Pixels to expand around detected areas",
                        value = settings.expandBlurArea.toFloat(),
                        range = 10f..100f,
                        onValueChange = { viewModel.updateBlurExpansion(it.toInt()) },
                        valueFormatter = { "${it.toInt()}px" }
                    )

                    SliderSetting(
                        title = "Blur Expansion",
                        description = "Pixels to expand around detected areas",
                        value = settings.expandBlurArea.toFloat(),
                        range = 10f..100f,
                        onValueChange = { viewModel.updateBlurExpansion(it.toInt()) },
                        valueFormatter = { "${it.toInt()}px" }
                    )
                }
            }

            // Privacy Controls Section
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
                        text = "Privacy Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Show Blur Borders",
                        description = "Display borders around blurred areas",
                        checked = settings.showBlurBorders,
                        onCheckedChange = { viewModel.updateBlurBorders(it) }
                    )

                    SwitchSetting(
                        title = "Full Screen Blur for NSFW",
                        description = "Apply full screen blur when NSFW content is detected",
                        checked = settings.enableFullScreenBlurForNSFW,
                        onCheckedChange = { viewModel.updateFullScreenBlur(it) }
                    )

                    SwitchSetting(
                        title = "Ultra Fast Mode",
                        description = "Maximum performance with reduced accuracy",
                        checked = settings.ultraFastModeEnabled,
                        onCheckedChange = { viewModel.updateUltraFastMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}