package com.hieltech.haramblur.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.AudioPlaybackState
import com.hieltech.haramblur.data.QuranAudioManager
import com.hieltech.haramblur.data.QuranRepository
import com.hieltech.haramblur.data.QuranResult
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.api.QuranAudioFile
import com.hieltech.haramblur.data.api.QuranChapter
import com.hieltech.haramblur.data.api.QuranVerse
import com.hieltech.haramblur.data.api.QuranJuz
import com.hieltech.haramblur.data.api.QuranReciter
import com.hieltech.haramblur.data.api.QuranSearchResult
import com.hieltech.haramblur.detection.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranUiState(
    val chapters: List<QuranChapter> = emptyList(),
    val juzs: List<QuranJuz> = emptyList(),
    val verses: List<QuranVerse> = emptyList(),
    val isLoadingChapters: Boolean = false,
    val isLoadingVerses: Boolean = false,
    val error: String? = null,
    val verseOfDay: QuranVerse? = null,
    val isLoadingVerseOfDay: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<QuranSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val bookmarkedVerses: Set<String> = emptySet(),
    val currentSurahNumber: Int = 0,
    val currentSurahName: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val appLanguage: Language = Language.ENGLISH,
    val showTranslation: Boolean = true,
    val fontSize: Float = 28f,
    val selectedTab: Int = 0, // 0 = Surahs, 1 = Juz, 2 = Bookmarks
    val lastReadSurah: Int = 0,
    val lastReadVerse: Int = 0,
    val preferredTranslationId: String = "",
    // Audio state
    val reciters: List<QuranReciter> = emptyList(),
    val selectedReciterId: Int = 7,
    val verseAudioFiles: List<QuranAudioFile> = emptyList(),
    val audioPlayback: AudioPlaybackState = AudioPlaybackState(),
    val isLoadingAudio: Boolean = false,
    val showReciterPicker: Boolean = false
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    val audioManager: QuranAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadChapters()
        loadVerseOfDay()
        loadJuzs()
        loadReciters()
        observeAudioPlayback()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                val language = settings.preferredLanguage
                _uiState.value = _uiState.value.copy(
                    appLanguage = language,
                    bookmarkedVerses = settings.quranBookmarkedVerses,
                    showTranslation = settings.quranShowTranslation,
                    fontSize = settings.quranFontSize,
                    lastReadSurah = settings.lastReadSurah,
                    lastReadVerse = settings.lastReadVerse,
                    preferredTranslationId = settings.preferredTranslationId.ifBlank {
                        quranRepository.getDefaultTranslationId(language.code)
                    }
                )
            } catch (_: Exception) {}
        }
    }

    private fun getLanguageCode(): String {
        return _uiState.value.appLanguage.code.let {
            if (it == "in") "id" else it // API uses "id" for Indonesian
        }
    }

    fun loadChapters() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChapters = true, error = null)
            when (val result = quranRepository.getChapters(getLanguageCode())) {
                is QuranResult.Success -> _uiState.value = _uiState.value.copy(
                    chapters = result.data, isLoadingChapters = false
                )
                is QuranResult.Error -> _uiState.value = _uiState.value.copy(
                    error = result.message, isLoadingChapters = false
                )
            }
        }
    }

    private fun loadJuzs() {
        viewModelScope.launch {
            when (val result = quranRepository.getJuzs()) {
                is QuranResult.Success -> _uiState.value = _uiState.value.copy(juzs = result.data)
                is QuranResult.Error -> {} // Silent fail for juzs
            }
        }
    }

    private fun loadVerseOfDay() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVerseOfDay = true)
            val translationId = _uiState.value.preferredTranslationId
            when (val result = quranRepository.getVerseOfDay(getLanguageCode(), translationId.ifBlank { null })) {
                is QuranResult.Success -> _uiState.value = _uiState.value.copy(
                    verseOfDay = result.data, isLoadingVerseOfDay = false
                )
                is QuranResult.Error -> _uiState.value = _uiState.value.copy(isLoadingVerseOfDay = false)
            }
        }
    }

    fun loadVerses(surahNumber: Int, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingVerses = true, error = null, currentSurahNumber = surahNumber
            )
            val chapter = _uiState.value.chapters.find { it.id == surahNumber }
            _uiState.value = _uiState.value.copy(currentSurahName = chapter?.nameSimple ?: "")
            val translationId = _uiState.value.preferredTranslationId
            when (val result = quranRepository.getVersesByChapter(
                chapterNumber = surahNumber,
                language = getLanguageCode(),
                translationId = translationId.ifBlank { null },
                page = page
            )) {
                is QuranResult.Success -> {
                    val newVerses = if (page == 1) result.data.verses
                        else _uiState.value.verses + result.data.verses
                    _uiState.value = _uiState.value.copy(
                        verses = newVerses,
                        isLoadingVerses = false,
                        currentPage = result.data.currentPage,
                        totalPages = result.data.totalPages
                    )
                    saveLastReadPosition(surahNumber, newVerses.lastOrNull()?.verseNumber ?: 0)
                }
                is QuranResult.Error -> _uiState.value = _uiState.value.copy(
                    error = result.message, isLoadingVerses = false
                )
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages && !state.isLoadingVerses) {
            loadVerses(state.currentSurahNumber, state.currentPage + 1)
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            when (val result = quranRepository.search(query, getLanguageCode())) {
                is QuranResult.Success -> _uiState.value = _uiState.value.copy(
                    searchResults = result.data.results, isSearching = false
                )
                is QuranResult.Error -> _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    fun toggleBookmark(verseKey: String) {
        val current = _uiState.value.bookmarkedVerses.toMutableSet()
        if (current.contains(verseKey)) current.remove(verseKey) else current.add(verseKey)
        _uiState.value = _uiState.value.copy(bookmarkedVerses = current)
        viewModelScope.launch {
            val settings = settingsRepository.getCurrentSettings()
            settingsRepository.updateSettings(settings.copy(quranBookmarkedVerses = current))
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    private fun saveLastReadPosition(surah: Int, verse: Int) {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(settings.copy(
                    lastReadSurah = surah, lastReadVerse = verse
                ))
                _uiState.value = _uiState.value.copy(lastReadSurah = surah, lastReadVerse = verse)
            } catch (_: Exception) {}
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ==================== Audio Methods ====================

    private fun observeAudioPlayback() {
        viewModelScope.launch {
            audioManager.playbackState.collect { playback ->
                _uiState.value = _uiState.value.copy(audioPlayback = playback)
            }
        }
    }

    private fun loadReciters() {
        viewModelScope.launch {
            when (val result = quranRepository.getRecitations(getLanguageCode())) {
                is QuranResult.Success -> {
                    val settings = settingsRepository.getCurrentSettings()
                    _uiState.value = _uiState.value.copy(
                        reciters = result.data,
                        selectedReciterId = settings.preferredReciterId
                    )
                }
                is QuranResult.Error -> {} // Silent fail
            }
        }
    }

    fun playChapterAudio(surahNumber: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAudio = true)
            val reciterId = _uiState.value.selectedReciterId
            when (val result = quranRepository.getChapterRecitationUrl(reciterId, surahNumber)) {
                is QuranResult.Success -> {
                    audioManager.playChapterAudio(surahNumber, result.data)
                    _uiState.value = _uiState.value.copy(isLoadingAudio = false)
                }
                is QuranResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAudio = false, error = result.message
                    )
                }
            }
        }
    }

    fun playVerseAudio(surahNumber: Int, startVerseIndex: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAudio = true)
            val reciterId = _uiState.value.selectedReciterId
            when (val result = quranRepository.getVerseRecitations(reciterId, surahNumber)) {
                is QuranResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        verseAudioFiles = result.data, isLoadingAudio = false
                    )
                    audioManager.playVerseAudio(surahNumber, result.data, startVerseIndex)
                }
                is QuranResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAudio = false, error = result.message
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        audioManager.togglePlayPause()
    }

    fun stopAudio() {
        audioManager.stop()
    }

    fun skipNext() {
        audioManager.skipNext()
    }

    fun skipPrevious() {
        audioManager.skipPrevious()
    }

    fun selectReciter(reciterId: Int) {
        _uiState.value = _uiState.value.copy(selectedReciterId = reciterId, showReciterPicker = false)
        viewModelScope.launch {
            val settings = settingsRepository.getCurrentSettings()
            settingsRepository.updateSettings(settings.copy(preferredReciterId = reciterId))
        }
        // If audio is currently playing, restart with new reciter
        val playback = _uiState.value.audioPlayback
        if (playback.isPlaying && playback.currentSurahNumber > 0) {
            playChapterAudio(playback.currentSurahNumber)
        }
    }

    fun toggleReciterPicker() {
        _uiState.value = _uiState.value.copy(showReciterPicker = !_uiState.value.showReciterPicker)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}

