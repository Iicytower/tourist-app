package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.DescriptionSource
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.repository.WikipediaService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber

class GenerateDescriptionUseCase(
    private val attractionRepository: AttractionRepository,
    private val webSearchService: WebSearchService,
    private val wikipediaService: WikipediaService,
    private val llmService: LlmService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(xid: String): Result<Pair<String, List<DescriptionSource>>> {
        Timber.tag("GenerateDesc").d("start xid=%s", xid)
        val attraction = attractionRepository.getByXid(xid)
            ?: return Result.failure(IllegalArgumentException("Atrakcja $xid nie istnieje w bazie"))

        val settings = settingsRepository.getSettings().first()

        val sources = mutableListOf<DescriptionSource>()
        val contextParts = mutableListOf<String>()
        contextParts += "Nazwa: ${attraction.name}\nKategoria: ${attraction.category.displayName}"

        Timber.tag("GenerateDesc").d("fetching web+wiki for: %s", attraction.name)
        coroutineScope {
            val webDeferred = async {
                webSearchService.search("${attraction.name} atrakcja turystyczna")
            }
            val wikiDeferred = async {
                wikipediaService.getArticle(attraction.name)
            }

            webDeferred.await()
                .onSuccess { text ->
                    Timber.tag("GenerateDesc").d("web search OK, %d chars", text.length)
                    contextParts += "[Web Search]\n$text"
                    sources += DescriptionSource(name = "Web Search", url = "https://tavily.com")
                }
                .onFailure { Timber.tag("GenerateDesc").w(it, "web search failed") }

            wikiDeferred.await()
                .onSuccess { result ->
                    if (result != null) {
                        Timber.tag("GenerateDesc").d("wikipedia OK, %d chars", result.extract.length)
                        contextParts += "[Wikipedia]\n${result.extract}"
                        sources += DescriptionSource(name = "Wikipedia", url = result.url)
                    } else {
                        Timber.tag("GenerateDesc").d("wikipedia: no article found")
                    }
                }
                .onFailure { Timber.tag("GenerateDesc").w(it, "wikipedia failed") }
        }

        Timber.tag("GenerateDesc").d("calling LLM (complete), context parts: %d", contextParts.size)
        val userPrompt = contextParts.joinToString("\n\n")
        val systemPrompt = buildString {
            append("Jesteś przewodnikiem turystycznym. ")
            append("Na podstawie podanych informacji napisz opis atrakcji w maksymalnie jednym akapicie (3-5 zdań). ")
            append("Odpowiedź powinna zawierać WYŁĄCZNIE opis — bez nagłówków, bez formatowania markdown, bez komentarzy wstępnych ani końcowych. ")
            append("Język odpowiedzi: ${settings.descriptionLanguage}.")
            if (settings.userInterests.isNotEmpty()) {
                append(" Uwzględnij zainteresowania: ${settings.userInterests.joinToString(", ") { it.displayName }}.")
            }
        }

        val result = llmService.complete(
            messages = listOf(ChatMessage.User(userPrompt)),
            systemPrompt = systemPrompt
        )

        return result.fold(
            onSuccess = { description ->
                Timber.tag("GenerateDesc").d("description: %d chars", description.length)
                attractionRepository.saveDescription(xid, description, sources)
                Result.success(Pair(description, sources))
            },
            onFailure = { e ->
                Timber.tag("GenerateDesc").e(e, "LLM complete failed")
                Result.failure(e)
            }
        )
    }
}
