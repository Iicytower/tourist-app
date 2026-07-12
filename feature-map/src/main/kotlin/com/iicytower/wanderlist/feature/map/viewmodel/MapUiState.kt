package com.iicytower.wanderlist.feature.map.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction

data class MapUiState(
    val searchResults: List<Attraction> = emptyList(),
    val myList: List<Attraction> = emptyList(),
    val showMyListOnly: Boolean = false,
    val selectedAttraction: Attraction? = null,
    val pinnedAttraction: Attraction? = null,
    val isLoading: Boolean = false,
    val initialCameraPosition: Triple<Double, Double, Double>? = null
)
