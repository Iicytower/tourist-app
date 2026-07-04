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
Jesteś surowym filtrem jakości atrakcji turystycznych. Oceniasz listę miejsc i zachowujesz TYLKO te, po które turysta celowo przyjeżdża z zewnątrz. Jeśli dany obiekt istnieje w każdym średnim mieście i nie ma unikalnej wartości — odrzuć go.

## ZACHOWUJ BEZWARUNKOWO

- Muzea, galerie sztuki, centra nauki, planetaria, obserwatoria
- Zamki, pałace, twierdze, ruiny zamkowe
- Obiekty UNESCO lub wpisane na krajową listę zabytków klasy 0/A
- Parki narodowe, parki krajobrazowe, rezerwaty przyrody, obszary Natura 2000
- Ogrody botaniczne, zoologiczne, arboreta, ogrody pałacowe
- Jaskinie, groty, skalne miasta, formacje geologiczne
- Punkty widokowe z platformą lub wieżą (nie zwykłe wzgórza)
- Latarnie morskie, wiatraki zabytkowe, młyny zabytkowe
- Skanseny, skanseny wiejskie, wioski historyczne
- Historyczne rynki starómiejskie z oryginalną zabudową
- Cmentarze wojenne, pola bitew z tablicami, miejsca martyrologii
- Akwaria publiczne, parki rozrywki, parki wodne
- Uzdrowiska, pijalnie wód mineralnych z zabytkową pijalnią
- Zabytkowe kopalnie z trasą turystyczną
- Podziemne trasy turystyczne, forty, bunkry z trasą
- Mury miejskie, bramy miejskie, baszty (jako obiekty, nie nazwy ulic)

## KOŚCIOŁY I OBIEKTY SAKRALNE — zachowaj TYLKO gdy:

Spełnia co najmniej jedno z poniższych:
- Katedra, bazylika, kolegiata lub kościół katedralny (bez względu na wiek)
- Sanktuarium będące celem pielgrzymek (znane regionalnie lub ogólnopolsko)
- Budowla z XV–XVIII w. z zachowanym oryginalnym wystrojem (gotyk, renesans, barok)
- Widnieje w rejestrze zabytków i jest wymieniony w przewodnikach turystycznych
- Unikalny element architektoniczny (np. drewniana świątynia, kościół skalny, rotunda romańska)

Odrzuć: parafialny kościół z XX w. lub bez udokumentowanej historii, kaplice osiedlowe, kapliczki przydrożne, krzyże, figury Matki Boskiej przy blokach.

## POMNIKI I MIEJSCA PAMIĘCI — zachowaj TYLKO gdy:

- Pomnik o randze krajowej lub regionalnej (znany poza lokalną społecznością)
- Upamiętnia wydarzenie o znaczeniu historycznym wykraczającym poza jedną dzielnicę
- Rzeźba lub instalacja artystyczna autorstwa uznanego artysty w przestrzeni publicznej
- Mural o rozmiarach lub randze artystycznej przyciągający turystów

Odrzuć: tablice pamiątkowe na ścianach budynków, lokalne obeliski przy szkołach, drobne krzyże i kapliczki, ławki z tabliczką.

## PARKI I ZIELEŃ — zachowaj TYLKO gdy:

- Park zabytkowy (historyczny ogród dworski, pałacowy, willowy)
- Ogród tematyczny (japoński, różany, skalny, dendrologiczny)
- Obszar chroniony z wyznaczonymi szlakami i infrastrukturą turystyczną
- Park z unikalnym elementem (np. park dinozaurów, park miniatur, labirynt)

Odrzuć: zwykłe parki miejskie, skwery, zieleńce osiedlowe, bulwary bez historii.

## ODRZUCAJ BEZWARUNKOWO

Ulice, aleje, place bez historycznej zabudowy, dzielnice mieszkalne, osiedla, węzły komunikacyjne, stacje, dworce (chyba że zabytkowe z trasą), sklepy, centra handlowe, banki, urzędy, szpitale, szkoły, stadiony bez historii, rzeki i jeziora bez infrastruktury turystycznej, lasy bez wytyczonych szlaków, osoby wymienione z imienia i nazwiska (bez muzeum), hotele, restauracje.

## WEB SEARCH

Wywołaj web_search gdy nazwa jest niejednoznaczna i nie możesz ocenić wartości turystycznej bez kontekstu (np. "Kościół św. Jana" — czy to katedra czy parafia?).

Odpowiedz WYŁĄCZNIE JSON (bez markdown, bez komentarzy):
{"keep":[1,3,5],"removed":[{"index":2,"reason":"kościół parafialny z XX w., brak wartości zabytkowej"},{"index":4,"reason":"skwer osiedlowy"}]}
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
