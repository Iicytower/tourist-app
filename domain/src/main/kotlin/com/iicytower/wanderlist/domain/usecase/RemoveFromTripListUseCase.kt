package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class RemoveFromTripListUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(xid: String, listId: Long): Result<Unit> = repo.removeFromList(xid, listId)
}
