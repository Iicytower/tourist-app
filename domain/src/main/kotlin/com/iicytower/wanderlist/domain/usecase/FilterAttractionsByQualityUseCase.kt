package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionQualityFilter
import com.iicytower.wanderlist.domain.repository.FilterResult

class FilterAttractionsByQualityUseCase(
    private val filter: AttractionQualityFilter
) {
    suspend operator fun invoke(attractions: List<Attraction>): Result<FilterResult> =
        filter.filter(attractions)
}
