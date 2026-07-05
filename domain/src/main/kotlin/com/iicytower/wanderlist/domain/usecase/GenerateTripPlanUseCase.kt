package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.core.agents.AgentPrompts
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolCallRef
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.model.TripPlan
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber

private val TOOL_WEB_SEARCH = ToolDefinition(
    name = "web_search",
    description = "Wyszukaj dodatkowe informacje o atrakcji lub miejscu.",
    parameters = mapOf("query" to mapOf("type" to "string", "description" to "Zapytanie wyszukiwania"))
)

class GenerateTripPlanUseCase(
    private val tripListRepository: TripListRepository,
    private val llmService: LlmService,
    private val webSearchService: WebSearchService
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend operator fun invoke(listId: Long): Result<TripPlan> = runCatching {
        val attractions = tripListRepository.getAttractionsForList(listId).first()
        if (attractions.isEmpty()) error("Lista jest pusta — nie można wygenerować planu")

        val attractionsList = attractions.mapIndexed { i, a ->
            "${i + 1}. ${a.name} | ${a.category.displayName} | lat:${a.latitude} lon:${a.longitude}"
        }.joinToString("\n")

        val userMessage = ChatMessage.User(
            "Wygeneruj plan wycieczki dla następujących atrakcji:\n$attractionsList"
        )
        val history = mutableListOf<ChatMessage>(userMessage)

        var continueLoop = true
        var finalJson: String? = null
        var iterations = 0

        while (continueLoop) {
            iterations++
            if (iterations > AppConstants.MAX_TOOL_CALL_ITERATIONS) {
                error("Nie udało się wygenerować pełnego planu — spróbuj ponownie")
            }

            val events = llmService.completeChat(history, AgentPrompts.tripPlan, listOf(TOOL_WEB_SEARCH))
                .getOrThrow()

            val toolCalls = events.filterIsInstance<LlmEvent.ToolCall>()
            if (toolCalls.isNotEmpty()) {
                history.add(ChatMessage.AssistantWithToolCalls(
                    toolCalls.map { ToolCallRef(it.id, it.name, it.rawArguments) }
                ))
                toolCalls.forEach { call ->
                    val query = call.arguments["query"] as? String ?: ""
                    val result = webSearchService.search(query)
                        .getOrElse { "Błąd wyszukiwania: ${it.message}" }
                    history.add(ChatMessage.ToolResult(call.id, result))
                }
            } else {
                val text = events.filterIsInstance<LlmEvent.TextChunk>().joinToString("") { it.text }
                finalJson = text
                continueLoop = false
            }
        }

        val plan = parsePlan(finalJson)
        tripListRepository.saveTripPlan(listId, plan)
        Timber.tag("GenerateTripPlan").d("Plan saved for listId=%d, days=%d", listId, plan.days.size)
        plan
    }

    suspend fun updateFromJson(listId: Long, planJson: String): Result<TripPlan> = runCatching {
        val plan = parsePlan(planJson)
        tripListRepository.saveTripPlan(listId, plan)
        plan
    }

    private fun parsePlan(raw: String?): TripPlan {
        requireNotNull(raw) { "LLM nie zwrócił odpowiedzi" }
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return json.decodeFromString<TripPlan>(clean)
    }
}
