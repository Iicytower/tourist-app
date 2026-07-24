package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.core.agents.AgentPrompts
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolCallRef
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.repository.WikipediaService
import kotlinx.coroutines.flow.first
import timber.log.Timber

private val TOOL_WEB_SEARCH = ToolDefinition(
    name = "web_search",
    description = "Wyszukaj informacje w internecie o obiekcie.",
    parameters = mapOf("query" to mapOf("type" to "string", "description" to "Zapytanie wyszukiwania"))
)

private val TOOL_WIKIPEDIA = ToolDefinition(
    name = "wikipedia",
    description = "Pobierz streszczenie artykułu Wikipedii o podanym tytule.",
    parameters = mapOf("title" to mapOf("type" to "string", "description" to "Tytuł artykułu"))
)

/**
 * Dedykowany agent do dopytywania o konkretną atrakcję — iteracyjna pętla
 * tool-calling (web_search + wikipedia) z limitem [AppConstants.MAX_TOOL_CALL_ITERATIONS].
 * Historia wieloturowej rozmowy jest utrzymywana przez wywołującego.
 */
class AskAboutAttractionUseCase(
    private val llmService: LlmService,
    private val webSearchService: WebSearchService,
    private val wikipediaService: WikipediaService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        attraction: Attraction,
        question: String,
        history: List<ChatMessage> = emptyList()
    ): Result<String> = runCatching {
        val settings = settingsRepository.getSettings().first()
        val systemPrompt = buildString {
            append(AgentPrompts.objectInfo)
            append("\n\nKontekst obiektu:")
            append("\nNazwa: ${attraction.name}")
            append("\nKategoria: ${attraction.category.displayName}")
            append("\nWspółrzędne: ${attraction.latitude}, ${attraction.longitude}")
            attraction.countryCode?.let { append("\nKraj (ISO): $it") }
            attraction.openingHours?.let { append("\nGodziny otwarcia (tag OSM): ${it.raw}") }
            attraction.description?.let { append("\nIstniejący opis w aplikacji: $it") }
            append("\nJęzyk odpowiedzi: ${settings.appLanguage}.")
        }

        val messages = (history + ChatMessage.User(question)).toMutableList()
        var iterations = 0
        var answer: String? = null

        while (answer == null) {
            iterations++
            if (iterations > AppConstants.MAX_TOOL_CALL_ITERATIONS) {
                error("Nie udało się znaleźć odpowiedzi w limicie wyszukiwań — spróbuj zadać pytanie inaczej")
            }

            val events = llmService.completeChat(messages, systemPrompt, listOf(TOOL_WEB_SEARCH, TOOL_WIKIPEDIA))
                .getOrThrow()

            events.filterIsInstance<LlmEvent.Error>().firstOrNull()?.let { error(it.message) }

            val toolCalls = events.filterIsInstance<LlmEvent.ToolCall>()
            if (toolCalls.isEmpty()) {
                val text = events.filterIsInstance<LlmEvent.TextChunk>().joinToString("") { it.text }
                if (text.isBlank()) error("Agent nie zwrócił odpowiedzi")
                answer = text
            } else {
                messages.add(ChatMessage.AssistantWithToolCalls(
                    toolCalls.map { ToolCallRef(it.id, it.name, it.rawArguments) }
                ))
                toolCalls.forEach { call ->
                    val result = executeTool(call)
                    Timber.tag("AskAboutAttraction").d("%s -> %s", call.name, result.take(200))
                    messages.add(ChatMessage.ToolResult(call.id, result))
                }
            }
        }
        answer
    }

    private suspend fun executeTool(call: LlmEvent.ToolCall): String = when (call.name) {
        "web_search" -> {
            val query = call.arguments["query"] as? String ?: return "Brak zapytania"
            webSearchService.search(query).getOrElse { "Błąd wyszukiwania: ${it.message}" }
        }
        "wikipedia" -> {
            val title = call.arguments["title"] as? String ?: return "Brak tytułu"
            wikipediaService.getArticle(title).fold(
                onSuccess = { it?.let { r -> "${r.extract}\nŹródło: ${r.url}" } ?: "Brak artykułu o tym tytule." },
                onFailure = { "Błąd Wikipedii: ${it.message}" }
            )
        }
        else -> "Nieznane narzędzie: ${call.name}"
    }
}
