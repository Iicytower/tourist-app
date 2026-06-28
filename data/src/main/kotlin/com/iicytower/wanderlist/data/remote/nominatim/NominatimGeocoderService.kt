package com.iicytower.wanderlist.data.remote.nominatim

import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.repository.GeocoderService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NominatimResult(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String
)

class NominatimGeocoderService(private val httpClient: HttpClient) : GeocoderService {

    override suspend fun geocode(query: String): Result<Pair<Location, String>> = runCatching {
        val results = httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "1")
            header("User-Agent", "WanderList/1.0 (tourist app)")
        }.body<List<NominatimResult>>()

        val first = results.firstOrNull()
            ?: throw Exception("Nie znaleziono miejsca: \"$query\"")

        val location = Location(first.lat.toDouble(), first.lon.toDouble())
        location to first.displayName
    }
}
