package com.iicytower.wanderlist.data.remote.wikipedia

import com.iicytower.wanderlist.data.remote.wikipedia.dto.WikipediaSummaryResponse
import com.iicytower.wanderlist.domain.model.WikipediaResult
import com.iicytower.wanderlist.domain.repository.WikipediaService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.net.URLEncoder

class WikipediaServiceImpl(
    private val httpClient: HttpClient
) : WikipediaService {

    override suspend fun getArticle(query: String): Result<WikipediaResult?> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val enResult = fetchFromWikipedia("en", encoded)
        if (enResult != null) return@runCatching enResult
        fetchFromWikipedia("pl", encoded)
    }

    private suspend fun fetchFromWikipedia(lang: String, encodedQuery: String): WikipediaResult? {
        return runCatching {
            val response = httpClient.get("https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedQuery")
            when {
                response.status.value == 404 -> null
                !response.status.isSuccess() -> error("HTTP ${response.status.value}")
                else -> {
                    val body: WikipediaSummaryResponse = response.body()
                    WikipediaResult(
                        extract = body.extract,
                        url = body.contentUrls?.desktop?.page ?: "https://$lang.wikipedia.org/wiki/$encodedQuery"
                    )
                }
            }
        }.getOrElse { if (it.message?.contains("404") == true) null else throw it }
    }
}
