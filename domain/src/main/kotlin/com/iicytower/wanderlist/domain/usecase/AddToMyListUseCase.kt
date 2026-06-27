package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import kotlinx.coroutines.flow.first

class AddToMyListUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(xid: String): Result<Unit> {
        val currentSize = attractionRepository.getMyList().first().size
        if (currentSize >= AppConstants.MY_LIST_MAX_SIZE) {
            return Result.failure(
                IllegalStateException("Twoja lista jest pełna (${AppConstants.MY_LIST_MAX_SIZE}/${AppConstants.MY_LIST_MAX_SIZE}). Usuń miejsce, żeby dodać nowe.")
            )
        }
        return attractionRepository.addToMyList(xid)
    }
}
