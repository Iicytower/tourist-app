package com.iicytower.wanderlist.feature.mylist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripListDetailViewModel(
    private val getTripListsUseCase: GetTripListsUseCase,
    private val getAttractionsForListUseCase: GetAttractionsForListUseCase,
    private val removeFromTripListUseCase: RemoveFromTripListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListDetailUiState())
    val uiState: StateFlow<TripListDetailUiState> = _uiState.asStateFlow()

    fun load(listId: Long) {
        viewModelScope.launch {
            combine(
                getTripListsUseCase(),
                getAttractionsForListUseCase(listId)
            ) { lists, attractions ->
                val tripList = lists.find { it.id == listId }
                Pair(tripList, attractions)
            }.collect { (tripList, attractions) ->
                _uiState.update { it.copy(tripList = tripList, attractions = attractions) }
            }
        }
    }

    fun requestRemove(xid: String) = _uiState.update { it.copy(confirmRemoveXid = xid) }
    fun cancelRemove() = _uiState.update { it.copy(confirmRemoveXid = null) }

    fun confirmRemove() {
        val xid = _uiState.value.confirmRemoveXid ?: return
        val listId = _uiState.value.tripList?.id ?: return
        _uiState.update { it.copy(confirmRemoveXid = null) }
        viewModelScope.launch {
            removeFromTripListUseCase(xid, listId).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
