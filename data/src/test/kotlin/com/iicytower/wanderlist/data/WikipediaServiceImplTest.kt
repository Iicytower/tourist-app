package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.data.remote.wikipedia.WikipediaServiceImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WikipediaServiceImplTest {

    private fun makeService(statusEn: HttpStatusCode, bodyEn: String, statusPl: HttpStatusCode = HttpStatusCode.NotFound, bodyPl: String = "{}"): WikipediaServiceImpl {
        var callCount = 0
        val mockEngine = MockEngine {
            callCount++
            if (callCount == 1) respond(bodyEn, statusEn, headersOf("Content-Type", ContentType.Application.Json.toString()))
            else respond(bodyPl, statusPl, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return WikipediaServiceImpl(client)
    }

    private val validBody = """{"title":"Wawel","extract":"Zamek Wawelski to...","content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Wawel"}}}"""

    @Test
    fun articleFound_returnsResultSuccess() = runTest {
        val service = makeService(HttpStatusCode.OK, validBody)
        val result = service.getArticle("Wawel")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.extract.contains("Zamek"))
    }

    @Test
    fun articleNotFound_returnsSuccessNull() = runTest {
        val service = makeService(HttpStatusCode.NotFound, "{}", HttpStatusCode.NotFound, "{}")
        val result = service.getArticle("NieistniejaceAtrakcja12345")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun englishNotFound_triesPl_returnsResult() = runTest {
        val plBody = """{"title":"Wawel","extract":"Zamek Wawelski...","content_urls":{"desktop":{"page":"https://pl.wikipedia.org/wiki/Wawel"}}}"""
        val service = makeService(HttpStatusCode.NotFound, "{}", HttpStatusCode.OK, plBody)
        val result = service.getArticle("Wawel")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun serverError_returnsFailure() = runTest {
        val service = makeService(HttpStatusCode.InternalServerError, "{}", HttpStatusCode.InternalServerError, "{}")
        val result = service.getArticle("test")
        assertTrue(result.isFailure)
    }
}
