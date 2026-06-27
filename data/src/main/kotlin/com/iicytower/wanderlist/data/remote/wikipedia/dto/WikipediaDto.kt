package com.iicytower.wanderlist.data.remote.wikipedia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WikipediaSummaryResponse(
    val title: String,
    val extract: String,
    @SerialName("content_urls") val contentUrls: WikipediaContentUrls? = null
)

@Serializable
data class WikipediaContentUrls(
    val desktop: WikipediaPageUrl? = null
)

@Serializable
data class WikipediaPageUrl(
    val page: String? = null
)
