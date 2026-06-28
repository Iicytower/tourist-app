package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.TripList
import kotlinx.coroutines.flow.Flow

interface TripListRepository {
    fun getLists(): Flow<List<TripList>>
    fun getListsForAttraction(xid: String): Flow<List<TripList>>
    fun getAttractionsForList(listId: Long): Flow<List<Attraction>>
    suspend fun createList(name: String): Result<Long>
    suspend fun deleteList(id: Long): Result<Unit>
    suspend fun addToList(xid: String, listId: Long): Result<Unit>
    suspend fun removeFromList(xid: String, listId: Long): Result<Unit>
}
