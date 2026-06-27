package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.Location

interface GeocoderService {
    suspend fun geocode(query: String): Result<Pair<Location, String>>
}
