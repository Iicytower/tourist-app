package com.iicytower.wanderlist.data.remote.overpass.dto

import kotlinx.serialization.Serializable

@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@Serializable
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap()
) {
    fun effectiveLat(): Double? = lat ?: center?.lat
    fun effectiveLon(): Double? = lon ?: center?.lon
}

@Serializable
data class OverpassCenter(val lat: Double, val lon: Double)
