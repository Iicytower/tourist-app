package com.iicytower.wanderlist.data.remote.wikipedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class WikipediaGeoSearchResponse(
    val query: WikiGeoQuery? = null
)

@Serializable
data class WikiGeoQuery(
    val geosearch: List<WikiGeoResult> = emptyList()
)

@Serializable
data class WikiGeoResult(
    val pageid: Long,
    val title: String,
    val lat: Double,
    val lon: Double,
    val dist: Double
)
