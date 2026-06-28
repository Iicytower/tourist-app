package com.iicytower.wanderlist.data.remote.openrouter

import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterCompleteResponse
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterRequest
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterStreamChunk
import com.iicytower.wanderlist.data.remote.openrouter.mapper.toLlmEvent
import com.iicytower.wanderlist.data.remote.openrouter.mapper.toOpenRouterMessages
import com.iicytower.wanderlist.data.remote.openrouter.mapper.toOpenRouterTools
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import timber.log.Timber

class OpenRouterLlmService(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) : LlmService {

    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"
    private val json = Json { ignoreUnknownKeys = true }

    override fun streamResponse(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<ToolDefinition>
    ): Flow<LlmEvent> = flow {
        val settings = settingsRepository.getSettings().first()
        val apiKey = settings.openRouterApiKey
        if (apiKey.isBlank()) {
            emit(LlmEvent.Error("Brak klucza API OpenRouter. Przejdz do Ustawien i dodaj klucz."))
            return@flow
        }
        val model = settings.aiModel

        val requestBody = OpenRouterRequest(
            model = model,
            messages = messages.toOpenRouterMessages(systemPrompt),
            tools = if (tools.isEmpty()) null else tools.toOpenRouterTools(),
            stream = true
        )

        runCatching {
            val response = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("HTTP-Referer", "https://wanderlist.app")
                setBody(requestBody)
                timeout { requestTimeoutMillis = Long.MAX_VALUE }
            }

            if (!response.status.isSuccess()) {
                val errorMsg = when (response.status.value) {
                    401 -> "Nieprawidlowy klucz API OpenRouter (401). Sprawdz klucz w Ustawieniach."
                    402 -> "Brak srodkow na koncie OpenRouter (402). Doladuj konto."
                    429 -> "Przekroczono limit zapytan OpenRouter (429). Sprobuj pozniej."
                    500, 502, 503 -> "Serwer OpenRouter jest chwilowo niedostepny (${response.status.value}). Sprobuj pozniej."
                    else -> "Blad serwera OpenRouter: ${response.status.value}"
                }
                emit(LlmEvent.Error(errorMsg))
                return@runCatching
            }

            val channel = response.bodyAsChannel()
            var lineCount = 0
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                lineCount++
                Timber.tag("OpenRouter").v("SSE[%d]: %s", lineCount, line.take(120))
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                if (data.isBlank()) continue

                runCatching {
                    val chunk = json.decodeFromString<OpenRouterStreamChunk>(data)
                    chunk.toLlmEvent()
                }.onFailure { e ->
                    Timber.tag("OpenRouter").w(e, "SSE parse failed: %s", data.take(200))
                }.getOrNull()?.let { emit(it) }
            }
            Timber.tag("OpenRouter").d("SSE done, read %d lines", lineCount)
            emit(LlmEvent.Done)
        }.onFailure { e ->
            emit(LlmEvent.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun complete(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Result<String> = runCatching {
        val settings = settingsRepository.getSettings().first()
        val apiKey = settings.openRouterApiKey
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Brak klucza API OpenRouter. Wejdz w Ustawienia i zapisz klucz."))

        val requestBody = OpenRouterRequest(
            model = settings.aiModel,
            messages = messages.toOpenRouterMessages(systemPrompt),
            stream = false
        )

        val response = httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("HTTP-Referer", "https://wanderlist.app")
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            error("OpenRouter HTTP ${response.status.value}")
        }

        val body = response.body<OpenRouterCompleteResponse>()
        body.choices.firstOrNull()?.message?.content?.trim()
            ?: error("Pusta odpowiedź od modelu")
    }

    override suspend fun testConnection(): Result<String> = runCatching {
        val settings = settingsRepository.getSettings().first()
        val apiKey = settings.openRouterApiKey
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Brak klucza API OpenRouter. Wejdz w Ustawienia i zapisz klucz."))

        val requestBody = OpenRouterRequest(
            model = settings.aiModel,
            messages = listOf(
                com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterMessage(role = "user", content = "Reply with one word: OK")
            ),
            stream = false
        )

        val response = httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("HTTP-Referer", "https://wanderlist.app")
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val detail = when (response.status.value) {
                401 -> "Nieprawidlowy klucz API (401)"
                402 -> "Brak srodkow na koncie OpenRouter (402)"
                429 -> "Przekroczono limit zapytan (429)"
                else -> "HTTP ${response.status.value}: $body"
            }
            error(detail)
        }
        body
    }
}
