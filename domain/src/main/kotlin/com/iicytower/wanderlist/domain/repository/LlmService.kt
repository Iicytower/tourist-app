package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

interface LlmService {
    fun streamResponse(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<ToolDefinition>
    ): Flow<LlmEvent>

    suspend fun testConnection(): Result<String>
}
