package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.LogLevel
import com.hieltech.haramblur.data.LogCategory
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.GenderAccuracy
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.llm.OpenRouterLLMService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
            
            // Female Content Detection Settings
            if (settings.enableFaceDetection) {
                SettingsSection(title = "👩 Female Content Detection") {
                    SwitchSetting(
                        title = "Detect Female Faces",
                        description = "Automatically detect and blur female faces",
                        checked = settings.blurFemaleFaces,
                        onCheckedChange = { viewModel.updateFemaleBlur(it) }
                    )
                    
                    SwitchSetting(
                        title = "Detect Female Body",
                        description = "Detect and blur female body parts and inappropriate content",
                        checked = settings.enableNSFWDetection,
                        onCheckedChange = { viewModel.updateNSFWDetection(it) }
                    )
                }
            }
            
            // Core Settings
            SettingsSection(title = "⚙️ Core Settings") {
                SliderSetting(
                    title = "Detection Accuracy",
                    description = "Higher values detect more content but may blur normal images",
                    value = settings.detectionSensitivity,
                    range = 0.3f..0.9f,
                    onValueChange = { viewModel.updateSensitivity(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )
                
                SwitchSetting(
                    title = "GPU Acceleration",
                    description = "Use GPU for 3x faster detection (recommended)",
                    checked = settings.enableGPUAcceleration,
                    onCheckedChange = { viewModel.updateGPUAcceleration(it) }
                )
                
                SwitchSetting(
                    title = "Real-time Processing",
                    description = "Process content instantly as it appears",
                    checked = settings.enableRealTimeProcessing,
                    onCheckedChange = { viewModel.updateRealTimeProcessing(it) }
                )
            }
            
            // Visual Settings
            SettingsSection(title = "🎨 Visual Settings") {
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
                    onValueChange = { value -> 
                        viewModel.updateBlurExpansion(value.toInt()) 
                    },
                    valueFormatter = { "${it.toInt()}px" }
                )
            }
            
            // Advanced Settings (Collapsed by default)
            SettingsSection(title = "⚙️ Advanced Settings") {
                SwitchSetting(
                    title = "Full Screen Blur for High Content",
                    description = "Blur entire screen when too much inappropriate content is detected",
                    checked = settings.fullScreenWarningEnabled,
                    onCheckedChange = { viewModel.updateFullScreenWarning(it) }
                )
                
                SliderSetting(
                    title = "Detection Confidence",
                    description = "Lower values detect more content but may have false positives",
                    value = settings.genderConfidenceThreshold,
                    range = 0.4f..0.8f,
                    onValueChange = { viewModel.updateGenderConfidenceThreshold(it) },
                    valueFormatter = { "${(it * 100).toInt()}%" }
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
            
            // LLM Decision Making Settings
            SettingsSection(title = "🤖 AI Decision Making") {
                SwitchSetting(
                    title = "Enable LLM Decisions",
                    description = "Use OpenRouter AI for faster, smarter content actions (requires API key)",
                    checked = settings.enableLLMDecisionMaking,
                    onCheckedChange = { viewModel.updateLLMDecisionMaking(it) }
                )
                
                if (settings.enableLLMDecisionMaking) {
                    // API Key Input
                    var showApiKey by remember { mutableStateOf(false) }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OpenRouter API Key",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        OutlinedTextField(
                            value = settings.openRouterApiKey,
                            onValueChange = { viewModel.updateOpenRouterApiKey(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your OpenRouter API key") },
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Filled.Lock else Icons.Filled.Star,
                                        contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                    )
                                }
                            },
                            supportingText = {
                                if (settings.openRouterApiKey.isBlank()) {
                                    Text(
                                        text = "Get your free API key at openrouter.ai",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "✓ API key configured",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                    
                    // Model Selection
                    var showModelDialog by remember { mutableStateOf(false) }
                    val selectedModel = OpenRouterLLMService.AVAILABLE_MODELS.find { it.id == settings.llmModel }
                        ?: OpenRouterLLMService.AVAILABLE_MODELS.first()
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI Model",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showModelDialog = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = selectedModel.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedModel.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Model Selection Dialog
                    if (showModelDialog) {
                        AlertDialog(
                            onDismissRequest = { showModelDialog = false },
                            title = { Text("Select AI Model") },
                            text = {
                                LazyColumn {
                                    items(OpenRouterLLMService.AVAILABLE_MODELS) { model ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            onClick = {
                                                viewModel.updateLLMModel(model.id)
                                                showModelDialog = false
                                            },
                                            colors = if (model.id == settings.llmModel) {
                                                CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            } else {
                                                CardDefaults.cardColors()
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = model.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = model.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (model.id.contains("free", ignoreCase = true)) {
                                                    Text(
                                                        text = "🆓 Free to use",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showModelDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    // Response Timeout Setting
                    SliderSetting(
                        title = "Response Timeout",
                        description = "Maximum time to wait for AI response (faster = quicker fallback)",
                        value = settings.llmTimeoutMs.toFloat() / 1000f,
                        range = 1f..10f,
                        onValueChange = { viewModel.updateLLMTimeout((it * 1000).toLong()) },
                        valueFormatter = { "${it.toInt()} seconds" }
                    )
                    
                    // Fallback Setting
                    SwitchSetting(
                        title = "Fallback to Rules",
                        description = "Use rule-based decisions if AI fails (recommended)",
                        checked = settings.llmFallbackToRules,
                        onCheckedChange = { viewModel.updateLLMFallbackToRules(it) }
                    )
                    
                    // Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "How AI Decisions Work",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "• AI analyzes content severity and app context\n" +
                                      "• Makes intelligent decisions in under 3 seconds\n" +
                                      "• Chooses optimal actions: scroll, navigate, or close\n" +
                                      "• Free models available with OpenRouter account\n" +
                                      "• Falls back to rule-based logic if AI fails",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Get Free API Key at OpenRouter.ai")
                            }
                        }
                    }
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
            
            // Logging and Support Settings - Enterprise/SaaS style
            SettingsSection(title = "📊 Logging & Support") {
                SwitchSetting(
                    title = "Detailed Logging",
                    description = "Enable detailed logging for troubleshooting and support",
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

                    Text(
                        text = "Log Categories",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LogCategory.values().forEach { category ->
                        SwitchSetting(
                            title = category.displayName,
                            description = category.description,
                            checked = settings.enableLogCategories.contains(category),
                            onCheckedChange = { enabled ->
                                viewModel.updateLogCategory(category, enabled)
                            }
                        )
                    }

                    SliderSetting(
                        title = "Log Retention",
                        description = "Days to keep logs before automatic cleanup",
                        value = settings.maxLogRetentionDays.toFloat(),
                        range = 1f..30f,
                        onValueChange = { viewModel.updateLogRetentionDays(it.toInt()) },
                        valueFormatter = { "${it.toInt()} days" }
                    )

                    SwitchSetting(
                        title = "Performance Logging",
                        description = "Log performance metrics and timing information",
                        checked = settings.enablePerformanceLogging,
                        onCheckedChange = { viewModel.updatePerformanceLogging(it) }
                    )

                    SwitchSetting(
                        title = "Error Reporting",
                        description = "Log errors and crashes for troubleshooting",
                        checked = settings.enableErrorReporting,
                        onCheckedChange = { viewModel.updateErrorReporting(it) }
                    )

                    SwitchSetting(
                        title = "User Action Logging",
                        description = "Log user actions for better support",
                        checked = settings.enableUserActionLogging,
                        onCheckedChange = { viewModel.updateUserActionLogging(it) }
                    )
                }

                // Log Management Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📋 Log Management",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToLogs,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Logs")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val logsText = viewModel.exportLogs()
                                        if (logsText != null) {
                                            shareTextContent(context, logsText, "HaramBlur Logs")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Logs")
                            }
                        }

                        Text(
                            text = "Logs are stored locally and help with troubleshooting. Export them when contacting support.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Support Section - Enterprise/SaaS style support access
            SettingsSection(title = "🆘 Support & Help") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Need Help?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Get assistance with setup, troubleshooting, and support. Access detailed logs for faster resolution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToSupport,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get Support")
                            }

                            OutlinedButton(
                                onClick = onNavigateToLogs,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Logs")
                            }
                        }
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
                            val exportData = viewModel.exportSettings()
                            shareTextContent(context, exportData, "HaramBlur Settings")
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

// Helper function to share text content
private fun shareTextContent(context: android.content.Context, content: String, subject: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shareIntent = Intent.createChooser(intent, "Share $subject")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)

    } catch (e: Exception) {
        // Handle error silently - user will see no action
        android.util.Log.e("SettingsScreen", "Failed to share content", e)
    }
}