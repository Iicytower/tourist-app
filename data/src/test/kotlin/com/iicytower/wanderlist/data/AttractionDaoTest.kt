package com.iicytower.wanderlist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iicytower.wanderlist.data.local.AppDatabase
import com.iicytower.wanderlist.data.local.dao.AttractionDao
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttractionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AttractionDao

    private fun entity(xid: String, inMyList: Boolean = false, isFromLastSearch: Boolean = false, description: String? = null) =
        AttractionEntity(
            xid = xid, name = "Name $xid", latitude = 50.0, longitude = 20.0,
            category = "CASTLES_AND_FORTIFICATIONS", isInMyList = inMyList,
            dateAddedToList = if (inMyList) System.currentTimeMillis() else null,
            description = description, descriptionSources = null, isFromLastSearch = isFromLastSearch
        )

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.attractionDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun upsertAndGetByXid() = runTest {
        dao.upsert(entity("xid1"))
        val result = dao.getByXid("xid1")
        assertNotNull(result)
        assertEquals("xid1", result!!.xid)
    }

    @Test
    fun getMyList_returnsOnlyMyListItems() = runTest {
        dao.upsert(entity("xid1", inMyList = true))
        dao.upsert(entity("xid2", inMyList = false))
        val myList = dao.getMyList().first()
        assertEquals(1, myList.size)
        assertEquals("xid1", myList[0].xid)
    }

    @Test
    fun clearLastSearchFlag_setsAllToFalse() = runTest {
        dao.upsertAll(listOf(entity("a", isFromLastSearch = true), entity("b", isFromLastSearch = true)))
        dao.clearLastSearchFlag()
        assertFalse(dao.getLastSearchResults().isNotEmpty())
    }

    @Test
    fun deleteOrphans_removesOnlyUnneededRecords() = runTest {
        dao.upsert(entity("orphan"))
        dao.upsert(entity("mylist", inMyList = true))
        dao.upsert(entity("search", isFromLastSearch = true))
        dao.upsert(entity("described", description = "desc"))
        dao.deleteOrphans()
        assertNull(dao.getByXid("orphan"))
        assertNotNull(dao.getByXid("mylist"))
        assertNotNull(dao.getByXid("search"))
        assertNotNull(dao.getByXid("described"))
    }

    @Test
    fun getMyListCount_returnsCorrectCount() = runTest {
        dao.upsert(entity("a", inMyList = true))
        dao.upsert(entity("b", inMyList = true))
        dao.upsert(entity("c", inMyList = false))
        assertEquals(2, dao.getMyListCount())
    }

    @Test
    fun replaceSearchResults_transactionWorks() = runTest {
        dao.upsert(entity("old", isFromLastSearch = true))
        val newResults = listOf(entity("new1", isFromLastSearch = true), entity("new2", isFromLastSearch = true))
        dao.replaceSearchResults(newResults)
        assertFalse(dao.getLastSearchResults().any { it.xid == "old" })
        assertEquals(2, dao.getLastSearchResults().size)
    }

    @Test
    fun addToMyList_setsFlag() = runTest {
        dao.upsert(entity("xid1"))
        dao.addToMyList("xid1", 12345L)
        val result = dao.getByXid("xid1")!!
        assertTrue(result.isInMyList)
        assertEquals(12345L, result.dateAddedToList)
    }

    @Test
    fun removeFromMyList_clearsFlag() = runTest {
        dao.upsert(entity("xid1", inMyList = true))
        dao.removeFromMyList("xid1")
        val result = dao.getByXid("xid1")!!
        assertFalse(result.isInMyList)
        assertNull(result.dateAddedToList)
    }
}
