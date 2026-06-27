package com.iicytower.wanderlist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttractionDao {

    @Query("SELECT * FROM attractions WHERE isInMyList = 1 ORDER BY dateAddedToList DESC")
    fun getMyList(): Flow<List<AttractionEntity>>

    @Query("SELECT * FROM attractions WHERE xid = :xid")
    suspend fun getByXid(xid: String): AttractionEntity?

    @Query("SELECT * FROM attractions WHERE isFromLastSearch = 1")
    suspend fun getLastSearchResults(): List<AttractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attraction: AttractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attractions: List<AttractionEntity>)

    @Query("UPDATE attractions SET isFromLastSearch = 0 WHERE isFromLastSearch = 1")
    suspend fun clearLastSearchFlag()

    @Query("DELETE FROM attractions WHERE isInMyList = 0 AND description IS NULL AND isFromLastSearch = 0")
    suspend fun deleteOrphans()

    @Query("UPDATE attractions SET isInMyList = 1, dateAddedToList = :timestamp WHERE xid = :xid")
    suspend fun addToMyList(xid: String, timestamp: Long)

    @Query("UPDATE attractions SET isInMyList = 0, dateAddedToList = NULL WHERE xid = :xid")
    suspend fun removeFromMyList(xid: String)

    @Query("SELECT COUNT(*) FROM attractions WHERE isInMyList = 1")
    suspend fun getMyListCount(): Int

    @Query("UPDATE attractions SET description = :description, descriptionSources = :sources WHERE xid = :xid")
    suspend fun saveDescription(xid: String, description: String, sources: String)

    @Transaction
    suspend fun replaceSearchResults(newResults: List<AttractionEntity>) {
        clearLastSearchFlag()
        deleteOrphans()
        upsertAll(newResults)
    }
}
