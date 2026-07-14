package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.GeocodeSuggestion
import com.iicytower.wanderlist.domain.model.Location

interface GeocoderService {
    suspend fun geocode(query: String, language: String = "pl"): Result<Pair<Location, String>>
    suspend fun suggest(query: String, language: String = "pl"): Result<List<GeocodeSuggestion>>
    suspend fun reverseGeocode(lat: Double, lon: Double, language: String = "pl"): Result<String>
}
