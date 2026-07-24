package com.iicytower.wanderlist.feature.assistant.viewmodel

import com.iicytower.wanderlist.domain.model.ChatMessage

/** Dane akcji do potwierdzenia — tekst dialogu buduje UI z zasobów (lokalizacja). */
sealed interface PendingToolConfirmation {
    data class RemoveFromList(val attractionName: String, val listName: String) : PendingToolConfirmation
    data class UpdateTripPlan(val listName: String) : PendingToolConfirmation
}

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isProcessing: Boolean = false,
    val streamingText: String = "",
    val showClearConfirmation: Boolean = false,
    val contextListId: Long? = null,
    val pendingConfirmation: PendingToolConfirmation? = null
)
