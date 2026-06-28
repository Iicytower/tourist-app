package com.iicytower.wanderlist.feature.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AttractionDetailViewModel(
    private val getAttractionDetailUseCase: GetAttractionDetailUseCase,
    private val generateDescriptionUseCase: GenerateDescriptionUseCase,
    private val addToMyListUseCase: AddToMyListUseCase,
    private val removeFromMyListUseCase: RemoveFromMyListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttractionDetailUiState())
    val uiState: StateFlow<AttractionDetailUiState> = _uiState.asStateFlow()

    fun load(xid: String, showDistance: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, showDistanceFromSearch = showDistance) }
            val attraction = getAttractionDetailUseCase(xid)
            if (attraction == null) {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono atrakcji") }
            } else {
                _uiState.update { it.copy(attraction = attraction, isLoading = false) }
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
                                attraction = state.attraction?.copy(
                                    description = description,
                                    descriptionSources = sources
                                ),
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

    fun toggleMyList() {
        val attraction = _uiState.value.attraction ?: return
        viewModelScope.launch {
            if (attraction.isInMyList) {
                removeFromMyListUseCase(attraction.xid).onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                _uiState.update { state ->
                    state.copy(attraction = state.attraction?.copy(isInMyList = false))
                }
            } else {
                addToMyListUseCase(attraction.xid).fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(attraction = state.attraction?.copy(isInMyList = true))
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message) }
                    }
                )
            }
        }
    }

    fun clearDescriptionError() {
        _uiState.update { it.copy(descriptionError = null) }
    }
}
