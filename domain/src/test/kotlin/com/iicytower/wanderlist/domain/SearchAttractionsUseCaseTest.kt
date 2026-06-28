package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SearchAttractionsUseCaseTest {

    private val repository = mockk<AttractionRepository>()
    private val useCase = SearchAttractionsUseCase(repository)

    private val params = SearchParams(50.0, 20.0, 10, emptySet())

    @Test
    fun `delegates to repository with correct params`() = runTest {
        coEvery { repository.searchAttractions(params) } returns Result.success(emptyList())

        useCase(params)

        coVerify(exactly = 1) { repository.searchAttractions(params) }
    }

    @Test
    fun `propagates success from repository`() = runTest {
        val attraction = Attraction(
            xid = "test", name = "Test", latitude = 50.0, longitude = 20.0,
            category = AttractionCategory.MUSEUMS_AND_GALLERIES,
            isInMyList = false, dateAddedToList = null,
            description = null, descriptionSources = emptyList(),
            isFromLastSearch = true, distanceKm = 1.5
        )
        coEvery { repository.searchAttractions(params) } returns Result.success(listOf(attraction))

        val result = useCase(params)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        coEvery { repository.searchAttractions(params) } returns Result.failure(RuntimeException("Błąd sieci"))

        val result = useCase(params)

        assertTrue(result.isFailure)
        assertEquals("Błąd sieci", result.exceptionOrNull()!!.message)
    }
}
