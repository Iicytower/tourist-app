package com.iicytower.wanderlist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TripPlan(
    val days: List<TripDay>
)

@Serializable
data class TripDay(
    val label: String,
    val points: List<TripPoint>
)

@Serializable
data class TripPoint(
    val xid: String,
    val name: String,
    val note: String? = null
)
