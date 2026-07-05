package com.iicytower.wanderlist.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.GeocodeSuggestion
import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.GeocoderService
import com.iicytower.wanderlist.domain.repository.LocationService
import com.iicytower.wanderlist.domain.usecase.FilterAttractionsByQualityUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchViewModel(
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val filterAttractionsByQualityUseCase: FilterAttractionsByQualityUseCase,
    private val locationService: LocationService,
    private val geocoderService: GeocoderService,
    private val attractionRepository: AttractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null

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

    fun updateLocationQuery(query: String) {
        _uiState.update { it.copy(locationQuery = query, locationSuggestions = if (query.length < 3) emptyList() else it.locationSuggestions) }
        suggestJob?.cancel()
        if (query.length < 3) return
        suggestJob = viewModelScope.launch {
            delay(400)
            Timber.tag("SearchVM").d("calling suggest for '%s'", query)
            geocoderService.suggest(query)
                .onSuccess { suggestions ->
                    Timber.tag("SearchVM").d("suggest → %d suggestions", suggestions.size)
                    _uiState.update { it.copy(locationSuggestions = suggestions) }
                }
                .onFailure { Timber.tag("SearchVM").e(it, "suggest error") }
        }
    }

    fun selectSuggestion(suggestion: GeocodeSuggestion) {
        _uiState.update {
            it.copy(
                searchLocation = Location(suggestion.lat, suggestion.lon),
                searchLocationLabel = suggestion.displayName,
                locationQuery = suggestion.displayName,
                locationSuggestions = emptyList()
            )
        }
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(locationSuggestions = emptyList()) }
    }

    fun openLocationPicker() {
        _uiState.update { it.copy(showLocationPicker = true) }
    }

    fun dismissLocationPicker() {
        _uiState.update { it.copy(showLocationPicker = false) }
    }

    fun onLocationPicked(lat: Double, lon: Double) {
        _uiState.update { it.copy(showLocationPicker = false) }
        viewModelScope.launch {
            val label = geocoderService.reverseGeocode(lat, lon)
                .getOrDefault("%.4f, %.4f".format(lat, lon))
            _uiState.update {
                it.copy(
                    searchLocation = Location(lat, lon),
                    searchLocationLabel = label
                )
            }
        }
    }

    fun searchLocationByName(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            geocoderService.geocode(query).fold(
                onSuccess = { (location, displayName) ->
                    _uiState.update {
                        it.copy(
                            searchLocation = location,
                            searchLocationLabel = displayName,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Nie znaleziono miejsca") }
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
                    val stats = attractionRepository.getLastSearchStats()
                    Timber.tag("SearchVM").d("Wyniki per źródło: %s", stats)
                    _uiState.update { state ->
                        state.copy(
                            results = sortResults(results, state.sortOrder),
                            isLoading = false,
                            hasSearched = true,
                            debugSourceStats = stats
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Błąd wyszukiwania", hasSearched = true) }
                }
            )
        }
    }

    fun filterByQuality() {
        val attractions = _uiState.value.results
        if (attractions.isEmpty() || _uiState.value.isFiltering) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true, error = null, filterRemovedCount = null) }
            filterAttractionsByQualityUseCase(attractions).fold(
                onSuccess = { result ->
                    _uiState.update { state ->
                        state.copy(
                            results = sortResults(result.kept, state.sortOrder),
                            isFiltering = false,
                            filterRemovedCount = result.removed.size
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isFiltering = false, error = e.message ?: "Błąd filtrowania") }
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
