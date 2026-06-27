package com.iicytower.wanderlist.data.remote.tavily.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TavilySearchRequest(
    val query: String,
    @SerialName("api_key") val apiKey: String,
    @SerialName("search_depth") val searchDepth: String = "basic",
    @SerialName("max_results") val maxResults: Int = 5
)

@Serializable
data class TavilySearchResponse(
    val results: List<TavilyResult>,
    val answer: String? = null
)

@Serializable
data class TavilyResult(
    val title: String,
    val url: String,
    val content: String,
    val score: Double
)
