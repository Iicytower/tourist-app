package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.data.remote.tavily.TavilyWebSearchService
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TavilyWebSearchServiceTest {

    private fun makeSettings(apiKey: String = "valid-key") = AppSettings(
        openRouterApiKey = "", tavilyApiKey = apiKey,
        aiModel = "model", defaultRadiusKm = 10, descriptionLanguage = "pl",
        userInterests = emptySet(), systemPromptDescription = "", systemPromptAssistant = "",
        tavilyUsageCount = 0, tavilyUsageMonth = "2026-06"
    )

    private fun makeService(responseStatus: HttpStatusCode, responseBody: String): TavilyWebSearchService {
        val mockEngine = MockEngine {
            respond(
                content = responseBody,
                status = responseStatus,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepo.getSettings() } returns flowOf(makeSettings())
        return TavilyWebSearchService(client, settingsRepo)
    }

    @Test
    fun successfulSearch_returnsFormattedText() = runTest {
        val body = """{"results":[{"title":"Zamek Wawelski","url":"https://test.com","content":"Opis zamku","score":0.9}],"answer":null}"""
        val service = makeService(HttpStatusCode.OK, body)
        val result = service.search("Zamek Wawelski Kraków")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.contains("Zamek Wawelski"))
        assertTrue(result.getOrNull()!!.contains("https://test.com"))
    }

    @Test
    fun incrementTavilyUsage_calledOnSuccess() = runTest {
        val body = """{"results":[],"answer":null}"""
        val mockEngine = MockEngine {
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepo.getSettings() } returns flowOf(makeSettings())
        val service = TavilyWebSearchService(client, settingsRepo)
        service.search("test")
        coVerify(exactly = 1) { settingsRepo.incrementTavilyUsage() }
    }

    @Test
    fun incrementTavilyUsage_notCalledOnError() = runTest {
        val service = makeService(HttpStatusCode.InternalServerError, "{}")
        val mockEngine = MockEngine {
            respond("{}", HttpStatusCode.InternalServerError, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepo.getSettings() } returns flowOf(makeSettings())
        val svc = TavilyWebSearchService(client, settingsRepo)
        svc.search("test")
        coVerify(exactly = 0) { settingsRepo.incrementTavilyUsage() }
    }

    @Test
    fun returns401_withMessage() = runTest {
        val service = makeService(HttpStatusCode.Unauthorized, "{}")
        val result = service.search("test")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Nieprawidłowy"))
    }

    @Test
    fun returns429_withMessage() = runTest {
        val service = makeService(HttpStatusCode.TooManyRequests, "{}")
        val result = service.search("test")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("limit"))
    }
}
