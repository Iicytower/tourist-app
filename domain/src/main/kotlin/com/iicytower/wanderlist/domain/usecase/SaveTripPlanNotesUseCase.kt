package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class SaveTripPlanNotesUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(listId: Long, notes: String?): Result<Unit> =
        repo.updateTripPlanNotes(listId, notes)
}
