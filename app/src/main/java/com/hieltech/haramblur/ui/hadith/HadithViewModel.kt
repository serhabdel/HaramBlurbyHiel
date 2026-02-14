package com.hieltech.haramblur.ui.hadith

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.HadithRepository
import com.hieltech.haramblur.data.HadithResult
import com.hieltech.haramblur.data.HadithSingleResult
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.api.Hadith
import com.hieltech.haramblur.detection.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HadithUiState(
    val hadiths: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val total: Int = 0,
    val selectedBook: String? = null,
    val searchQuery: String = "",
    val hasApiKey: Boolean = false,
    val hadithOfDay: Hadith? = null,
    val isLoadingHadithOfDay: Boolean = false,
    val preferredBook: String? = null,
    val appLanguage: Language = Language.ENGLISH
)

data class HadithBook(
    val slug: String,
    @StringRes val displayNameResId: Int
) {
    fun getDisplayName(context: Context): String {
        return context.getString(displayNameResId)
    }
}

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithUiState())
    val uiState: StateFlow<HadithUiState> = _uiState.asStateFlow()

    // Hardcoded API key placeholder — user will replace via BuildConfig or settings
    private var apiKey: String = ""

    val availableBooks = listOf(
        HadithBook("sahih-bukhari", R.string.hadith_book_sahih_bukhari),
        HadithBook("sahih-muslim", R.string.hadith_book_sahih_muslim),
        HadithBook("al-tirmidhi", R.string.hadith_book_jami_tirmidhi),
        HadithBook("abu-dawood", R.string.hadith_book_sunan_abu_dawud),
        HadithBook("ibn-e-majah", R.string.hadith_book_sunan_ibn_majah),
        HadithBook("sunan-nasai", R.string.hadith_book_sunan_nasai),
        HadithBook("musnad-ahmad", R.string.hadith_book_musnad_ahmad),
        HadithBook("muwatta-imam-malik", R.string.hadith_book_muwatta_malik)
    )

    init {
        // Try to load API key from BuildConfig or default
        try {
            val field = Class.forName("com.hieltech.haramblur.BuildConfig")
                .getDeclaredField("HADITH_API_KEY")
            apiKey = field.get(null) as? String ?: ""
        } catch (_: Exception) {
            apiKey = ""
        }

        // Observe settings for preferred book
        observeSettings()

        if (apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE") {
            _uiState.value = _uiState.value.copy(hasApiKey = true)
            loadHadiths()
            loadHadithOfDay()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val preferredBook = settings.preferredHadithBook
                val currentPreferred = _uiState.value.preferredBook
                _uiState.value = _uiState.value.copy(
                    preferredBook = preferredBook,
                    appLanguage = settings.preferredLanguage
                )

                // If preferred book changed and no book is actively selected, apply it
                if (preferredBook != currentPreferred && _uiState.value.selectedBook == null && preferredBook != null) {
                    selectBook(preferredBook)
                }
            }
        }
    }

    fun setApiKey(key: String) {
        apiKey = key
        _uiState.value = _uiState.value.copy(hasApiKey = key.isNotEmpty())
        if (key.isNotEmpty()) {
            loadHadiths()
            loadHadithOfDay()
        }
    }

    fun selectBook(bookSlug: String?) {
        _uiState.value = _uiState.value.copy(
            selectedBook = bookSlug,
            hadiths = emptyList(),
            currentPage = 1
        )
        loadHadiths()
    }

    fun updatePreferredBook(bookSlug: String?) {
        viewModelScope.launch {
            val current = settingsRepository.settings.value
            settingsRepository.updateSettings(current.copy(preferredHadithBook = bookSlug))
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /** Returns hadiths filtered by the current search query (client-side) */
    fun getFilteredHadiths(): List<Hadith> {
        val state = _uiState.value
        if (state.searchQuery.isBlank()) return state.hadiths
        val query = state.searchQuery.lowercase()
        return state.hadiths.filter { hadith ->
            hadith.englishText.lowercase().contains(query) ||
            hadith.arabicText.contains(query) ||
            hadith.narrator.lowercase().contains(query) ||
            hadith.headingEnglish.lowercase().contains(query) ||
            hadith.hadithNumber.contains(query)
        }
    }

    fun loadHadithOfDay() {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHadithOfDay = true)

            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val book = _uiState.value.preferredBook ?: "sahih-bukhari"

            when (val result = hadithRepository.getHadithOfDay(apiKey, book, dayOfYear)) {
                is HadithSingleResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        hadithOfDay = result.hadith,
                        isLoadingHadithOfDay = false
                    )
                }
                is HadithSingleResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingHadithOfDay = false)
                }
            }
        }
    }

    fun loadHadiths(page: Int = 1) {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            _uiState.value = _uiState.value.copy(hasApiKey = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = hadithRepository.getHadiths(
                apiKey = apiKey,
                book = _uiState.value.selectedBook,
                page = page
            )

            when (result) {
                is HadithResult.Success -> {
                    val existingHadiths = if (page > 1) _uiState.value.hadiths else emptyList()
                    _uiState.value = _uiState.value.copy(
                        hadiths = existingHadiths + result.hadiths,
                        isLoading = false,
                        currentPage = result.currentPage,
                        lastPage = result.lastPage,
                        total = result.total,
                        error = null
                    )
                }
                is HadithResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.isLoading && state.currentPage < state.lastPage) {
            loadHadiths(state.currentPage + 1)
        }
    }

    fun retry() {
        loadHadiths(_uiState.value.currentPage)
    }
}
