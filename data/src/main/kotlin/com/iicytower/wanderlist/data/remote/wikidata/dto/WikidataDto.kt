package com.iicytower.wanderlist.data.remote.wikidata.dto

import kotlinx.serialization.Serializable

@Serializable
data class SparqlResponse(
    val results: SparqlResults = SparqlResults()
)

@Serializable
data class SparqlResults(
    val bindings: List<SparqlBinding> = emptyList()
)

@Serializable
data class SparqlBinding(
    val place: SparqlValue? = null,
    val placeLabel: SparqlValue? = null,
    val lat: SparqlValue? = null,
    val lon: SparqlValue? = null,
    val type: SparqlValue? = null
)

@Serializable
data class SparqlValue(val value: String = "")
