package com.iicytower.wanderlist.feature.settings

import com.iicytower.wanderlist.core.constant.DefaultSettings
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetSettingsUseCase
import com.iicytower.wanderlist.feature.settings.viewmodel.ConnectionTestState
import com.iicytower.wanderlist.feature.settings.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val llmService = mockk<LlmService>()
    private val webSearchService = mockk<WebSearchService>()
    private lateinit var viewModel: SettingsViewModel

    private val fakeSettings = AppSettings(
        openRouterApiKey = "key",
        tavilyApiKey = "tkey",
        aiModel = "model",
        defaultRadiusKm = 10,
        descriptionLanguage = "pl",
        userInterests = emptySet(),
        systemPromptDescription = "desc",
        systemPromptAssistant = "assistant",
        tavilyUsageCount = 5,
        tavilyUsageMonth = "2026-06"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns flowOf(fakeSettings)
        viewModel = SettingsViewModel(getSettingsUseCase, settingsRepository, llmService, webSearchService)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun init_loadsSettings() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(fakeSettings, viewModel.uiState.value.settings)
    }

    @Test
    fun testOpenRouterConnection_success_setsSuccessState() = runTest {
        every { llmService.streamResponse(any(), any(), any()) } returns flowOf(
            LlmEvent.TextChunk("OK"),
            LlmEvent.Done
        )
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.testOpenRouterConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ConnectionTestState.SUCCESS, viewModel.uiState.value.openRouterTestState)
    }

    @Test
    fun testOpenRouterConnection_failure_setsFailureState() = runTest {
        every { llmService.streamResponse(any(), any(), any()) } returns flowOf(
            LlmEvent.Error("error"),
            LlmEvent.Done
        )
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.testOpenRouterConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ConnectionTestState.FAILURE, viewModel.uiState.value.openRouterTestState)
    }

    @Test
    fun testTavilyConnection_success_setsSuccessState() = runTest {
        coEvery { webSearchService.search(any()) } returns Result.success("results")
        viewModel.testTavilyConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ConnectionTestState.SUCCESS, viewModel.uiState.value.tavilyTestState)
    }

    @Test
    fun testTavilyConnection_failure_setsFailureState() = runTest {
        coEvery { webSearchService.search(any()) } returns Result.failure(Exception("fail"))
        viewModel.testTavilyConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ConnectionTestState.FAILURE, viewModel.uiState.value.tavilyTestState)
    }

    @Test
    fun resetSystemPromptDescription_callsRepositoryWithDefault() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.resetSystemPromptDescription()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { settingsRepository.updateSystemPromptDescription(DefaultSettings.SYSTEM_PROMPT_DESCRIPTION) }
    }

    @Test
    fun updateInterests_callsRepository() = runTest {
        val interests = setOf(AttractionCategory.MUSEUMS_AND_GALLERIES)
        viewModel.updateInterests(interests)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { settingsRepository.updateUserInterests(interests) }
    }

    @Test
    fun toggleOpenRouterKeyVisibility_togglesFlag() = runTest {
        assertFalse(viewModel.uiState.value.openRouterKeyVisible)
        viewModel.toggleOpenRouterKeyVisibility()
        assertTrue(viewModel.uiState.value.openRouterKeyVisible)
        viewModel.toggleOpenRouterKeyVisibility()
        assertFalse(viewModel.uiState.value.openRouterKeyVisible)
    }
}
