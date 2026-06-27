package com.iicytower.wanderlist.feature.detail

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import com.iicytower.wanderlist.feature.detail.viewmodel.AttractionDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
class AttractionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAttractionDetailUseCase = mockk<GetAttractionDetailUseCase>()
    private val generateDescriptionUseCase = mockk<GenerateDescriptionUseCase>()
    private val addToMyListUseCase = mockk<AddToMyListUseCase>()
    private val removeFromMyListUseCase = mockk<RemoveFromMyListUseCase>()
    private lateinit var viewModel: AttractionDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AttractionDetailViewModel(
            getAttractionDetailUseCase, generateDescriptionUseCase,
            addToMyListUseCase, removeFromMyListUseCase
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun makeAttraction(inMyList: Boolean = false, description: String? = null) =
        Attraction("xid1", "Zamek Wawelski", 50.0, 20.0, AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            inMyList, null, description, emptyList(), false, null)

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
    fun toggleMyList_whenNotInList_callsAdd() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction(inMyList = false)
        coEvery { addToMyListUseCase("xid1") } returns Result.success(Unit)
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleMyList()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { addToMyListUseCase("xid1") }
    }

    @Test
    fun toggleMyList_whenListFull_setsError() = runTest {
        coEvery { getAttractionDetailUseCase("xid1") } returns makeAttraction(inMyList = false)
        coEvery { addToMyListUseCase("xid1") } returns Result.failure(IllegalStateException("Lista pełna"))
        viewModel.load("xid1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleMyList()
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
