package com.iicytower.wanderlist.feature.mylist.viewmodel

import com.iicytower.wanderlist.domain.model.TripPlan

data class TripPlanUiState(
    val plan: TripPlan? = null,
    val notes: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false
)
