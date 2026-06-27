# TASK-09: Moduł `data` — klient LLM (OpenRouter, SSE, tool calling)

## Cel
Implementacja `LlmService` przez OpenRouter API: streaming odpowiedzi (Server-Sent Events) i obsługa tool calling. Architektura musi umożliwiać łatwe dodawanie nowych agentów w przyszłości.

## Zależności
- TASK-03 (interfejs `LlmService`, typy `LlmEvent`, `ToolDefinition`, `ChatMessage`)
- TASK-06 (współdzielony `HttpClient`)

## Zakres

### 1. DTO — zapytanie i odpowiedź (`data/remote/openrouter/dto/`)

**`OpenRouterRequestDto.kt`**
```kotlin
@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val tools: List<OpenRouterTool>? = null,
    val stream: Boolean = true
)

@Serializable
data class OpenRouterMessage(
    val role: String,           // "system", "user", "assistant", "tool"
    val content: String,
    @SerialName("tool_call_id") val toolCallId: String? = null
)

@Serializable
data class OpenRouterTool(
    val type: String = "function",
    val function: OpenRouterFunction
)

@Serializable
data class OpenRouterFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
```

**`OpenRouterResponseDto.kt`** (format SSE chunków):
```kotlin
@Serializable
data class OpenRouterStreamChunk(
    val choices: List<OpenRouterChoice>
)

@Serializable
data class OpenRouterChoice(
    val delta: OpenRouterDelta
)

@Serializable
data class OpenRouterDelta(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenRouterToolCall>? = null
)

@Serializable
data class OpenRouterToolCall(
    val id: String,
    val function: OpenRouterFunctionCall
)

@Serializable
data class OpenRouterFunctionCall(
    val name: String,
    val arguments: String    // JSON jako string (tak działa OpenAI-compatible API)
)
```

### 2. Mapper (`data/remote/openrouter/mapper/LlmMapper.kt`)

- `List<ChatMessage>.toOpenRouterMessages(systemPrompt: String): List<OpenRouterMessage>`
- `List<ToolDefinition>.toOpenRouterTools(): List<OpenRouterTool>`
- `OpenRouterStreamChunk.toLlmEvent(): LlmEvent?`

### 3. Implementacja serwisu (`data/remote/openrouter/OpenRouterLlmService.kt`)

Implementuje `LlmService` z modułu `domain`.

```kotlin
class OpenRouterLlmService(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) : LlmService {
    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"

    override fun streamResponse(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<ToolDefinition>
    ): Flow<LlmEvent> = flow {
        // 1. Pobierz model i klucz API z settingsRepository
        // 2. POST z stream=true, odbierz SSE przez httpClient.preparePost { ... }.execute { response -> ... }
        // 3. Parsuj linie: pomiń "data: [DONE]", parsuj "data: {...}" jako OpenRouterStreamChunk
        // 4. Mapuj każdy chunk → LlmEvent i emituj
        // 5. Po zakończeniu strumienia emituj LlmEvent.Done
        // 6. Błędy HTTP → emituj LlmEvent.Error
    }
}
```

Obsługa SSE z Ktor:
- Nagłówek `Accept: text/event-stream`
- `response.bodyAsChannel()` → czytaj linie iteracyjnie
- Każda linia zaczyna się od `data: ` — odciąć prefix, sparsować JSON

### 4. Definicje narzędzi asystenta (`data/remote/openrouter/AssistantTools.kt`)

Stałe z definicjami narzędzi zgodne z sekcją 28 specyfikacji technicznej:

```kotlin
object AssistantTools {
    val SEARCH_ATTRACTIONS = ToolDefinition(
        name = "search_attractions",
        description = "Wyszukuje atrakcje turystyczne w pobliżu wskazanego punktu.",
        parameters = mapOf(
            "latitude"   to mapOf("type" to "number"),
            "longitude"  to mapOf("type" to "number"),
            "radius_km"  to mapOf("type" to "integer"),
            "categories" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "nullable" to true)
        )
    )

    val WEB_SEARCH = ToolDefinition(
        name = "web_search",
        description = "Wyszukuje informacje w internecie.",
        parameters = mapOf("query" to mapOf("type" to "string"))
    )

    val GET_MY_LIST = ToolDefinition(
        name = "get_my_list",
        description = "Zwraca miejsca zapisane przez użytkownika na Mojej Liście.",
        parameters = emptyMap()
    )
}
```

### 5. Obsługa tool calling w warstwie danych

Wywołanie narzędzia przez LLM nie jest obsługiwane wewnątrz `LlmService` — to odpowiedzialność warstwy ViewModel/UseCase:
- `LlmEvent.ToolCall` emitowany przez serwis
- ViewModel interpretuje nazwę narzędzia, wywołuje odpowiedni use case
- Wynik wstrzykiwany z powrotem do historii wiadomości jako `ChatMessage` z rolą `"tool"`
- Ponowne wywołanie `streamResponse()` z uzupełnioną historią

Szczegóły pętli tool-calling → TASK-16 (feature-assistant).

### 6. Koin module

```kotlin
val llmModule = module {
    single<LlmService> { OpenRouterLlmService(get(), get()) }
}
```

## Testy

**`data/test/`** (MockK + MockEngine Ktor):

- `OpenRouterLlmServiceTest`:
  - Strumień tekstu → sekwencja `LlmEvent.TextChunk` + `LlmEvent.Done`
  - Chunk z `tool_calls` → `LlmEvent.ToolCall` z poprawną nazwą i argumentami
  - 401 (błędny klucz) → `LlmEvent.Error`
  - Parsowanie `data: [DONE]` nie powoduje błędu
  - Przeplatane tekst + tool_call w jednym strumieniu

- `LlmMapperTest`:
  - `ChatMessage.User` → `OpenRouterMessage` z rolą `"user"`
  - `ChatMessage.Assistant` → `OpenRouterMessage` z rolą `"assistant"`
  - `ToolDefinition` → `OpenRouterTool` z poprawną strukturą JSON

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi
- Streaming SSE działa na prawdziwym OpenRouter (ręczny test)
- Tool calling: `LlmEvent.ToolCall` emitowany dla odpowiedzi z `tool_calls`
- Dodanie nowego agenta = nowy zestaw `ToolDefinition` + nowy `systemPrompt` — bez zmian w `LlmService`
