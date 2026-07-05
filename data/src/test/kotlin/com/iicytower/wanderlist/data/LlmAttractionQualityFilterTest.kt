package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.remote.llmfilter.LlmAttractionQualityFilter
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.WebSearchService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LlmAttractionQualityFilterTest {

    private val llmService = mockk<LlmService>()
    private val webSearchService = mockk<WebSearchService>()
    private val filter = LlmAttractionQualityFilter(llmService, webSearchService)

    private fun attraction(xid: String) = Attraction(
        xid = xid, name = "Attraction $xid", latitude = 50.0, longitude = 20.0,
        category = AttractionCategory.CASTLES_AND_FORTIFICATIONS,
        isInMyList = false, dateAddedToList = null,
        description = null, descriptionSources = emptyList(),
        isFromLastSearch = false, distanceKm = null
    )

    @Test
    fun `fails open with all attractions kept when model keeps calling tools past the limit`() = runTest {
        val attractions = listOf(attraction("a1"), attraction("a2"))
        coEvery { llmService.completeChat(any(), any(), any()) } returns Result.success(
            listOf(LlmEvent.ToolCall(id = "call1", name = "web_search", arguments = mapOf("query" to "test")))
        )
        coEvery { webSearchService.search(any()) } returns Result.success("wynik wyszukiwania")

        val result = filter.filter(attractions)

        assertTrue(result.isSuccess)
        assertEquals(attractions, result.getOrThrow().kept)
        assertTrue(result.getOrThrow().removed.isEmpty())
        coVerify(exactly = AppConstants.MAX_TOOL_CALL_ITERATIONS) {
            llmService.completeChat(any(), any(), any())
        }
    }

    @Test
    fun `keeps only listed attractions when model returns final json after a tool call`() = runTest {
        val attractions = listOf(attraction("a1"), attraction("a2"))
        coEvery { webSearchService.search(any()) } returns Result.success("wynik wyszukiwania")
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(LlmEvent.ToolCall(id = "call1", name = "web_search", arguments = mapOf("query" to "test")))),
            Result.success(listOf(LlmEvent.TextChunk("""{"keep":[1],"removed":[{"index":2,"reason":"nieturystyczne"}]}""")))
        )

        val result = filter.filter(attractions)

        assertTrue(result.isSuccess)
        assertEquals(listOf(attractions[0]), result.getOrThrow().kept)
        assertEquals(1, result.getOrThrow().removed.size)
    }
}
