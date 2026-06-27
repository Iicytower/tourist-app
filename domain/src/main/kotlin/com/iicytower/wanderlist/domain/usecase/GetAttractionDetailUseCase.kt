package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionRepository

class GetAttractionDetailUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(xid: String): Attraction? =
        attractionRepository.getByXid(xid)
}
