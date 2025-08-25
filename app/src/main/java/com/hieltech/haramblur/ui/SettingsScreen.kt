package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.GenderAccuracy
import com.hieltech.haramblur.detection.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HaramBlur Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Detection Settings Section
            SettingsSection(title = "👁️ Detection Settings") {
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
            }
            
            // Gender-Specific Blur Settings
            if (settings.enableFaceDetection) {
                SettingsSection(title = "🚹🚺 Gender Blur Settings") {
                    SwitchSetting(
                        title = "Blur Male Faces",
                        description = "Apply blur to detected male faces",
                        checked = settings.blurMaleFaces,
                        onCheckedChange = { viewModel.updateMaleBlur(it) }
                    )
                    
                    SwitchSetting(
                        title = "Blur Female Faces",
                        description = "Apply blur to detected female faces",
                        checked = settings.blurFemaleFaces,
                        onCheckedChange = { viewModel.updateFemaleBlur(it) }
                    )
                }
            }
            
            // Detection Sensitivity
            SettingsSection(title = "🎯 Detection Sensitivity") {
                Column {
                    Text(
                        text = "Sensitivity: ${(settings.detectionSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            settings.detectionSensitivity < 0.3f -> "Conservative - May miss some content"
                            settings.detectionSensitivity < 0.7f -> "Balanced - Good accuracy"
                            else -> "Aggressive - May blur normal content"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Slider(
                        value = settings.detectionSensitivity,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Blur Intensity Settings
            SettingsSection(title = "💪 Blur Strength") {
                RadioButtonGroup(
                    title = "Blur Intensity",
                    options = BlurIntensity.values().map { it.displayName to it.description },
                    selectedIndex = BlurIntensity.values().indexOf(settings.blurIntensity),
                    onSelectionChange = { index ->
                        viewModel.updateBlurIntensity(BlurIntensity.values()[index])
                    }
                )
            }
            
            // Blur Style Settings
            SettingsSection(title = "🎨 Blur Style") {
                RadioButtonGroup(
                    title = "Visual Effect",
                    options = BlurStyle.values().map { it.displayName to it.description },
                    selectedIndex = BlurStyle.values().indexOf(settings.blurStyle),
                    onSelectionChange = { index ->
                        viewModel.updateBlurStyle(BlurStyle.values()[index])
                    }
                )
            }
            
            // Performance Settings
            SettingsSection(title = "⚡ Performance") {
                RadioButtonGroup(
                    title = "Processing Speed",
                    options = ProcessingSpeed.values().map { it.displayName to it.description },
                    selectedIndex = ProcessingSpeed.values().indexOf(settings.processingSpeed),
                    onSelectionChange = { index ->
                        viewModel.updateProcessingSpeed(ProcessingSpeed.values()[index])
                    }
                )
                
                SwitchSetting(
                    title = "Real-time Processing",
                    description = "Process content as soon as it appears",
                    checked = settings.enableRealTimeProcessing,
                    onCheckedChange = { viewModel.updateRealTimeProcessing(it) }
                )
            }
            
            // Enhanced Detection Settings
            SettingsSection(title = "🔍 Enhanced Detection") {
                RadioButtonGroup(
                    title = "Gender Detection Accuracy",
                    options = GenderAccuracy.values().map { it.name to it.description },
                    selectedIndex = GenderAccuracy.values().indexOf(settings.genderDetectionAccuracy),
                    onSelectionChange = { index ->
                        viewModel.updateGenderDetectionAccuracy(GenderAccuracy.values()[index])
                    }
                )
                
                SliderSetting(
                    title = "Content Density Threshold",
                    description = "Percentage of screen content that triggers full-screen blur",
                    value = settings.contentDensityThreshold,
                    range = 0.1f..0.8f,
                    onValueChange = { viewModel.updateContentDensityThreshold(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )
                
                SwitchSetting(
                    title = "Ultra-Fast Mode",
                    description = "Maximum speed processing with reduced accuracy",
                    checked = settings.ultraFastModeEnabled,
                    onCheckedChange = { viewModel.updateUltraFastMode(it) }
                )
                
                SliderSetting(
                    title = "Mandatory Reflection Time",
                    description = "Required wait time before continuing after warnings",
                    value = settings.mandatoryReflectionTime.toFloat(),
                    range = 5f..30f,
                    onValueChange = { viewModel.updateMandatoryReflectionTime(it.toInt()) },
                    valueFormatter = { "${it.toInt()} seconds" }
                )
                
                SwitchSetting(
                    title = "Full-Screen Warnings",
                    description = "Show warning dialogs for high-density inappropriate content",
                    checked = settings.fullScreenWarningEnabled,
                    onCheckedChange = { viewModel.updateFullScreenWarning(it) }
                )
            }
            
            // Performance Enhancement Settings
            SettingsSection(title = "⚡ Performance Enhancement") {
                SliderSetting(
                    title = "Max Processing Time",
                    description = "Maximum time allowed for content detection",
                    value = settings.maxProcessingTimeMs.toFloat(),
                    range = 25f..200f,
                    onValueChange = { viewModel.updateMaxProcessingTime(it.toLong()) },
                    valueFormatter = { "${it.toInt()}ms" }
                )
                
                SwitchSetting(
                    title = "GPU Acceleration",
                    description = "Use GPU for faster content detection",
                    checked = settings.enableGPUAcceleration,
                    onCheckedChange = { viewModel.updateGPUAcceleration(it) }
                )
                
                SliderSetting(
                    title = "Frame Skip Threshold",
                    description = "Skip frames during rapid scrolling",
                    value = settings.frameSkipThreshold.toFloat(),
                    range = 1f..10f,
                    onValueChange = { viewModel.updateFrameSkipThreshold(it.toInt()) },
                    valueFormatter = { "${it.toInt()} frames" }
                )
                
                SliderSetting(
                    title = "Image Downscale Ratio",
                    description = "Reduce image size for faster processing",
                    value = settings.imageDownscaleRatio,
                    range = 0.25f..1.0f,
                    onValueChange = { viewModel.updateImageDownscaleRatio(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )
            }
            
            // Advanced Settings
            SettingsSection(title = "⚙️ Advanced") {
                SliderSetting(
                    title = "Blur Area Expansion",
                    description = "Extra pixels around detected areas",
                    value = settings.expandBlurArea.toFloat(),
                    range = 0f..100f,
                    onValueChange = { viewModel.updateBlurExpansion(it.toInt()) },
                    valueFormatter = { "${it.toInt()} pixels" }
                )
                
                SwitchSetting(
                    title = "Full Screen Blur for NSFW",
                    description = "Blur entire screen for inappropriate content",
                    checked = settings.enableFullScreenBlurForNSFW,
                    onCheckedChange = { viewModel.updateFullScreenBlur(it) }
                )
                
                SwitchSetting(
                    title = "Show Blur Borders",
                    description = "Display borders around blurred areas",
                    checked = settings.showBlurBorders,
                    onCheckedChange = { viewModel.updateBlurBorders(it) }
                )
                
                SliderSetting(
                    title = "Gender Confidence Threshold",
                    description = "Minimum confidence required for gender detection",
                    value = settings.genderConfidenceThreshold,
                    range = 0.5f..0.95f,
                    onValueChange = { viewModel.updateGenderConfidenceThreshold(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )
                
                SliderSetting(
                    title = "NSFW Confidence Threshold",
                    description = "Minimum confidence required for NSFW detection",
                    value = settings.nsfwConfidenceThreshold,
                    range = 0.5f..0.95f,
                    onValueChange = { viewModel.updateNSFWConfidenceThreshold(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )
                
                SwitchSetting(
                    title = "Fallback Detection",
                    description = "Use backup detection methods when primary fails",
                    checked = settings.enableFallbackDetection,
                    onCheckedChange = { viewModel.updateFallbackDetection(it) }
                )
                
                SwitchSetting(
                    title = "Performance Monitoring",
                    description = "Monitor and log detection performance metrics",
                    checked = settings.enablePerformanceMonitoring,
                    onCheckedChange = { viewModel.updatePerformanceMonitoring(it) }
                )
            }
            
            // Islamic Guidance Settings
            SettingsSection(title = "🕌 Islamic Guidance") {
                SwitchSetting(
                    title = "Quranic Guidance",
                    description = "Show Quranic verses and Islamic guidance when blocking content",
                    checked = settings.enableQuranicGuidance,
                    onCheckedChange = { viewModel.updateQuranicGuidance(it) }
                )
                
                if (settings.enableQuranicGuidance) {
                    RadioButtonGroup(
                        title = "Preferred Language",
                        options = Language.values().map { it.displayName to "Language for Islamic guidance and Quranic verses" },
                        selectedIndex = Language.values().indexOf(settings.preferredLanguage),
                        onSelectionChange = { index ->
                            viewModel.updatePreferredLanguage(Language.values()[index])
                        }
                    )
                    
                    SliderSetting(
                        title = "Verse Display Duration",
                        description = "How long to display Quranic verses",
                        value = settings.verseDisplayDuration.toFloat(),
                        range = 5f..30f,
                        onValueChange = { viewModel.updateVerseDisplayDuration(it.toInt()) },
                        valueFormatter = { "${it.toInt()} seconds" }
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
                        valueFormatter = { "${it.toInt()} seconds" }
                    )
                }
            }
            
            // Site Blocking Settings (Note: Site blocking is always enabled for user protection)
            SettingsSection(title = "🚫 Content Protection") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Site Blocking Protection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Automatic blocking of inappropriate websites is always enabled to provide comprehensive protection. This feature works silently in the background to keep you safe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Quick Actions
            SettingsSection(title = "🚀 Quick Actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    
                    Button(
                        onClick = { viewModel.applyOptimalSettings() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimal")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            // TODO: Implement export functionality with file picker
                            val exportData = viewModel.exportSettings()
                            // For now, this would need to be handled by the calling activity
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            // TODO: Implement import functionality with file picker
                            // For now, this would need to be handled by the calling activity
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun RadioButtonGroup(
    title: String,
    options: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        options.forEachIndexed { index, (name, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedIndex == index),
                        onClick = { onSelectionChange(index) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedIndex == index),
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = name)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}