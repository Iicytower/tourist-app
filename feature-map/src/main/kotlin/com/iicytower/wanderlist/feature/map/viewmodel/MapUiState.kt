package com.iicytower.wanderlist.feature.map.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.RoutePoint

data class MapUiState(
    val searchResults: List<Attraction> = emptyList(),
    val myList: List<Attraction> = emptyList(),
    val showMyListOnly: Boolean = false,
    val selectedAttraction: Attraction? = null,
    val pinnedAttraction: Attraction? = null,
    val isLoading: Boolean = false,
    val initialCameraPosition: Triple<Double, Double, Double>? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val showRouteSheet: Boolean = false,
    val routeError: String? = null
)
