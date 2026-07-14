package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.AttractionRepository

class AddToMyListUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(xid: String): Result<Unit> =
        attractionRepository.addToMyList(xid)
}
