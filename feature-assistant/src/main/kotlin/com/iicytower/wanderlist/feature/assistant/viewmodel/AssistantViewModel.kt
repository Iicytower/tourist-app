package com.iicytower.wanderlist.feature.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.SearchParams
import com.iicytower.wanderlist.domain.model.ToolCallRef
import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore
import com.iicytower.wanderlist.feature.assistant.AssistantToolDefs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
    private val getTripListsUseCase: GetTripListsUseCase,
    private val getAttractionsForListUseCase: GetAttractionsForListUseCase,
    private val addToTripListUseCase: AddToTripListUseCase,
    private val removeFromTripListUseCase: RemoveFromTripListUseCase,
    private val createTripListUseCase: CreateTripListUseCase,
    private val webSearchService: WebSearchService,
    private val settingsRepository: SettingsRepository,
    private val getTripPlanUseCase: GetTripPlanUseCase,
    private val generateTripPlanUseCase: GenerateTripPlanUseCase,
    private val tripPlanRevertStore: TripPlanRevertStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()

    private var pendingDecision: CompletableDeferred<Boolean>? = null

    // Plan wycieczki pobrany w tle po wejściu z "Omów z asystentem" (FEAT-14) — wstrzykiwany
    // ukrycie do pierwszej wiadomości wysłanej do LLM, żeby asystent od razu znał kontekst.
    private var planContextDeferred: Deferred<String?>? = null
    private var planContextPending = false

    fun setContextList(listId: Long) {
        // no-op if already set — prevents re-trigger on recomposition
        if (_uiState.value.contextListId == listId) return
        _uiState.update { it.copy(contextListId = listId) }
        planContextPending = true
        planContextDeferred = viewModelScope.async { formatTripPlan(listId) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.currentInput.trim()
        if (text.isBlank() || _uiState.value.isProcessing) return

        _uiState.update { it.copy(
            messages = it.messages + ChatMessage.User(text),
            currentInput = "",
            isProcessing = true,
            streamingText = ""
        ) }

        viewModelScope.launch {
            val planContext = if (planContextPending) {
                planContextPending = false
                planContextDeferred?.await()
            } else null
            conversationHistory.add(
                if (planContext != null) {
                    ChatMessage.User("$planContext\n\nPytanie użytkownika: $text")
                } else {
                    ChatMessage.User(text)
                }
            )
            runChatLoop()
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun confirmClearChat() {
        pendingDecision?.complete(false)
        conversationHistory.clear()
        // historia wyczyszczona — jeśli wciąż jesteśmy w kontekście planu, wstrzyknij go ponownie
        planContextPending = planContextDeferred != null
        _uiState.update { AssistantUiState() }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun confirmPendingAction() {
        pendingDecision?.complete(true)
    }

    fun rejectPendingAction() {
        pendingDecision?.complete(false)
    }

    /** Wstrzymuje pętlę czatu do decyzji użytkownika w dialogu potwierdzenia. */
    private suspend fun awaitUserConfirmation(confirmation: PendingToolConfirmation): Boolean {
        val decision = CompletableDeferred<Boolean>()
        pendingDecision = decision
        _uiState.update { it.copy(pendingConfirmation = confirmation) }
        val approved = decision.await()
        pendingDecision = null
        _uiState.update { it.copy(pendingConfirmation = null) }
        return approved
    }

    private suspend fun runChatLoop() {
        val settings = settingsRepository.getSettings().first()
        var continueLoop = true

        while (continueLoop) {
            var hadToolCall = false
            var hadError = false

            llmService.completeChat(
                conversationHistory.toList(),
                settings.systemPromptAssistant + "\nJęzyk odpowiedzi: ${settings.appLanguage}.",
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
            "get_trip_lists" -> {
                val result = getTripListsUseCase().first().toTripListsString()
                Timber.tag("Assistant").d("get_trip_lists result: %s", result.take(300))
                result
            }
            "get_list_attractions" -> {
                val listId = (args["list_id"] as? Number)?.toLong() ?: return "Brak list_id"
                val result = getAttractionsForListUseCase(listId).first().toToolResultString()
                Timber.tag("Assistant").d("get_list_attractions(id=%d) result: %s", listId, result.take(300))
                result
            }
            "add_to_list" -> {
                val xid = args["xid"] as? String ?: return "Brak xid"
                val listId = (args["list_id"] as? Number)?.toLong() ?: return "Brak list_id"
                addToTripListUseCase(xid, listId).fold(
                    onSuccess = { "Dodano atrakcje $xid do listy $listId." },
                    onFailure = { "Blad dodawania: ${it.message}" }
                )
            }
            "remove_from_list" -> {
                val xid = args["xid"] as? String ?: return "Brak xid"
                val listId = (args["list_id"] as? Number)?.toLong() ?: return "Brak list_id"
                val attractionName = runCatching {
                    getAttractionsForListUseCase(listId).first().find { it.xid == xid }?.name
                }.getOrNull() ?: xid
                val listName = runCatching {
                    getTripListsUseCase().first().find { it.id == listId }?.name
                }.getOrNull() ?: "id=$listId"
                val approved = awaitUserConfirmation(
                    PendingToolConfirmation.RemoveFromList(attractionName, listName)
                )
                if (!approved) return "Użytkownik odmówił usunięcia atrakcji $xid z listy $listId. Nie ponawiaj tej operacji bez wyraźnej prośby użytkownika."
                removeFromTripListUseCase(xid, listId).fold(
                    onSuccess = { "Usunieto atrakcje $xid z listy $listId." },
                    onFailure = { "Blad usuwania: ${it.message}" }
                )
            }
            "create_list" -> {
                val name = args["name"] as? String ?: return "Brak nazwy listy"
                createTripListUseCase(name).fold(
                    onSuccess = { newId -> "Utworzono liste \"$name\" (id=$newId)." },
                    onFailure = { "Blad tworzenia listy: ${it.message}" }
                )
            }
            "get_trip_plan" -> {
                val listId = (args["list_id"] as? Number)?.toLong() ?: return "Brak list_id"
                formatTripPlan(listId) ?: "Brak planu wycieczki dla listy $listId."
            }
            "update_trip_plan" -> {
                val listId = (args["list_id"] as? Number)?.toLong() ?: return "Brak list_id"
                val planJson = args["plan_json"] as? String ?: return "Brak plan_json"
                val listName = runCatching {
                    getTripListsUseCase().first().find { it.id == listId }?.name
                }.getOrNull() ?: "id=$listId"
                val approved = awaitUserConfirmation(
                    PendingToolConfirmation.UpdateTripPlan(listName)
                )
                if (!approved) return "Użytkownik odmówił nadpisania planu wycieczki listy $listId. Nie ponawiaj tej operacji bez wyraźnej prośby użytkownika."
                val previousPlan = runCatching { getTripPlanUseCase(listId).first }.getOrNull()
                generateTripPlanUseCase.updateFromJson(listId, planJson).fold(
                    onSuccess = {
                        previousPlan?.let { tripPlanRevertStore.store(listId, it) }
                        "Plan wycieczki dla listy $listId zaktualizowany."
                    },
                    onFailure = { "Blad aktualizacji planu: ${it.message}" }
                )
            }
            else -> "Nieznane narzedzie: $name"
        }
    }

    private suspend fun formatTripPlan(listId: Long): String? {
        val (plan, notes) = getTripPlanUseCase(listId)
        if (plan == null) return null
        return buildString {
            appendLine("Plan wycieczki dla listy $listId:")
            plan.days.forEach { day ->
                appendLine(day.label)
                day.points.forEach { point ->
                    append("  - ${point.name}")
                    if (!point.note.isNullOrBlank()) append(" (${point.note})")
                    appendLine()
                }
            }
            if (!notes.isNullOrBlank()) appendLine("Notatki użytkownika: $notes")
        }
    }

    private fun List<Attraction>.toToolResultString(): String {
        if (isEmpty()) return "Brak wynikow."
        return joinToString("\n") { a ->
            "Nazwa: ${a.name} | Kategoria: ${a.category.displayName} | Lat: ${a.latitude} | Lon: ${a.longitude}"
        }
    }

    private fun List<TripList>.toTripListsString(): String {
        if (isEmpty()) return "Brak list wycieczek."
        return mapIndexed { i, list ->
            "Lista ${i + 1} (id=${list.id}): ${list.name} — ${list.attractionCount} atrakcji"
        }.joinToString("\n")
    }
}
