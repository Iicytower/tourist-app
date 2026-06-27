package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.remote.overpass.OverpassApiClient
import com.iicytower.wanderlist.domain.model.SearchParams
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OverpassApiClientTest {

    private val sampleResponse = """
        {
          "elements": [
            {
              "type": "node",
              "id": 123,
              "lat": 50.0619,
              "lon": 19.9369,
              "tags": {
                "name": "Zamek Wawelski",
                "historic": "castle"
              }
            },
            {
              "type": "node",
              "id": 456,
              "lat": 50.065,
              "lon": 19.940,
              "tags": {
                "tourism": "museum"
              }
            },
            {
              "type": "way",
              "id": 789,
              "center": {"lat": 50.070, "lon": 19.945},
              "tags": {
                "name": "Muzeum Czartoryskich",
                "tourism": "museum"
              }
            }
          ]
        }
    """.trimIndent()

    private fun buildClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    private val params = SearchParams(
        latitude = 50.0619,
        longitude = 19.9369,
        radiusKm = 5,
        categories = emptySet()
    )

    @Test
    fun searchAttractions_parsesNamedElements() = runTest {
        val client = OverpassApiClient(buildClient(sampleResponse))
        val result = client.searchAttractions(params)
        assertTrue(result.isSuccess)
        val attractions = result.getOrThrow()
        assertEquals(2, attractions.size)
        assertTrue(attractions.any { it.name == "Zamek Wawelski" })
        assertTrue(attractions.any { it.name == "Muzeum Czartoryskich" })
    }

    @Test
    fun searchAttractions_skipsElementsWithoutName() = runTest {
        val client = OverpassApiClient(buildClient(sampleResponse))
        val result = client.searchAttractions(params)
        val attractions = result.getOrThrow()
        assertTrue(attractions.none { it.xid == "n456" })
    }

    @Test
    fun searchAttractions_assignsCorrectCategory() = runTest {
        val client = OverpassApiClient(buildClient(sampleResponse))
        val attractions = client.searchAttractions(params).getOrThrow()
        val castle = attractions.find { it.name == "Zamek Wawelski" }
        assertEquals(AttractionCategory.CASTLES_AND_FORTIFICATIONS, castle?.category)
    }

    @Test
    fun searchAttractions_returnsFailure_onHttpError() = runTest {
        val client = OverpassApiClient(buildClient("", HttpStatusCode.TooManyRequests))
        val result = client.searchAttractions(params)
        assertTrue(result.isFailure)
    }

    @Test
    fun searchAttractions_deduplicates_byXid() = runTest {
        val duplicateResponse = """
            {"elements": [
              {"type":"node","id":1,"lat":50.0,"lon":20.0,"tags":{"name":"A","historic":"castle"}},
              {"type":"node","id":1,"lat":50.0,"lon":20.0,"tags":{"name":"A","historic":"castle"}}
            ]}
        """.trimIndent()
        val client = OverpassApiClient(buildClient(duplicateResponse))
        val attractions = client.searchAttractions(params).getOrThrow()
        assertEquals(1, attractions.size)
    }
}
