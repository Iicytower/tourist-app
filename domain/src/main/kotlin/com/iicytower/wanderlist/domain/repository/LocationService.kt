package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.Location

interface LocationService {
    suspend fun getCurrentLocation(): Result<Location>
    fun hasLocationPermission(): Boolean
}
