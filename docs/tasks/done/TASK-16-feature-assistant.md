# TASK-16: Moduł `feature-assistant` — ekran Asystenta AI

## Cel
Implementacja ekranu czatu z asystentem AI: streaming odpowiedzi, tool calling (search_attractions, web_search, get_my_list), pamięć sesji.

## Zależności
- TASK-09 (`LlmService`, `AssistantTools`)
- TASK-03 (`SendChatMessageUseCase`, `SearchAttractionsUseCase`, `GetMyListUseCase`)
- TASK-07 (`WebSearchService`)
- TASK-10 (`LocationService`)
- TASK-11 (nawigacja)

## Zakres

### 1. Stan UI (`feature-assistant/viewmodel/AssistantUiState.kt`)

```kotlin
data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isProcessing: Boolean = false,          // LLM przetwarza
    val streamingText: String = "",             // aktualnie streamowany tekst
    val showClearConfirmation: Boolean = false
)
```

### 2. ViewModel (`feature-assistant/viewmodel/AssistantViewModel.kt`)

```kotlin
class AssistantViewModel(
    private val llmService: LlmService,
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val getMyListUseCase: GetMyListUseCase,
    private val webSearchService: WebSearchService,
    private val locationService: LocationService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val uiState: StateFlow<AssistantUiState>

    // Historia rozmowy (cały kontekst sesji, nie persystowany)
    private val conversationHistory = mutableListOf<ChatMessage>()

    fun updateInput(text: String)
    fun sendMessage()
    fun clearChat()
    fun confirmClearChat()
    fun dismissClearConfirmation()
}
```

**Pętla tool calling w `sendMessage()`:**

```
1. Dodaj wiadomość użytkownika do historii
2. Wywołaj llmService.streamResponse(history, systemPrompt, tools)
3. Obserwuj Flow<LlmEvent>:
   a. TextChunk → akumuluj w streamingText (live preview)
   b. ToolCall → wykonaj narzędzie (patrz niżej), dodaj wynik do historii, wróć do kroku 2
   c. Done → przenieś streamingText do messages jako ChatMessage.Assistant
   d. Error → dodaj ChatMessage.Error do messages
4. isProcessing = false
```

**Obsługa narzędzi:**

```kotlin
private suspend fun executeTool(name: String, args: Map<String, Any>): String {
    return when (name) {
        "search_attractions" -> {
            val lat = args["latitude"] as Double
            val lon = args["longitude"] as Double
            val radius = (args["radius_km"] as Number).toInt()
            val categories = (args["categories"] as? List<String>)?.mapNotNull { ... }
            val result = searchAttractionsUseCase(SearchParams(lat, lon, radius, categories?.toSet() ?: emptySet()))
            result.fold(
                onSuccess = { attractions -> attractions.toToolResultString() },
                onFailure = { "Błąd wyszukiwania: ${it.message}" }
            )
        }
        "web_search" -> {
            val query = args["query"] as String
            webSearchService.search(query).getOrElse { "Błąd wyszukiwania: ${it.message}" }
        }
        "get_my_list" -> {
            val myList = getMyListUseCase().first()
            myList.toToolResultString()   // nazwa, kategoria, koordynaty — bez opisów
        }
        else -> "Nieznane narzędzie: $name"
    }
}
```

Format `toToolResultString()` dla `search_attractions` i `get_my_list`:
```
Nazwa: Zamek Wawelski | Kategoria: Zamki i fortyfikacje | Lat: 50.054 | Lon: 19.935
Nazwa: Muzeum Czartoryskich | Kategoria: Muzea i galerie | Lat: 50.063 | Lon: 19.935
...
```
Bez opisów — oszczędność tokenów LLM.

**Pamięć sesji:**
- `conversationHistory` w pamięci ViewModelu — żyje tyle co ViewModel (cały czas gdy aplikacja nie jest ubijana)
- `clearChat()` → czyści `conversationHistory` i `uiState.messages`
- Nie persystowana między sesjami (wymaganie z spec: sesja = app open→close)

### 3. UI (`feature-assistant/ui/AssistantScreen.kt`)

**Nagłówek:**
- Tytuł „Asystent"
- Przycisk „Wyczyść" → `showClearConfirmation = true`
- `AlertDialog` potwierdzający czyszczenie

**Lista wiadomości (`LazyColumn`, autoscroll do dołu):**
- Dymek użytkownika (wyrównany do prawej, kolor akcentu)
- Dymek asystenta (wyrównany do lewej, kolor neutralny)
- Dymek błędu (styl error)
- Aktywny streaming: dymek asystenta z aktualnym `streamingText` + animowany wskaźnik (migające wielokropki lub cursor)
- `isProcessing = true` bez tekstu → `CircularProgressIndicator` w miejscu dymka

**Pole wprowadzania:**
- `TextField` na dole ekranu (sticky)
- Przycisk „Wyślij" — nieaktywny gdy `currentInput.isBlank()` lub `isProcessing = true`
- Klawiatura wysyła na Enter (opcjonalnie)

## Testy

**`feature-assistant/test/`**:

- `AssistantViewModelTest` (JUnit + MockK + TestCoroutineScope):
  - `sendMessage()` → `isProcessing = true` → `isProcessing = false` po zakończeniu
  - Strumień TextChunk → `streamingText` akumulowany, po Done przeniesiony do `messages`
  - `LlmEvent.ToolCall("web_search", args)` → `webSearchService.search()` wywołane
  - `LlmEvent.ToolCall("get_my_list", {})` → `getMyListUseCase()` wywołane
  - `LlmEvent.Error` → `ChatMessage.Error` dodany do messages
  - `clearChat()` → messages puste, conversationHistory pusta
  - Wiadomość użytkownika dodana do historii przed wywołaniem LLM

## Weryfikacja ukończenia
- `./gradlew :feature-assistant:test` przechodzi
- Streaming odpowiedzi widoczny słowo po słowie na emulatorze
- Tool calling działa end-to-end: „pokaż mi zamki w Krakowie" → LLM wywołuje `search_attractions` → wyniki wracają do LLM → odpowiedź tekstowa
- Czyszczenie czatu działa z potwierdzeniem
