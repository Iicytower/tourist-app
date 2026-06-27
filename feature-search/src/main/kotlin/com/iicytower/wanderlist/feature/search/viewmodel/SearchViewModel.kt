package com.iicytower.wanderlist.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.LocationService
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun setLocationFromGps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            locationService.getCurrentLocation().fold(
                onSuccess = { location ->
                    _uiState.update {
                        it.copy(
                            searchLocation = location,
                            searchLocationLabel = "Moja lokalizacja (%.4f, %.4f)".format(location.latitude, location.longitude),
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Błąd GPS") }
                }
            )
        }
    }

    fun setLocationFromCoordinates(lat: Double, lon: Double, label: String) {
        _uiState.update {
            it.copy(
                searchLocation = Location(lat, lon),
                searchLocationLabel = label,
                error = null
            )
        }
    }

    fun setRadius(radiusKm: Int) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
    }

    fun setCategories(categories: Set<AttractionCategory>) {
        _uiState.update { it.copy(selectedCategories = categories) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val sorted = sortResults(state.results, order)
            state.copy(sortOrder = order, results = sorted)
        }
    }

    fun search() {
        val location = _uiState.value.searchLocation ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val params = SearchParams(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = _uiState.value.radiusKm,
                categories = _uiState.value.selectedCategories
            )
            searchAttractionsUseCase(params).fold(
                onSuccess = { results ->
                    _uiState.update { state ->
                        state.copy(
                            results = sortResults(results, state.sortOrder),
                            isLoading = false,
                            hasSearched = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Błąd wyszukiwania", hasSearched = true) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun sortResults(results: List<com.iicytower.wanderlist.domain.model.Attraction>, order: SortOrder) = when (order) {
        SortOrder.BY_DISTANCE -> results.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        SortOrder.BY_CATEGORY -> results.sortedBy { it.category.name }
    }
}
