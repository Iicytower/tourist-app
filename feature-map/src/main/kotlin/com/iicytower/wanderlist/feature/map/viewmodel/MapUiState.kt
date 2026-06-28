package com.iicytower.wanderlist.feature.map.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.Location

data class MapUiState(
    val searchResults: List<Attraction> = emptyList(),
    val myList: List<Attraction> = emptyList(),
    val showMyListOnly: Boolean = false,
    val searchCenterLocation: Location? = null,
    val selectedAttraction: Attraction? = null,
    val userLocation: Location? = null,
    val isLoading: Boolean = false,
    val initialCameraPosition: Triple<Double, Double, Double>? = null
)
