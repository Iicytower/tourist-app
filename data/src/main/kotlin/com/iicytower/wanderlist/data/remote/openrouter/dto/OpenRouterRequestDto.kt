package com.iicytower.wanderlist.data.remote.openrouter.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val tools: List<OpenRouterTool>? = null,
    val stream: Boolean = true
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenRouterMessageToolCall>? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OpenRouterMessageToolCall(
    val id: String,
    @EncodeDefault val type: String = "function",
    val function: OpenRouterMessageFunction
)

@Serializable
data class OpenRouterMessageFunction(
    val name: String,
    val arguments: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OpenRouterTool(
    @EncodeDefault val type: String = "function",
    val function: OpenRouterFunction
)

@Serializable
data class OpenRouterFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
