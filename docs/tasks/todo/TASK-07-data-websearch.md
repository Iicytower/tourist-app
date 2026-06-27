# TASK-07: Moduł `data` — Web Search (Tavily)

## Cel
Implementacja `WebSearchService` przez Tavily API. Interfejs zdefiniowany w `domain` — zamiana na innego dostawcę (Brave Search itp.) wymaga tylko nowej implementacji w `data`.

## Zależności
- TASK-03 (interfejs `WebSearchService` w `domain`)
- TASK-06 (współdzielony `HttpClient`)

## Zakres

### 1. DTO (`data/remote/tavily/dto/TavilySearchDto.kt`)

```kotlin
@Serializable
data class TavilySearchRequest(
    val query: String,
    @SerialName("api_key") val apiKey: String,
    @SerialName("search_depth") val searchDepth: String = "basic",
    @SerialName("max_results") val maxResults: Int = 5
)

@Serializable
data class TavilySearchResponse(
    val results: List<TavilyResult>,
    val answer: String? = null
)

@Serializable
data class TavilyResult(
    val title: String,
    val url: String,
    val content: String,
    val score: Double
)
```

### 2. Implementacja serwisu (`data/remote/tavily/TavilyWebSearchService.kt`)

Implementuje `WebSearchService` z modułu `domain`.

```kotlin
class TavilyWebSearchService(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) : WebSearchService {
    private val baseUrl = "https://api.tavily.com/search"

    override suspend fun search(query: String): Result<String> {
        // 1. Pobierz klucz API z settingsRepository
        // 2. POST do Tavily z TavilySearchRequest
        // 3. Zmapuj wyniki → plaintext (title + content połączone)
        // 4. Zwróć Result<String>
        // 5. Po sukcesie: settingsRepository.incrementTavilyUsage()
    }
}
```

Format wyjściowy (plaintext dla LLM):
```
[1] Title: {title}
Source: {url}
{content}

[2] Title: {title}
...
```

Obsługa błędów:
- 401 (błędny klucz) → `Result.failure("Nieprawidłowy klucz Tavily API")`
- 429 (limit) → `Result.failure("Przekroczono limit zapytań Tavily")`
- Inne 4xx/5xx → `Result.failure` z kodem HTTP
- Timeout → `Result.failure`

### 3. Koin module

```kotlin
val webSearchModule = module {
    single<WebSearchService> { TavilyWebSearchService(get(), get()) }
}
```

### Uwaga architektoniczna
Interfejs `WebSearchService` definiuje tylko `search(query: String): Result<String>`. Licznik użycia Tavily jest szczegółem implementacji `TavilyWebSearchService` — inny dostawca może go nie potrzebować. Licznik zarządzany przez `SettingsRepository`.

## Testy

**`data/test/`** (MockK + MockEngine Ktor):

- `TavilyWebSearchServiceTest`:
  - Udane zapytanie → `Result.success` ze sformatowanym tekstem
  - Weryfikacja że `incrementTavilyUsage()` wywołane po sukcesie
  - Weryfikacja że `incrementTavilyUsage()` NIE wywołane po błędzie
  - 401 → `Result.failure` z odpowiednim komunikatem
  - 429 → `Result.failure` z komunikatem o limicie

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi
- `TavilyWebSearchService` implementuje `WebSearchService`
- Zamiana na innego dostawcę = nowa klasa w `data/remote/`, zmiana bindowania w Koin — bez dotykania `domain` ani `feature-*`
