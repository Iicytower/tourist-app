package com.iicytower.wanderlist.data.remote.overpass

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.data.remote.overpass.dto.OverpassResponse
import com.iicytower.wanderlist.data.remote.overpass.mapper.toAttraction
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

class OverpassApiClient(private val httpClient: HttpClient) : RemoteAttractionSource {

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        val query = OverpassQueryBuilder.build(params)
        val response = httpClient.post(OVERPASS_URL) {
            setBody(FormDataContent(Parameters.build { append("data", query) }))
        }.body<OverpassResponse>()

        response.elements
            .mapNotNull { it.toAttraction(params.latitude, params.longitude) }
            .distinctBy { it.xid }
    }
}
