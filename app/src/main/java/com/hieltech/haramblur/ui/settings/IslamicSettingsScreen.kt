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
import com.hieltech.haramblur.ui.components.CitySelector
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

            // Prayer Times & Islamic Calendar Section
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
                        text = "Prayer Times & Islamic Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Islamic prayer times and calendar features",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SwitchSetting(
                        title = "Enable Prayer Times",
                        description = "Show prayer times and Islamic calendar",
                        checked = settings.enablePrayerTimes,
                        onCheckedChange = { viewModel.updatePrayerTimesEnabled(it) }
                    )

                    // Location Settings Section
                    if (settings.enablePrayerTimes) {
                        Text(
                            text = "Location Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = "Accurate prayer times depend on your location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SwitchSetting(
                            title = "Auto-detect Location",
                            description = "Use device GPS for automatic location detection",
                            checked = settings.autoDetectLocation,
                            onCheckedChange = { viewModel.updateAutoDetectLocation(it) }
                        )

                        // Current location display
                        if (settings.locationLatitude != null && settings.locationLongitude != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Current Location",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${settings.locationCity ?: "Unknown"}, ${settings.locationCountry ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Lat: ${String.format("%.4f", settings.locationLatitude)}, Lng: ${String.format("%.4f", settings.locationLongitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Manual location entry (when auto-detect is disabled)
                        if (!settings.autoDetectLocation) {
                            CitySelector(
                                selectedCity = settings.preferredCity,
                                selectedCountry = settings.preferredCountry,
                                onCitySelected = { city, country ->
                                    viewModel.updatePreferredCity(city)
                                    viewModel.updatePreferredCountry(country)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (settings.enablePrayerTimes) {
                        SwitchSetting(
                            title = "Prayer Notifications",
                            description = "Get notified before prayer times",
                            checked = settings.enablePrayerNotifications,
                            onCheckedChange = { viewModel.updatePrayerNotifications(it) }
                        )

                        if (settings.enablePrayerNotifications) {
                            SliderSetting(
                                title = "Advance Notice",
                                description = "Minutes before prayer to notify",
                                value = settings.prayerNotificationAdvanceTime.toFloat(),
                                range = 5f..60f,
                                onValueChange = { viewModel.updateNotificationAdvanceTime(it.toInt()) },
                                valueFormatter = { "${it.toInt()} min" }
                            )
                        }

                        // Location Settings
                        Text(
                            text = "Location Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Prayer times are calculated based on your location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Location display
                        if (settings.locationLatitude != null && settings.locationLongitude != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Current Location",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${settings.locationCity ?: "Unknown"}, ${settings.locationCountry ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Lat: ${String.format("%.4f", settings.locationLatitude)}, Lng: ${String.format("%.4f", settings.locationLongitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Calculation Method
                        RadioButtonGroup(
                            title = "Calculation Method",
                            options = listOf(
                                "1" to "University of Islamic Sciences, Karachi",
                                "2" to "Islamic Society of North America (ISNA)",
                                "3" to "Muslim World League",
                                "4" to "Umm Al-Qura University, Makkah",
                                "5" to "Egyptian General Authority of Survey"
                            ),
                            selectedIndex = (settings.prayerCalculationMethod - 1).coerceIn(0, 4),
                            onSelectionChange = { index ->
                                viewModel.updateCalculationMethod(index + 1)
                            }
                        )

                        SliderSetting(
                            title = "Update Interval",
                            description = "Minutes between prayer times updates",
                            value = settings.prayerTimesUpdateInterval.toFloat(),
                            range = 15f..120f,
                            onValueChange = { viewModel.updatePrayerTimesUpdateInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )
                    }

                    SwitchSetting(
                        title = "Enable Islamic Calendar",
                        description = "Show Islamic calendar and Hijri dates",
                        checked = settings.enableIslamicCalendar,
                        onCheckedChange = { viewModel.updateIslamicCalendarEnabled(it) }
                    )

                    if (settings.enableIslamicCalendar) {
                        SliderSetting(
                            title = "Calendar Update Interval",
                            description = "Minutes between calendar updates",
                            value = settings.islamicCalendarUpdateInterval.toFloat(),
                            range = 30f..240f,
                            onValueChange = { viewModel.updateIslamicCalendarUpdateInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )
                    }

                    SwitchSetting(
                        title = "Enable Qibla Direction",
                        description = "Show Qibla direction from your location",
                        checked = settings.enableQiblaDirection,
                        onCheckedChange = { viewModel.updateQiblaDirectionEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}