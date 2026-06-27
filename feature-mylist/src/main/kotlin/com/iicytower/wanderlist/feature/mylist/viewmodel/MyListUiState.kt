package com.iicytower.wanderlist.feature.mylist.viewmodel

import com.iicytower.wanderlist.domain.model.Attraction

enum class MyListSortOrder { DATE_ADDED, DISTANCE, NAME, CATEGORY }

data class MyListUiState(
    val attractions: List<Attraction> = emptyList(),
    val sortOrder: MyListSortOrder = MyListSortOrder.DATE_ADDED,
    val error: String? = null,
    val confirmDeleteXid: String? = null
)
