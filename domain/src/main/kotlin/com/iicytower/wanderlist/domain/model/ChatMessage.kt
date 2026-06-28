package com.iicytower.wanderlist.domain.model

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Assistant(val text: String) : ChatMessage()
    data class AssistantWithToolCalls(val toolCalls: List<ToolCallRef>) : ChatMessage()
    data class ToolResult(val toolCallId: String, val content: String) : ChatMessage()
    data class Error(val message: String) : ChatMessage()
}

data class ToolCallRef(val id: String, val name: String, val arguments: String)
