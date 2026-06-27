package com.iicytower.wanderlist.feature.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.usecase.SendChatMessageUseCase
import com.iicytower.wanderlist.feature.assistant.AssistantToolDefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val getMyListUseCase: GetMyListUseCase,
    private val webSearchService: WebSearchService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()

    fun updateInput(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.currentInput.trim()
        if (text.isBlank() || _uiState.value.isProcessing) return

        val userMsg = ChatMessage.User(text)
        conversationHistory.add(userMsg)
        _uiState.update { it.copy(
            messages = it.messages + userMsg,
            currentInput = "",
            isProcessing = true,
            streamingText = ""
        ) }

        viewModelScope.launch {
            runChatLoop()
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun confirmClearChat() {
        conversationHistory.clear()
        _uiState.update { AssistantUiState() }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    private suspend fun runChatLoop() {
        val settings = settingsRepository.getSettings().first()
        var continueLoop = true

        while (continueLoop) {
            val accumulatedText = StringBuilder()
            var hadToolCall = false
            var hadError = false

            sendChatMessageUseCase(
                conversationHistory.toList(),
                settings.systemPromptAssistant,
                AssistantToolDefs.ALL
            ).collect { event ->
                when (event) {
                    is LlmEvent.TextChunk -> {
                        accumulatedText.append(event.text)
                        _uiState.update { it.copy(streamingText = accumulatedText.toString()) }
                    }
                    is LlmEvent.ToolCall -> {
                        hadToolCall = true
                        val result = executeTool(event.name, event.arguments)
                        conversationHistory.add(ChatMessage.ToolResult(event.id, result))
                    }
                    is LlmEvent.Done -> {
                        if (accumulatedText.isNotEmpty()) {
                            val assistantMsg = ChatMessage.Assistant(accumulatedText.toString())
                            conversationHistory.add(assistantMsg)
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages + assistantMsg,
                                    streamingText = ""
                                )
                            }
                        }
                    }
                    is LlmEvent.Error -> {
                        hadError = true
                        val errorMsg = ChatMessage.Error(event.message)
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + errorMsg,
                                streamingText = ""
                            )
                        }
                    }
                }
            }

            continueLoop = hadToolCall && !hadError
        }

        _uiState.update { it.copy(isProcessing = false) }
    }

    private suspend fun executeTool(name: String, args: Map<String, Any>): String {
        return when (name) {
            "search_attractions" -> {
                val lat = (args["latitude"] as? Number)?.toDouble() ?: return "Brak wspolrzednych"
                val lon = (args["longitude"] as? Number)?.toDouble() ?: return "Brak wspolrzednych"
                val radius = (args["radius_km"] as? Number)?.toInt() ?: 10
                val categories = (args["categories"] as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.mapNotNull { runCatching { AttractionCategory.valueOf(it) }.getOrNull() }
                    ?.toSet()
                    ?: emptySet()
                searchAttractionsUseCase(SearchParams(lat, lon, radius, categories))
                    .fold(
                        onSuccess = { it.toToolResultString() },
                        onFailure = { "Blad wyszukiwania: ${it.message}" }
                    )
            }
            "web_search" -> {
                val query = args["query"] as? String ?: return "Brak zapytania"
                webSearchService.search(query).getOrElse { "Blad wyszukiwania: ${it.message}" }
            }
            "get_my_list" -> {
                getMyListUseCase().first().toToolResultString()
            }
            else -> "Nieznane narzedzie: $name"
        }
    }

    private fun List<Attraction>.toToolResultString(): String {
        if (isEmpty()) return "Brak wynikow."
        return joinToString("\n") { a ->
            "Nazwa: ${a.name} | Kategoria: ${a.category.displayName} | Lat: ${a.latitude} | Lon: ${a.longitude}"
        }
    }
}
