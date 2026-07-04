package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class DeleteTripPlanUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(listId: Long): Result<Unit> = repo.deleteTripPlan(listId)
}
