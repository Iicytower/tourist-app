package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.Attraction

interface AttractionQualityFilter {
    suspend fun filter(attractions: List<Attraction>): Result<FilterResult>
}

data class FilterResult(
    val kept: List<Attraction>,
    val removed: List<RemovedAttraction>
)

data class RemovedAttraction(
    val name: String,
    val reason: String
)
