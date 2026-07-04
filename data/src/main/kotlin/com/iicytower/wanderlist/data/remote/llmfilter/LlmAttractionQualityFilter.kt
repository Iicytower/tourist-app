package com.iicytower.wanderlist.data.remote.llmfilter

import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.model.ToolCallRef
import com.iicytower.wanderlist.domain.model.ToolDefinition
import com.iicytower.wanderlist.domain.repository.AttractionQualityFilter
import com.iicytower.wanderlist.domain.repository.FilterResult
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.RemovedAttraction
import com.iicytower.wanderlist.domain.repository.WebSearchService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val MODEL = "google/gemini-flash-2.5"

private val TOOL_WEB_SEARCH = ToolDefinition(
    name = "web_search",
    description = "Wyszukaj informacje o miejscu, gdy nie jesteś pewien czy jest atrakcją turystyczną.",
    parameters = mapOf("query" to mapOf("type" to "string", "description" to "Zapytanie wyszukiwania"))
)

private val SYSTEM_PROMPT = """
Jesteś filtrem jakości atrakcji turystycznych. Twoje zadanie: z podanej listy miejsc zachować TYLKO te, które są realnymi atrakcjami turystycznymi.

Zachowuj miejsca należące do kategorii:
Muzea, Galerie sztuki, Parki przyrody, Rezerwaty przyrody, Zabytki, Architektura historyczna, Zamki, Pałace, Parki rozrywki, Aquaparki, Plaże, Jeziora, Szlaki górskie, Punkty widokowe, Ogrody zoologiczne, Ogrody botaniczne, Winnice, Festiwale, Imprezy kulturalne, Jaskinie, Groty turystyczne, Skanseny, Wioski tematyczne, Stare kopalnie, Wieże obserwacyjne, Bazary i targi, Browary z turystyką, Dzielnice sztuki ulicznej, Murale, Miejsca kultu religijnego (kościoły, katedry, sanktuaria, meczety, synagogi), Latarnie morskie, Ścieżki w koronach drzew, Planetaria, Obserwatoria astronomiczne, Wraki statków, Miejsca nurkowania, Bulwary i deptaki, Tunele turystyczne, Podziemne trasy, Skalne miasta, Labirynty skalne, Domy i muzea biograficzne, Ogrody japońskie/tematyczne, Uzdrowiska, Pijalnie wód mineralnych, Parki krajobrazowe, Pomniki i monumenty, Cmentarze historyczne, Miejsca bitew.

ODRZUCAJ: ulice, dzielnice mieszkalne, stacje, sklepy, banki, urzędy, osiedla, rzeki bez specjalnej atrakcji, lasy bez wytyczonych tras, osoby (biografie bez muzeum), zwykłe budynki bez znaczenia historycznego.

Możesz wywołać web_search dla niepewnych przypadków — szczególnie gdy nazwa jest niejednoznaczna.

Odpowiedz WYŁĄCZNIE JSON (bez markdown, bez komentarzy):
{"keep":[1,3,5],"removed":[{"index":2,"reason":"ulica, nie atrakcja"},{"index":4,"reason":"dzielnica mieszkalna"}]}
""".trimIndent()

@Serializable
private data class FilterResponse(
    val keep: List<Int> = emptyList(),
    val removed: List<RemovedEntry> = emptyList()
)

@Serializable
private data class RemovedEntry(val index: Int, val reason: String)

class LlmAttractionQualityFilter(
    private val llmService: LlmService,
    private val webSearchService: WebSearchService
) : AttractionQualityFilter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun filter(attractions: List<Attraction>): Result<FilterResult> = runCatching {
        if (attractions.isEmpty()) return@runCatching FilterResult(emptyList(), emptyList())

        val numbered = attractions.mapIndexed { i, a ->
            "${i + 1}. ${a.name} | ${a.category.displayName} | lat:${a.latitude} lon:${a.longitude}"
        }.joinToString("\n")

        val userMessage = ChatMessage.User("Lista miejsc do oceny:\n$numbered")
        val history = mutableListOf<ChatMessage>(userMessage)

        var continueLoop = true
        var finalJson: String? = null

        while (continueLoop) {
            val events = llmService.completeChat(history, SYSTEM_PROMPT, listOf(TOOL_WEB_SEARCH))
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

        parse(finalJson, attractions)
    }

    private fun parse(raw: String?, attractions: List<Attraction>): FilterResult {
        if (raw == null) {
            Timber.w("LlmAttractionQualityFilter: null response — fail open")
            return FilterResult(attractions, emptyList())
        }
        return runCatching {
            val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val response = json.decodeFromString<FilterResponse>(clean)
            val keepIndices = response.keep.toSet()
            val kept = attractions.filterIndexed { i, _ -> (i + 1) in keepIndices }
            val removed = response.removed.mapNotNull { entry ->
                val idx = entry.index - 1
                attractions.getOrNull(idx)?.let { RemovedAttraction(it.name, entry.reason) }
            }
            Timber.d("LlmAttractionQualityFilter: kept=${kept.size}, removed=${removed.size}")
            removed.forEach { Timber.d("  REMOVED: ${it.name} — ${it.reason}") }
            FilterResult(kept, removed)
        }.getOrElse {
            Timber.w(it, "LlmAttractionQualityFilter: parse error — fail open")
            FilterResult(attractions, emptyList())
        }
    }
}
