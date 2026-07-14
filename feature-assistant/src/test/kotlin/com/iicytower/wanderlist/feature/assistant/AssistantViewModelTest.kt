package com.iicytower.wanderlist.feature.assistant

import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.domain.model.TripPlan
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore
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
    private val llmService = mockk<LlmService>()
    private val searchAttractionsUseCase = mockk<SearchAttractionsUseCase>()
    private val getTripListsUseCase = mockk<GetTripListsUseCase>()
    private val getAttractionsForListUseCase = mockk<GetAttractionsForListUseCase>()
    private val addToTripListUseCase = mockk<AddToTripListUseCase>()
    private val removeFromTripListUseCase = mockk<RemoveFromTripListUseCase>()
    private val createTripListUseCase = mockk<CreateTripListUseCase>()
    private val webSearchService = mockk<WebSearchService>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val getTripPlanUseCase = mockk<GetTripPlanUseCase>()
    private val generateTripPlanUseCase = mockk<GenerateTripPlanUseCase>()
    private val tripPlanRevertStore = TripPlanRevertStore()
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
        every { getTripListsUseCase() } returns flowOf(emptyList())
        viewModel = AssistantViewModel(
            llmService = llmService,
            searchAttractionsUseCase = searchAttractionsUseCase,
            getTripListsUseCase = getTripListsUseCase,
            getAttractionsForListUseCase = getAttractionsForListUseCase,
            addToTripListUseCase = addToTripListUseCase,
            removeFromTripListUseCase = removeFromTripListUseCase,
            createTripListUseCase = createTripListUseCase,
            webSearchService = webSearchService,
            settingsRepository = settingsRepository,
            getTripPlanUseCase = getTripPlanUseCase,
            generateTripPlanUseCase = generateTripPlanUseCase,
            tripPlanRevertStore = tripPlanRevertStore
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun mockLlmSuccess(vararg events: LlmEvent) {
        coEvery { llmService.completeChat(any(), any(), any()) } returns Result.success(listOf(*events))
    }

    @Test
    fun sendMessage_addsUserMessageToList() = runTest {
        mockLlmSuccess(LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        val messages = viewModel.uiState.value.messages
        assertTrue(messages.any { it is ChatMessage.User && (it as ChatMessage.User).text == "Hello" })
    }

    @Test
    fun sendMessage_clearsInputAfterSend() = runTest {
        mockLlmSuccess(LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        assertEquals("", viewModel.uiState.value.currentInput)
    }

    @Test
    fun sendMessage_isProcessingFalseAfterCompletion() = runTest {
        mockLlmSuccess(LlmEvent.TextChunk("Hi"), LlmEvent.Done)
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun textChunks_accumulateIntoAssistantMessage() = runTest {
        mockLlmSuccess(
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
    }

    @Test
    fun llmError_addsErrorMessage() = runTest {
        mockLlmSuccess(LlmEvent.Error("connection failed"))
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
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "web_search", mapOf("query" to "Krakow")),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Found it"), LlmEvent.Done))
        )
        viewModel.updateInput("Search Krakow")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { webSearchService.search("Krakow") }
    }

    @Test
    fun toolCall_getTripLists_callsGetTripListsUseCase() = runTest {
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "get_trip_lists", emptyMap()),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Lists retrieved"), LlmEvent.Done))
        )
        viewModel.updateInput("Show my lists")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(atLeast = 1) { getTripListsUseCase() }
    }

    @Test
    fun removeFromList_waitsForConfirmation_andExecutesOnConfirm() = runTest {
        coEvery { getAttractionsForListUseCase(5L) } returns flowOf(emptyList())
        coEvery { removeFromTripListUseCase("xid1", 5L) } returns Result.success(Unit)
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "remove_from_list", mapOf("xid" to "xid1", "list_id" to 5)),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Usunięto"), LlmEvent.Done))
        )
        viewModel.updateInput("Usuń xid1 z listy 5")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingConfirmation)
        coVerify(exactly = 0) { removeFromTripListUseCase(any(), any()) }

        viewModel.confirmPendingAction()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingConfirmation)
        coVerify(exactly = 1) { removeFromTripListUseCase("xid1", 5L) }
    }

    @Test
    fun removeFromList_rejectedByUser_doesNotExecute() = runTest {
        coEvery { getAttractionsForListUseCase(5L) } returns flowOf(emptyList())
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "remove_from_list", mapOf("xid" to "xid1", "list_id" to 5)),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Rozumiem, nie usuwam"), LlmEvent.Done))
        )
        viewModel.updateInput("Usuń xid1 z listy 5")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.rejectPendingAction()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingConfirmation)
        coVerify(exactly = 0) { removeFromTripListUseCase(any(), any()) }
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun updateTripPlan_confirmed_storesPreviousPlanForRevert() = runTest {
        val previousPlan = TripPlan(days = emptyList())
        val newPlan = TripPlan(days = emptyList())
        coEvery { getTripPlanUseCase(5L) } returns (previousPlan to null)
        coEvery { generateTripPlanUseCase.updateFromJson(5L, any()) } returns Result.success(newPlan)
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "update_trip_plan", mapOf("list_id" to 5, "plan_json" to "{}")),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Zaktualizowano"), LlmEvent.Done))
        )
        viewModel.updateInput("Zmień plan listy 5")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.confirmPendingAction()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { generateTripPlanUseCase.updateFromJson(5L, "{}") }
        assertEquals(previousPlan, tripPlanRevertStore.consume(5L))
    }

    @Test
    fun addToList_executesWithoutConfirmation() = runTest {
        coEvery { addToTripListUseCase("xid1", 5L) } returns Result.success(Unit)
        coEvery { llmService.completeChat(any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(
                LlmEvent.ToolCall("id1", "add_to_list", mapOf("xid" to "xid1", "list_id" to 5)),
                LlmEvent.Done
            )),
            Result.success(listOf(LlmEvent.TextChunk("Dodano"), LlmEvent.Done))
        )
        viewModel.updateInput("Dodaj xid1 do listy 5")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingConfirmation)
        coVerify(exactly = 1) { addToTripListUseCase("xid1", 5L) }
    }

    @Test
    fun clearChat_emptiesMessages() = runTest {
        mockLlmSuccess(LlmEvent.TextChunk("Hi"), LlmEvent.Done)
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
