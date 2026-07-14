package com.iicytower.wanderlist.domain

import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AddToMyListUseCaseTest {

    private val repository = mockk<AttractionRepository>()
    private val useCase = AddToMyListUseCase(repository)

    @Test
    fun `delegates to repository and returns its result`() = runTest {
        coEvery { repository.addToMyList("xid") } returns Result.success(Unit)

        val result = useCase("xid")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.addToMyList("xid") }
    }

    @Test
    fun `propagates failure from repository (eg list full) without a separate size check`() = runTest {
        val failure = IllegalStateException("Lista pelna (50/50)")
        coEvery { repository.addToMyList("new_xid") } returns Result.failure(failure)

        val result = useCase("new_xid")

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }
}
