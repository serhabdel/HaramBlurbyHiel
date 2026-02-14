package com.hieltech.haramblur.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.HadithRepository
import com.hieltech.haramblur.data.HadithResult
import com.hieltech.haramblur.data.api.Hadith
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val hasApiKey: Boolean = false
)

data class HadithBook(
    val slug: String,
    val displayName: String
)

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val hadithRepository: HadithRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithUiState())
    val uiState: StateFlow<HadithUiState> = _uiState.asStateFlow()

    // Hardcoded API key placeholder — user will replace via BuildConfig or settings
    private var apiKey: String = ""

    val availableBooks = listOf(
        HadithBook("sahih-bukhari", "Sahih Bukhari"),
        HadithBook("sahih-muslim", "Sahih Muslim"),
        HadithBook("al-tirmidhi", "Jami Tirmidhi"),
        HadithBook("abu-dawood", "Sunan Abu Dawud"),
        HadithBook("ibn-e-majah", "Sunan Ibn Majah"),
        HadithBook("sunan-nasai", "Sunan An-Nasa'i"),
        HadithBook("musnad-ahmad", "Musnad Ahmad"),
        HadithBook("muwatta-imam-malik", "Muwatta Malik")
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

        if (apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE") {
            _uiState.value = _uiState.value.copy(hasApiKey = true)
            loadHadiths()
        }
    }

    fun setApiKey(key: String) {
        apiKey = key
        _uiState.value = _uiState.value.copy(hasApiKey = key.isNotEmpty())
        if (key.isNotEmpty()) {
            loadHadiths()
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
