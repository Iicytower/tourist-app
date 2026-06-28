package com.iicytower.wanderlist.feature.mylist.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.TripList

data class TripListDetailUiState(
    val tripList: TripList? = null,
    val attractions: List<Attraction> = emptyList(),
    val error: String? = null,
    val confirmRemoveXid: String? = null
)
