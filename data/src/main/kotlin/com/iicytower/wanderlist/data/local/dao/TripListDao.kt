package com.iicytower.wanderlist.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import com.iicytower.wanderlist.data.local.entity.AttractionListCrossRefEntity
import com.iicytower.wanderlist.data.local.entity.TripListEntity
import kotlinx.coroutines.flow.Flow

data class TripListWithCount(
    @Embedded val tripList: TripListEntity,
    @ColumnInfo(name = "attractionCount") val attractionCount: Int
)

@Dao
interface TripListDao {

    @Query("""
        SELECT t.*, COUNT(c.attractionXid) as attractionCount
        FROM trip_lists t
        LEFT JOIN attraction_list_crossref c ON t.id = c.listId
        GROUP BY t.id
        ORDER BY t.createdAt ASC
    """)
    fun getListsWithCount(): Flow<List<TripListWithCount>>

    @Query("""
        SELECT t.*, 0 as attractionCount
        FROM trip_lists t
        INNER JOIN attraction_list_crossref c ON t.id = c.listId
        WHERE c.attractionXid = :xid
    """)
    fun getListsForAttraction(xid: String): Flow<List<TripListWithCount>>

    @Query("""
        SELECT a.* FROM attractions a
        INNER JOIN attraction_list_crossref c ON a.xid = c.attractionXid
        WHERE c.listId = :listId
        ORDER BY c.addedAt DESC
    """)
    fun getAttractionsForList(listId: Long): Flow<List<AttractionEntity>>

    @Insert
    suspend fun insertList(entity: TripListEntity): Long

    @Query("DELETE FROM trip_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToList(crossRef: AttractionListCrossRefEntity)

    @Query("DELETE FROM attraction_list_crossref WHERE attractionXid = :xid AND listId = :listId")
    suspend fun removeFromList(xid: String, listId: Long)

    @Query("SELECT COUNT(*) FROM attraction_list_crossref WHERE listId = :listId")
    suspend fun getCountForList(listId: Long): Int

    @Query("SELECT COUNT(*) FROM attraction_list_crossref WHERE attractionXid = :xid")
    suspend fun getListCountForAttraction(xid: String): Int

    @Transaction
    suspend fun removeFromListAndSync(xid: String, listId: Long, attractionDao: AttractionDao) {
        removeFromList(xid, listId)
        if (getListCountForAttraction(xid) == 0) {
            attractionDao.removeFromMyList(xid)
        }
    }
}
