package com.iicytower.wanderlist.data.remote.opentripmap

import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDetailDto
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDto

interface OpenTripMapClient {
    suspend fun searchAttractions(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        kinds: String
    ): Result<List<OtmAttractionDto>>

    suspend fun getAttractionDetail(xid: String): Result<OtmAttractionDetailDto>
}
