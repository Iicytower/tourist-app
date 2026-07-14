package com.iicytower.wanderlist.data.remote.wikipedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class WikipediaGeoSearchResponse(
    val query: WikiGeoQuery? = null
)

@Serializable
data class WikiGeoQuery(
    val pages: Map<String, WikiGeoPage> = emptyMap()
)

@Serializable
data class WikiGeoPage(
    val pageid: Long,
    val title: String,
    val coordinates: List<WikiCoordinate> = emptyList(),
    val thumbnail: WikiThumbnail? = null
)

@Serializable
data class WikiCoordinate(val lat: Double, val lon: Double)

@Serializable
data class WikiThumbnail(val source: String)
