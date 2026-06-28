package com.iicytower.wanderlist.domain.model

data class TripList(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val attractionCount: Int = 0
)
