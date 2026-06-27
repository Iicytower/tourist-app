package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.DescriptionSource
import com.iicytower.wanderlist.domain.model.LlmEvent
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.repository.WikipediaService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

class GenerateDescriptionUseCase(
    private val attractionRepository: AttractionRepository,
    private val webSearchService: WebSearchService,
    private val wikipediaService: WikipediaService,
    private val llmService: LlmService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(xid: String): Result<Pair<String, List<DescriptionSource>>> {
        val attraction = attractionRepository.getByXid(xid)
            ?: return Result.failure(IllegalArgumentException("Atrakcja $xid nie istnieje w bazie"))

        val settings = settingsRepository.getSettings().first()

        val sources = mutableListOf<DescriptionSource>()
        val contextParts = mutableListOf<String>()
        contextParts += "Nazwa: ${attraction.name}\nKategoria: ${attraction.category.displayName}"

        coroutineScope {
            val webDeferred = async {
                webSearchService.search("${attraction.name} atrakcja turystyczna")
            }
            val wikiDeferred = async {
                wikipediaService.getArticle(attraction.name)
            }

            webDeferred.await().onSuccess { text ->
                contextParts += "[Web Search]\n$text"
                sources += DescriptionSource(name = "Web Search", url = "https://tavily.com")
            }

            wikiDeferred.await().onSuccess { result ->
                if (result != null) {
                    contextParts += "[Wikipedia]\n${result.extract}"
                    sources += DescriptionSource(name = "Wikipedia", url = result.url)
                }
            }
        }

        val userPrompt = contextParts.joinToString("\n\n")
        val events = llmService.streamResponse(
            messages = listOf(ChatMessage.User(userPrompt)),
            systemPrompt = settings.systemPromptDescription
                .replace("{language}", settings.descriptionLanguage)
                .replace("{interests}", settings.userInterests.joinToString(", ") { it.displayName }),
            tools = emptyList()
        ).toList()

        val errorEvent = events.filterIsInstance<LlmEvent.Error>().firstOrNull()
        if (errorEvent != null) {
            return Result.failure(RuntimeException(errorEvent.message))
        }

        val description = events
            .filterIsInstance<LlmEvent.TextChunk>()
            .joinToString("") { it.text }

        attractionRepository.saveDescription(xid, description, sources)
        return Result.success(Pair(description, sources))
    }
}
