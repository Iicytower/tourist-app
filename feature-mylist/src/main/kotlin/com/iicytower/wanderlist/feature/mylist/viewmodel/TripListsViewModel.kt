package com.iicytower.wanderlist.feature.mylist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.DeleteTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripListsViewModel(
    private val getTripListsUseCase: GetTripListsUseCase,
    private val createTripListUseCase: CreateTripListUseCase,
    private val deleteTripListUseCase: DeleteTripListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListsUiState())
    val uiState: StateFlow<TripListsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTripListsUseCase().collect { lists ->
                _uiState.update { it.copy(lists = lists) }
            }
        }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun createList(name: String) {
        viewModelScope.launch {
            createTripListUseCase(name).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun requestDelete(id: Long) = _uiState.update { it.copy(confirmDeleteId = id) }
    fun cancelDelete() = _uiState.update { it.copy(confirmDeleteId = null) }

    fun confirmDelete() {
        val id = _uiState.value.confirmDeleteId ?: return
        _uiState.update { it.copy(confirmDeleteId = null) }
        viewModelScope.launch {
            deleteTripListUseCase(id).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
