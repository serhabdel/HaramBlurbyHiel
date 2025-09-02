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
import com.hieltech.haramblur.ui.components.CitySelector
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.detection.Language

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
                title = { Text(stringResource(R.string.islamic_settings_title)) },
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
                        text = stringResource(R.string.quranic_guidance_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SwitchSetting(
                        title = stringResource(R.string.quranic_guidance_title),
                        description = stringResource(R.string.quranic_guidance_description),
                        checked = settings.enableQuranicGuidance,
                        onCheckedChange = { viewModel.updateQuranicGuidance(it) }
                    )

                    if (settings.enableQuranicGuidance) {
                        RadioButtonGroup(
                            title = stringResource(R.string.preferred_language_title),
                            options = Language.values().map {
                                it.displayName to stringResource(R.string.preferred_language_description)
                            },
                            selectedIndex = Language.values().indexOf(settings.preferredLanguage),
                            onSelectionChange = { index ->
                                viewModel.updatePreferredLanguage(Language.values()[index])
                            }
                        )

                        SliderSetting(
                            title = stringResource(R.string.verse_display_duration_title),
                            description = stringResource(R.string.verse_display_duration_description),
                            value = settings.verseDisplayDuration.toFloat(),
                            range = 5f..30f,
                            onValueChange = { viewModel.updateVerseDisplayDuration(it.toInt()) },
                            valueFormatter = { "${it.toInt()}s" }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.arabic_text_display_title),
                            description = stringResource(R.string.arabic_text_display_description),
                            checked = settings.enableArabicText,
                            onCheckedChange = { viewModel.updateArabicText(it) }
                        )

                        SliderSetting(
                            title = stringResource(R.string.custom_reflection_time_title),
                            description = stringResource(R.string.custom_reflection_time_description),
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
                        text = stringResource(R.string.dhikr_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.dhikr_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SwitchSetting(
                        title = stringResource(R.string.enable_dhikr_title),
                        description = stringResource(R.string.enable_dhikr_description),
                        checked = settings.dhikrEnabled,
                        onCheckedChange = { viewModel.updateDhikrEnabled(it) }
                    )

                    if (settings.dhikrEnabled) {
                        SwitchSetting(
                            title = stringResource(R.string.morning_dhikr_title),
                            description = stringResource(R.string.morning_dhikr_description),
                            checked = settings.dhikrMorningEnabled,
                            onCheckedChange = { viewModel.updateDhikrMorningEnabled(it) }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.evening_dhikr_title),
                            description = stringResource(R.string.evening_dhikr_description),
                            checked = settings.dhikrEveningEnabled,
                            onCheckedChange = { viewModel.updateDhikrEveningEnabled(it) }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.anytime_dhikr_title),
                            description = stringResource(R.string.anytime_dhikr_description),
                            checked = settings.dhikrAnytimeEnabled,
                            onCheckedChange = { viewModel.updateDhikrAnytimeEnabled(it) }
                        )

                        SliderSetting(
                            title = stringResource(R.string.display_interval_title),
                            description = stringResource(R.string.display_interval_description),
                            value = settings.dhikrIntervalMinutes.toFloat(),
                            range = 15f..240f,
                            onValueChange = { viewModel.updateDhikrInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )

                        SliderSetting(
                            title = stringResource(R.string.display_duration_title),
                            description = stringResource(R.string.display_duration_description),
                            value = settings.dhikrDisplayDuration.toFloat(),
                            range = 5f..30f,
                            onValueChange = { viewModel.updateDhikrDisplayDuration(it.toInt()) },
                            valueFormatter = { "${it.toInt()}s" }
                        )

                        RadioButtonGroup(
                            title = stringResource(R.string.display_position_title),
                            options = listOf(
                                "TOP_RIGHT" to stringResource(R.string.position_top_right),
                                "TOP_LEFT" to stringResource(R.string.position_top_left),
                                "BOTTOM_RIGHT" to stringResource(R.string.position_bottom_right),
                                "BOTTOM_LEFT" to stringResource(R.string.position_bottom_left),
                                "CENTER" to stringResource(R.string.position_center)
                            ),
                            selectedIndex = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                                .indexOf(settings.dhikrPosition),
                            onSelectionChange = { index ->
                                val positions = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                                viewModel.updateDhikrPosition(positions[index])
                            }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.show_transliteration_title),
                            description = stringResource(R.string.show_transliteration_description),
                            checked = settings.dhikrShowTransliteration,
                            onCheckedChange = { viewModel.updateDhikrShowTransliteration(it) }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.show_translation_title),
                            description = stringResource(R.string.show_translation_description),
                            checked = settings.dhikrShowTranslation,
                            onCheckedChange = { viewModel.updateDhikrShowTranslation(it) }
                        )

                        SwitchSetting(
                            title = stringResource(R.string.animation_title),
                            description = stringResource(R.string.animation_description),
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
                        text = stringResource(R.string.prayer_times_calendar_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.prayer_times_calendar_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SwitchSetting(
                        title = stringResource(R.string.enable_prayer_times_title),
                        description = stringResource(R.string.enable_prayer_times_description),
                        checked = settings.enablePrayerTimes,
                        onCheckedChange = { viewModel.updatePrayerTimesEnabled(it) }
                    )

                    // Location Settings Section
                    if (settings.enablePrayerTimes) {
                        Text(
                            text = stringResource(R.string.location_settings_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = stringResource(R.string.location_settings_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SwitchSetting(
                            title = stringResource(R.string.auto_detect_location_title),
                            description = stringResource(R.string.auto_detect_location_description),
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
                                        text = stringResource(R.string.current_location_title),
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
                            title = stringResource(R.string.prayer_notifications_title),
                            description = stringResource(R.string.prayer_notifications_description),
                            checked = settings.enablePrayerNotifications,
                            onCheckedChange = { viewModel.updatePrayerNotifications(it) }
                        )

                        if (settings.enablePrayerNotifications) {
                            SliderSetting(
                                title = stringResource(R.string.advance_notice_title),
                                description = stringResource(R.string.advance_notice_description),
                                value = settings.prayerNotificationAdvanceTime.toFloat(),
                                range = 5f..60f,
                                onValueChange = { viewModel.updateNotificationAdvanceTime(it.toInt()) },
                                valueFormatter = { "${it.toInt()} min" }
                            )
                        }

                        // Calculation Method
                        RadioButtonGroup(
                            title = stringResource(R.string.calculation_method_title),
                            options = listOf(
                                "1" to stringResource(R.string.method_karachi),
                                "2" to stringResource(R.string.method_isna),
                                "3" to stringResource(R.string.method_muslim_world_league),
                                "4" to stringResource(R.string.method_umm_al_qura),
                                "5" to stringResource(R.string.method_egyptian)
                            ),
                            selectedIndex = (settings.prayerCalculationMethod - 1).coerceIn(0, 4),
                            onSelectionChange = { index ->
                                viewModel.updateCalculationMethod(index + 1)
                            }
                        )

                        SliderSetting(
                            title = stringResource(R.string.update_interval_title),
                            description = stringResource(R.string.update_interval_description),
                            value = settings.prayerTimesUpdateInterval.toFloat(),
                            range = 15f..120f,
                            onValueChange = { viewModel.updatePrayerTimesUpdateInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )
                    }

                    SwitchSetting(
                        title = stringResource(R.string.enable_islamic_calendar_title),
                        description = stringResource(R.string.enable_islamic_calendar_description),
                        checked = settings.enableIslamicCalendar,
                        onCheckedChange = { viewModel.updateIslamicCalendarEnabled(it) }
                    )

                    if (settings.enableIslamicCalendar) {
                        SliderSetting(
                            title = stringResource(R.string.calendar_update_interval_title),
                            description = stringResource(R.string.calendar_update_interval_description),
                            value = settings.islamicCalendarUpdateInterval.toFloat(),
                            range = 30f..240f,
                            onValueChange = { viewModel.updateIslamicCalendarUpdateInterval(it.toInt()) },
                            valueFormatter = { "${it.toInt()} min" }
                        )
                    }

                    SwitchSetting(
                        title = stringResource(R.string.enable_qibla_direction_title),
                        description = stringResource(R.string.enable_qibla_direction_description),
                        checked = settings.enableQiblaDirection,
                        onCheckedChange = { viewModel.updateQiblaDirectionEnabled(it) }
                    )
                }
            }

            // Dhikr Debug Panel (only show if dhikr is enabled)
            // Temporarily disabled due to dependency injection issues
            /*
            if (settings.dhikrEnabled) {
                DhikrDebugPanel(
                    dhikrManager = dhikrManager,
                    permissionHelper = permissionHelper,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            */

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}