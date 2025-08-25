package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hieltech.haramblur.data.IslamicGuidance
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.detection.BlockingCategory
import kotlinx.coroutines.delay

/**
 * Dialog for displaying Quranic verses with Islamic guidance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranicVerseDialog(
    verse: QuranicVerse?,
    guidance: IslamicGuidance?,
    category: BlockingCategory,
    selectedLanguage: com.hieltech.haramblur.detection.Language = com.hieltech.haramblur.detection.Language.ENGLISH,
    reflectionTimeSeconds: Int = 15,
    showCloseOption: Boolean = true,
    showContinueOption: Boolean = true,
    enableArabicText: Boolean = true,
    onLanguageChange: (com.hieltech.haramblur.detection.Language) -> Unit = {},
    onAction: (WarningDialogAction) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var remainingTime by remember { mutableStateOf(reflectionTimeSeconds) }
    var canContinue by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    
    // Countdown timer
    LaunchedEffect(reflectionTimeSeconds) {
        remainingTime = reflectionTimeSeconds
        canContinue = false
        
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
        canContinue = true
    }
    
    Dialog(
        onDismissRequest = { 
            if (canContinue) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = canContinue,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with title and language selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getCategoryTitle(category),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row {
                        // Language selector
                        Box {
                            IconButton(onClick = { showLanguageMenu = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Select Language",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                com.hieltech.haramblur.detection.Language.values().forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language.displayName) },
                                        onClick = {
                                            onLanguageChange(language)
                                            showLanguageMenu = false
                                        },
                                        leadingIcon = if (language == selectedLanguage) {
                                            { Icon(Icons.Default.Settings, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Quranic verse section
                    verse?.let { quranicVerse ->
                        QuranicVerseContent(
                            verse = quranicVerse,
                            selectedLanguage = selectedLanguage,
                            enableArabicText = enableArabicText
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    // Islamic guidance section
                    guidance?.let { islamicGuidance ->
                        IslamicGuidanceContent(
                            guidance = islamicGuidance,
                            selectedLanguage = selectedLanguage
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reflection timer
                if (!canContinue) {
                    ReflectionTimer(remainingTime = remainingTime)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action buttons
                ActionButtons(
                    canContinue = canContinue,
                    showCloseOption = showCloseOption,
                    showContinueOption = showContinueOption,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun QuranicVerseContent(
    verse: QuranicVerse,
    selectedLanguage: com.hieltech.haramblur.detection.Language,
    enableArabicText: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Surah and verse reference
            Text(
                text = "${verse.surahName} (${verse.surahNumber}:${verse.verseNumber})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Arabic text (if enabled)
            if (enableArabicText) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = verse.arabicText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            lineHeight = 32.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Transliteration
            if (verse.transliteration.isNotBlank()) {
                Text(
                    text = verse.transliteration,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Translation in selected language
            verse.translations[selectedLanguage]?.let { translation ->
                Text(
                    text = "\"$translation\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Context and reflection
            if (verse.context.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Context:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = verse.context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (verse.reflection.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Reflection:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = verse.reflection,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IslamicGuidanceContent(
    guidance: IslamicGuidance,
    selectedLanguage: com.hieltech.haramblur.detection.Language
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Islamic Guidance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = guidance.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Action recommendations
            if (guidance.actionRecommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Recommended Actions:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                guidance.actionRecommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Du'a text
            guidance.duaText?.let { dua ->
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Du'a:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = dua,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflectionTimer(remainingTime: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take a moment to reflect",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${remainingTime}s",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (15 - remainingTime) / 15f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    }
}

@Composable
private fun ActionButtons(
    canContinue: Boolean,
    showCloseOption: Boolean,
    showContinueOption: Boolean,
    onAction: (WarningDialogAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showCloseOption) {
            OutlinedButton(
                onClick = { onAction(WarningDialogAction.Close) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ).brush
                )
            ) {
                Text("Close App")
            }
        }
        
        if (showContinueOption) {
            Button(
                onClick = { onAction(WarningDialogAction.Continue) },
                modifier = Modifier.weight(1f),
                enabled = canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    if (canContinue) "Continue" else "Please wait...",
                    color = if (canContinue) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun getCategoryTitle(category: BlockingCategory): String {
    return when (category) {
        BlockingCategory.EXPLICIT_CONTENT -> "Content Warning"
        BlockingCategory.ADULT_ENTERTAINMENT -> "Adult Content Blocked"
        BlockingCategory.INAPPROPRIATE_IMAGERY -> "Inappropriate Content"
        BlockingCategory.GAMBLING -> "Gambling Site Blocked"
        BlockingCategory.DATING_SITES -> "Dating Site Notice"
        BlockingCategory.SUSPICIOUS_CONTENT -> "Content Advisory"
        BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> "Social Media Warning"
        BlockingCategory.VIOLENCE -> "Violent Content Warning"
        BlockingCategory.HATE_SPEECH -> "Harmful Content Blocked"
        BlockingCategory.SUBSTANCE_ABUSE -> "Substance-Related Content"
    }
}

// Extension property for Language display names
private val com.hieltech.haramblur.detection.Language.displayName: String
    get() = when (this) {
        com.hieltech.haramblur.detection.Language.ENGLISH -> "English"
        com.hieltech.haramblur.detection.Language.ARABIC -> "العربية"
        com.hieltech.haramblur.detection.Language.URDU -> "اردو"
        com.hieltech.haramblur.detection.Language.FRENCH -> "Français"
        com.hieltech.haramblur.detection.Language.INDONESIAN -> "Bahasa Indonesia"
        com.hieltech.haramblur.detection.Language.TURKISH -> "Türkçe"
        com.hieltech.haramblur.detection.Language.MALAY -> "Bahasa Melayu"
        com.hieltech.haramblur.detection.Language.BENGALI -> "বাংলা"
        com.hieltech.haramblur.detection.Language.PERSIAN -> "فارسی"
        com.hieltech.haramblur.detection.Language.SPANISH -> "Español"
    }