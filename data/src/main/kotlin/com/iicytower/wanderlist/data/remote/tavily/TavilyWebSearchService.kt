package com.iicytower.wanderlist.data.remote.tavily

import com.iicytower.wanderlist.data.remote.tavily.dto.TavilySearchRequest
import com.iicytower.wanderlist.data.remote.tavily.dto.TavilySearchResponse
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first

class TavilyWebSearchService(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) : WebSearchService {

    private val baseUrl = "https://api.tavily.com/search"

    override suspend fun search(query: String): Result<String> {
        val settings = settingsRepository.getSettings().first()
        val apiKey = settings.tavilyApiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Brak klucza API Tavily. Przejdz do Ustawien i dodaj klucz."))
        }
        return runCatching {
            val response = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(TavilySearchRequest(query = query, apiKey = apiKey))
            }
            when {
                response.status.value == 401 -> error("Nieprawidłowy klucz Tavily API")
                response.status.value == 429 -> error("Przekroczono limit zapytań Tavily")
                !response.status.isSuccess() -> error("Błąd Tavily HTTP ${response.status.value}")
                else -> {
                    val body: TavilySearchResponse = response.body()
                    formatResults(body)
                }
            }
        }.onSuccess {
            settingsRepository.incrementTavilyUsage()
        }
    }

    private fun formatResults(response: TavilySearchResponse): String =
        response.results.mapIndexed { index, result ->
            "[${index + 1}] Title: ${result.title}\nSource: ${result.url}\n${result.content}"
        }.joinToString("\n\n")
}
