package com.iicytower.wanderlist.feature.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolCallRef
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.feature.assistant.AssistantToolDefs
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val llmService: LlmService,
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
            var hadToolCall = false
            var hadError = false

            llmService.completeChat(
                conversationHistory.toList(),
                settings.systemPromptAssistant,
                AssistantToolDefs.ALL
            ).fold(
                onSuccess = { events ->
                    var accumulatedText = ""
                    val toolCalls = events.filterIsInstance<LlmEvent.ToolCall>()

                    if (toolCalls.isNotEmpty()) {
                        // Per OpenAI spec: assistant message with tool_calls must precede tool results
                        conversationHistory.add(ChatMessage.AssistantWithToolCalls(
                            toolCalls.map { ToolCallRef(it.id, it.name, it.rawArguments) }
                        ))
                        hadToolCall = true
                    }

                    events.forEach { event ->
                        when (event) {
                            is LlmEvent.TextChunk -> accumulatedText += event.text
                            is LlmEvent.ToolCall -> {
                                val result = executeTool(event.name, event.arguments)
                                conversationHistory.add(ChatMessage.ToolResult(event.id, result))
                            }
                            is LlmEvent.Done -> {
                                if (accumulatedText.isNotEmpty()) {
                                    val assistantMsg = ChatMessage.Assistant(accumulatedText)
                                    conversationHistory.add(assistantMsg)
                                    _uiState.update { it.copy(messages = it.messages + assistantMsg) }
                                }
                            }
                            is LlmEvent.Error -> {
                                hadError = true
                                _uiState.update { it.copy(
                                    messages = it.messages + ChatMessage.Error(event.message)
                                ) }
                            }
                        }
                    }
                },
                onFailure = { e ->
                    hadError = true
                    _uiState.update { it.copy(
                        messages = it.messages + ChatMessage.Error(e.message ?: "Błąd połączenia")
                    ) }
                }
            )

            continueLoop = hadToolCall && !hadError
        }

        _uiState.update { it.copy(isProcessing = false, streamingText = "") }
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
                val result = getMyListUseCase().first().toToolResultString()
                Timber.tag("Assistant").d("get_my_list result: %s", result.take(300))
                result
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
