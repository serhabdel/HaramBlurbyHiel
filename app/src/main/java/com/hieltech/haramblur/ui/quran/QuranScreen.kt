package com.hieltech.haramblur.ui.quran

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.api.QuranChapter

private val QuranGreen = Color(0xFF1B5E20)
private val QuranGreenLight = Color(0xFF4CAF50)
private val QuranGold = Color(0xFFFFD700)
private val QuranGoldDark = Color(0xFFB8860B)
private val MakkahColor = Color(0xFF6D4C41)
private val MadinahColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    viewModel: QuranViewModel = hiltViewModel(),
    onSurahClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf(
        stringResource(R.string.quran_tab_surahs),
        stringResource(R.string.quran_tab_juz),
        stringResource(R.string.quran_tab_bookmarks)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──
        item {
            QuranHeaderCard()
        }

        // ── Verse of the Day ──
        item {
            VerseOfDayCard(
                verse = uiState.verseOfDay,
                isLoading = uiState.isLoadingVerseOfDay,
                onTap = {
                    uiState.verseOfDay?.let { v ->
                        onSurahClick(v.chapterId)
                    }
                }
            )
        }

        // ── Continue Reading ──
        if (uiState.lastReadSurah > 0) {
            item {
                ContinueReadingCard(
                    surahNumber = uiState.lastReadSurah,
                    verseNumber = uiState.lastReadVerse,
                    surahName = uiState.chapters.find { it.id == uiState.lastReadSurah }?.nameSimple ?: "",
                    onClick = { onSurahClick(uiState.lastReadSurah) }
                )
            }
        }

        // ── Search Bar ──
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                label = { Text(stringResource(R.string.quran_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // ── Search Results ──
        if (uiState.searchQuery.length >= 2) {
            if (uiState.isSearching) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QuranGreen)
                    }
                }
            } else {
                items(uiState.searchResults) { result ->
                    SearchResultCard(
                        verseKey = result.verseKey,
                        text = result.text,
                        onClick = {
                            val parts = result.verseKey.split(":")
                            if (parts.size == 2) onSurahClick(parts[0].toIntOrNull() ?: 1)
                        }
                    )
                }
            }
        } else {
            // ── Tab Row ──
            item {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = QuranGreen
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.setTab(index) },
                            text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // ── Tab Content ──
            when (uiState.selectedTab) {
                0 -> { // Surahs
                    if (uiState.isLoadingChapters) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = QuranGreen)
                            }
                        }
                    } else {
                        itemsIndexed(uiState.chapters) { _, chapter ->
                            SurahListItem(chapter = chapter, onClick = { onSurahClick(chapter.id) })
                        }
                    }
                }
                1 -> { // Juz
                    if (uiState.juzs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = QuranGreen)
                            }
                        }
                    } else {
                        items(uiState.juzs) { juz ->
                            JuzListItem(juz = juz, chapters = uiState.chapters, onClick = {
                                // Navigate to the first chapter in the juz
                                val firstChapter = juz.verseMapping.keys.firstOrNull()
                                    ?.split(":")?.firstOrNull()?.toIntOrNull() ?: 1
                                onSurahClick(firstChapter)
                            })
                        }
                    }
                }
                2 -> { // Bookmarks
                    if (uiState.bookmarkedVerses.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        stringResource(R.string.quran_no_bookmarks),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.bookmarkedVerses.toList().sorted()) { verseKey ->
                            BookmarkItem(
                                verseKey = verseKey,
                                chapters = uiState.chapters,
                                onClick = {
                                    val parts = verseKey.split(":")
                                    if (parts.size == 2) onSurahClick(parts[0].toIntOrNull() ?: 1)
                                },
                                onRemove = { viewModel.toggleBookmark(verseKey) }
                            )
                        }
                    }
                }
            }
        }

        // ── Error ──
        uiState.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.loadChapters(); viewModel.clearError() }) {
                            Text(stringResource(R.string.quran_retry))
                        }
                    }
                }
            }
        }
    }
}

// ==================== Helper Composables ====================

@Composable
private fun QuranHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(QuranGreen, Color(0xFF2E7D32)))
                )
                .padding(24.dp)
        ) {
            Column {
                Text("﷽", fontSize = 28.sp, color = QuranGold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.quran_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White
                )
                Text(
                    stringResource(R.string.quran_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun VerseOfDayCard(
    verse: com.hieltech.haramblur.data.api.QuranVerse?,
    isLoading: Boolean,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = verse != null) { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = QuranGoldDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quran_verse_of_day), fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = QuranGreen)
            } else if (verse != null) {
                Text(
                    verse.textUthmani,
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (verse.translationText.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(verse.translationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Text("— ${verse.verseKey}",
                    style = MaterialTheme.typography.labelSmall,
                    color = QuranGoldDark, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(surahNumber: Int, verseNumber: Int, surahName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = QuranGreen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.quran_continue_reading), fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                Text("$surahName — ${stringResource(R.string.quran_ayah)} $verseNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun SurahListItem(chapter: QuranChapter, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah number badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(QuranGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${chapter.id}", fontWeight = FontWeight.Bold, color = QuranGreen, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chapter.nameSimple, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val placeColor = if (chapter.revelationPlace == "makkah") MakkahColor else MadinahColor
                    Text(
                        chapter.revelationPlace.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = placeColor
                    )
                    Text(" • ", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("${chapter.versesCount} ${stringResource(R.string.quran_ayahs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                if (chapter.translatedName.isNotBlank() && chapter.translatedName != chapter.nameSimple) {
                    Text(chapter.translatedName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            // Arabic name
            Text(
                chapter.nameArabic,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl),
                color = QuranGreen, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun JuzListItem(
    juz: com.hieltech.haramblur.data.api.QuranJuz,
    chapters: List<QuranChapter>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(QuranGoldDark.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${juz.juzNumber}", fontWeight = FontWeight.Bold, color = QuranGoldDark, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${stringResource(R.string.quran_juz)} ${juz.juzNumber}",
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                val chapterNames = juz.verseMapping.keys.mapNotNull { key ->
                    val chId = key.toIntOrNull()
                    chapters.find { it.id == chId }?.nameSimple
                }.distinct().take(3)
                if (chapterNames.isNotEmpty()) {
                    Text(chapterNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (juz.versesCount > 0) {
                Text("${juz.versesCount} ${stringResource(R.string.quran_ayahs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SearchResultCard(verseKey: String, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(verseKey, fontWeight = FontWeight.Bold, color = QuranGreen,
                style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BookmarkItem(
    verseKey: String,
    chapters: List<QuranChapter>,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val parts = verseKey.split(":")
    val chapterName = if (parts.isNotEmpty()) {
        chapters.find { it.id == (parts[0].toIntOrNull() ?: 0) }?.nameSimple ?: ""
    } else ""

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = QuranGoldDark, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(verseKey, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                if (chapterName.isNotBlank()) {
                    Text(chapterName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.quran_remove_bookmark),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

