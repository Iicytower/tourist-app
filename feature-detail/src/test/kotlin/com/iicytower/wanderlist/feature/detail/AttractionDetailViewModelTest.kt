package com.iicytower.wanderlist.feature.detail

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import com.iicytower.wanderlist.feature.detail.viewmodel.AttractionDetailViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttractionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAttractionDetailUseCase = mockk<GetAttractionDetailUseCase>()
    private val generateDescriptionUseCase = mockk<GenerateDescriptionUseCase>()
    private val getTripListsUseCase = mockk<GetTripListsUseCase>()
    private val addToTripListUseCase = mockk<AddToTripListUseCase>()
    private val removeFromTripListUseCase = mockk<RemoveFromTripListUseCase>()
    private val createTripListUseCase = mockk<CreateTripListUseCase>()
    private val tripListRepository = mockk<TripListRepository>()
    private lateinit var viewModel: AttractionDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getTripListsUseCase() } returns flowOf(emptyList())
        every { tripListRepository.getListsForAttraction(any()) } returns flowOf(emptyList())
        viewModel = AttractionDetailViewModel(
            getAttractionDetailUseCase, generateDescriptionUseCase,
            getTripListsUseCase, addToTripListUseCase, removeFromTripListUseCase,
            createTripListUseCase, tripListRepository
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun makeAttraction(description: String? = null) =
        Attraction("xid1", "Zamek Wawelski", 50.0, 20.0, AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            false, null, description, emptyList(), false, null)

    private fun makeTripList(id: Long = 1L) =
        TripList(id, "Wycieczka", System.currentTimeMillis(), 0)

    @Test
    fun load_withShowDistance_true_setsFlag() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction()
        viewModel.load("xid1", showDistance = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDistanceFromSearch)
    }

    @Test
    fun load_withShowDistance_false_doesNotSetFlag() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction()
        viewModel.load("xid1", showDistance = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDistanceFromSearch)
    }

    @Test
    fun toggleList_whenNotInList_callsAdd() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction()
        coEvery { addToTripListUseCase("xid1", 1L) } returns Result.success(Unit)
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleList(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { addToTripListUseCase("xid1", 1L) }
    }

    @Test
    fun toggleList_whenInList_callsRemove() = runTest {
        every { tripListRepository.getListsForAttraction("xid1") } returns flowOf(listOf(makeTripList(1L)))
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction()
        coEvery { removeFromTripListUseCase("xid1", 1L) } returns Result.success(Unit)
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleList(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { removeFromTripListUseCase("xid1", 1L) }
    }

    @Test
    fun toggleList_whenListFull_setsError() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction()
        coEvery { addToTripListUseCase("xid1", 1L) } returns Result.failure(IllegalStateException("Lista pełna"))
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleList(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadDescription_whenDescriptionExists_doesNotCallUseCase() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction(description = "Istniejący opis")
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.loadDescription()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { generateDescriptionUseCase("xid1") }
    }
}
