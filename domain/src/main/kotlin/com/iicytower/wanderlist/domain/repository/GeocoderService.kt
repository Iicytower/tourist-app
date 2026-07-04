package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.GeocodeSuggestion
import com.iicytower.wanderlist.domain.model.Location

interface GeocoderService {
    suspend fun geocode(query: String): Result<Pair<Location, String>>
    suspend fun suggest(query: String): Result<List<GeocodeSuggestion>>
    suspend fun reverseGeocode(lat: Double, lon: Double): Result<String>
}
