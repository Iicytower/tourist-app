package com.iicytower.wanderlist.feature.detail.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction

data class AttractionDetailUiState(
    val attraction: Attraction? = null,
    val isLoading: Boolean = false,
    val isDescriptionLoading: Boolean = false,
    val error: String? = null,
    val descriptionError: String? = null,
    val showDistanceFromSearch: Boolean = false
)
