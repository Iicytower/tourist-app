package com.iicytower.wanderlist.feature.mylist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore
import com.iicytower.wanderlist.domain.usecase.DeleteTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.RevertTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.SaveTripPlanNotesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripPlanViewModel(
    private val getTripPlanUseCase: GetTripPlanUseCase,
    private val saveTripPlanNotesUseCase: SaveTripPlanNotesUseCase,
    private val deleteTripPlanUseCase: DeleteTripPlanUseCase,
    private val revertTripPlanUseCase: RevertTripPlanUseCase,
    private val tripPlanRevertStore: TripPlanRevertStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripPlanUiState())
    val uiState: StateFlow<TripPlanUiState> = _uiState.asStateFlow()

    private var listId: Long = -1

    init {
        viewModelScope.launch {
            tripPlanRevertStore.previousPlans.collect { plans ->
                _uiState.update { it.copy(canRevert = plans.containsKey(listId)) }
            }
        }
    }

    fun load(listId: Long) {
        this.listId = listId
        viewModelScope.launch {
            val (plan, notes) = getTripPlanUseCase(listId)
            _uiState.update { it.copy(
                plan = plan,
                notes = notes ?: "",
                isLoading = false,
                canRevert = tripPlanRevertStore.previousPlans.value.containsKey(listId)
            ) }
        }
    }

    fun revertPlan() {
        viewModelScope.launch {
            revertTripPlanUseCase(listId).fold(
                onSuccess = { previous -> _uiState.update { it.copy(plan = previous) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun updateNotes(text: String) = _uiState.update { it.copy(notes = text) }

    fun saveNotes() {
        val notes = _uiState.value.notes.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            saveTripPlanNotesUseCase(listId, notes).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirmation = true) }
    fun cancelDelete() = _uiState.update { it.copy(showDeleteConfirmation = false) }

    fun confirmDelete(onDeleted: () -> Unit) {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            deleteTripPlanUseCase(listId).fold(
                onSuccess = { onDeleted() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
