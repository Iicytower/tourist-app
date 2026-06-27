package com.iicytower.wanderlist.data.remote.openrouter

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
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

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
            }

            if (!response.status.isSuccess()) {
                emit(LlmEvent.Error("HTTP ${response.status.value}"))
                return@runCatching
            }

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                if (data.isBlank()) continue

                runCatching {
                    val chunk = json.decodeFromString<OpenRouterStreamChunk>(data)
                    chunk.toLlmEvent()
                }.getOrNull()?.let { emit(it) }
            }
            emit(LlmEvent.Done)
        }.onFailure { e ->
            emit(LlmEvent.Error(e.message ?: "Unknown error"))
        }
    }
}
