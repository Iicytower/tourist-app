package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.TripListRepository
import kotlinx.coroutines.flow.Flow

class GetAttractionsForListUseCase(private val repo: TripListRepository) {
    operator fun invoke(listId: Long): Flow<List<Attraction>> = repo.getAttractionsForList(listId)
}
