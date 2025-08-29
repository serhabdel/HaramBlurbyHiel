package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun ServiceControlCard(
    isServicePaused: Boolean,
    onTogglePause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServicePaused) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isServicePaused) "🛑" else "🛡️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Service Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isServicePaused) {
                        "⚠️ All HaramBlur services are currently PAUSED"
                    } else {
                        "✅ All services are active and protecting your screen"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isServicePaused) {
                        "Tap to RESUME all detection and blocking services"
                    } else {
                        "Tap to PAUSE all detection and blocking services"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            
            Switch(
                checked = !isServicePaused,
                onCheckedChange = { onTogglePause() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                    uncheckedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                modifier = Modifier.scale(1.2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State management
    var currentMode by remember { mutableStateOf(SettingsMode.SIMPLE) }
    var searchQuery by remember { mutableStateOf("") }
    var showPresetConfirmation by remember { mutableStateOf<PresetData?>(null) }
    var expandedSections by remember {
        mutableStateOf(
            mapOf(
                SettingsCategory.ESSENTIAL to true,
                SettingsCategory.DETECTION to true,
                SettingsCategory.PERFORMANCE to true,
                SettingsCategory.ISLAMIC to true,
                SettingsCategory.AI to true,
                SettingsCategory.DEVELOPER to true
            )
        )
    }

    // Get available presets - memoized to prevent expensive recomputation
    val availablePresets = remember { viewModel.getAvailablePresets() }
    val currentPreset = remember(settings.currentPreset) { 
        availablePresets.find { it.name == settings.currentPreset } ?: availablePresets[1]
    }
    
    // Remember scroll state to prevent memory issues during scrolling
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Preset Selection Section
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Maximum Protection Preset
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
                }
            )

            // Optimal Performance Preset
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
                }
            )

            // Custom Preset
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
                }
            )

            // Service Control Section
            ServiceControlCard(
                isServicePaused = settings.isServicePaused,
                onTogglePause = { viewModel.toggleServicePause() }
            )

            // Search bar for advanced mode
            AnimatedVisibility(
                visible = currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // Essential Settings (always visible)
            ExpandableSettingsSection(
                title = "Essential Settings",
                description = "Core functionality that everyone needs",
                icon = "⚙️",
                isExpanded = expandedSections[SettingsCategory.ESSENTIAL] ?: true,
                onToggle = {
                    expandedSections = expandedSections.toMutableMap().apply {
                        this[SettingsCategory.ESSENTIAL] = !(this[SettingsCategory.ESSENTIAL] ?: true)
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            // Detection & Privacy Settings (Simple mode)
            AnimatedVisibility(
                visible = currentMode == SettingsMode.SIMPLE || currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandableSettingsSection(
                    title = "Detection & Privacy",
                    description = "Face and content detection settings",
                    icon = "👁️",
                    isExpanded = expandedSections[SettingsCategory.DETECTION] ?: false,
                    onToggle = {
                        expandedSections = expandedSections.toMutableMap().apply {
                            this[SettingsCategory.DETECTION] = !(this[SettingsCategory.DETECTION] ?: false)
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SliderSetting(
                            title = "Detection Sensitivity",
                            description = "Higher values detect more content but may blur normal images",
                            value = settings.detectionSensitivity,
                            range = 0.3f..0.9f,
                            onValueChange = { viewModel.updateSensitivity(it) },
                            valueFormatter = { "${(it * 100).toInt()}%" }
                        )

                        SliderSetting(
                            title = "Gender Confidence Threshold",
                            description = "Lower values detect more faces but may have false positives",
                            value = settings.genderConfidenceThreshold,
                            range = 0.3f..0.8f,
                            onValueChange = { viewModel.updateGenderConfidenceThreshold(it) },
                            valueFormatter = { "${(it * 100).toInt()}%" }
                        )

                        SliderSetting(
                            title = "NSFW Confidence Threshold",
                            description = "Lower values detect more content but may have false positives",
                            value = settings.nsfwConfidenceThreshold,
                            range = 0.4f..0.7f,
                            onValueChange = { viewModel.updateNSFWConfidenceThreshold(it) },
                            valueFormatter = { "${(it * 100).toInt()}%" }
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
                
                SliderSetting(
                    title = "Blur Area Expansion",
                    description = "Pixels to expand around detected areas",
                    value = settings.expandBlurArea.toFloat(),
                    range = 10f..100f,
                            onValueChange = { viewModel.updateBlurExpansion(it.toInt()) },
                    valueFormatter = { "${it.toInt()}px" }
                )
                    }
                }
            }

            // Performance & Advanced Settings (Advanced mode only)
            AnimatedVisibility(
                visible = currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandableSettingsSection(
                    title = "Performance & Advanced",
                    description = "Speed and resource optimization",
                    icon = "⚡",
                    isExpanded = expandedSections[SettingsCategory.PERFORMANCE] ?: false,
                    onToggle = {
                        expandedSections = expandedSections.toMutableMap().apply {
                            this[SettingsCategory.PERFORMANCE] = !(this[SettingsCategory.PERFORMANCE] ?: false)
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SwitchSetting(
                            title = "GPU Acceleration",
                            description = "Use GPU for faster detection (recommended)",
                            checked = settings.enableGPUAcceleration,
                            onCheckedChange = { viewModel.updateGPUAcceleration(it) }
                        )

                SwitchSetting(
                            title = "Real-time Processing",
                            description = "Process content instantly as it appears",
                            checked = settings.enableRealTimeProcessing,
                            onCheckedChange = { viewModel.updateRealTimeProcessing(it) }
                        )

                        RadioButtonGroup(
                            title = "Processing Speed",
                            options = ProcessingSpeed.values().map { it.name to it.description },
                            selectedIndex = ProcessingSpeed.values().indexOf(settings.processingSpeed),
                            onSelectionChange = { index ->
                                viewModel.updateProcessingSpeed(ProcessingSpeed.values()[index])
                            }
                        )

                        RadioButtonGroup(
                            title = "Gender Detection Accuracy",
                            options = GenderAccuracy.values().map { it.name to it.description },
                            selectedIndex = GenderAccuracy.values().indexOf(settings.genderDetectionAccuracy),
                            onSelectionChange = { index ->
                                viewModel.updateGenderDetectionAccuracy(GenderAccuracy.values()[index])
                            }
                )
                
                SliderSetting(
                            title = "Max Processing Time",
                            description = "Maximum time allowed for detection per frame",
                            value = settings.maxProcessingTimeMs.toFloat(),
                            range = 25f..200f,
                            onValueChange = { viewModel.updateMaxProcessingTime(it.toLong()) },
                            valueFormatter = { "${it.toInt()}ms" }
                        )

                        SwitchSetting(
                            title = "Performance Monitoring",
                            description = "Log performance metrics and timing information",
                            checked = settings.enablePerformanceMonitoring,
                            onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                        )
                    }
                }
            }

            // Islamic Guidance Settings (Simple mode)
            AnimatedVisibility(
                visible = currentMode == SettingsMode.SIMPLE || currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandableSettingsSection(
                    title = "Islamic Guidance",
                    description = "Quranic verses and spiritual guidance",
                    icon = "🕌",
                    isExpanded = expandedSections[SettingsCategory.ISLAMIC] ?: false,
                    onToggle = {
                        expandedSections = expandedSections.toMutableMap().apply {
                            this[SettingsCategory.ISLAMIC] = !(this[SettingsCategory.ISLAMIC] ?: false)
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        
                        // Dhikr Settings
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Dhikr (Remembrance)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
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
                    }
                }
            }

            // AI & Automation Settings (Advanced mode only)
            AnimatedVisibility(
                visible = currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandableSettingsSection(
                    title = "AI & Automation",
                    description = "OpenRouter LLM and intelligent decisions",
                    icon = "🤖",
                    isExpanded = expandedSections[SettingsCategory.AI] ?: false,
                    onToggle = {
                        expandedSections = expandedSections.toMutableMap().apply {
                            this[SettingsCategory.AI] = !(this[SettingsCategory.AI] ?: false)
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SwitchSetting(
                    title = "Enable LLM Decisions",
                            description = "Use AI for faster, smarter content actions",
                    checked = settings.enableLLMDecisionMaking,
                    onCheckedChange = { viewModel.updateLLMDecisionMaking(it) }
                )
                
                if (settings.enableLLMDecisionMaking) {
                            // API Key input would go here
                    SliderSetting(
                        title = "Response Timeout",
                                description = "Maximum time to wait for AI response",
                        value = settings.llmTimeoutMs.toFloat() / 1000f,
                        range = 1f..10f,
                        onValueChange = { viewModel.updateLLMTimeout((it * 1000).toLong()) },
                                valueFormatter = { "${it.toInt()}s" }
                    )
                    
                    SwitchSetting(
                        title = "Fallback to Rules",
                                description = "Use rule-based decisions if AI fails",
                        checked = settings.llmFallbackToRules,
                        onCheckedChange = { viewModel.updateLLMFallbackToRules(it) }
                            )
                        }
                    }
                }
            }

            // Developer & Logging Settings (Advanced mode only)
            AnimatedVisibility(
                visible = currentMode == SettingsMode.ADVANCED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandableSettingsSection(
                    title = "Developer & Logging",
                    description = "Debugging and troubleshooting tools",
                    icon = "📊",
                    isExpanded = expandedSections[SettingsCategory.DEVELOPER] ?: false,
                    onToggle = {
                        expandedSections = expandedSections.toMutableMap().apply {
                            this[SettingsCategory.DEVELOPER] = !(this[SettingsCategory.DEVELOPER] ?: false)
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SwitchSetting(
                    title = "Detailed Logging",
                            description = "Enable detailed logging for troubleshooting",
                    checked = settings.enableDetailedLogging,
                    onCheckedChange = { viewModel.updateDetailedLogging(it) }
                )

                if (settings.enableDetailedLogging) {
                    RadioButtonGroup(
                        title = "Log Level",
                        options = LogLevel.values().map { it.displayName to it.description },
                        selectedIndex = LogLevel.values().indexOf(settings.logLevel),
                        onSelectionChange = { index ->
                            viewModel.updateLogLevel(LogLevel.values()[index])
                        }
                    )

                    SliderSetting(
                        title = "Log Retention",
                        description = "Days to keep logs before automatic cleanup",
                        value = settings.maxLogRetentionDays.toFloat(),
                        range = 1f..30f,
                        onValueChange = { viewModel.updateLogRetentionDays(it.toInt()) },
                        valueFormatter = { "${it.toInt()} days" }
                    )
                }
            }

            // Import/Export Section
            ImportExportCard(
                onExportClick = {
                    scope.launch {
                        val exportData = viewModel.exportSettingsWithPresetInfo()
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "HaramBlur Settings")
                                putExtra(android.content.Intent.EXTRA_TEXT, exportData)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val shareIntent = android.content.Intent.createChooser(intent, "Share HaramBlur Settings")
                            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsScreen", "Failed to share content", e)
                        }
                    }
                },
                onImportClick = {
                    // TODO: Implement file picker for import
                }
            )
            
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

// Helper function to share text content  
fun shareTextContent(context: android.content.Context, content: String, subject: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, content)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = android.content.Intent.createChooser(intent, "Share $subject")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)

    } catch (e: Exception) {
        // Handle error silently - user will see no action
        android.util.Log.e("SettingsScreen", "Failed to share content", e)
    }
}

}