package com.iicytower.wanderlist.feature.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val getMyListUseCase: GetMyListUseCase,
    private val attractionRepository: AttractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onMapReady() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lastSearch = attractionRepository.getLastSearchResults()
            _uiState.update { it.copy(searchResults = lastSearch, isLoading = false) }
        }
        viewModelScope.launch {
            getMyListUseCase().collect { myList ->
                _uiState.update { it.copy(myList = myList) }
            }
        }
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
