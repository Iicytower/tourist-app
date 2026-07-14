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
import timber.log.Timber

@Serializable
private data class NominatimResult(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
private data class PhotonFeatureCollection(
    val features: List<PhotonFeature>
)

@Serializable
private data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: PhotonGeometry
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double>
)

class NominatimGeocoderService(private val httpClient: HttpClient) : GeocoderService {

    // Photon wspiera tylko część języków; nieobsługiwany kod = HTTP 400, więc pomijamy parametr
    private val photonSupportedLanguages = setOf("en", "de", "fr")

    override suspend fun geocode(query: String, language: String): Result<Pair<Location, String>> = runCatching {
        val results = httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "1")
            parameter("accept-language", language)
            header("User-Agent", "WanderList/1.0 (tourist app)")
        }.body<List<NominatimResult>>()

        val first = results.firstOrNull()
            ?: throw Exception("Nie znaleziono miejsca: \"$query\"")

        Location(first.lat.toDouble(), first.lon.toDouble()) to first.displayName
    }

    override suspend fun suggest(query: String, language: String): Result<List<GeocodeSuggestion>> = runCatching {
        // Photon ma prefix-matching; Nominatim /search nie obsługuje prefiksów (szuka pełnych słów)
        val collection = httpClient.get("https://photon.komoot.io/api/") {
            parameter("q", query)
            parameter("limit", "8")
            if (language in photonSupportedLanguages) parameter("lang", language)
            header("User-Agent", "WanderList/1.0 (tourist app)")
        }.body<PhotonFeatureCollection>()
        val seen = mutableSetOf<String>()
        val suggestions = collection.features.mapNotNull { feature ->
            val p = feature.properties
            val lon = feature.geometry.coordinates.getOrNull(0) ?: return@mapNotNull null
            val lat = feature.geometry.coordinates.getOrNull(1) ?: return@mapNotNull null
            val namePart = p.name ?: return@mapNotNull null
            val displayName = listOfNotNull(namePart, p.city?.takeIf { it != namePart }, p.state, p.country)
                .joinToString(", ")
            val dedupeKey = "${namePart}|${p.country}"
            if (!seen.add(dedupeKey)) return@mapNotNull null
            GeocodeSuggestion(displayName, lat, lon)
        }.take(5)
        Timber.tag("Nominatim").d("suggest('%s') → %d results", query, suggestions.size)
        suggestions
    }.onFailure { Timber.tag("Nominatim").e(it, "suggest failed for '%s'", query) }

    override suspend fun reverseGeocode(lat: Double, lon: Double, language: String): Result<String> = runCatching {
        httpClient.get("https://nominatim.openstreetmap.org/reverse") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("format", "json")
            parameter("zoom", "14")
            parameter("accept-language", language)
            header("User-Agent", "WanderList/1.0 (tourist app)")
        }.body<NominatimResult>().displayName
    }.onFailure { Timber.tag("Nominatim").e(it, "reverseGeocode failed") }
}
