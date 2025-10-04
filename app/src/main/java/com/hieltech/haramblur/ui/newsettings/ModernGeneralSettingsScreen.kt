package com.hieltech.haramblur.ui.newsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.AppTheme
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.UserGender
import com.hieltech.haramblur.data.PresetData
import com.hieltech.haramblur.data.PresetManager
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.ui.components.CustomAppTimeLimitsManager
import com.hieltech.haramblur.ui.components.DefaultTimeLimitsSection
import com.hieltech.haramblur.ui.components.PresetButton
import com.hieltech.haramblur.ui.components.PresetConfirmationDialog
import com.hieltech.haramblur.ui.components.RadioButtonGroup
import com.hieltech.haramblur.ui.components.ServiceControlCard
import com.hieltech.haramblur.ui.components.SettingsMode
import com.hieltech.haramblur.ui.components.SettingsModeToggle
import com.hieltech.haramblur.ui.components.SliderSetting
import com.hieltech.haramblur.ui.components.SwitchSetting
import com.hieltech.haramblur.ui.newsettings.ModernSettingsHeader
import com.hieltech.haramblur.ui.newsettings.ModernSettingsSection
import com.hieltech.haramblur.ui.newsettings.EnhancedSwitchSetting
import com.hieltech.haramblur.ui.newsettings.EnhancedSliderSetting
import com.hieltech.haramblur.ui.newsettings.EnhancedRadioGroup
import com.hieltech.haramblur.ui.newsettings.StatusIndicatorCard
import com.hieltech.haramblur.ui.newsettings.ServiceStatus
import com.hieltech.haramblur.ui.newsettings.NavigationSettingItem
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernGeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    settingsMode: MutableState<SettingsMode>? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showPresetConfirmation by remember { mutableStateOf<PresetData?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.general_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ModernSettingsHeader(
                    title = stringResource(R.string.general_settings_title),
                    subtitle = stringResource(R.string.nav_general_description),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatusIndicatorCard(
                    title = stringResource(R.string.protection_stats),
                    status = if (!settings.isServicePaused) ServiceStatus.ACTIVE else ServiceStatus.PAUSED,
                    description = if (!settings.isServicePaused) {
                        "All protection features are active and monitoring"
                    } else {
                        "Protection is temporarily paused. Tap to resume."
                    },
                    onClick = { viewModel.toggleServicePause() }
                )
            }

            item {
                LanguageSettingsSection(settings = settings, onLanguageSelected = viewModel::updatePreferredLanguage)
            }

            item {
                QuickPresetSection(
                    settings = settings,
                    onPresetSelected = { preset -> showPresetConfirmation = preset }
                )
            }

            item {
                CoreProtectionSection(settings = settings, viewModel = viewModel)
            }

            item {
                AppearanceSection(settings = settings, onThemeSelected = viewModel::updateAppTheme)
            }

            item {
                UsageTimeSection(settings = settings, viewModel = viewModel)
            }

            settingsMode?.let { modeState ->
                item {
                    SettingsModeToggle(
                        currentMode = modeState.value,
                        onModeChange = { modeState.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    showPresetConfirmation?.let { preset ->
        PresetConfirmationDialog(
            preset = preset,
            settingsDiff = viewModel.getSettingsDiff(preset.settings),
            onConfirm = {
                viewModel.applyPresetWithBackup(preset)
                showPresetConfirmation = null
            },
            onCancel = { showPresetConfirmation = null }
        )
    }
}

@Composable
private fun LanguageSettingsSection(
    settings: AppSettings,
    onLanguageSelected: (com.hieltech.haramblur.detection.Language) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.language_settings_title),
        subtitle = stringResource(R.string.app_language_title)
    ) {
        RadioButtonGroup(
            title = stringResource(R.string.select_language_title),
            options = com.hieltech.haramblur.detection.Language.values().map {
                it.displayName to stringResource(R.string.language_settings_title)
            },
            selectedIndex = com.hieltech.haramblur.detection.Language.values().indexOf(settings.preferredLanguage),
            onSelectionChange = { index ->
                val selectedLanguage = com.hieltech.haramblur.detection.Language.values()[index]
                onLanguageSelected(selectedLanguage)
            }
        )

        if (settings.preferredLanguage != com.hieltech.haramblur.detection.Language.ENGLISH) {
            Surface(
                tonalElevation = 0.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = settings.preferredLanguage.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.language_changed_restart),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPresetSection(
    settings: AppSettings,
    onPresetSelected: (PresetData) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.quick_presets_title),
        subtitle = stringResource(R.string.quick_presets_description)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PresetButton(
                name = stringResource(R.string.maximum_protection_name),
                description = stringResource(R.string.maximum_protection_description),
                icon = "🛡️",
                gradientColors = listOf(Color(0xFFD32F2F), Color(0xFFB71C1C)),
                isSelected = settings.currentPreset == "Maximum Protection",
                onClick = { onPresetSelected(PresetManager.createMaximumProtectionPreset()) },
                modifier = Modifier.weight(1f)
            )

            PresetButton(
                name = stringResource(R.string.optimal_performance_name),
                description = stringResource(R.string.optimal_performance_description),
                icon = "⚡",
                gradientColors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                isSelected = settings.currentPreset == "Optimal Performance",
                onClick = { onPresetSelected(PresetManager.createOptimalPerformancePreset()) },
                modifier = Modifier.weight(1f)
            )
        }

        PresetButton(
            name = stringResource(R.string.custom_settings_name),
            description = stringResource(R.string.custom_settings_description),
            icon = "⚙️",
            gradientColors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1)),
            isSelected = settings.currentPreset == stringResource(R.string.custom_settings_name),
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CoreProtectionSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.basic_settings_title),
        subtitle = stringResource(R.string.nav_detection_description),
        icon = Icons.Default.Lock
    ) {
        EnhancedSwitchSetting(
            title = stringResource(R.string.face_detection_title),
            description = stringResource(R.string.face_detection_description),
            icon = Icons.Default.Face,
            checked = settings.enableFaceDetection,
            onCheckedChange = viewModel::updateFaceDetection
        )

        Spacer(modifier = Modifier.height(8.dp))

        EnhancedSwitchSetting(
            title = stringResource(R.string.nsfw_detection_title),
            description = stringResource(R.string.nsfw_detection_description),
            icon = Icons.Default.Warning,
            checked = settings.enableNSFWDetection,
            onCheckedChange = viewModel::updateNSFWDetection,
            badge = if (settings.enableNSFWDetection) "Active" else null
        )

        AnimatedVisibility(
            visible = settings.enableFaceDetection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Smart gender-based protection status (no manual toggles)
                when (settings.userGender) {
                    UserGender.MALE -> {
                        // Male users: Clean status card, automatic behavior
                        Surface(
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🧔",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "Male Profile Active",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Female faces and inappropriate content are automatically blurred for Islamic modesty. All detections happen privately on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    UserGender.FEMALE -> {
                        // Female users: Clean status card, automatic behavior
                        Surface(
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "👩",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "Female Profile Active",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Male faces and inappropriate content are automatically blurred for Islamic modesty. All detections happen privately on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    
                    UserGender.NOT_SPECIFIED -> {
                        // Production mode: Force users to complete wizard - NO manual controls
                        Surface(
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.height(32.dp).width(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gender Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Smart protection requires gender selection. Please complete the initial setup wizard to enable automatic content filtering.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        EnhancedRadioGroup(
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
    }
}

@Composable
private fun AppearanceSection(
    settings: AppSettings,
    onThemeSelected: (AppTheme) -> Unit
) {
    ModernSettingsSection(
        title = stringResource(R.string.app_theme_title),
        subtitle = "Customize the look and feel of the app",
        icon = Icons.Default.Settings
    ) {
        val themeOptions = listOf(
            AppTheme.ISLAMIC_LIGHT to (stringResource(R.string.islamic_light_theme) to "Clean Islamic-inspired light theme"),
            AppTheme.ISLAMIC_DARK to (stringResource(R.string.islamic_dark_theme) to "Elegant Islamic-inspired dark theme"),
            AppTheme.MODERN_LIGHT to (stringResource(R.string.modern_light_theme) to "Contemporary light design"),
            AppTheme.MODERN_DARK to (stringResource(R.string.modern_dark_theme) to "Modern dark interface"),
            AppTheme.MINIMAL_LIGHT to (stringResource(R.string.minimal_light_theme) to "Simple and focused light theme"),
            AppTheme.MINIMAL_DARK to (stringResource(R.string.minimal_dark_theme) to "Minimal dark aesthetic")
        )

        EnhancedRadioGroup(
            title = "Choose Theme",
            options = themeOptions.map { it.second },
            selectedIndex = themeOptions.indexOfFirst { it.first == settings.appTheme }.coerceAtLeast(0),
            onSelectionChange = { index ->
                val selectedTheme = themeOptions.getOrNull(index)?.first ?: AppTheme.ISLAMIC_LIGHT
                onThemeSelected(selectedTheme)
            }
        )
    }
}

@Composable
private fun UsageTimeSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    ModernSettingsSection(
        title = stringResource(R.string.usage_time_management_title),
        subtitle = stringResource(R.string.usage_time_management_description)
    ) {
        SwitchSetting(
            title = stringResource(R.string.enable_usage_time_notifications_title),
            description = stringResource(R.string.enable_usage_time_notifications_description),
            checked = settings.enableUsageTimeNotifications,
            onCheckedChange = viewModel::updateUsageTimeNotifications
        )

        AnimatedVisibility(
            visible = settings.enableUsageTimeNotifications,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DefaultTimeLimitsSection(
                    socialMediaLimit = settings.defaultSocialMediaTimeLimit,
                    messagingLimit = settings.defaultMessagingTimeLimit,
                    onSocialMediaLimitChange = viewModel::updateDefaultSocialMediaTimeLimit,
                    onMessagingLimitChange = viewModel::updateDefaultMessagingTimeLimit
                )

                SliderSetting(
                    title = stringResource(R.string.notification_frequency_title),
                    description = stringResource(R.string.notification_frequency_description),
                    value = settings.usageNotificationFrequency.toFloat(),
                    range = 15f..120f,
                    onValueChange = { viewModel.updateUsageNotificationFrequency(it.toInt()) },
                    valueFormatter = { "${it.toInt()} minutes" }
                )

                SwitchSetting(
                    title = stringResource(R.string.daily_reset_title),
                    description = stringResource(R.string.daily_reset_description),
                    checked = settings.enableDailyUsageReset,
                    onCheckedChange = viewModel::updateDailyUsageReset
                )

                CustomAppTimeLimitsManager(
                    customLimits = settings.customAppTimeLimits,
                    onAddCustomLimit = viewModel::updateCustomAppTimeLimit,
                    onRemoveCustomLimit = viewModel::removeCustomAppTimeLimit,
                    onEditCustomLimit = viewModel::updateCustomAppTimeLimit
                )
            }
        }
    }
}
