package com.iicytower.wanderlist.feature.assistant.viewmodel

import com.iicytower.wanderlist.domain.model.ChatMessage

data class PendingToolConfirmation(
    val description: String
)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isProcessing: Boolean = false,
    val streamingText: String = "",
    val showClearConfirmation: Boolean = false,
    val contextListId: Long? = null,
    val pendingConfirmation: PendingToolConfirmation? = null
)
