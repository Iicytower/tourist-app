# TASK-03: Moduł `domain`

## Cel
Implementacja warstwy domenowej: modele biznesowe, interfejsy repozytoriów, interfejsy usług i use case'y. Warstwa `domain` nie zna żadnych szczegółów implementacyjnych (brak Room, Ktor, DataStore).

## Zależności
- TASK-02 (modele z `core`)

## Zakres

### 1. Modele domenowe (`domain/model/`)

**`Attraction.kt`**
```kotlin
data class Attraction(
    val xid: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: AttractionCategory,
    val isInMyList: Boolean,
    val dateAddedToList: Long?,
    val description: String?,
    val descriptionSources: List<DescriptionSource>,
    val isFromLastSearch: Boolean,
    val distanceKm: Double?   // obliczona w momencie wyszukiwania, null gdy brak kontekstu
)
```

**`DescriptionSource.kt`**
```kotlin
data class DescriptionSource(
    val name: String,
    val url: String
)
```

**`SearchParams.kt`**
```kotlin
data class SearchParams(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Int,
    val categories: Set<AttractionCategory>
)
```

**`AppSettings.kt`**
```kotlin
data class AppSettings(
    val openRouterApiKey: String,
    val tavilyApiKey: String,
    val aiModel: String,
    val defaultRadiusKm: Int,
    val descriptionLanguage: String,
    val userInterests: Set<AttractionCategory>,
    val systemPromptDescription: String,
    val systemPromptAssistant: String,
    val tavilyUsageCount: Int,
    val tavilyUsageMonth: String
)
```

**`ChatMessage.kt`**
```kotlin
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Assistant(val text: String) : ChatMessage()
    data class Error(val message: String) : ChatMessage()
}
```

**`Location.kt`**
```kotlin
data class Location(
    val latitude: Double,
    val longitude: Double
)
```

### 2. Interfejsy repozytoriów (`domain/repository/`)

**`AttractionRepository.kt`**
```kotlin
interface AttractionRepository {
    fun getMyList(): Flow<List<Attraction>>
    suspend fun getByXid(xid: String): Attraction?
    suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>>
    suspend fun addToMyList(xid: String): Result<Unit>
    suspend fun removeFromMyList(xid: String): Result<Unit>
    suspend fun saveDescription(xid: String, description: String, sources: List<DescriptionSource>): Result<Unit>
    suspend fun getLastSearchResults(): List<Attraction>
}
```

**`SettingsRepository.kt`**
```kotlin
interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateOpenRouterApiKey(key: String)
    suspend fun updateTavilyApiKey(key: String)
    suspend fun updateAiModel(model: String)
    suspend fun updateDefaultRadius(radiusKm: Int)
    suspend fun updateDescriptionLanguage(language: String)
    suspend fun updateUserInterests(interests: Set<AttractionCategory>)
    suspend fun updateSystemPromptDescription(prompt: String)
    suspend fun updateSystemPromptAssistant(prompt: String)
    suspend fun incrementTavilyUsage()
    suspend fun resetTavilyUsageIfNewMonth()
}
```

### 3. Interfejsy usług (`domain/repository/`)

**`WebSearchService.kt`** — interfejs wymienialny (Tavily domyślnie, Brave w przyszłości)
```kotlin
interface WebSearchService {
    suspend fun search(query: String): Result<String>   // zwraca surowy tekst wyników
}
```

**`LocationService.kt`**
```kotlin
interface LocationService {
    suspend fun getCurrentLocation(): Result<Location>
    fun hasLocationPermission(): Boolean
}
```

**`LlmService.kt`**
```kotlin
interface LlmService {
    fun streamResponse(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<ToolDefinition>
    ): Flow<LlmEvent>
}
```

**`ToolDefinition.kt`** i **`LlmEvent.kt`** — typy domenowe dla tool calling i streamingu:
```kotlin
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>   // JSON schema jako mapa
)

sealed class LlmEvent {
    data class TextChunk(val text: String) : LlmEvent()
    data class ToolCall(val name: String, val arguments: Map<String, Any>) : LlmEvent()
    data object Done : LlmEvent()
    data class Error(val message: String) : LlmEvent()
}
```

### 4. Use case'y (`domain/usecase/`)

Każdy use case to osobna klasa z operatorem `invoke`. Lista:

**`SearchAttractionsUseCase`** — odpytuje `AttractionRepository.searchAttractions()`, filtruje wyniki do `MY_LIST_MAX_SIZE` miejsc z listy.

**`GetMyListUseCase`** — zwraca `Flow<List<Attraction>>` z `AttractionRepository.getMyList()`.

**`AddToMyListUseCase`** — sprawdza czy lista nie przekracza `MY_LIST_MAX_SIZE`, jeśli tak zwraca `Result.failure` z odpowiednim komunikatem, w przeciwnym razie wywołuje `AttractionRepository.addToMyList()`.

**`RemoveFromMyListUseCase`** — deleguje do `AttractionRepository.removeFromMyList()`.

**`GenerateDescriptionUseCase`** — orkiestruje wzbogacanie wielo-źródłowe:
1. Pobiera dane z OpenTripMap (przez `AttractionRepository`)
2. Wywołuje `WebSearchService.search()`
3. Wywołuje Wikipedia (osobny interfejs `WikipediaService`)
4. Przekazuje dostępne wyniki do `LlmService`
5. Zwraca opis + listę użytych źródeł
Każde ze źródeł traktowane niezależnie — brak jednego nie blokuje pozostałych. Szczegóły w TASK-13.

**`GetSettingsUseCase`** — zwraca `Flow<AppSettings>` z `SettingsRepository`.

**`UpdateSettingsUseCase`** — zestawka use case'ów per pole (lub jeden z parametrami — do decyzji podczas implementacji).

**`GetAttractionDetailUseCase`** — pobiera pojedynczą atrakcję z `AttractionRepository.getByXid()`.

**`SendChatMessageUseCase`** — deleguje do `LlmService.streamResponse()`.

### 5. Interfejs Wikipedia (`domain/repository/WikipediaService.kt`)
```kotlin
interface WikipediaService {
    suspend fun getArticle(query: String): Result<String?>   // null = brak artykułu
}
```

## Testy
**`domain/test/`**:
- `AddToMyListUseCaseTest`: lista pełna (50) → `Result.failure` z komunikatem; lista niepełna → sukces; MockK dla `AttractionRepository`
- `SearchAttractionsUseCaseTest`: przekazywanie parametrów do repozytorium, propagacja błędu z repozytorium
- `GetMyListUseCaseTest`: delegacja do repozytorium

## Weryfikacja ukończenia
- `./gradlew :domain:test` przechodzi
- Moduł `domain` nie importuje nic z `data`, `feature-*`, Room, Ktor, Android Framework (poza `android.content.Context` jeśli niezbędne)
- Wszystkie interfejsy repozytoriów i usług zdefiniowane
- Wszystkie use case'y zdefiniowane z poprawną sygnaturą
