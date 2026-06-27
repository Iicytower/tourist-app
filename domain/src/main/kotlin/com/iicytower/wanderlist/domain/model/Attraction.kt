package com.iicytower.wanderlist.domain.model

import com.iicytower.wanderlist.core.model.AttractionCategory

data class Attraction(
    val xid: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: AttractionCategory,
    val isInMyList: Boolean,
    val dateAddedToList: Long?,
    val description: String?,
    val descriptionSources: List<DescriptionSource>,
    val isFromLastSearch: Boolean,
    val distanceKm: Double?
)
