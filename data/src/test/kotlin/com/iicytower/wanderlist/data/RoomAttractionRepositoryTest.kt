package com.iicytower.wanderlist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.local.AppDatabase
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import com.iicytower.wanderlist.data.repository.RoomAttractionRepository
import com.iicytower.wanderlist.domain.model.DescriptionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomAttractionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RoomAttractionRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomAttractionRepository(db.attractionDao())
    }

    @After
    fun tearDown() { db.close() }

    private fun insertEntity(xid: String, inMyList: Boolean = false) {
        db.attractionDao().let {
            kotlinx.coroutines.runBlocking {
                it.upsert(
                    AttractionEntity(
                        xid = xid, name = "Name $xid", latitude = 50.0, longitude = 20.0,
                        category = AttractionCategory.CASTLES_AND_FORTIFICATIONS.name,
                        isInMyList = inMyList,
                        dateAddedToList = if (inMyList) System.currentTimeMillis() else null,
                        description = null, descriptionSources = null, isFromLastSearch = false
                    )
                )
            }
        }
    }

    @Test
    fun addToMyList_fails_when_list_is_full() = runTest {
        repeat(50) { i -> insertEntity("xid_$i", inMyList = true) }
        val result = repository.addToMyList("new_xid")
        assertTrue(result.isFailure)
    }

    @Test
    fun addToMyList_succeeds_when_not_full() = runTest {
        insertEntity("xid1")
        val result = repository.addToMyList("xid1")
        assertTrue(result.isSuccess)
        val myList = repository.getMyList().first()
        assertTrue(myList.any { it.xid == "xid1" })
    }

    @Test
    fun saveDescription_and_mapper_deserializes_json() = runTest {
        insertEntity("xid1")
        val sources = listOf(DescriptionSource("Wikipedia", "https://wikipedia.org/wiki/test"))
        repository.saveDescription("xid1", "Opis testowy", sources)
        val attraction = repository.getByXid("xid1")
        assertNotNull(attraction)
        assertEquals("Opis testowy", attraction!!.description)
        assertEquals(1, attraction.descriptionSources.size)
        assertEquals("Wikipedia", attraction.descriptionSources[0].name)
    }
}
