package com.iicytower.wanderlist.feature.assistant.viewmodel

import com.iicytower.wanderlist.domain.model.ChatMessage

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isProcessing: Boolean = false,
    val streamingText: String = "",
    val showClearConfirmation: Boolean = false
)
