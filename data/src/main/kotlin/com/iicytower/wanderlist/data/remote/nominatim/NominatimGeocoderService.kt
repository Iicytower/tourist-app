package com.iicytower.wanderlist.data.remote.nominatim

import com.iicytower.wanderlist.domain.model.GeocodeSuggestion
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

        Location(first.lat.toDouble(), first.lon.toDouble()) to first.displayName
    }

    override suspend fun suggest(query: String): Result<List<GeocodeSuggestion>> = runCatching {
        httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "5")
            parameter("addressdetails", "0")
            header("User-Agent", "WanderList/1.0 (tourist app)")
            header("Accept-Language", "*")
        }.body<List<NominatimResult>>().map {
            GeocodeSuggestion(it.displayName, it.lat.toDouble(), it.lon.toDouble())
        }
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): Result<String> = runCatching {
        httpClient.get("https://nominatim.openstreetmap.org/reverse") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("format", "json")
            parameter("zoom", "14")
            header("User-Agent", "WanderList/1.0 (tourist app)")
            header("Accept-Language", "*")
        }.body<NominatimResult>().displayName
    }
}
