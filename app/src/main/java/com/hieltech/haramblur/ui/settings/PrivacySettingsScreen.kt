package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_settings_title)) },
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
                        text = stringResource(R.string.blur_style_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.blur_style_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RadioButtonGroup(
                        title = stringResource(R.string.blur_style_title),
                        options = listOf(
                            stringResource(R.string.blur_style_artistic) to stringResource(R.string.blur_style_artistic_description),
                            stringResource(R.string.blur_style_solid) to stringResource(R.string.blur_style_solid_description),
                            stringResource(R.string.blur_style_pixelated) to stringResource(R.string.blur_style_pixelated_description),
                            stringResource(R.string.blur_style_noise) to stringResource(R.string.blur_style_noise_description),
                            stringResource(R.string.blur_style_combined) to stringResource(R.string.blur_style_combined_description)
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
                        text = stringResource(R.string.blur_customization_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SliderSetting(
                        title = stringResource(R.string.blur_area_expansion_title),
                        description = stringResource(R.string.blur_area_expansion_description),
                        value = settings.expandBlurArea.toFloat(),
                        range = 10f..100f,
                        onValueChange = { viewModel.updateBlurExpansion(it.toInt()) },
                        valueFormatter = { "${it.toInt()}px" }
                    )

                    SliderSetting(
                        title = stringResource(R.string.blur_expansion_title),
                        description = stringResource(R.string.blur_expansion_description),
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
                        text = stringResource(R.string.privacy_controls_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.show_blur_borders_title),
                        description = stringResource(R.string.show_blur_borders_description),
                        checked = settings.showBlurBorders,
                        onCheckedChange = { viewModel.updateBlurBorders(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.full_screen_blur_nsfw_title),
                        description = stringResource(R.string.full_screen_blur_nsfw_description),
                        checked = settings.enableFullScreenBlurForNSFW,
                        onCheckedChange = { viewModel.updateFullScreenBlur(it) }
                    )

                    SwitchSetting(
                        title = stringResource(R.string.ultra_fast_mode_title),
                        description = stringResource(R.string.ultra_fast_mode_description),
                        checked = settings.ultraFastModeEnabled,
                        onCheckedChange = { viewModel.updateUltraFastMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
