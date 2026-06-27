package com.iicytower.wanderlist.core.util

import kotlin.math.*

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

fun formatDistance(distanceKm: Double): String {
    return if (distanceKm < 1.0) {
        "${(distanceKm * 1000).roundToInt()} m"
    } else {
        val rounded = (distanceKm * 10).roundToInt() / 10.0
        if (rounded == rounded.toLong().toDouble()) {
            "${rounded.toLong()} km"
        } else {
            "${rounded.toString().replace('.', ',')} km"
        }
    }
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
