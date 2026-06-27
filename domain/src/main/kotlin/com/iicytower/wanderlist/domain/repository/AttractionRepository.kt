package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.DescriptionSource
import com.iicytower.wanderlist.domain.model.SearchParams
import kotlinx.coroutines.flow.Flow

interface AttractionRepository {
    fun getMyList(): Flow<List<Attraction>>
    suspend fun getByXid(xid: String): Attraction?
    suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>>
    suspend fun addToMyList(xid: String): Result<Unit>
    suspend fun removeFromMyList(xid: String): Result<Unit>
    suspend fun saveDescription(xid: String, description: String, sources: List<DescriptionSource>): Result<Unit>
    suspend fun getLastSearchResults(): List<Attraction>
}
