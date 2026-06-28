package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import kotlinx.coroutines.flow.Flow

class GetMyListUseCase(
    private val attractionRepository: AttractionRepository
) {
    operator fun invoke(): Flow<List<Attraction>> = attractionRepository.getMyList()
}
