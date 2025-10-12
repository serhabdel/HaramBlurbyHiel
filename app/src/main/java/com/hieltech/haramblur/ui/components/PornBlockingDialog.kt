package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
// Removed Dialog imports - using overlay approach instead
import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.IslamicGuidance
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.detection.SiteBlockingResult
import kotlinx.coroutines.delay

/**
 * Enhanced dialog for porn site blocking with prominent Quranic verse display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PornBlockingDialog(
    blockingResult: SiteBlockingResult,
    guidance: IslamicGuidance? = null,
    selectedLanguage: Language = Language.ENGLISH,
    enableArabicText: Boolean = true,
    onLanguageChange: (Language) -> Unit = {},
    onAction: (WarningDialogAction) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var remainingTime by remember { mutableStateOf(30) } // Extended time for porn sites
    var canContinue by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    // Extended countdown timer for porn sites
    LaunchedEffect(Unit) {
        remainingTime = 30
        canContinue = false

        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
        canContinue = true
    }

    // Use full-screen overlay instead of system Dialog to avoid window token issues
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)) // Darker overlay for porn sites
            .clickable(enabled = false) { /* Prevent click-through */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A) // Dark background for serious tone
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A),
                                Color(0xFF1A1A1A),
                                Color(0xFF0D0D0D)
                            )
                        )
                    )
            ) {
                // Enhanced header with warning
                PornBlockingHeader(
                    category = blockingResult.category ?: BlockingCategory.EXPLICIT_CONTENT,
                    confidence = blockingResult.confidence,
                    onLanguageMenuToggle = { showLanguageMenu = !showLanguageMenu }
                )

                // Language selector
                if (showLanguageMenu) {
                    PornLanguageDropdown(
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

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable content with enhanced Quranic display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // Urgent warning message
                    UrgentWarningCard()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced Quranic verse display
                    guidance?.verse?.let { verse ->
                        EnhancedQuranicVerseCard(
                            verse = verse,
                            selectedLanguage = selectedLanguage,
                            enableArabicText = enableArabicText
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Islamic guidance with porn-specific advice
                    guidance?.let { islamicGuidance ->
                        PornSpecificGuidanceCard(
                            guidance = islamicGuidance,
                            selectedLanguage = selectedLanguage
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Extended reflection timer
                if (!canContinue) {
                    ExtendedReflectionTimerCard(remainingTime = remainingTime)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Enhanced action buttons for porn sites
                PornBlockingActionButtons(
                    canContinue = canContinue,
                    category = blockingResult.category,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun PornBlockingHeader(
    category: BlockingCategory,
    confidence: Float,
    onLanguageMenuToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2E7D32), // Islamic green
                        Color(0xFF1B5E20)  // Dark green
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Protection Active",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )

                Column {
                    Text(
                        text = "Protected by Allah's Guidance",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Seeking Allah's Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            IconButton(onClick = onLanguageMenuToggle) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(R.string.language_settings),
                        tint = Color.White
                    )
            }
        }
    }
}

@Composable
private fun UrgentWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9) // Light green background
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Allah's Protection is With You",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This content may harm your spiritual well-being. Let's turn to something beneficial instead.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EnhancedQuranicVerseCard(
    verse: QuranicVerse,
    selectedLanguage: Language,
    enableArabicText: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f) // Green tint
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Surah reference with enhanced styling
            Text(
                text = "${verse.surahName} (${verse.surahNumber}:${verse.verseNumber})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Arabic text with enhanced display
            if (enableArabicText) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = verse.arabicText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 24.sp,
                                lineHeight = 36.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2E7D32)
                        )
                    }
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
                    color = Color(0xFF424242)
                )
            }

            // Translation with enhanced styling
            verse.translations[selectedLanguage]?.let { translation ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFE8F5E8),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\"$translation\"",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            lineHeight = 28.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Context with enhanced display
            if (verse.context.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4) // Light yellow
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Context:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = verse.context,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PornSpecificGuidanceCard(
    guidance: IslamicGuidance,
    selectedLanguage: Language
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // Light blue
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.protection_guidance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Text(
                text = guidance.guidance,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF424242)
            )

            // Action recommendations with enhanced styling
            if (guidance.actionRecommendations.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.immediate_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                guidance.actionRecommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF424242),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Dua text with enhanced display
            guidance.duaText?.let { dua ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dua_for_protection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = dua,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PornBlockingDetailsCard(blockingResult: SiteBlockingResult) {
    // Removed - no longer showing technical blocking details
    // This improves UX by not exposing harmful keywords or patterns
}

@Composable
private fun ExtendedReflectionTimerCard(remainingTime: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Extended reflection period timer: $remainingTime seconds remaining. Take this time to reflect on Allah's guidance and seek protection from temptation."
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE0B2) // Light orange
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
                text = stringResource(R.string.extended_reflection_period),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            val reflectionText = "$remainingTime" + "s"
            val reflectionDescription = "$remainingTime seconds remaining in extended reflection period"

            Text(
                text = reflectionText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
                modifier = Modifier.semantics {
                    contentDescription = reflectionDescription
                }
            )

            LinearProgressIndicator(
                progress = { (30 - remainingTime) / 30f },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Extended reflection timer progress: ${((30 - remainingTime) * 100 / 30).toInt()}% complete"
                    },
                color = Color(0xFFD32F2F),
                trackColor = Color(0xFFFFE0B2)
            )

            Text(
                text = stringResource(R.string.take_extended_time),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF424242)
            )
        }
    }
}

@Composable
private fun PornBlockingActionButtons(
    canContinue: Boolean,
    category: BlockingCategory?,
    onAction: (WarningDialogAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary action: Close and protect
        Button(
            onClick = { onAction(WarningDialogAction.Close) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Close this harmful content and seek Allah's protection"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.close_seek_protection),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Secondary action: Continue (only for lower severity)
        if (category?.severity ?: 5 < 4 && canContinue) {
            OutlinedButton(
                onClick = { onAction(WarningDialogAction.Continue) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Continue to content after extended reflection period"
                    },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9800) // Orange
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9800))
            ) {
                Text(
                    text = stringResource(R.string.continue_after_reflection),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // For high-severity porn sites, add additional warning
        if (category == BlockingCategory.EXPLICIT_CONTENT && canContinue) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE) // Light red
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.high_risk_content_warning),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.content_explicit_harmful),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB71C1C),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Additional confirmation button for explicit content
                    Button(
                        onClick = { onAction(WarningDialogAction.Continue) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.understand_risk_continue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Show remaining time if cannot continue
        if (!canContinue) {
            Text(
                text = stringResource(R.string.complete_reflection_period),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PornLanguageDropdown(
    selectedLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageChange: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
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
                        Text(language.displayName, color = Color.White)
                        if (language == selectedLanguage) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = stringResource(R.string.selected),
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}