package com.iicytower.wanderlist.feature.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class MapViewModel(
    private val getMyListUseCase: GetMyListUseCase,
    private val attractionRepository: AttractionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onMapReady() {
        viewModelScope.launch {
            val savedPosition = settingsRepository.getLastMapPosition()
            val lastSearch = attractionRepository.getLastSearchResults()
            val initialPosition = when {
                savedPosition != null -> savedPosition
                lastSearch.isNotEmpty() -> Triple(
                    lastSearch.map { it.latitude }.average(),
                    lastSearch.map { it.longitude }.average(),
                    12.0
                )
                else -> Triple(50.06, 19.94, 11.0)
            }
            Timber.tag("MAP").d("camera: saved=%s results=%d pos=[%.4f,%.4f,z=%.1f]",
                savedPosition, lastSearch.size, initialPosition.first, initialPosition.second, initialPosition.third)
            _uiState.update { it.copy(initialCameraPosition = initialPosition, searchResults = lastSearch, isLoading = false) }
        }
        viewModelScope.launch {
            getMyListUseCase().collect { myList ->
                _uiState.update { it.copy(myList = myList) }
            }
        }
    }

    fun saveMapPosition(lat: Double, lon: Double, zoom: Double) {
        viewModelScope.launch { settingsRepository.updateLastMapPosition(lat, lon, zoom) }
    }

    fun toggleMyListMode() {
        _uiState.update { it.copy(showMyListOnly = !it.showMyListOnly, selectedAttraction = null) }
    }

    fun selectAttraction(xid: String?) {
        val attraction = if (xid == null) null else {
            val all = _uiState.value.searchResults + _uiState.value.myList
            all.find { it.xid == xid }
        }
        _uiState.update { it.copy(selectedAttraction = attraction) }
    }
}
