package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class DeleteTripListUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(listId: Long): Result<Unit> = repo.deleteList(listId)
}
