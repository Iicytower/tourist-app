package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.TripPlan
import com.iicytower.wanderlist.domain.repository.TripListRepository

class GetTripPlanUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(listId: Long): Pair<TripPlan?, String?> = repo.getTripPlan(listId)
}
