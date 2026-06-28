package com.iicytower.wanderlist.data.remote.wikidata

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.core.util.calculateDistanceKm
import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.data.remote.wikidata.dto.SparqlBinding
import com.iicytower.wanderlist.data.remote.wikidata.dto.SparqlResponse
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

private val WIKIDATA_TYPE_TO_CATEGORY: Map<String, AttractionCategory> = mapOf(
    // Zamki i fortyfikacje
    "Q23413" to AttractionCategory.CASTLES_AND_FORTIFICATIONS,
    "Q43501" to AttractionCategory.CASTLES_AND_FORTIFICATIONS,
    "Q16560" to AttractionCategory.CASTLES_AND_FORTIFICATIONS,
    "Q1081138" to AttractionCategory.CASTLES_AND_FORTIFICATIONS,
    "Q57821" to AttractionCategory.CASTLES_AND_FORTIFICATIONS,
    // Koscioly i sakralia
    "Q16970" to AttractionCategory.CHURCHES_AND_SACRED,
    "Q2977" to AttractionCategory.CHURCHES_AND_SACRED,
    "Q160742" to AttractionCategory.CHURCHES_AND_SACRED,
    "Q44613" to AttractionCategory.CHURCHES_AND_SACRED,
    "Q663514" to AttractionCategory.CHURCHES_AND_SACRED,
    "Q317557" to AttractionCategory.CHURCHES_AND_SACRED,
    // Muzea i galerie
    "Q33506" to AttractionCategory.MUSEUMS_AND_GALLERIES,
    "Q207694" to AttractionCategory.MUSEUMS_AND_GALLERIES,
    "Q1007870" to AttractionCategory.MUSEUMS_AND_GALLERIES,
    // Ruiny i archeologiczne
    "Q839954" to AttractionCategory.RUINS_AND_ARCHAEOLOGICAL,
    "Q2065736" to AttractionCategory.RUINS_AND_ARCHAEOLOGICAL,
    "Q4989906" to AttractionCategory.RUINS_AND_ARCHAEOLOGICAL,
    // Przyroda i parki
    "Q22698" to AttractionCategory.NATURE_AND_PARKS,
    "Q179049" to AttractionCategory.NATURE_AND_PARKS,
    "Q473972" to AttractionCategory.NATURE_AND_PARKS,
    "Q56061" to AttractionCategory.NATURE_AND_PARKS,
    "Q1437459" to AttractionCategory.NATURE_AND_PARKS,
    // Punkty widokowe
    "Q12518" to AttractionCategory.VIEWPOINTS,
    "Q1440300" to AttractionCategory.VIEWPOINTS,
    // Militaria
    "Q57733" to AttractionCategory.MILITARY,
    "Q644371" to AttractionCategory.MILITARY,
    // Miejsca pamieci
    "Q5003624" to AttractionCategory.MEMORIALS_AND_CEMETERIES,
    "Q39614" to AttractionCategory.MEMORIALS_AND_CEMETERIES,
    "Q60184" to AttractionCategory.MEMORIALS_AND_CEMETERIES,
    // Jaskinie i geologia
    "Q35509" to AttractionCategory.CAVES_AND_GEOLOGY,
    "Q695850" to AttractionCategory.CAVES_AND_GEOLOGY
)

private val ALLOWED_TYPES = WIKIDATA_TYPE_TO_CATEGORY.keys.joinToString(",") { "wd:$it" }

class WikidataSparqlSource(private val httpClient: HttpClient) : RemoteAttractionSource {

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        val query = buildQuery(params.latitude, params.longitude, params.radiusKm)
        val response = httpClient.get("https://query.wikidata.org/sparql") {
            parameter("query", query)
            parameter("format", "json")
            header("User-Agent", "WanderList/1.0 (tourist app; szymonfirkowicz@gmail.com)")
            header("Accept", "application/sparql-results+json")
        }.body<SparqlResponse>()

        val allowedCategories = params.categories.ifEmpty { AttractionCategory.entries.toSet() }

        response.results.bindings
            .mapNotNull { it.toAttraction(params.latitude, params.longitude) }
            .filter { it.category in allowedCategories }
            .distinctBy { it.xid }
    }

    private fun buildQuery(lat: Double, lon: Double, radiusKm: Int): String =
        """
        SELECT DISTINCT ?place ?placeLabel ?lat ?lon ?type WHERE {
          SERVICE wikibase:around {
            ?place wdt:P625 ?coords .
            bd:serviceParam wikibase:center "Point($lon $lat)"^^geo:wktLiteral .
            bd:serviceParam wikibase:radius "$radiusKm" .
          }
          ?place wdt:P31 ?type .
          FILTER(?type IN ($ALLOWED_TYPES))
          BIND(geof:latitude(?coords) AS ?lat)
          BIND(geof:longitude(?coords) AS ?lon)
          SERVICE wikibase:label { bd:serviceParam wikibase:language "pl,en". }
        }
        LIMIT 100
        """.trimIndent()
}

private fun SparqlBinding.toAttraction(searchLat: Double, searchLon: Double): Attraction? {
    val name = placeLabel?.value?.takeIf { it.isNotBlank() && !it.startsWith("Q") } ?: return null
    val lat = lat?.value?.toDoubleOrNull() ?: return null
    val lon = lon?.value?.toDoubleOrNull() ?: return null
    val qid = place?.value?.substringAfterLast("/") ?: return null
    val typeQid = type?.value?.substringAfterLast("/") ?: ""
    val category = WIKIDATA_TYPE_TO_CATEGORY[typeQid] ?: AttractionCategory.MUSEUMS_AND_GALLERIES
    return Attraction(
        xid = "wd$qid",
        name = name,
        latitude = lat,
        longitude = lon,
        category = category,
        isInMyList = false,
        dateAddedToList = null,
        description = null,
        descriptionSources = emptyList(),
        isFromLastSearch = true,
        distanceKm = calculateDistanceKm(searchLat, searchLon, lat, lon)
    )
}
