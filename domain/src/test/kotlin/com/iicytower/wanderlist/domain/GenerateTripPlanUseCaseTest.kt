package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GenerateTripPlanUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GenerateTripPlanUseCaseTest {

    private val tripListRepository = mockk<TripListRepository>()
    private val llmService = mockk<LlmService>()
    private val webSearchService = mockk<WebSearchService>()
    private val useCase = GenerateTripPlanUseCase(tripListRepository, llmService, webSearchService)

    private fun attraction(xid: String) = Attraction(
        xid = xid, name = "Attraction $xid", latitude = 50.0, longitude = 20.0,
        category = AttractionCategory.CASTLES_AND_FORTIFICATIONS,
        isInMyList = false, dateAddedToList = null,
        description = null, descriptionSources = emptyList(),
        isFromLastSearch = false, distanceKm = null
    )

    @Test
    fun `fails with controlled error when model keeps calling tools past the limit`() = runTest {
        every { tripListRepository.getAttractionsForList(1L) } returns flowOf(listOf(attraction("a1")))
        coEvery { llmService.completeChat(any(), any(), any()) } returns Result.success(
            listOf(LlmEvent.ToolCall(id = "call1", name = "web_search", arguments = mapOf("query" to "test")))
        )
        coEvery { webSearchService.search(any()) } returns Result.success("wynik wyszukiwania")

        val result = useCase(1L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Nie udało się wygenerować"))
        coVerify(exactly = AppConstants.MAX_TOOL_CALL_ITERATIONS) {
            llmService.completeChat(any(), any(), any())
        }
    }

    @Test
    fun `succeeds when model returns final json after a couple of tool calls`() = runTest {
        every { tripListRepository.getAttractionsForList(1L) } returns flowOf(listOf(attraction("a1")))
        coEvery { webSearchService.search(any()) } returns Result.success("wynik wyszukiwania")
        coEvery { tripListRepository.saveTripPlan(1L, any()) } returns Result.success(Unit)

        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(LlmEvent.ToolCall(id = "call1", name = "web_search", arguments = mapOf("query" to "test")))),
            Result.success(listOf(LlmEvent.TextChunk("""{"days":[{"label":"Dzien 1","points":[{"xid":"a1","name":"Attraction a1"}]}]}""")))
        )

        val result = useCase(1L)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().days.size)
    }
}
