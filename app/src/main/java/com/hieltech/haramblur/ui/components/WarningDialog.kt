package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.data.WarningDialogState
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import kotlinx.coroutines.delay

/**
 * Warning dialog component for full-screen blur scenarios with Islamic guidance
 */
@Composable
fun WarningDialog(
    state: WarningDialogState,
    onAction: (WarningDialogAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    // Countdown timer effect
    LaunchedEffect(state.reflectionTimeSeconds, state.remainingTimeSeconds) {
        if (state.remainingTimeSeconds > 0) {
            delay(1000)
            // This would be handled by the parent component to update remaining time
        }
    }
    
    Dialog(
        onDismissRequest = { 
            // Prevent dismissal by clicking outside during reflection period
            if (state.canContinue) {
                onAction(WarningDialogAction.Dismiss)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state.canContinue,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with warning icon and title
                WarningDialogHeader(
                    title = state.title,
                    category = state.category,
                    onClose = if (state.showCloseOption) {
                        { onAction(WarningDialogAction.Close) }
                    } else null
                )
                
                // Main message
                if (state.message.isNotEmpty()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Quranic verse section
                state.quranicVerse?.let { verse ->
                    QuranicVerseSection(
                        verse = verse,
                        language = state.language,
                        onLanguageChange = { language ->
                            onAction(WarningDialogAction.ChangeLanguage(language))
                        }
                    )
                }
                
                // Reflection timer
                ReflectionTimer(
                    totalTime = state.reflectionTimeSeconds,
                    remainingTime = state.remainingTimeSeconds,
                    isComplete = state.canContinue
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action buttons
                WarningDialogActions(
                    showCloseOption = state.showCloseOption,
                    showContinueOption = state.showContinueOption,
                    canContinue = state.canContinue,
                    onClose = { onAction(WarningDialogAction.Close) },
                    onContinue = { onAction(WarningDialogAction.Continue) }
                )
            }
        }
    }
}

@Composable
private fun WarningDialogHeader(
    title: String,
    category: BlockingCategory?,
    onClose: (() -> Unit)?
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
                contentDescription = "Warning",
                tint = when (category?.severity ?: 3) {
                    5 -> MaterialTheme.colorScheme.error
                    4 -> Color(0xFFFF6B35) // Orange-red
                    3 -> Color(0xFFFF8C00) // Orange
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                category?.let {
                    Text(
                        text = it.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        onClose?.let {
            IconButton(onClick = it) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuranicVerseSection(
    verse: QuranicVerse,
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Surah and verse reference
            Text(
                text = "${verse.surahName} ${verse.surahNumber}:${verse.verseNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Arabic text (always shown)
            Text(
                text = verse.arabicText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium
            )
            
            // Translation
            verse.translations[language]?.let { translation ->
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (language.isRTL) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Language selector
            LanguageSelector(
                currentLanguage = language,
                availableLanguages = verse.translations.keys.toList(),
                onLanguageChange = onLanguageChange
            )
            
            // Reflection text
            if (verse.reflection.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = verse.reflection,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    currentLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageChange: (Language) -> Unit
) {
    if (availableLanguages.size <= 1) return
    
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentLanguage.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Translation Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageChange(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReflectionTimer(
    totalTime: Int,
    remainingTime: Int,
    isComplete: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isComplete) "Reflection Period Complete" else "Reflection Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (!isComplete) {
                LinearProgressIndicator(
                    progress = { (totalTime - remainingTime).toFloat() / totalTime },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Text(
                    text = "${remainingTime}s remaining",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Please take this time to reflect on your intentions and seek Allah's guidance.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "You may now choose to continue or close this content.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WarningDialogActions(
    showCloseOption: Boolean,
    showContinueOption: Boolean,
    canContinue: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showCloseOption) {
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        if (showContinueOption) {
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (canContinue) "Continue" else "Please Wait",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}