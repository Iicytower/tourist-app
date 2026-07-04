package com.iicytower.wanderlist.feature.mylist.viewmodel

import com.iicytower.wanderlist.domain.model.TripList

data class TripListsUiState(
    val lists: List<TripList> = emptyList(),
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val confirmDeleteId: Long? = null
)
