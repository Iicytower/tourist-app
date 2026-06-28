package com.iicytower.wanderlist.data.remote.openrouter.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterStreamChunk(
    val choices: List<OpenRouterChoice> = emptyList()
)

@Serializable
data class OpenRouterCompleteResponse(
    val choices: List<OpenRouterCompleteChoice> = emptyList()
)

@Serializable
data class OpenRouterCompleteChoice(
    val message: OpenRouterCompleteMessage = OpenRouterCompleteMessage()
)

@Serializable
data class OpenRouterCompleteMessage(
    val content: String? = null
)

@Serializable
data class OpenRouterChoice(
    val delta: OpenRouterDelta = OpenRouterDelta()
)

@Serializable
data class OpenRouterDelta(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenRouterToolCall>? = null
)

@Serializable
data class OpenRouterToolCall(
    val id: String = "",
    val function: OpenRouterFunctionCall = OpenRouterFunctionCall()
)

@Serializable
data class OpenRouterFunctionCall(
    val name: String = "",
    val arguments: String = ""
)
