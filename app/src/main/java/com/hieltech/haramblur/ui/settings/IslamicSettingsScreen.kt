package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // After user responds, sync permission status and optionally refresh
        viewModel.syncLocationPermissionStatus()
        viewModel.refreshLocation()
    }

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
        IslamicFeaturesErrorBoundary(errorState = IslamicErrorState.NoError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(responsiveLayoutMargins()),
                verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
            ) {
            // Quranic Guidance Section
            ExpandableSettingsSection(
                title = stringResource(R.string.quranic_guidance_title),
                description = stringResource(R.string.quranic_guidance_description),
                icon = "📖",
                isExpanded = settings.enableQuranicGuidance,
                onToggle = { viewModel.updateQuranicGuidance(!settings.enableQuranicGuidance) },
                badge = if (settings.enableQuranicGuidance) "Enabled" else null
            ) {
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

            // Dhikr Settings Section
            ExpandableSettingsSection(
                title = stringResource(R.string.dhikr_title),
                description = stringResource(R.string.dhikr_description),
                icon = "🕌",
                isExpanded = settings.dhikrEnabled,
                onToggle = { viewModel.updateDhikrEnabled(!settings.dhikrEnabled) },
                badge = if (settings.dhikrEnabled) "Active" else null
            ) {
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

                // Updated dhikr interval range: 5-240 minutes (minimum 5 minutes)
                SliderSetting(
                    title = stringResource(R.string.display_interval_title),
                    description = stringResource(R.string.display_interval_description),
                    value = settings.dhikrIntervalMinutes.toFloat(),
                    range = 5f..240f, // Updated from 15f..240f to 5f..240f
                    onValueChange = { viewModel.updateDhikrInterval(it.toInt()) },
                    valueFormatter = { "${it.toInt()} min" }
                )

                // Updated dhikr display duration: 5-60 seconds with default 30 seconds
                SliderSetting(
                    title = stringResource(R.string.display_duration_title),
                    description = stringResource(R.string.display_duration_description),
                    value = settings.dhikrDisplayDuration.toFloat(),
                    range = 5f..60f, // Updated max from 60f to 60f (keeping same range)
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

            // Prayer Times & Islamic Calendar Section
            ExpandableSettingsSection(
                title = stringResource(R.string.prayer_times_calendar_title),
                description = stringResource(R.string.prayer_times_calendar_description),
                icon = "🕐",
                isExpanded = settings.enablePrayerTimes,
                onToggle = { viewModel.updatePrayerTimesEnabled(!settings.enablePrayerTimes) },
                badge = if (settings.enablePrayerTimes) "Enabled" else null
            ) {
                Text(
                    text = "This section is temporarily simplified to complete the build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Location & Qibla Section
            ExpandableSettingsSection(
                title = "Location & Qibla",
                description = "Configure location services and Qibla direction",
                icon = "🧭",
                isExpanded = settings.enableQiblaDirection,
                onToggle = { viewModel.updateQiblaDirectionEnabled(!settings.enableQiblaDirection) },
                badge = if (settings.enableQiblaDirection) "Active" else null
            ) {

                // Toggles
                SwitchSetting(
                    title = stringResource(R.string.enable_prayer_times_title),
                    description = stringResource(R.string.enable_prayer_times_description),
                    checked = settings.enablePrayerTimes,
                    onCheckedChange = { viewModel.updatePrayerTimesEnabled(it) }
                )

                SwitchSetting(
                    title = stringResource(R.string.enable_qibla_direction_title),
                    description = stringResource(R.string.enable_qibla_direction_description),
                    checked = settings.enableQiblaDirection,
                    onCheckedChange = { viewModel.updateQiblaDirectionEnabled(it) }
                )

                SwitchSetting(
                    title = "Enable Qibla Compass",
                    description = "Show interactive Qibla compass inside prayer widget",
                    checked = settings.qiblaCompassEnabled,
                    onCheckedChange = { viewModel.updateQiblaCompassEnabled(it) }
                )

                // Location method
                Text(
                    text = stringResource(R.string.location_settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                RadioButtonGroup(
                    title = "Location Method",
                    options = listOf(
                        "GPS" to "Use device GPS",
                        "Manual" to "Select city manually"
                    ),
                    selectedIndex = if (settings.locationMethod == com.hieltech.haramblur.data.LocationMethod.GPS) 0 else 1,
                    onSelectionChange = { idx ->
                        viewModel.updateLocationMethod(
                            if (idx == 0) com.hieltech.haramblur.data.LocationMethod.GPS else com.hieltech.haramblur.data.LocationMethod.MANUAL_CITY
                        )
                    }
                )

                // Permission request button and status
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text(text = stringResource(id = R.string.grant_location_permission))
                    }
                    Text(text = viewModel.getLocationStatusSummary(), style = MaterialTheme.typography.bodySmall)
                }

                // Manual city selection
                if (settings.locationMethod == com.hieltech.haramblur.data.LocationMethod.MANUAL_CITY) {
                    CitySelector(
                        selectedCity = settings.selectedCityName ?: settings.preferredCity,
                        selectedCountry = settings.selectedCountry ?: settings.preferredCountry,
                        onCitySelected = { selection -> viewModel.updateSelectedCity(selection) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Compass preferences
                SliderSetting(
                    title = "Qibla Tolerance",
                    description = "Degrees within which the compass considers aligned",
                    value = settings.qiblaToleranceDegrees,
                    range = 1f..30f,
                    onValueChange = { viewModel.updateQiblaToleranceDegrees(it) },
                    valueFormatter = { "${it.toInt()}°" }
                )

                SliderSetting(
                    title = "Compass Animation Speed",
                    description = "Adjust rotation animation speed",
                    value = settings.compassAnimationSpeed,
                    range = 0.2f..3.0f,
                    onValueChange = { viewModel.updateCompassAnimationSpeed(it) },
                    valueFormatter = { "x${"%.1f".format(it)}" }
                )

                RadioButtonGroup(
                    title = "Compass Size",
                    options = listOf("Small" to "", "Medium" to "", "Large" to ""),
                    selectedIndex = when (settings.compassPreferredSize) {
                        com.hieltech.haramblur.data.compass.CompassSize.SMALL -> 0
                        com.hieltech.haramblur.data.compass.CompassSize.MEDIUM -> 1
                        com.hieltech.haramblur.data.compass.CompassSize.LARGE -> 2
                    },
                    onSelectionChange = { idx ->
                        val size = when (idx) {
                            0 -> com.hieltech.haramblur.data.compass.CompassSize.SMALL
                            1 -> com.hieltech.haramblur.data.compass.CompassSize.MEDIUM
                            else -> com.hieltech.haramblur.data.compass.CompassSize.LARGE
                        }
                        viewModel.updateCompassPreferredSize(size)
                    }
                )

                SwitchSetting(
                    title = "Show Degree Markings",
                    description = "Display tick marks around the dial",
                    checked = settings.compassShowDegreeMarkings,
                    onCheckedChange = { viewModel.updateCompassShowDegreeMarkings(it) }
                )

                SwitchSetting(
                    title = "Haptic Feedback",
                    description = "Vibrate when aligned to Qibla",
                    checked = settings.compassHapticFeedback,
                    onCheckedChange = { viewModel.updateCompassHapticFeedback(it) }
                )

                SwitchSetting(
                    title = "Calibration Reminders",
                    description = "Show guidance when sensor accuracy is low",
                    checked = settings.compassCalibrationReminders,
                    onCheckedChange = { viewModel.updateCompassCalibrationReminders(it) }
                )
            }

            // Dhikr Debug Panel (only show if dhikr is enabled)
            /*
            if (settings.dhikrEnabled) {
                DhikrDebugPanel(
                    dhikrManager = dhikrManager,
                    permissionHelper = permissionHelper,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            */

            Spacer(modifier = Modifier.height(responsiveSpacing() * 2))
            }
        }
    }
}