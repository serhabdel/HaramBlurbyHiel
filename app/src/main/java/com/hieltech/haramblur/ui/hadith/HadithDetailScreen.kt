package com.hieltech.haramblur.ui.hadith

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.api.Hadith
import com.hieltech.haramblur.detection.Language

// Reuse color palette from HadithScreen
private val HadithGreen = Color(0xFF1B5E20)
private val HadithGreenLight = Color(0xFF4CAF50)
private val HadithGold = Color(0xFFFFD700)
private val HadithGoldDark = Color(0xFFB8860B)
private val SahihGreen = Color(0xFF2E7D32)
private val HasanBlue = Color(0xFF1565C0)
private val DaifRed = Color(0xFFD32F2F)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HadithDetailScreen(
    hadiths: List<Hadith>,
    initialIndex: Int = 0,
    appLanguage: Language = Language.ENGLISH,
    onBack: () -> Unit = {},
    onShare: (Hadith) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (hadiths.size - 1).coerceAtLeast(0)),
        pageCount = { hadiths.size }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        HadithDetailTopBar(
            currentIndex = pagerState.currentPage,
            total = hadiths.size,
            hadith = hadiths.getOrNull(pagerState.currentPage),
            onBack = onBack,
            onShare = { hadiths.getOrNull(pagerState.currentPage)?.let(onShare) }
        )

        // Pager
        if (hadiths.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.hadith_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                HadithDetailPage(
                    hadith = hadiths[page],
                    appLanguage = appLanguage
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithDetailTopBar(
    currentIndex: Int,
    total: Int,
    hadith: Hadith?,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.hadith),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (hadith != null) {
                    Text(
                        text = "#${hadith.hadithNumber} · ${currentIndex + 1}/$total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.hadith_back))
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.hadith_share))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun HadithDetailPage(
    hadith: Hadith,
    appLanguage: Language
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grade & Book info row
        HadithMetadataRow(hadith)

        // Arabic text section (always shown)
        if (hadith.arabicText.isNotEmpty()) {
            ArabicTextSection(hadith)
        }

        // Translation section (based on app language)
        TranslationSection(hadith, appLanguage)

        // Narrator section
        if (hadith.narrator.isNotEmpty()) {
            NarratorSection(hadith)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun HadithMetadataRow(hadith: Hadith) {
    val gradeColor = when {
        hadith.grade.contains("Sahih", ignoreCase = true) -> SahihGreen
        hadith.grade.contains("Hasan", ignoreCase = true) -> HasanBlue
        hadith.grade.contains("Da", ignoreCase = true) -> DaifRed
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Book name
        if (hadith.bookName.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = hadith.bookName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Chapter
            if (hadith.chapterNumber.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HadithGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${stringResource(R.string.hadith_chapter)} ${hadith.chapterNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HadithGoldDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Grade
            if (hadith.grade.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = gradeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gradeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = hadith.grade,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArabicTextSection(hadith: Hadith) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HadithGreen.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Heading
            if (hadith.headingArabic.isNotEmpty()) {
                Text(
                    text = hadith.headingArabic,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 30.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = HadithGreen,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(
                    color = HadithGreen.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Arabic body
            Text(
                text = hadith.arabicText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    textDirection = TextDirection.Rtl,
                    lineHeight = 42.sp
                ),
                fontWeight = FontWeight.Medium,
                color = HadithGreen,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TranslationSection(hadith: Hadith, appLanguage: Language) {
    // Determine which translation to show based on app language
    val translationText = when (appLanguage) {
        Language.URDU -> hadith.urduText.ifEmpty { hadith.englishText }
        Language.ARABIC -> hadith.englishText // Arabic user already sees Arabic above, show English as secondary
        else -> hadith.englishText // All other languages fall back to English (API limitation)
    }

    val headingText = when (appLanguage) {
        Language.URDU -> hadith.headingUrdu.ifEmpty { hadith.headingEnglish }
        else -> hadith.headingEnglish
    }

    val translationLabel = when (appLanguage) {
        Language.URDU -> stringResource(R.string.hadith_urdu_text)
        else -> stringResource(R.string.hadith_english_text)
    }

    if (translationText.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Label
                Text(
                    text = translationLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                // Heading
                if (headingText.isNotEmpty()) {
                    Text(
                        text = headingText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Body
                val isRtl = appLanguage == Language.URDU || appLanguage == Language.PERSIAN
                Text(
                    text = translationText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NarratorSection(hadith: Hadith) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = HadithGreen.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = HadithGreen
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.hadith_narrator),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = hadith.narrator,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


