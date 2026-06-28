package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.LlmService
import kotlinx.coroutines.flow.Flow

class SendChatMessageUseCase(
    private val llmService: LlmService
) {
    operator fun invoke(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<ToolDefinition>
    ): Flow<LlmEvent> = llmService.streamResponse(messages, systemPrompt, tools)
}
