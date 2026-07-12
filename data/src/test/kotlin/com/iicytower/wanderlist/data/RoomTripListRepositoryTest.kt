package com.iicytower.wanderlist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.local.AppDatabase
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import com.iicytower.wanderlist.data.local.entity.TripListEntity
import com.iicytower.wanderlist.data.repository.RoomTripListRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomTripListRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RoomTripListRepository
    private var listId: Long = 0

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomTripListRepository(db.tripListDao(), db.attractionDao())
        listId = runBlocking { db.tripListDao().insertList(TripListEntity(name = "Trip", createdAt = System.currentTimeMillis())) }
    }

    @After
    fun tearDown() { db.close() }

    private fun insertAttraction(xid: String) {
        runBlocking {
            db.attractionDao().upsert(
                AttractionEntity(
                    xid = xid, name = "Name $xid", latitude = 50.0, longitude = 20.0,
                    category = AttractionCategory.CASTLES_AND_FORTIFICATIONS.name,
                    isInMyList = false, dateAddedToList = null,
                    description = null, descriptionSources = null, isFromLastSearch = false
                )
            )
        }
    }

    @Test
    fun addToList_fails_when_list_is_full() = runTest {
        repeat(AppConstants.MY_LIST_MAX_SIZE) { i ->
            val xid = "xid_$i"
            insertAttraction(xid)
            repository.addToList(xid, listId)
        }
        insertAttraction("overflow")
        val result = repository.addToList("overflow", listId)
        assertTrue(result.isFailure)
    }

    @Test
    fun addToList_succeeds_when_not_full() = runTest {
        insertAttraction("xid1")
        val result = repository.addToList("xid1", listId)
        assertTrue(result.isSuccess)
        val attractions = repository.getAttractionsForList(listId).first()
        assertTrue(attractions.any { it.xid == "xid1" })
    }

    @Test
    fun addToList_concurrent_calls_never_exceed_limit() = runTest {
        repeat(AppConstants.MY_LIST_MAX_SIZE - 1) { i ->
            val xid = "xid_$i"
            insertAttraction(xid)
            repository.addToList(xid, listId)
        }
        insertAttraction("candidate_a")
        insertAttraction("candidate_b")

        val results = listOf(
            async { repository.addToList("candidate_a", listId) },
            async { repository.addToList("candidate_b", listId) }
        ).awaitAll()

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        val finalCount = repository.getAttractionsForList(listId).first().size
        assertEquals(AppConstants.MY_LIST_MAX_SIZE, finalCount)
    }
}
