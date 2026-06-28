package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.domain.repository.TripListRepository
import kotlinx.coroutines.flow.Flow

class GetTripListsUseCase(private val repo: TripListRepository) {
    operator fun invoke(): Flow<List<TripList>> = repo.getLists()
}
