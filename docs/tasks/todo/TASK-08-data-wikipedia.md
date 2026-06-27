# TASK-08: Moduł `data` — klient Wikipedia

## Cel
Implementacja `WikipediaService` — pobieranie artykułu Wikipedii dla atrakcji na potrzeby wzbogacania opisu.

## Zależności
- TASK-03 (interfejs `WikipediaService` w `domain`)
- TASK-06 (współdzielony `HttpClient`)

## Zakres

### 1. DTO (`data/remote/wikipedia/dto/WikipediaDto.kt`)

Wikipedia REST API (`/page/summary/{title}`):

```kotlin
@Serializable
data class WikipediaSummaryResponse(
    val title: String,
    val extract: String,                        // plaintext streszczenie
    @SerialName("content_urls") val contentUrls: WikipediaContentUrls? = null
)

@Serializable
data class WikipediaContentUrls(
    val desktop: WikipediaPageUrl? = null
)

@Serializable
data class WikipediaPageUrl(
    val page: String? = null                    // pełny URL artykułu
)
```

### 2. Implementacja serwisu (`data/remote/wikipedia/WikipediaServiceImpl.kt`)

Implementuje `WikipediaService` z modułu `domain`.

```kotlin
class WikipediaServiceImpl(
    private val httpClient: HttpClient
) : WikipediaService {
    private val baseUrl = "https://en.wikipedia.org/api/rest_v1/page/summary"
    // Dla polskich artykułów: https://pl.wikipedia.org/api/rest_v1/page/summary

    override suspend fun getArticle(query: String): Result<String?> {
        // 1. GET /page/summary/{encodedQuery}
        // 2. 200 → Result.success(extract)
        // 3. 404 → Result.success(null)   // brak artykułu = normalny przypadek
        // 4. Inne błędy → Result.failure
    }
}
```

Strategia wyszukiwania:
1. Próba z angielską Wikipedią (szersze pokrycie atrakcji)
2. Jeśli 404 — próba z polską Wikipedią
3. Jeśli obie 404 — `Result.success(null)`

Zwracany `String` to sam tekst (`extract`) — bez metadanych. URL artykułu do przechowania jako źródło opisu przekazywany osobno lub zawarty w kontekście (do decyzji podczas implementacji).

### 3. Koin module

```kotlin
val wikipediaModule = module {
    single<WikipediaService> { WikipediaServiceImpl(get()) }
}
```

## Testy

**`data/test/`** (MockK + MockEngine Ktor):

- `WikipediaServiceImplTest`:
  - Artykuł znaleziony → `Result.success("tekst...")` 
  - Artykuł nie znaleziony (404) → `Result.success(null)`
  - Błąd sieciowy → `Result.failure`
  - Timeout → `Result.failure`
  - Sprawdzenie że 404 nie jest traktowany jako błąd (brak artykułu to normalny przypadek)

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi
- `WikipediaServiceImpl` implementuje `WikipediaService`
- Brak artykułu (404) zwraca `Result.success(null)`, nie `Result.failure`
