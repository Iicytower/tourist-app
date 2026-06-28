package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AddToMyListUseCaseTest {

    private val repository = mockk<AttractionRepository>()
    private val useCase = AddToMyListUseCase(repository)

    private fun makeAttraction(xid: String) = Attraction(
        xid = xid, name = "Test", latitude = 0.0, longitude = 0.0,
        category = AttractionCategory.CASTLES_AND_FORTIFICATIONS,
        isInMyList = true, dateAddedToList = null,
        description = null, descriptionSources = emptyList(),
        isFromLastSearch = false, distanceKm = null
    )

    @Test
    fun `returns failure when list is full`() = runTest {
        val fullList = (1..AppConstants.MY_LIST_MAX_SIZE).map { makeAttraction("xid_$it") }
        every { repository.getMyList() } returns flowOf(fullList)

        val result = useCase("new_xid")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("pełna"))
        coVerify(exactly = 0) { repository.addToMyList(any()) }
    }

    @Test
    fun `adds to list when not full`() = runTest {
        every { repository.getMyList() } returns flowOf(listOf(makeAttraction("xid_1")))
        coEvery { repository.addToMyList("new_xid") } returns Result.success(Unit)

        val result = useCase("new_xid")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.addToMyList("new_xid") }
    }

    @Test
    fun `adds to list when empty`() = runTest {
        every { repository.getMyList() } returns flowOf(emptyList())
        coEvery { repository.addToMyList("xid") } returns Result.success(Unit)

        val result = useCase("xid")

        assertTrue(result.isSuccess)
    }
}
