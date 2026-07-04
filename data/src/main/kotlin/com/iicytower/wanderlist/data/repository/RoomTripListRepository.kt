package com.iicytower.wanderlist.data.repository

import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.data.local.dao.AttractionDao
import com.iicytower.wanderlist.data.local.dao.TripListDao
import com.iicytower.wanderlist.data.local.entity.AttractionListCrossRefEntity
import com.iicytower.wanderlist.data.local.entity.TripListEntity
import com.iicytower.wanderlist.data.local.mapper.toDomain
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.domain.model.TripPlan
import com.iicytower.wanderlist.domain.repository.TripListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomTripListRepository(
    private val tripListDao: TripListDao,
    private val attractionDao: AttractionDao
) : TripListRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getLists(): Flow<List<TripList>> =
        tripListDao.getListsWithCount().map { list ->
            list.map { it.toTripList() }
        }

    override fun getListsForAttraction(xid: String): Flow<List<TripList>> =
        tripListDao.getListsForAttraction(xid).map { list ->
            list.map { it.toTripList() }
        }

    override fun getAttractionsForList(listId: Long): Flow<List<Attraction>> =
        tripListDao.getAttractionsForList(listId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createList(name: String): Result<Long> = runCatching {
        tripListDao.insertList(TripListEntity(name = name, createdAt = System.currentTimeMillis()))
    }

    override suspend fun deleteList(id: Long): Result<Unit> = runCatching {
        tripListDao.deleteList(id)
    }

    override suspend fun addToList(xid: String, listId: Long): Result<Unit> = runCatching {
        val count = tripListDao.getCountForList(listId)
        if (count >= AppConstants.MY_LIST_MAX_SIZE) {
            error("Lista jest pełna (${AppConstants.MY_LIST_MAX_SIZE}/${AppConstants.MY_LIST_MAX_SIZE})")
        }
        tripListDao.addToList(AttractionListCrossRefEntity(attractionXid = xid, listId = listId, addedAt = System.currentTimeMillis()))
        attractionDao.addToMyList(xid, System.currentTimeMillis())
    }

    override suspend fun removeFromList(xid: String, listId: Long): Result<Unit> = runCatching {
        tripListDao.removeFromListAndSync(xid, listId, attractionDao)
    }

    override suspend fun saveTripPlan(listId: Long, plan: TripPlan): Result<Unit> = runCatching {
        tripListDao.updateTripPlan(listId, json.encodeToString(plan))
    }

    override suspend fun updateTripPlanNotes(listId: Long, notes: String?): Result<Unit> = runCatching {
        tripListDao.updateTripPlanNotes(listId, notes)
    }

    override suspend fun deleteTripPlan(listId: Long): Result<Unit> = runCatching {
        tripListDao.updateTripPlan(listId, null)
        tripListDao.updateTripPlanNotes(listId, null)
    }

    override suspend fun getTripPlan(listId: Long): Pair<TripPlan?, String?> {
        val data = tripListDao.getTripPlanData(listId) ?: return Pair(null, null)
        val plan = data.tripPlanJson?.let { runCatching { json.decodeFromString<TripPlan>(it) }.getOrNull() }
        return Pair(plan, data.tripPlanNotes)
    }
}

private fun com.iicytower.wanderlist.data.local.dao.TripListWithCount.toTripList() = TripList(
    id = tripList.id,
    name = tripList.name,
    createdAt = tripList.createdAt,
    attractionCount = attractionCount,
    hasTripPlan = tripList.tripPlanJson != null
)
