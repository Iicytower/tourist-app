package com.iicytower.wanderlist.data.remote

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams

interface RemoteAttractionSource {
    suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>>
}
