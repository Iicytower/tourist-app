package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.TripPlan
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore

class RevertTripPlanUseCase(
    private val tripListRepository: TripListRepository,
    private val revertStore: TripPlanRevertStore
) {
    suspend operator fun invoke(listId: Long): Result<TripPlan> = runCatching {
        val previous = revertStore.consume(listId)
            ?: error("Brak poprzedniej wersji planu do przywrócenia")
        tripListRepository.saveTripPlan(listId, previous).getOrThrow()
        previous
    }
}
