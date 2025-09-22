package com.hieltech.haramblur.ui.newsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.CitySelector
import com.hieltech.haramblur.ui.components.ExpandableSettingsSection
import com.hieltech.haramblur.ui.components.IslamicErrorState
import com.hieltech.haramblur.ui.components.IslamicFeaturesErrorBoundary
import com.hieltech.haramblur.ui.components.RadioButtonGroup
import com.hieltech.haramblur.ui.components.SliderSetting
import com.hieltech.haramblur.ui.components.SwitchSetting
import com.hieltech.haramblur.ui.newsettings.ModernSettingsHeader
import com.hieltech.haramblur.ui.newsettings.ModernSettingsSection
import com.hieltech.haramblur.ui.newsettings.EnhancedSwitchSetting
import com.hieltech.haramblur.ui.newsettings.EnhancedSliderSetting
import com.hieltech.haramblur.ui.newsettings.EnhancedRadioGroup
import com.hieltech.haramblur.ui.newsettings.NavigationSettingItem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernIslamicSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.islamic_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        IslamicFeaturesErrorBoundary(errorState = IslamicErrorState.NoError) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    ModernSettingsHeader(
                        title = stringResource(R.string.islamic_settings_title),
                        subtitle = stringResource(R.string.nav_islamic_description),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    QuranicGuidanceSection(settings = settings, viewModel = viewModel)
                }

                item {
                    DhikrSection(settings = settings, viewModel = viewModel)
                }

                item {
                    PrayerTimesSection(settings = settings, viewModel = viewModel)
                }

            }
        }
    }
}

@Composable
private fun QuranicGuidanceSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.quranic_guidance_title),
        subtitle = stringResource(R.string.quranic_guidance_description),
        icon = Icons.Default.Star
    ) {
        EnhancedSwitchSetting(
            title = "Enable Quranic Guidance",
            description = "Show relevant Quranic verses when inappropriate content is blocked",
            icon = Icons.Default.Star,
            checked = settings.enableQuranicGuidance,
            onCheckedChange = { viewModel.updateQuranicGuidance(it) },
            badge = if (settings.enableQuranicGuidance) "Active" else null
        )

        AnimatedVisibility(
            visible = settings.enableQuranicGuidance,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                EnhancedRadioGroup(
                    title = stringResource(R.string.preferred_language_title),
                    options = Language.values().map { language ->
                        language.displayName to "Display verses in ${language.displayName}"
                    },
                    selectedIndex = Language.values().indexOf(settings.preferredLanguage),
                    onSelectionChange = { index -> viewModel.updatePreferredLanguage(Language.values()[index]) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSliderSetting(
                    title = stringResource(R.string.verse_display_duration_title),
                    description = stringResource(R.string.verse_display_duration_description),
                    value = settings.verseDisplayDuration.toFloat(),
                    valueRange = 5f..30f,
                    onValueChange = { viewModel.updateVerseDisplayDuration(it.toInt()) },
                    valueFormatter = { "${it.toInt()}s" },
                    icon = Icons.Default.Favorite
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSwitchSetting(
                    title = stringResource(R.string.arabic_text_display_title),
                    description = stringResource(R.string.arabic_text_display_description),
                    icon = Icons.Default.Settings,
                    checked = settings.enableArabicText,
                    onCheckedChange = viewModel::updateArabicText
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSliderSetting(
                    title = stringResource(R.string.custom_reflection_time_title),
                    description = stringResource(R.string.custom_reflection_time_description),
                    value = settings.customReflectionTime.toFloat(),
                    valueRange = 5f..60f,
                    onValueChange = { viewModel.updateCustomReflectionTime(it.toInt()) },
                    valueFormatter = { "${it.toInt()}s" }
                )
            }
        }
    }
}

@Composable
private fun DhikrSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.dhikr_title),
        subtitle = stringResource(R.string.dhikr_description),
        icon = Icons.Default.Refresh
    ) {
        EnhancedSwitchSetting(
            title = "Enable Dhikr Reminders",
            description = "Show Islamic remembrance reminders throughout the day",
            icon = Icons.Default.Refresh,
            checked = settings.dhikrEnabled,
            onCheckedChange = { viewModel.updateDhikrEnabled(it) },
            badge = if (settings.dhikrEnabled) "Active" else null
        )

        AnimatedVisibility(
            visible = settings.dhikrEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                EnhancedSwitchSetting(
                    title = stringResource(R.string.morning_dhikr_title),
                    description = stringResource(R.string.morning_dhikr_description),
                    checked = settings.dhikrMorningEnabled,
                    onCheckedChange = viewModel::updateDhikrMorningEnabled
                )

                EnhancedSwitchSetting(
                    title = stringResource(R.string.evening_dhikr_title),
                    description = stringResource(R.string.evening_dhikr_description),
                    checked = settings.dhikrEveningEnabled,
                    onCheckedChange = viewModel::updateDhikrEveningEnabled
                )

                EnhancedSwitchSetting(
                    title = stringResource(R.string.anytime_dhikr_title),
                    description = stringResource(R.string.anytime_dhikr_description),
                    checked = settings.dhikrAnytimeEnabled,
                    onCheckedChange = viewModel::updateDhikrAnytimeEnabled
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSliderSetting(
                    title = stringResource(R.string.display_interval_title),
                    description = stringResource(R.string.display_interval_description),
                    value = settings.dhikrIntervalMinutes.toFloat(),
                    valueRange = 5f..240f,
                    onValueChange = { viewModel.updateDhikrInterval(it.toInt()) },
                    valueFormatter = { "${it.toInt()} min" },
                    icon = Icons.Default.Build
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSliderSetting(
                    title = stringResource(R.string.display_duration_title),
                    description = stringResource(R.string.display_duration_description),
                    value = settings.dhikrDisplayDuration.toFloat(),
                    valueRange = 5f..60f,
                    onValueChange = { viewModel.updateDhikrDisplayDuration(it.toInt()) },
                    valueFormatter = { "${it.toInt()}s" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedRadioGroup(
                    title = stringResource(R.string.display_position_title),
                    options = listOf(
                        stringResource(R.string.position_top_right) to "Display in top-right corner",
                        stringResource(R.string.position_top_left) to "Display in top-left corner",
                        stringResource(R.string.position_bottom_right) to "Display in bottom-right corner",
                        stringResource(R.string.position_bottom_left) to "Display in bottom-left corner",
                        stringResource(R.string.position_center) to "Display in center of screen"
                    ),
                    selectedIndex = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                        .indexOf(settings.dhikrPosition),
                    onSelectionChange = { index ->
                        val positions = listOf("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT", "CENTER")
                        viewModel.updateDhikrPosition(positions[index])
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSwitchSetting(
                    title = stringResource(R.string.show_transliteration_title),
                    description = stringResource(R.string.show_transliteration_description),
                    checked = settings.dhikrShowTransliteration,
                    onCheckedChange = viewModel::updateDhikrShowTransliteration
                )

                EnhancedSwitchSetting(
                    title = stringResource(R.string.show_translation_title),
                    description = stringResource(R.string.show_translation_description),
                    checked = settings.dhikrShowTranslation,
                    onCheckedChange = viewModel::updateDhikrShowTranslation
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.prayer_times_title),
        subtitle = stringResource(R.string.prayer_times_description),
        icon = Icons.Default.Notifications
    ) {
        EnhancedSwitchSetting(
            title = stringResource(R.string.enable_prayer_times_title),
            description = stringResource(R.string.enable_prayer_times_description),
            icon = Icons.Default.Notifications,
            checked = settings.enablePrayerTimes,
            onCheckedChange = viewModel::updatePrayerTimesEnabled,
            badge = if (settings.enablePrayerTimes) "Active" else null
        )

        AnimatedVisibility(
            visible = settings.enablePrayerTimes,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                EnhancedSwitchSetting(
                    title = stringResource(R.string.prayer_times_notification_title),
                    description = stringResource(R.string.prayer_times_notification_description),
                    icon = Icons.Default.Notifications,
                    checked = settings.enablePrayerNotifications,
                    onCheckedChange = viewModel::updatePrayerNotifications
                )

                Spacer(modifier = Modifier.height(8.dp))

                CitySelector(
                    selectedCity = settings.selectedCityName ?: settings.preferredCity,
                    selectedCountry = settings.selectedCountry ?: settings.preferredCountry,
                    onCitySelected = viewModel::updateSelectedCity
                )
            }
        }
    }
}
