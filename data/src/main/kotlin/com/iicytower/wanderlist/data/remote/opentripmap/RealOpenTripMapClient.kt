package com.iicytower.wanderlist.data.remote.opentripmap

import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDetailDto
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class RealOpenTripMapClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) : OpenTripMapClient {

    private val baseUrl = "https://api.opentripmap.com/0.1/en/places"

    override suspend fun searchAttractions(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        kinds: String
    ): Result<List<OtmAttractionDto>> = runCatching {
        val response = httpClient.get("$baseUrl/radius") {
            parameter("radius", radiusMeters)
            parameter("lon", longitude)
            parameter("lat", latitude)
            parameter("kinds", kinds)
            parameter("format", "json")
            parameter("limit", 100)
            parameter("apikey", apiKey)
        }
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}")
        }
        response.body()
    }

    override suspend fun getAttractionDetail(xid: String): Result<OtmAttractionDetailDto> = runCatching {
        val response = httpClient.get("$baseUrl/xid/$xid") {
            parameter("apikey", apiKey)
        }
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}")
        }
        response.body()
    }
}
