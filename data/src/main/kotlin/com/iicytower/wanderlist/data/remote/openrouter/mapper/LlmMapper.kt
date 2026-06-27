package com.iicytower.wanderlist.data.remote.openrouter.mapper

import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterDelta
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterFunction
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterMessage
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterStreamChunk
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterTool
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val json = Json { ignoreUnknownKeys = true }

fun List<ChatMessage>.toOpenRouterMessages(systemPrompt: String): List<OpenRouterMessage> {
    val system = OpenRouterMessage(role = "system", content = systemPrompt)
    val rest = map { msg ->
        when (msg) {
            is ChatMessage.User -> OpenRouterMessage(role = "user", content = msg.text)
            is ChatMessage.Assistant -> OpenRouterMessage(role = "assistant", content = msg.text)
            is ChatMessage.ToolResult -> OpenRouterMessage(role = "tool", content = msg.content, toolCallId = msg.toolCallId)
            is ChatMessage.Error -> OpenRouterMessage(role = "assistant", content = "[Error: ${msg.message}]")
        }
    }
    return listOf(system) + rest
}

fun List<ToolDefinition>.toOpenRouterTools(): List<OpenRouterTool> = map { tool ->
    OpenRouterTool(
        function = OpenRouterFunction(
            name = tool.name,
            description = tool.description,
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    tool.parameters.forEach { (name, schema) ->
                        putJsonObject(name) {
                            @Suppress("UNCHECKED_CAST")
                            (schema as? Map<String, Any>)?.forEach { (k, v) ->
                                put(k, v.toString())
                            }
                        }
                    }
                }
            }
        )
    )
}

fun OpenRouterStreamChunk.toLlmEvent(): LlmEvent? {
    val delta: OpenRouterDelta = choices.firstOrNull()?.delta ?: return null
    return when {
        !delta.toolCalls.isNullOrEmpty() -> {
            val call = delta.toolCalls.first()
            val args = runCatching {
                val parsed = json.parseToJsonElement(call.function.arguments).jsonObject
                parsed.entries.associate { (k, v) ->
                    k to (v as? JsonPrimitive)?.content as Any
                }
            }.getOrDefault(emptyMap())
            LlmEvent.ToolCall(id = call.id, name = call.function.name, arguments = args)
        }
        delta.content != null -> LlmEvent.TextChunk(delta.content)
        else -> null
    }
}
