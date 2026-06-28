package com.iicytower.wanderlist.data.remote.wikipedia

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.core.util.calculateDistanceKm
import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.data.remote.wikipedia.dto.WikiGeoResult
import com.iicytower.wanderlist.data.remote.wikipedia.dto.WikipediaGeoSearchResponse
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

private const val MAX_RADIUS_METERS = 10_000
private const val GEO_LIMIT = 50

class WikipediaGeoSearchSource(private val httpClient: HttpClient) : RemoteAttractionSource {

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        val radiusMeters = (params.radiusKm * 1000).coerceAtMost(MAX_RADIUS_METERS)
        val response = httpClient.get("https://pl.wikipedia.org/w/api.php") {
            parameter("action", "query")
            parameter("list", "geosearch")
            parameter("gscoord", "${params.latitude}|${params.longitude}")
            parameter("gsradius", radiusMeters)
            parameter("gslimit", GEO_LIMIT)
            parameter("format", "json")
            header("User-Agent", "WanderList/1.0 (tourist app)")
        }.body<WikipediaGeoSearchResponse>()

        val allowedCategories = params.categories.ifEmpty { AttractionCategory.entries.toSet() }

        response.query?.geosearch.orEmpty()
            .map { it.toAttraction(params.latitude, params.longitude) }
            .filter { it.category in allowedCategories }
            .distinctBy { it.xid }
    }
}

private fun WikiGeoResult.toAttraction(searchLat: Double, searchLon: Double): Attraction =
    Attraction(
        xid = "wg$pageid",
        name = title,
        latitude = lat,
        longitude = lon,
        category = inferCategory(title),
        isInMyList = false,
        dateAddedToList = null,
        description = null,
        descriptionSources = emptyList(),
        isFromLastSearch = true,
        distanceKm = calculateDistanceKm(searchLat, searchLon, lat, lon)
    )

private fun inferCategory(title: String): AttractionCategory {
    val t = title.lowercase()
    return when {
        t.containsAny("zamek", "fort", "twierdza", "warownia", "baszta", "cytadela") ->
            AttractionCategory.CASTLES_AND_FORTIFICATIONS
        t.containsAny("kosciol", "kościół", "bazylika", "katedra", "klasztor", "kaplica", "opactwo", "sanktuarium") ->
            AttractionCategory.CHURCHES_AND_SACRED
        t.containsAny("muzeum", "galeria") ->
            AttractionCategory.MUSEUMS_AND_GALLERIES
        t.containsAny("ruiny", "grodzisko", "stanowisko archeologiczne", "wczesnoslowianski") ->
            AttractionCategory.RUINS_AND_ARCHAEOLOGICAL
        t.containsAny("park narodowy", "rezerwat", "puszcza", "las") ->
            AttractionCategory.NATURE_AND_PARKS
        t.containsAny("punkt widokowy", "wieza widokowa", "wieża widokowa") ->
            AttractionCategory.VIEWPOINTS
        t.containsAny("cmentarz", "mauzoleum", "pomnik", "memorial", "memoriał") ->
            AttractionCategory.MEMORIALS_AND_CEMETERIES
        t.containsAny("jaskinia", "grota", "stalaktyt") ->
            AttractionCategory.CAVES_AND_GEOLOGY
        t.containsAny("wiatrak", "mlyn", "młyn") ->
            AttractionCategory.MILLS_AND_TECH
        t.containsAny("bunker", "bunkier", "schronisko") ->
            AttractionCategory.MILITARY
        else -> AttractionCategory.MUSEUMS_AND_GALLERIES
    }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it) }
