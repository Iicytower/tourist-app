package com.iicytower.wanderlist.feature.map

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.feature.map.viewmodel.MapViewModel
import io.mockk.coEvery
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
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMyListUseCase = mockk<GetMyListUseCase>()
    private val attractionRepository = mockk<AttractionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getMyListUseCase() } returns flowOf(emptyList())
        coEvery { attractionRepository.getLastSearchResults() } returns emptyList()
        coEvery { settingsRepository.getLastMapPosition() } returns null
        viewModel = MapViewModel(getMyListUseCase, attractionRepository, settingsRepository)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun makeAttraction(xid: String) = Attraction(
        xid, "Name", 50.0, 20.0, AttractionCategory.MUSEUMS_AND_GALLERIES,
        false, null, null, emptyList(), true, 1.0
    )

    @Test
    fun toggleMyListMode_switchesFlag() = runTest {
        assertFalse(viewModel.uiState.value.showMyListOnly)
        viewModel.toggleMyListMode()
        assertTrue(viewModel.uiState.value.showMyListOnly)
        viewModel.toggleMyListMode()
        assertFalse(viewModel.uiState.value.showMyListOnly)
    }

    @Test
    fun selectAttraction_setsSelectedAttraction() = runTest {
        val attraction = makeAttraction("xid1")
        coEvery { attractionRepository.getLastSearchResults() } returns listOf(attraction)
        viewModel.onMapReady()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectAttraction("xid1")
        assertEquals("xid1", viewModel.uiState.value.selectedAttraction?.xid)
    }

    @Test
    fun selectAttraction_null_clearsSelection() = runTest {
        viewModel.selectAttraction(null)
        assertNull(viewModel.uiState.value.selectedAttraction)
    }

    @Test
    fun onMapReady_loadsSearchResults() = runTest {
        val attraction = makeAttraction("xid1")
        coEvery { attractionRepository.getLastSearchResults() } returns listOf(attraction)
        viewModel.onMapReady()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.searchResults.size)
    }

    @Test
    fun getMyListUseCase_observed_updatesMyList() = runTest {
        val attraction = makeAttraction("my1")
        every { getMyListUseCase() } returns flowOf(listOf(attraction))
        viewModel.onMapReady()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.myList.size)
    }
}
