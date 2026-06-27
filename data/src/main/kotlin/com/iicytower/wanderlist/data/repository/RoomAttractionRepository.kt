package com.iicytower.wanderlist.data.repository

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.data.local.dao.AttractionDao
import com.iicytower.wanderlist.data.local.mapper.toDomain
import com.iicytower.wanderlist.data.local.mapper.toEntity
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.DescriptionSource
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
private data class DescriptionSourceDto(val name: String, val url: String)

class RoomAttractionRepository(
    private val dao: AttractionDao
) : AttractionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getMyList(): Flow<List<Attraction>> =
        dao.getMyList().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getByXid(xid: String): Attraction? =
        dao.getByXid(xid)?.toDomain()

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> =
        Result.failure(UnsupportedOperationException("searchAttractions requires OpenTripMapClient"))

    override suspend fun addToMyList(xid: String): Result<Unit> = runCatching {
        val count = dao.getMyListCount()
        if (count >= AppConstants.MY_LIST_MAX_SIZE) {
            error("Lista pełna (${AppConstants.MY_LIST_MAX_SIZE}/${AppConstants.MY_LIST_MAX_SIZE})")
        }
        dao.addToMyList(xid, System.currentTimeMillis())
    }

    override suspend fun removeFromMyList(xid: String): Result<Unit> = runCatching {
        dao.removeFromMyList(xid)
    }

    override suspend fun saveDescription(
        xid: String,
        description: String,
        sources: List<DescriptionSource>
    ): Result<Unit> = runCatching {
        val sourcesJson = json.encodeToString(sources.map { DescriptionSourceDto(it.name, it.url) })
        dao.saveDescription(xid, description, sourcesJson)
    }

    override suspend fun getLastSearchResults(): List<Attraction> =
        dao.getLastSearchResults().map { it.toDomain() }

    suspend fun saveSearchResults(attractions: List<Attraction>) {
        dao.replaceSearchResults(attractions.map { it.toEntity() })
    }
}
