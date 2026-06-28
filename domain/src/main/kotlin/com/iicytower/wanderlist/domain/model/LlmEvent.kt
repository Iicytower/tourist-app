package com.iicytower.wanderlist.domain.model

sealed class LlmEvent {
    data class TextChunk(val text: String) : LlmEvent()
    data class ToolCall(val id: String, val name: String, val arguments: Map<String, Any>, val rawArguments: String = "{}") : LlmEvent()
    data object Done : LlmEvent()
    data class Error(val message: String) : LlmEvent()
}
