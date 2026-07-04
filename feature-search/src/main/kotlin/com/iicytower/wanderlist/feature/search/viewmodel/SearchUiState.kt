package com.iicytower.wanderlist.feature.search.viewmodel

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.GeocodeSuggestion
import com.iicytower.wanderlist.domain.model.Location

data class SearchUiState(
    val searchLocation: Location? = null,
    val searchLocationLabel: String = "",
    val locationQuery: String = "",
    val locationSuggestions: List<GeocodeSuggestion> = emptyList(),
    val showLocationPicker: Boolean = false,
    val radiusKm: Int = AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val selectedCategories: Set<AttractionCategory> = emptySet(),
    val results: List<Attraction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.BY_DISTANCE,
    val hasSearched: Boolean = false,
    val debugSourceStats: Map<String, Int> = emptyMap(),
    val isFiltering: Boolean = false,
    val filterRemovedCount: Int? = null
)

enum class SortOrder { BY_DISTANCE, BY_CATEGORY }
