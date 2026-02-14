package com.hieltech.haramblur.ui.quran

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AudioPlaybackState
import com.hieltech.haramblur.data.api.QuranReciter
import com.hieltech.haramblur.data.api.QuranVerse

private val ReaderGreen = Color(0xFF1B5E20)
private val ReaderGold = Color(0xFFFFD700)
private val ReaderGoldDark = Color(0xFFB8860B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onShare: (QuranVerse) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val audioPlayback = uiState.audioPlayback

    // Load verses when this screen first appears
    LaunchedEffect(surahNumber) {
        viewModel.loadVerses(surahNumber)
    }

    // Reciter picker dialog
    if (uiState.showReciterPicker) {
        ReciterPickerDialog(
            reciters = uiState.reciters,
            selectedId = uiState.selectedReciterId,
            onSelect = { viewModel.selectReciter(it) },
            onDismiss = { viewModel.toggleReciterPicker() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.currentSurahName.ifBlank { stringResource(R.string.quran_title) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${stringResource(R.string.quran_surah)} $surahNumber",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.quran_back))
                    }
                },
                actions = {
                    // Play surah audio button
                    IconButton(onClick = {
                        if (audioPlayback.isPlaying && audioPlayback.currentSurahNumber == surahNumber) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playChapterAudio(surahNumber)
                        }
                    }) {
                        if (uiState.isLoadingAudio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ReaderGreen
                            )
                        } else {
                            Icon(
                                if (audioPlayback.isPlaying && audioPlayback.currentSurahNumber == surahNumber)
                                    Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.quran_audio_play),
                                tint = ReaderGreen
                            )
                        }
                    }
                    // Reciter selector
                    IconButton(onClick = { viewModel.toggleReciterPicker() }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.quran_audio_select_reciter))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Audio player bar
            AnimatedVisibility(
                visible = audioPlayback.isPlaying || audioPlayback.isLoading,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                AudioPlayerBar(
                    playbackState = audioPlayback,
                    reciterName = uiState.reciters.find { it.id == uiState.selectedReciterId }?.name ?: "",
                    onPlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stopAudio() },
                    onNext = { viewModel.skipNext() },
                    onPrevious = { viewModel.skipPrevious() }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoadingVerses && uiState.verses.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ReaderGreen)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.quran_loading_verses),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (uiState.verses.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Text(uiState.error ?: stringResource(R.string.quran_no_verses),
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadVerses(surahNumber) }) {
                        Text(stringResource(R.string.quran_retry))
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bismillah header (except Al-Fatiha & At-Tawbah)
                if (surahNumber != 1 && surahNumber != 9) {
                    item {
                        Text(
                            "\ufdfd",
                            fontSize = 36.sp,
                            color = ReaderGoldDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }

                itemsIndexed(uiState.verses, key = { _, v -> v.verseKey }) { index, verse ->
                    val isCurrentAudioVerse = audioPlayback.isPlaying &&
                            audioPlayback.currentVerseKey == verse.verseKey
                    VerseItem(
                        verse = verse,
                        isBookmarked = uiState.bookmarkedVerses.contains(verse.verseKey),
                        isCurrentAudioVerse = isCurrentAudioVerse,
                        showTranslation = uiState.showTranslation,
                        fontSize = uiState.fontSize,
                        onBookmark = { viewModel.toggleBookmark(verse.verseKey) },
                        onShare = { onShare(verse) },
                        onPlayVerse = { viewModel.playVerseAudio(surahNumber, index) }
                    )
                }

                // Load more trigger
                item {
                    LaunchedEffect(uiState.verses.size) {
                        if (uiState.currentPage < uiState.totalPages) {
                            viewModel.loadNextPage()
                        }
                    }
                    if (uiState.isLoadingVerses) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ReaderGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseItem(
    verse: QuranVerse,
    isBookmarked: Boolean,
    isCurrentAudioVerse: Boolean,
    showTranslation: Boolean,
    fontSize: Float,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onPlayVerse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentAudioVerse)
                ReaderGreen.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isCurrentAudioVerse) androidx.compose.foundation.BorderStroke(
            2.dp, ReaderGreen.copy(alpha = 0.4f)
        ) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            // Verse header row: badge + actions
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verse key badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(ReaderGreen, Color(0xFF2E7D32))))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(verse.verseKey, color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                }
                // Action icons
                Row {
                    IconButton(onClick = onPlayVerse, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.quran_audio_play),
                            tint = ReaderGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onBookmark, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) ReaderGoldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.quran_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Arabic text
            Text(
                verse.textUthmani,
                style = MaterialTheme.typography.headlineSmall.copy(
                    textDirection = TextDirection.Rtl,
                    lineHeight = (fontSize * 2).sp
                ),
                fontSize = fontSize.sp,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation
            if (showTranslation && verse.translationText.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(
                    verse.translationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}


@Composable
private fun AudioPlayerBar(
    playbackState: AudioPlaybackState,
    reciterName: String,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Progress indicator
            if (playbackState.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (playbackState.positionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = ReaderGreen,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(6.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info column
                Column(Modifier.weight(1f)) {
                    Text(
                        if (playbackState.currentVerseKey.isNotEmpty())
                            "${stringResource(R.string.quran_audio_now_playing)} • ${playbackState.currentVerseKey}"
                        else stringResource(R.string.quran_audio_now_playing),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reciterName.isNotBlank()) {
                        Text(
                            reciterName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playbackState.totalTracks > 1) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.quran_audio_previous),
                                modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                        if (playbackState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ReaderGreen)
                        } else {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (playbackState.isPlaying) R.string.quran_audio_pause else R.string.quran_audio_play
                                ),
                                tint = ReaderGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (playbackState.totalTracks > 1) {
                        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.quran_audio_next),
                                modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.quran_audio_stop),
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReciterPickerDialog(
    reciters: List<QuranReciter>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quran_audio_select_reciter), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(reciters) { _, reciter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (reciter.id == selectedId) ReaderGreen.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(reciter.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = reciter.id == selectedId,
                            onClick = { onSelect(reciter.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = ReaderGreen)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(reciter.name, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (reciter.id == selectedId) FontWeight.Bold else FontWeight.Normal)
                            reciter.style?.let { style ->
                                Text(style, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.quran_back)) }
        }
    )
}
