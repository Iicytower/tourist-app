package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.totalDistanceKm
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.PlanRouteUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanRouteUseCaseTest {

    private val repository = mockk<AttractionRepository>()
    private val useCase = PlanRouteUseCase(repository)

    private fun attraction(xid: String, lat: Double, lon: Double) = Attraction(
        xid = xid, name = xid, latitude = lat, longitude = lon,
        category = AttractionCategory.MUSEUMS_AND_GALLERIES,
        isInMyList = true, dateAddedToList = null, description = null,
        descriptionSources = emptyList(), isFromLastSearch = false, distanceKm = null
    )

    @Test
    fun `orders points greedily by nearest neighbor`() = runTest {
        // a (start) -> najbliżej b -> potem c
        val a = attraction("a", 50.0, 20.0)
        val b = attraction("b", 50.01, 20.0)
        val c = attraction("c", 50.5, 20.0)
        every { repository.getMyList() } returns flowOf(listOf(a, c, b))

        val route = useCase().getOrThrow()

        assertEquals(listOf("a", "b", "c"), route.map { it.attraction.xid })
        assertEquals(listOf(1, 2, 3), route.map { it.order })
    }

    @Test
    fun `fails with less than two attractions`() = runTest {
        every { repository.getMyList() } returns flowOf(listOf(attraction("a", 50.0, 20.0)))
        assertTrue(useCase().isFailure)
    }

    @Test
    fun `total distance sums consecutive segments`() = runTest {
        val a = attraction("a", 50.0, 20.0)
        val b = attraction("b", 51.0, 20.0)
        every { repository.getMyList() } returns flowOf(listOf(a, b))

        val route = useCase().getOrThrow()

        // ~111 km na stopień szerokości geograficznej
        assertEquals(111.0, route.totalDistanceKm(), 1.0)
    }
}
