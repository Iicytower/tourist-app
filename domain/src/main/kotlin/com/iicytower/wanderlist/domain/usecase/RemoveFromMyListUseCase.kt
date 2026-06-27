package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.AttractionRepository

class RemoveFromMyListUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(xid: String): Result<Unit> =
        attractionRepository.removeFromMyList(xid)
}
