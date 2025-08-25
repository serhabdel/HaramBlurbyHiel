package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.detection.SiteBlockingResult
import kotlinx.coroutines.delay

/**
 * Dialog for displaying blocked site warnings with Islamic guidance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedSiteDialog(
    blockingResult: SiteBlockingResult,
    guidance: IslamicGuidance? = null,
    selectedLanguage: Language = Language.ENGLISH,
    enableArabicText: Boolean = true,
    onLanguageChange: (Language) -> Unit = {},
    onAction: (WarningDialogAction) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var remainingTime by remember { mutableStateOf(blockingResult.reflectionTimeSeconds) }
    var canContinue by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    
    // Countdown timer
    LaunchedEffect(blockingResult.reflectionTimeSeconds) {
        remainingTime = blockingResult.reflectionTimeSeconds
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
                .fillMaxHeight(0.9f)
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
                // Header with blocking icon and title
                BlockedSiteHeader(
                    category = blockingResult.category ?: BlockingCategory.SUSPICIOUS_CONTENT,
                    confidence = blockingResult.confidence,
                    onLanguageMenuToggle = { showLanguageMenu = !showLanguageMenu }
                )
                
                // Language selector dropdown
                if (showLanguageMenu) {
                    LanguageDropdown(
                        selectedLanguage = selectedLanguage,
                        availableLanguages = guidance?.verse?.translations?.keys?.toList() 
                            ?: listOf(Language.ENGLISH, Language.ARABIC),
                        onLanguageChange = { language ->
                            onLanguageChange(language)
                            showLanguageMenu = false
                        },
                        onDismiss = { showLanguageMenu = false }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Blocking information
                    BlockingInfoCard(
                        blockingResult = blockingResult,
                        selectedLanguage = selectedLanguage
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quranic verse section
                    guidance?.verse?.let { verse ->
                        QuranicVerseCard(
                            verse = verse,
                            selectedLanguage = selectedLanguage,
                            enableArabicText = enableArabicText
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Islamic guidance section
                    guidance?.let { islamicGuidance ->
                        IslamicGuidanceCard(
                            guidance = islamicGuidance,
                            selectedLanguage = selectedLanguage
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reflection timer
                if (!canContinue) {
                    ReflectionTimerCard(remainingTime = remainingTime)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action buttons
                BlockedSiteActionButtons(
                    canContinue = canContinue,
                    category = blockingResult.category,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun BlockedSiteHeader(
    category: BlockingCategory,
    confidence: Float,
    onLanguageMenuToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Site Blocked",
                tint = when (category.severity) {
                    5 -> MaterialTheme.colorScheme.error
                    4 -> Color(0xFFFF6B35) // Orange-red
                    3 -> Color(0xFFFF8C00) // Orange
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = "Site Blocked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onLanguageMenuToggle) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Language Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LanguageDropdown(
    selectedLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageChange: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            availableLanguages.forEach { language ->
                TextButton(
                    onClick = { onLanguageChange(language) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(language.displayName)
                        if (language == selectedLanguage) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockingInfoCard(
    blockingResult: SiteBlockingResult,
    selectedLanguage: Language
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "This site has been blocked for your protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            blockingResult.blockingReason?.let { reason ->
                Text(
                    text = "Reason: $reason",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            blockingResult.matchedPattern?.let { pattern ->
                Text(
                    text = "Matched pattern: $pattern",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Text(
                text = "Confidence: ${(blockingResult.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuranicVerseCard(
    verse: QuranicVerse,
    selectedLanguage: Language,
    enableArabicText: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Surah and verse reference
            Text(
                text = "${verse.surahName} (${verse.surahNumber}:${verse.verseNumber})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
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
                
                Spacer(modifier = Modifier.height(8.dp))
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
            
            // Context
            if (verse.context.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Context:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = verse.context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IslamicGuidanceCard(
    guidance: IslamicGuidance,
    selectedLanguage: Language
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Islamic Guidance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Text(
                text = guidance.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Action recommendations
            if (guidance.actionRecommendations.isNotEmpty()) {
                Text(
                    text = "Recommended Actions:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Du'a:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
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
private fun ReflectionTimerCard(remainingTime: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Reflection period timer: $remainingTime seconds remaining. Please take this time to reflect and seek Allah's guidance."
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reflection Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Text(
                text = "${remainingTime}s",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.semantics {
                    contentDescription = "$remainingTime seconds remaining in reflection period"
                }
            )
            
            LinearProgressIndicator(
                progress = { (15 - remainingTime) / 15f },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Reflection timer progress: ${((15 - remainingTime) * 100 / 15).toInt()}% complete"
                    },
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer
            )
            
            Text(
                text = "Please take this time to reflect and seek Allah's guidance",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockedSiteActionButtons(
    canContinue: Boolean,
    category: BlockingCategory?,
    onAction: (WarningDialogAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Close/Navigate Away button
        Button(
            onClick = { onAction(WarningDialogAction.Close) },
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = "Navigate away from this blocked site and go to a safe location"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null, // Icon is decorative, button has semantic description
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Go Back")
        }
        
        // Continue button (only for lower severity categories)
        if (category?.severity ?: 5 < 4) {
            Button(
                onClick = { onAction(WarningDialogAction.Continue) },
                enabled = canContinue,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = if (canContinue) {
                            "Continue to the blocked site after reflection period"
                        } else {
                            "Please wait for the reflection period to complete before continuing"
                        }
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    if (canContinue) "Continue" else "Please wait...",
                    color = if (canContinue) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}