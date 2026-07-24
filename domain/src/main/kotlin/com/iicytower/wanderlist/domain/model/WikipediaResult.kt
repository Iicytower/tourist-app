package com.iicytower.wanderlist.domain.model

data class WikipediaResult(
    val extract: String,
    val url: String,
    val imageUrl: String? = null
)
