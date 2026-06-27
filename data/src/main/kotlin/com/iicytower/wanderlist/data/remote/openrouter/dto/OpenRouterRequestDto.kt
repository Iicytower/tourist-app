package com.iicytower.wanderlist.data.remote.openrouter.dto

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
    val content: String,
    @SerialName("tool_call_id") val toolCallId: String? = null
)

@Serializable
data class OpenRouterTool(
    val type: String = "function",
    val function: OpenRouterFunction
)

@Serializable
data class OpenRouterFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
