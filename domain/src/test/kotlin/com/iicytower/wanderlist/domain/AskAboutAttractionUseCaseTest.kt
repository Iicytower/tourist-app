package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.WikipediaResult
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.repository.WikipediaService
import com.iicytower.wanderlist.domain.usecase.AskAboutAttractionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AskAboutAttractionUseCaseTest {

    private val llmService = mockk<LlmService>()
    private val webSearchService = mockk<WebSearchService>()
    private val wikipediaService = mockk<WikipediaService>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val useCase = AskAboutAttractionUseCase(llmService, webSearchService, wikipediaService, settingsRepository)

    private val attraction = Attraction(
        xid = "n1", name = "Wawel", latitude = 50.05, longitude = 19.93,
        category = AttractionCategory.CASTLES_AND_FORTIFICATIONS,
        isInMyList = false, dateAddedToList = null, description = null,
        descriptionSources = emptyList(), isFromLastSearch = true, distanceKm = 1.0
    )

    private val settings = AppSettings(
        openRouterApiKey = "key", tavilyApiKey = "tkey", aiModel = "model",
        defaultRadiusKm = 10, appLanguage = "pl", userInterests = emptySet(),
        systemPromptDescription = "d", systemPromptAssistant = "a",
        tavilyUsageCount = 0, tavilyUsageMonth = "2026-07"
    )

    @Before
    fun setup() {
        every { settingsRepository.getSettings() } returns flowOf(settings)
    }

    @Test
    fun `returns final answer after tool calls`() = runTest {
        coEvery { webSearchService.search(any()) } returns Result.success("Bilet 35 zl")
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "web_search", mapOf("query" to "Wawel bilety")),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Bilet kosztuje 35 zł."), LlmEvent.Done))
        )

        val result = useCase(attraction, "ile kosztuje bilet?")

        assertEquals("Bilet kosztuje 35 zł.", result.getOrThrow())
        coVerify(exactly = 1) { webSearchService.search("Wawel bilety") }
    }

    @Test
    fun `wikipedia tool is executed`() = runTest {
        coEvery { wikipediaService.getArticle("Wawel") } returns Result.success(
            WikipediaResult(extract = "Zamek królewski", url = "https://pl.wikipedia.org/wiki/Wawel")
        )
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "wikipedia", mapOf("title" to "Wawel")),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("To zamek królewski."), LlmEvent.Done))
        )

        val result = useCase(attraction, "co to jest?")

        assertEquals("To zamek królewski.", result.getOrThrow())
        coVerify(exactly = 1) { wikipediaService.getArticle("Wawel") }
    }

    @Test
    fun `fails after exceeding tool call iteration limit`() = runTest {
        coEvery { webSearchService.search(any()) } returns Result.success("wynik")
        coEvery { llmService.completeChat(any(), any(), any()) } returns Result.success(listOf(
            LlmEvent.ToolCall("id1", "web_search", mapOf("query" to "q")),
            LlmEvent.Done
        ))

        val result = useCase(attraction, "pytanie")

        assertTrue(result.isFailure)
    }

    @Test
    fun `llm error becomes failure`() = runTest {
        coEvery { llmService.completeChat(any(), any(), any()) } returns Result.success(listOf(
            LlmEvent.Error("boom")
        ))

        val result = useCase(attraction, "pytanie")

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }
}
