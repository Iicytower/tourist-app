package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.AttractionRepository

class SearchAttractionsUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(params: SearchParams): Result<List<Attraction>> =
        attractionRepository.searchAttractions(params)
}
