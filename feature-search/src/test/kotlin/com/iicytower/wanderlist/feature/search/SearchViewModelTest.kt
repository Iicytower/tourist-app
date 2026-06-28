package com.iicytower.wanderlist.feature.search

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.GeocoderService
import com.iicytower.wanderlist.domain.repository.LocationService
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.feature.search.viewmodel.SearchViewModel
import com.iicytower.wanderlist.feature.search.viewmodel.SortOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val locationService = mockk<LocationService>()
    private val geocoderService = mockk<GeocoderService>()
    private val attractionRepository = mockk<AttractionRepository>(relaxed = true)
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(searchUseCase, locationService, geocoderService, attractionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeAttraction(xid: String, dist: Double = 1.0, category: AttractionCategory = AttractionCategory.MUSEUMS_AND_GALLERIES) =
        Attraction(xid, "Name", 50.0, 20.0, category, false, null, null, emptyList(), true, dist)

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
    fun search_isLoading_falseAfterCompletion() = runTest {
        viewModel.setLocationFromCoordinates(50.0, 20.0, "Test")
        coEvery { searchUseCase(any()) } returns Result.success(emptyList())
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
