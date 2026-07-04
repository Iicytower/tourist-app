package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class AddToTripListUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(xid: String, listId: Long): Result<Unit> = repo.addToList(xid, listId)
}
