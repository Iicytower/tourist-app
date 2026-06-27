package com.iicytower.wanderlist.domain.model

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Assistant(val text: String) : ChatMessage()
    data class ToolResult(val toolCallId: String, val content: String) : ChatMessage()
    data class Error(val message: String) : ChatMessage()
}
