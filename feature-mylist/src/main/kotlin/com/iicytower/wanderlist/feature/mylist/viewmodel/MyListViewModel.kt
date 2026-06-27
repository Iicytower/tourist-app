package com.iicytower.wanderlist.feature.mylist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyListViewModel(
    private val getMyListUseCase: GetMyListUseCase,
    private val removeFromMyListUseCase: RemoveFromMyListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMyListUseCase().collect { list ->
                _uiState.update { state ->
                    state.copy(attractions = sortAttractions(list, state.sortOrder))
                }
            }
        }
    }

    fun setSortOrder(order: MyListSortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = order,
                attractions = sortAttractions(state.attractions, order)
            )
        }
    }

    fun requestDelete(xid: String) {
        _uiState.update { it.copy(confirmDeleteXid = xid) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(confirmDeleteXid = null) }
    }

    fun confirmDelete() {
        val xid = _uiState.value.confirmDeleteXid ?: return
        _uiState.update { it.copy(confirmDeleteXid = null) }
        viewModelScope.launch {
            removeFromMyListUseCase(xid).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun sortAttractions(list: List<Attraction>, order: MyListSortOrder): List<Attraction> =
        when (order) {
            MyListSortOrder.DATE_ADDED -> list.sortedByDescending { it.dateAddedToList }
            MyListSortOrder.DISTANCE -> list.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            MyListSortOrder.NAME -> list.sortedBy { it.name }
            MyListSortOrder.CATEGORY -> list.sortedBy { it.category.name }
        }
}
