package com.iicytower.wanderlist.data.remote.overpass

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.data.remote.overpass.dto.OverpassElement
import com.iicytower.wanderlist.data.remote.overpass.dto.OverpassResponse
import com.iicytower.wanderlist.data.remote.overpass.mapper.toAttraction
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

// Publiczny Overpass API ogranicza do 2 równoczesnych zapytań z jednego IP (patrz /api/status).
private const val MAX_CONCURRENT_REQUESTS = 2

class OverpassApiClient(private val httpClient: HttpClient) : RemoteAttractionSource {

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        val queries = OverpassQueryBuilder.buildQueriesPerCategory(params)
        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        coroutineScope {
            queries.map { (category, query) ->
                async {
                    semaphore.withPermit {
                        runQuery(query).getOrElse { e ->
                            Timber.tag("OverpassApiClient").w(e, "Zapytanie dla kategorii %s nie powiodło się", category)
                            emptyList()
                        }
                    }
                }
            }.awaitAll().flatten()
                .mapNotNull { it.toAttraction(params.latitude, params.longitude, params.language) }
                .distinctBy { it.xid }
        }
    }

    private suspend fun runQuery(query: String): Result<List<OverpassElement>> = runCatching {
        httpClient.post(OVERPASS_URL) {
            setBody(FormDataContent(Parameters.build { append("data", query) }))
            timeout { requestTimeoutMillis = 28_000 }
        }.body<OverpassResponse>().elements
    }
}
