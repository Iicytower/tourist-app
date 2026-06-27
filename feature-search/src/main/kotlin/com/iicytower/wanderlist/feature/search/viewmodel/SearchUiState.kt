package com.iicytower.wanderlist.feature.search.viewmodel

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.Location

data class SearchUiState(
    val searchLocation: Location? = null,
    val searchLocationLabel: String = "",
    val radiusKm: Int = AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val selectedCategories: Set<AttractionCategory> = emptySet(),
    val results: List<Attraction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.BY_DISTANCE,
    val hasSearched: Boolean = false
)

enum class SortOrder { BY_DISTANCE, BY_CATEGORY }
