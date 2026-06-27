package com.iicytower.wanderlist.data.remote.opentripmap.dto

import kotlinx.serialization.Serializable

@Serializable
data class OtmAttractionDto(
    val xid: String,
    val name: String,
    val dist: Double,
    val rate: Int = 0,
    val kinds: String,
    val point: OtmPoint
)

@Serializable
data class OtmPoint(
    val lon: Double,
    val lat: Double
)

@Serializable
data class OtmAttractionDetailDto(
    val xid: String,
    val name: String,
    val kinds: String,
    val point: OtmPoint,
    val wikipedia: String? = null,
    val wikidata: String? = null,
    val preview: OtmPreview? = null,
    val info: OtmInfo? = null
)

@Serializable
data class OtmInfo(val descr: String? = null)

@Serializable
data class OtmPreview(val source: String? = null)
