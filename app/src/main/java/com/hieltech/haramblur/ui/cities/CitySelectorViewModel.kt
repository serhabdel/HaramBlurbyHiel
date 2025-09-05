package com.hieltech.haramblur.ui.cities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.cities.CitiesRepository
import com.hieltech.haramblur.data.cities.CitySearchConfig
import com.hieltech.haramblur.data.cities.CitySearchResult
import com.hieltech.haramblur.data.cities.CitySearchState
import com.hieltech.haramblur.data.cities.toSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CitySelectorViewModel @Inject constructor(
    private val repository: CitiesRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _selectionEvents = MutableSharedFlow<SelectedCityEvent>(replay = 0)
    val selectionEvents = _selectionEvents.asSharedFlow()

    val uiState: StateFlow<CitySearchState> = queryFlow
        .debounce(CitySearchConfig.DEBOUNCE_MS)
        .flatMapLatest { query ->
            // Small wrapper flow to map to state while indicating loading/empty
            kotlinx.coroutines.flow.flow {
                if (query.length < CitySearchConfig.MIN_QUERY_LENGTH) {
                    emit(CitySearchState.Empty)
                } else {
                    emit(CitySearchState.Loading)
                    val results = repository.search(query)
                    if (results.isEmpty()) {
                        emit(CitySearchState.Error("No results. Check connection or try a different query."))
                    } else {
                        emit(CitySearchState.Success(results))
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CitySearchState.Empty
        )

    fun onQueryChange(newQuery: String) {
        viewModelScope.launch {
            queryFlow.emit(newQuery)
        }
    }

    fun selectCity(result: CitySearchResult) {
        viewModelScope.launch {
            _selectionEvents.emit(SelectedCityEvent(result))
        }
    }

    data class SelectedCityEvent(val result: CitySearchResult)
}
