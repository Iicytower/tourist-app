package com.iicytower.wanderlist.domain.model

import com.iicytower.wanderlist.core.util.calculateDistanceKm

/** Punkt trasy zwiedzania; [order] liczony od 1. */
data class RoutePoint(
    val attraction: Attraction,
    val order: Int
)

/** Suma odległości po sferze (Haversine) między kolejnymi punktami trasy. */
fun List<RoutePoint>.totalDistanceKm(): Double =
    zipWithNext().sumOf { (a, b) ->
        calculateDistanceKm(
            a.attraction.latitude, a.attraction.longitude,
            b.attraction.latitude, b.attraction.longitude
        )
    }
