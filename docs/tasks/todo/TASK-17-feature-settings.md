# TASK-17: Moduł `feature-settings` — ekran Ustawień

## Cel
Implementacja ekranu ustawień: klucze API z testowaniem połączenia, wybór modelu AI, edycja system promptów, zainteresowania, język opisów, domyślny promień.

## Zależności
- TASK-03 (`GetSettingsUseCase`, use case'y aktualizujące ustawienia)
- TASK-05 (`DataStoreSettingsRepository`, `SecureKeyStorage`)
- TASK-09 (`LlmService` — do testu połączenia)
- TASK-07 (`TavilyWebSearchService` — do testu połączenia)

## Zakres

### 1. Stan UI (`feature-settings/viewmodel/SettingsUiState.kt`)

```kotlin
data class SettingsUiState(
    val settings: AppSettings? = null,
    val isLoading: Boolean = false,
    val openRouterTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val tavilyTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val openRouterKeyVisible: Boolean = false,
    val tavilyKeyVisible: Boolean = false
)

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILURE }
```

### 2. ViewModel (`feature-settings/viewmodel/SettingsViewModel.kt`)

```kotlin
class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val llmService: LlmService,
    private val webSearchService: WebSearchService
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState>

    fun updateOpenRouterKey(key: String)
    fun updateTavilyKey(key: String)
    fun updateAiModel(model: String)
    fun updateDefaultRadius(radiusKm: Int)
    fun updateDescriptionLanguage(language: String)
    fun updateInterests(interests: Set<AttractionCategory>)
    fun updateSystemPromptDescription(prompt: String)
    fun updateSystemPromptAssistant(prompt: String)
    fun resetSystemPromptDescription()     // przywraca domyślny z DefaultSettings
    fun resetSystemPromptAssistant()

    fun testOpenRouterConnection()         // wyślij proste zapytanie do LLM
    fun testTavilyConnection()             // wyślij proste zapytanie do Tavily

    fun toggleOpenRouterKeyVisibility()
    fun toggleTavilyKeyVisibility()
}
```

**Test połączenia OpenRouter:** wywołaj `llmService.streamResponse()` z prostym pytaniem „Odpowiedz jednym słowem: OK", bez narzędzi. Sukces = otrzymano `LlmEvent.TextChunk`.

**Test połączenia Tavily:** wywołaj `webSearchService.search("test")`. Sukces = `Result.success`.

### 3. UI (`feature-settings/ui/SettingsScreen.kt`)

Podzielony na sekcje (LazyColumn):

**Sekcja: Klucze API**
```
[OpenRouter API Key]
  TextField (masked/visible toggle)
  Przycisk "Przetestuj" → openRouterTestState: IDLE/TESTING/SUCCESS/FAILURE + ikona
  Link "Skąd wziąć klucz?" (przeglądarka)

[Tavily API Key]
  TextField (masked/visible toggle)
  Przycisk "Przetestuj" → tavilyTestState
  Licznik: "Wykorzystano X / 1000 zapytań w tym miesiącu"
  Link "Skąd wziąć klucz?"
```

**Sekcja: Model AI**
```
[Model AI]
  TextField z nazwą modelu (np. "google/gemini-2.5-flash-lite")
  Opis: "Modele dostępne na openrouter.ai/models"
```

**Sekcja: System Prompty**
```
[Agent opisów]
  MultiLine TextField
  Przycisk "Przywróć domyślny"

[Asystent]
  MultiLine TextField
  Przycisk "Przywróć domyślny"
```

**Sekcja: Zainteresowania**
```
Lista checkboxów (10 kategorii z AttractionCategory):
  [ ] Zamki i fortyfikacje
  [ ] Kościoły i obiekty sakralne
  ... (wszystkie 10)
```

**Sekcja: Preferencje**
```
[Język opisów]
  Dropdown: Polski (pl) | English (en) | Deutsch (de) | Français (fr) | Español (es)

[Domyślny promień]
  Slider 1–50 km z etykietą wartości
```

### 4. Koin module

```kotlin
val settingsViewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
```

## Testy

**`feature-settings/test/`**:

- `SettingsViewModelTest` (JUnit + MockK):
  - `getSettingsUseCase()` emituje dane → `uiState.settings` zaktualizowane
  - `testOpenRouterConnection()` → `openRouterTestState = TESTING`, po sukcesie = `SUCCESS`
  - `testOpenRouterConnection()` gdy LLM zwraca błąd → `openRouterTestState = FAILURE`
  - `resetSystemPromptDescription()` → wywołuje `updateSystemPromptDescription(DefaultSettings.SYSTEM_PROMPT_DESCRIPTION)`
  - `updateInterests(setOf(...))` → `settingsRepository.updateUserInterests()` wywołane

## Weryfikacja ukończenia
- `./gradlew :feature-settings:test` przechodzi
- Klucze API maskowane (nie widoczne domyślnie)
- Test połączenia działa — ikona sukcesu/błędu pojawia się
- Reset systemu promptów przywraca tekst domyślny z `DefaultSettings`
- Zainteresowania zapisywane i wczytywane poprawnie
