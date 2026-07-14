package com.iicytower.wanderlist.feature.search

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.GeocoderService
import com.iicytower.wanderlist.domain.repository.LocationService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.usecase.FilterAttractionsByQualityUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.feature.search.viewmodel.SearchViewModel
import com.iicytower.wanderlist.feature.search.viewmodel.SortOrder
import com.iicytower.wanderlist.domain.repository.FilterResult
import com.iicytower.wanderlist.domain.repository.RemovedAttraction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val searchUseCase = mockk<SearchAttractionsUseCase>()
    private val filterUseCase = mockk<FilterAttractionsByQualityUseCase>()
    private val locationService = mockk<LocationService>()
    private val geocoderService = mockk<GeocoderService>()
    private val attractionRepository = mockk<AttractionRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private lateinit var viewModel: SearchViewModel

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
        viewModel = SearchViewModel(searchUseCase, filterUseCase, locationService, geocoderService, attractionRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeAttraction(xid: String, dist: Double = 1.0, category: AttractionCategory = AttractionCategory.MUSEUMS_AND_GALLERIES) =
        Attraction(xid, "Name", 50.0, 20.0, category, false, null, null, emptyList(), true, dist)

    @Test
    fun init_preselectsInterestsAndRadiusFromSettings() = runTest {
        val interests = setOf(AttractionCategory.CASTLES_AND_FORTIFICATIONS, AttractionCategory.VIEWPOINTS)
        every { settingsRepository.getSettings() } returns flowOf(
            fakeSettings.copy(userInterests = interests, defaultRadiusKm = 25)
        )
        val vm = SearchViewModel(searchUseCase, filterUseCase, locationService, geocoderService, attractionRepository, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(interests, vm.uiState.value.selectedCategories)
        assertEquals(25, vm.uiState.value.radiusKm)
    }

    @Test
    fun search_autoQualityFilterEnabled_runsFilterAfterSearch() = runTest {
        every { settingsRepository.getSettings() } returns flowOf(fakeSettings.copy(autoQualityFilter = true))
        val vm = SearchViewModel(searchUseCase, filterUseCase, locationService, geocoderService, attractionRepository, settingsRepository)
        val attractions = listOf(makeAttraction("a"), makeAttraction("b"))
        vm.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.success(attractions)
        coEvery { filterUseCase(any()) } returns Result.success(
            FilterResult(kept = listOf(attractions.first()), removed = listOf(RemovedAttraction(attractions.last().name, "nudne")))
        )
        vm.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.results.size)
        assertEquals(1, vm.uiState.value.filterRemovedCount)
    }

    @Test
    fun search_autoQualityFilterDisabled_doesNotFilter() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.success(listOf(makeAttraction("a")))
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.filterRemovedCount)
        assertEquals(1, viewModel.uiState.value.results.size)
    }

    @Test
    fun search_withEmptyResults_setsHasSearched() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.success(emptyList())
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.hasSearched)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun search_withError_setsErrorMessage() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.failure(RuntimeException("Błąd sieci"))
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        assertEquals("Błąd sieci", viewModel.uiState.value.error)
    }

    @Test
    fun setLocationFromGps_failure_setsError() = runTest {
        coEvery { locationService.getCurrentLocation() } returns Result.failure(Exception("Brak GPS"))
        viewModel.setLocationFromGps()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Brak GPS", viewModel.uiState.value.error)
    }

    @Test
    fun setSortOrder_BY_CATEGORY_sortsList() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        val attractions = listOf(
            makeAttraction("a", 2.0, AttractionCategory.MUSEUMS_AND_GALLERIES),
            makeAttraction("b", 1.0, AttractionCategory.CASTLES_AND_FORTIFICATIONS)
        )
        coEvery { searchUseCase(any()) } returns Result.success(attractions)
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setSortOrder(SortOrder.BY_CATEGORY)
        val result = viewModel.uiState.value.results
        assertTrue(result[0].category.name < result[1].category.name)
    }

    @Test
    fun clearError_setsErrorToNull() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.failure(RuntimeException("err"))
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun search_olderSlowerSearch_doesNotOverwriteNewerResults() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        val oldResults = listOf(makeAttraction("old"))
        val newResults = listOf(makeAttraction("new"))
        // Symulacja warstwy data: runCatching połyka CancellationException i zwraca failure
        coEvery { searchUseCase(match { it.radiusKm == 5 }) } coAnswers {
            try {
                delay(10_000)
                Result.success(oldResults)
            } catch (e: CancellationException) {
                Result.failure(e)
            }
        }
        coEvery { searchUseCase(match { it.radiusKm == 20 }) } coAnswers {
            delay(100)
            Result.success(newResults)
        }

        viewModel.setRadius(5)
        viewModel.search()
        testDispatcher.scheduler.advanceTimeBy(50)
        viewModel.setRadius(20)
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("new"), state.results.map { it.xid })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun search_duringFiltering_cancelsFilterAndKeepsNewResults() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        val firstResults = listOf(makeAttraction("a"), makeAttraction("b"))
        val secondResults = listOf(makeAttraction("c"))
        coEvery { searchUseCase(match { it.radiusKm == 5 }) } returns Result.success(firstResults)
        coEvery { searchUseCase(match { it.radiusKm == 20 }) } returns Result.success(secondResults)
        coEvery { filterUseCase(any()) } coAnswers {
            try {
                delay(10_000)
                Result.success(FilterResult(kept = listOf(firstResults[0]), removed = listOf(RemovedAttraction("b", "test"))))
            } catch (e: CancellationException) {
                Result.failure(e)
            }
        }

        viewModel.setRadius(5)
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filterByQuality()
        testDispatcher.scheduler.advanceTimeBy(50)
        viewModel.setRadius(20)
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("c"), state.results.map { it.xid })
        assertFalse(state.isFiltering)
        assertNull(state.filterRemovedCount)
        assertNull(state.error)
    }

    @Test
    fun search_isLoading_falseAfterCompletion() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.success(emptyList())
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
