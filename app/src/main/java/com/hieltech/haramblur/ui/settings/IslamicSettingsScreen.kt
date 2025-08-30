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
fun IslamicSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Islamic Settings") },
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
            // Quranic Guidance Section
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
                        text = "Quranic Guidance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = "Quranic Guidance",
                        description = "Show Quranic verses when blocking content",
                        checked = settings.enableQuranicGuidance,
                        onCheckedChange = { viewModel.updateQuranicGuidance(it) }
                    )

                    if (settings.enableQuranicGuidance) {
                        RadioButtonGroup(
                            title = "Preferred Language",
                            options = com.hieltech.haramblur.detection.Language.values().map {
                                it.displayName to "Language for Islamic guidance"
                            },
                            selectedIndex = com.hieltech.haramblur.detection.Language.values().indexOf(settings.preferredLanguage),
                            onSelectionChange = { index ->
                                viewModel.updatePreferredLanguage(com.hieltech.haramblur.detection.Language.values()[index])
                            }
                        )

                        SliderSetting(
                            title = "Verse Display Duration",
                            description = "How long to display Quranic verses",
                            value = settings.verseDisplayDuration.toFloat(),
                            range = 5f..30f,
                            onValueChange = { viewModel.updateVerseDisplayDuration(it.toInt()) },
                            valueFormatter = { "${it.toInt()}s" }
                        )

                        SwitchSetting(
                            title = "Arabic Text Display",
                            description = "Show original Arabic text alongside translations",
                            checked = settings.enableArabicText,
                            onCheckedChange = { viewModel.updateArabicText(it) }
                        )

                        SliderSetting(
                            title = "Custom Reflection Time",
                            description = "Additional reflection time for spiritual guidance",
                            value = settings.customReflectionTime.toFloat(),
                            range = 5f..60f,
                            onValueChange = { viewModel.updateCustomReflectionTime(it.toInt()) },
                            valueFormatter = { "${it.toInt()}s" }
                        )
                    }
                }
            }

            // Dhikr Settings Section
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
                        text = "Dhikr (Remembrance)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Islamic remembrances to help maintain spiritual awareness",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SwitchSetting(
                        title = "Enable Dhikr",
                        description = "Show Islamic remembrances throughout the day",
                        checked = settings.dhikrEnabled,
                        onCheckedChange = { viewModel.updateDhikrEnabled(it) }
                    )

                    if (settings.dhikrEnabled) {
                        SwitchSetting(
                            title = "Morning Dhikr",
                            description = "Display morning remembrances (5 AM - 10 AM)",
                            checked = settings.dhikrMorningEnabled,
                            onCheckedChange = { viewModel.updateDhikrMorningEnabled(it) }
                        )

                        SwitchSetting(
                            title = "Evening Dhikr",
                            description = "Display evening remembrances (5 PM - 10 PM)",
                            checked = settings.dhikrEveningEnabled,
                            onCheckedChange = { viewModel.updateDhikrEveningEnabled(it) }
                        )

                        SwitchSetting(
                            title = "Anytime Dhikr",
                            description = "Display general remembrances throughout the day",
                            checked = settings.dhikrAnytimeEnabled,
                            onCheckedChange = { viewModel.updateDhikrAnytimeEnabled(it) }
                        )

                        SliderSetting(
                            title = "Display Interval",
                            description = "Minutes between dhikr displays",
                            value = settings.dhikrIntervalMinutes.toFloat(),
                            range = 15f..240f,
                            onValueChange = { viewModel.updateDhikrInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )

                        SliderSetting(
                            title = "Display Duration",
                            description = "How long to display each dhikr",
                            value = settings.dhikrDisplayDuration.toFloat(),
                            range = 5f..30f,
                            onValueChange = { viewModel.updateDhikrDisplayDuration(it.toInt()) },
                            valueFormatter = { "${it.toInt()}s" }
                        )

                        RadioButtonGroup(
                            title = "Display Position",
                            options = listOf(
                                "TOP_RIGHT" to "Top Right",
                                "TOP_LEFT" to "Top Left",
                                "BOTTOM_RIGHT" to "Bottom Right",
                                "BOTTOM_LEFT" to "Bottom Left",
                                "CENTER" to "Center"
                            ),
                            selectedIndex = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                                .indexOf(settings.dhikrPosition),
                            onSelectionChange = { index ->
                                val positions = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                                viewModel.updateDhikrPosition(positions[index])
                            }
                        )

                        SwitchSetting(
                            title = "Show Transliteration",
                            description = "Display romanized pronunciation",
                            checked = settings.dhikrShowTransliteration,
                            onCheckedChange = { viewModel.updateDhikrShowTransliteration(it) }
                        )

                        SwitchSetting(
                            title = "Show Translation",
                            description = "Display English translation",
                            checked = settings.dhikrShowTranslation,
                            onCheckedChange = { viewModel.updateDhikrShowTranslation(it) }
                        )

                        SwitchSetting(
                            title = "Animation",
                            description = "Enable slide-in animation",
                            checked = settings.dhikrAnimationEnabled,
                            onCheckedChange = { viewModel.updateDhikrAnimationEnabled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}