package com.iicytower.wanderlist.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(UserAgent) {
        agent = "WanderList-Android/1.0"
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = Timber.tag("Ktor").d(message)
        }
        level = LogLevel.HEADERS
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        // 10s bywało za mało pod współbieżnym obciążeniem wielu równoległych zapytań
        // (np. OverpassApiClient odpytujący po kategorii) — połączenia do innych źródeł
        // (Wikipedia, Wikidata) traciły slot i padały z ECONNABORTED w emulatorze (BUG-06).
        connectTimeoutMillis = 20_000
        // Bez jawnego socketTimeoutMillis silnik OkHttp używał swojego domyślnego (krótkiego)
        // read timeoutu, co ucinało oczekiwanie na odpowiedź OpenRoutera przy większych promptach
        // (np. filtrowanie jakości dużej listy atrakcji) — SocketTimeoutException mimo że
        // requestTimeoutMillis=30s jeszcze nie minęło. Zapytania LLM (completeChat) same
        // nadpisują to per-request dłuższym limitem — patrz OpenRouterLlmService.
        socketTimeoutMillis = 30_000
    }
}
