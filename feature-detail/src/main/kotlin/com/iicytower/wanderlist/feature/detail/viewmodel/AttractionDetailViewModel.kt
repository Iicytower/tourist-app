package com.iicytower.wanderlist.feature.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AttractionDetailViewModel(
    private val getAttractionDetailUseCase: GetAttractionDetailUseCase,
    private val generateDescriptionUseCase: GenerateDescriptionUseCase,
    private val getTripListsUseCase: GetTripListsUseCase,
    private val addToTripListUseCase: AddToTripListUseCase,
    private val removeFromTripListUseCase: RemoveFromTripListUseCase,
    private val createTripListUseCase: CreateTripListUseCase,
    private val tripListRepository: TripListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttractionDetailUiState())
    val uiState: StateFlow<AttractionDetailUiState> = _uiState.asStateFlow()

    fun load(xid: String, showDistance: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, showDistanceFromSearch = showDistance) }
            val attraction = getAttractionDetailUseCase(xid)
            if (attraction == null) {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono atrakcji") }
                return@launch
            }
            _uiState.update { it.copy(attraction = attraction, isLoading = false) }

            combine(
                getTripListsUseCase(),
                tripListRepository.getListsForAttraction(xid)
            ) { allLists, attractionLists ->
                val ids = attractionLists.map { it.id }.toSet()
                Pair(allLists, ids)
            }.collect { (allLists, ids) ->
                _uiState.update { state ->
                    state.copy(
                        tripLists = allLists,
                        attractionListIds = ids,
                        attraction = state.attraction?.copy(isInMyList = ids.isNotEmpty())
                    )
                }
            }
        }
    }

    fun loadDescription(force: Boolean = false) {
        val xid = _uiState.value.attraction?.xid ?: return
        if (!force && _uiState.value.attraction?.description != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDescriptionLoading = true, descriptionError = null) }
            try {
                generateDescriptionUseCase(xid).fold(
                    onSuccess = { (description, sources) ->
                        Timber.tag("Description").d("Generated %d chars for xid=%s", description.length, xid)
                        _uiState.update { state ->
                            state.copy(
                                attraction = state.attraction?.copy(description = description, descriptionSources = sources),
                                isDescriptionLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        Timber.tag("Description").e(e, "generateDescription failed for xid=%s", xid)
                        _uiState.update { it.copy(isDescriptionLoading = false, descriptionError = e.message ?: "Błąd generowania opisu") }
                    }
                )
            } catch (e: Exception) {
                Timber.tag("Description").e(e, "generateDescription threw for xid=%s", xid)
                _uiState.update { it.copy(isDescriptionLoading = false, descriptionError = e.message ?: "Błąd generowania opisu") }
            }
        }
    }

    fun openListSheet() = _uiState.update { it.copy(showListSheet = true) }
    fun closeListSheet() = _uiState.update { it.copy(showListSheet = false) }

    fun toggleList(listId: Long) {
        val xid = _uiState.value.attraction?.xid ?: return
        val isInList = listId in _uiState.value.attractionListIds
        viewModelScope.launch {
            if (isInList) {
                removeFromTripListUseCase(xid, listId).onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } else {
                addToTripListUseCase(xid, listId).onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun createAndAddToList(name: String) {
        val xid = _uiState.value.attraction?.xid ?: return
        viewModelScope.launch {
            createTripListUseCase(name).fold(
                onSuccess = { listId ->
                    addToTripListUseCase(xid, listId).onFailure { e ->
                        _uiState.update { it.copy(error = e.message) }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun clearDescriptionError() {
        _uiState.update { it.copy(descriptionError = null) }
    }
}
