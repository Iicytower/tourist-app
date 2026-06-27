package com.iicytower.wanderlist.domain.model

import com.iicytower.wanderlist.core.model.AttractionCategory

data class SearchParams(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Int,
    val categories: Set<AttractionCategory>
)
