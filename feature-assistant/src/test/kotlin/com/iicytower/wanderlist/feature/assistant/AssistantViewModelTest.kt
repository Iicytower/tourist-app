package com.iicytower.wanderlist.feature.assistant

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.usecase.SendChatMessageUseCase
import com.iicytower.wanderlist.feature.assistant.viewmodel.AssistantViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sendChatMessageUseCase = mockk<SendChatMessageUseCase>()
    private val searchAttractionsUseCase = mockk<SearchAttractionsUseCase>()
    private val getMyListUseCase = mockk<GetMyListUseCase>()
    private val webSearchService = mockk<WebSearchService>()
    private val settingsRepository = mockk<SettingsRepository>()
    private lateinit var viewModel: AssistantViewModel

    private val fakeSettings = AppSettings(
        openRouterApiKey = "key",
        tavilyApiKey = "tkey",
        aiModel = "model",
        defaultRadiusKm = 10,
        descriptionLanguage = "pl",
        userInterests = emptySet(),
        systemPromptDescription = "desc prompt",
        systemPromptAssistant = "assistant prompt",
        tavilyUsageCount = 0,
        tavilyUsageMonth = "2026-06"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.getSettings() } returns flowOf(fakeSettings)
        every { getMyListUseCase() } returns flowOf(emptyList())
        viewModel = AssistantViewModel(
            sendChatMessageUseCase,
            searchAttractionsUseCase,
            getMyListUseCase,
            webSearchService,
            settingsRepository
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun mockLlmFlow(vararg events: LlmEvent) {
        every { sendChatMessageUseCase(any(), any(), any()) } returns flowOf(*events)
    }

    @Test
    fun sendMessage_addsUserMessageToList() = runTest {
        mockLlmFlow(LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        val messages = viewModel.uiState.value.messages
        assertTrue(messages.any { it is ChatMessage.User && (it as ChatMessage.User).text == "Hello" })
    }

    @Test
    fun sendMessage_clearsInputAfterSend() = runTest {
        mockLlmFlow(LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        assertEquals("", viewModel.uiState.value.currentInput)
    }

    @Test
    fun sendMessage_isProcessingTrueWhileRunning_thenFalse() = runTest {
        mockLlmFlow(LlmEvent.TextChunk("Hi"), LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun textChunks_accumulateInStreamingText_thenMoveToMessages() = runTest {
        mockLlmFlow(
            LlmEvent.TextChunk("Hello"),
            LlmEvent.TextChunk(" world"),
            LlmEvent.Done
        )
        viewModel.updateInput("Hi")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        val assistantMsgs = viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Assistant>()
        assertEquals(1, assistantMsgs.size)
        assertEquals("Hello world", assistantMsgs.first().text)
        assertEquals("", viewModel.uiState.value.streamingText)
    }

    @Test
    fun llmError_addsErrorMessage() = runTest {
        mockLlmFlow(LlmEvent.Error("connection failed"))
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        val errorMsgs = viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Error>()
        assertEquals(1, errorMsgs.size)
        assertEquals("connection failed", errorMsgs.first().message)
    }

    @Test
    fun toolCall_webSearch_callsWebSearchService() = runTest {
        coEvery { webSearchService.search(any()) } returns Result.success("results")
        every { sendChatMessageUseCase(any(), any(), any()) } returnsMany listOf(
            flowOf(
                LlmEvent.ToolCall("id1", "web_search", mapOf("query" to "Krakow")),
                LlmEvent.Done
            ),
            flowOf(LlmEvent.TextChunk("Found it"), LlmEvent.Done)
        )
        viewModel.updateInput("Search Krakow")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { webSearchService.search("Krakow") }
    }

    @Test
    fun toolCall_getMyList_callsGetMyListUseCase() = runTest {
        every { sendChatMessageUseCase(any(), any(), any()) } returnsMany listOf(
            flowOf(
                LlmEvent.ToolCall("id1", "get_my_list", emptyMap()),
                LlmEvent.Done
            ),
            flowOf(LlmEvent.TextChunk("List retrieved"), LlmEvent.Done)
        )
        viewModel.updateInput("Show my list")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(atLeast = 1) { getMyListUseCase() }
    }

    @Test
    fun clearChat_emptiesMessages() = runTest {
        mockLlmFlow(LlmEvent.TextChunk("Hi"), LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmClearChat()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun clearChat_showsConfirmationDialog() = runTest {
        viewModel.clearChat()
        assertTrue(viewModel.uiState.value.showClearConfirmation)
    }

    @Test
    fun dismissClearConfirmation_hidesDialog() = runTest {
        viewModel.clearChat()
        viewModel.dismissClearConfirmation()
        assertFalse(viewModel.uiState.value.showClearConfirmation)
    }
}
