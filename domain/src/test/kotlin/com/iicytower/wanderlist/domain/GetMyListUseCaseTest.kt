package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetMyListUseCaseTest {

    private val repository = mockk<AttractionRepository>()
    private val useCase = GetMyListUseCase(repository)

    @Test
    fun `delegates to repository`() = runTest {
        every { repository.getMyList() } returns flowOf(emptyList())

        useCase()

        verify(exactly = 1) { repository.getMyList() }
    }

    @Test
    fun `emits list from repository`() = runTest {
        val attraction = Attraction(
            xid = "a1", name = "Zamek", latitude = 50.0, longitude = 20.0,
            category = AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            isInMyList = true, dateAddedToList = 1000L,
            description = null, descriptionSources = emptyList(),
            isFromLastSearch = false, distanceKm = null
        )
        every { repository.getMyList() } returns flowOf(listOf(attraction))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("Zamek", result.first().name)
    }
}
