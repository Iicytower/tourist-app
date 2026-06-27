# TASK-13: Ekran Szczegółu Atrakcji (shared screen)

## Cel
Implementacja ekranu szczegółu atrakcji: wyświetlanie danych, generowanie opisu (wzbogacanie wielo-źródłowe z OpenTripMap + Tavily + Wikipedia), nawigacja do Map i dodawanie do Mojej Listy.

Ekran dostępny ze wszystkich trzech ekranów: Szukaj, Mapa, Moja Lista. Najlepiej zaimplementowany jako moduł `feature-detail` lub wewnątrz `feature-search` (do decyzji podczas implementacji — ekran jest shared, więc osobny moduł jest czystszy).

## Zależności
- TASK-03 (`GetAttractionDetailUseCase`, `GenerateDescriptionUseCase`, `AddToMyListUseCase`, `RemoveFromMyListUseCase`)
- TASK-07, TASK-08, TASK-09 (źródła opisu — przez use case)
- TASK-11 (nawigacja)

## Zakres

### 1. Stan UI (`viewmodel/AttractionDetailUiState.kt`)

```kotlin
data class AttractionDetailUiState(
    val attraction: Attraction? = null,
    val isLoading: Boolean = false,
    val isDescriptionLoading: Boolean = false,
    val error: String? = null,
    val descriptionError: String? = null,
    val showDistanceFromSearch: Boolean = false   // true gdy otwarty z wyników wyszukiwania
)
```

### 2. ViewModel (`viewmodel/AttractionDetailViewModel.kt`)

```kotlin
class AttractionDetailViewModel(
    private val getAttractionDetailUseCase: GetAttractionDetailUseCase,
    private val generateDescriptionUseCase: GenerateDescriptionUseCase,
    private val addToMyListUseCase: AddToMyListUseCase,
    private val removeFromMyListUseCase: RemoveFromMyListUseCase
) : ViewModel() {
    val uiState: StateFlow<AttractionDetailUiState>

    fun load(xid: String, showDistance: Boolean)
    fun loadDescription()
    fun toggleMyList()
    fun clearDescriptionError()
}
```

### 3. `GenerateDescriptionUseCase` — implementacja

Use case orkiestrujący wzbogacanie wielo-źródłowe:

```kotlin
class GenerateDescriptionUseCase(
    private val attractionRepository: AttractionRepository,
    private val webSearchService: WebSearchService,
    private val wikipediaService: WikipediaService,
    private val llmService: LlmService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(xid: String): Result<Pair<String, List<DescriptionSource>>>
}
```

Logika:
1. Pobierz dane atrakcji z `attractionRepository.getByXid(xid)` — dane bazowe z OpenTripMap
2. Równolegle (coroutineScope + async):
   - `webSearchService.search("${attraction.name} atrakcja turystyczna")`
   - `wikipediaService.getArticle(attraction.name)`
3. Zbierz dostępne wyniki (ignoruj failures — każde źródło niezależne)
4. Zbuduj prompt z dostępnych danych + system prompt agenta opisów z ustawień
5. Wywołaj `llmService.streamResponse()` — zbierz wszystkie chunki w jeden string
6. Zapisz opis i źródła przez `attractionRepository.saveDescription()`
7. Zwróć `Result.success(Pair(description, sources))`

**Przypadek brzegowy:** jeśli wszystkie 3 źródła zawiodą (dane bazowe z Room są zawsze dostępne — OpenTripMap dane zapisane przy wyszukiwaniu, więc to dotyczy web search + wiki) → generuj opis tylko na podstawie danych z Room.

**Jeśli LLM zawiedzie** → `Result.failure` z komunikatem.

Źródła uwzględnione w opisie zapisywane jako `DescriptionSource(name, url)`.

### 4. UI (`ui/AttractionDetailScreen.kt`)

Layout (LazyColumn):

```
[Nazwa atrakcji]
[Miniatura mapy — MapLibre, non-interactive, zaznaczony marker]
[Kategoria + ikona]
[Odległość od punktu X — tylko gdy showDistanceFromSearch = true]

[Sekcja opisu]
  - jeśli description != null: tekst + lista źródeł z linkami (clickable URL)
  - jeśli isDescriptionLoading: CircularProgressIndicator z "Generuję opis..."
  - jeśli descriptionError != null: komunikat błędu + przycisk "Spróbuj ponownie"
  - jeśli description == null i nie ładuje: przycisk "Załaduj opis"

[Przycisk "Nawiguj do"] → Intent do Google Maps; fallback do przeglądarki (geo: URI lub URL)
[Przycisk "Dodaj / Usuń z Mojej Listy"]
```

**Nawigacja do Google Maps:**
```kotlin
val uri = Uri.parse("google.navigation:q=$lat,$lon")
val intent = Intent(Intent.ACTION_VIEW, uri).apply {
    setPackage("com.google.android.apps.maps")
}
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    // fallback: otwórz przeglądarkę z URL Google Maps
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")))
}
```

## Testy

**`test/`**:

- `AttractionDetailViewModelTest` (JUnit + MockK):
  - `load(xid, showDistance=true)` → `uiState.showDistanceFromSearch = true`
  - `load(xid, showDistance=false)` → `uiState.showDistanceFromSearch = false`
  - `toggleMyList()` gdy `isInMyList=false` → wywołuje `addToMyListUseCase`
  - `toggleMyList()` gdy lista pełna → `uiState.error` ustawiony
  - `loadDescription()` gdy opis już istnieje → nie wywołuje `GenerateDescriptionUseCase`

- `GenerateDescriptionUseCaseTest` (JUnit + MockK):
  - Wszystkie źródła dostępne → opis wygenerowany, 3 źródła w liście
  - Wikipedia niedostępna → opis z 2 źródeł (OpenTripMap + web search)
  - Web search i Wikipedia niedostępne → opis z danych bazowych
  - LLM zwraca błąd → `Result.failure`
  - `saveDescription()` wywołane po sukcesie z poprawnym xid

## Weryfikacja ukończenia
- `./gradlew :feature-search:test` (lub moduł feature-detail) przechodzi
- Opis generuje się end-to-end (ręczny test na emulatorze z prawdziwymi kluczami API)
- Odległość widoczna tylko gdy `showDistanceFromSearch=true`
- Nawigacja do Google Maps lub fallback do przeglądarki działa
