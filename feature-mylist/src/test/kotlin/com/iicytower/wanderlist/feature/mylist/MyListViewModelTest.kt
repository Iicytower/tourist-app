package com.iicytower.wanderlist.feature.mylist

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import com.iicytower.wanderlist.feature.mylist.viewmodel.MyListSortOrder
import com.iicytower.wanderlist.feature.mylist.viewmodel.MyListViewModel
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
class MyListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMyListUseCase = mockk<GetMyListUseCase>()
    private val removeFromMyListUseCase = mockk<RemoveFromMyListUseCase>()
    private lateinit var viewModel: MyListViewModel

    private fun makeAttraction(xid: String, name: String, dateAdded: Long? = null, distanceKm: Double? = null) =
        Attraction(
            xid, name, 50.0, 20.0, AttractionCategory.MUSEUMS_AND_GALLERIES,
            true, dateAdded, null, emptyList(), false, distanceKm
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getMyListUseCase() } returns flowOf(emptyList())
        viewModel = MyListViewModel(getMyListUseCase, removeFromMyListUseCase)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun init_loadsMyList() = runTest {
        val list = listOf(makeAttraction("a", "A"), makeAttraction("b", "B"))
        every { getMyListUseCase() } returns flowOf(list)
        viewModel = MyListViewModel(getMyListUseCase, removeFromMyListUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.attractions.size)
    }

    @Test
    fun setSortOrder_byName_sortsAlphabetically() = runTest {
        val list = listOf(makeAttraction("b", "Brama"), makeAttraction("a", "Akademia"))
        every { getMyListUseCase() } returns flowOf(list)
        viewModel = MyListViewModel(getMyListUseCase, removeFromMyListUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setSortOrder(MyListSortOrder.NAME)
        assertEquals("Akademia", viewModel.uiState.value.attractions.first().name)
    }

    @Test
    fun setSortOrder_byDistance_sortsAscending() = runTest {
        val list = listOf(makeAttraction("far", "Daleko", distanceKm = 10.0), makeAttraction("near", "Blisko", distanceKm = 1.0))
        every { getMyListUseCase() } returns flowOf(list)
        viewModel = MyListViewModel(getMyListUseCase, removeFromMyListUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setSortOrder(MyListSortOrder.DISTANCE)
        assertEquals("Blisko", viewModel.uiState.value.attractions.first().name)
    }

    @Test
    fun requestDelete_setsConfirmXid() = runTest {
        viewModel.requestDelete("abc")
        assertEquals("abc", viewModel.uiState.value.confirmDeleteXid)
    }

    @Test
    fun cancelDelete_clearsConfirmXid() = runTest {
        viewModel.requestDelete("abc")
        viewModel.cancelDelete()
        assertNull(viewModel.uiState.value.confirmDeleteXid)
    }

    @Test
    fun confirmDelete_callsRemoveUseCase() = runTest {
        coEvery { removeFromMyListUseCase("abc") } returns Result.success(Unit)
        viewModel.requestDelete("abc")
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.confirmDeleteXid)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun confirmDelete_onFailure_setsError() = runTest {
        coEvery { removeFromMyListUseCase("abc") } returns Result.failure(Exception("Błąd"))
        viewModel.requestDelete("abc")
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Błąd", viewModel.uiState.value.error)
    }

    @Test
    fun clearError_removesError() = runTest {
        coEvery { removeFromMyListUseCase("abc") } returns Result.failure(Exception("Błąd"))
        viewModel.requestDelete("abc")
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
