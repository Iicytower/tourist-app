package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.repository.TripListRepository

class CreateTripListUseCase(private val repo: TripListRepository) {
    suspend operator fun invoke(name: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Nazwa listy nie może być pusta"))
        return repo.createList(trimmed)
    }
}
