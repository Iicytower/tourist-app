# TASK-12: Moduł `feature-search` — ekran Szukaj

## Cel
Implementacja ekranu wyszukiwania atrakcji: wybór punktu X (GPS/mapa/nazwa), suwak promienia, lista wyników z paginacją, sortowanie.

## Zależności
- TASK-03 (use case'y `SearchAttractionsUseCase`)
- TASK-10 (LocationService — przez use case)
- TASK-11 (nawigacja do szczegółu)

## Zakres

### 1. Stan UI (`feature-search/viewmodel/SearchUiState.kt`)

```kotlin
data class SearchUiState(
    val searchLocation: Location? = null,
    val searchLocationLabel: String = "",        // czytelna nazwa punktu X
    val radiusKm: Int = AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val selectedCategories: Set<AttractionCategory> = emptySet(),
    val results: List<Attraction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.BY_DISTANCE,
    val hasSearched: Boolean = false             // czy kiedykolwiek wyszukano
)

enum class SortOrder { BY_DISTANCE, BY_CATEGORY }
```

### 2. ViewModel (`feature-search/viewmodel/SearchViewModel.kt`)

```kotlin
class SearchViewModel(
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val locationService: LocationService
) : ViewModel() {
    val uiState: StateFlow<SearchUiState>

    fun setLocationFromGps()                              // wywołuje LocationService
    fun setLocationFromCoordinates(lat: Double, lon: Double, label: String)
    fun setRadius(radiusKm: Int)
    fun setCategories(categories: Set<AttractionCategory>)
    fun setSortOrder(order: SortOrder)
    fun search()                                          // wywołuje use case
    fun clearError()
}
```

### 3. UI (`feature-search/ui/SearchScreen.kt`)

**Sekcja wyboru punktu X:**
- Pole tekstowe z wyszukiwaniem nazwy miejsca (geocoding przez OpenTripMap lub Nominatim — do decyzji podczas implementacji; na MVP wystarczy wpisanie koordynat lub GPS)
- Przycisk „Moja lokalizacja" (GPS)
- Przycisk „Wybierz na mapie" → mini-mapa (MapLibre) do ręcznego wskazania
- Wyświetlanie aktualnie wybranego punktu jako tekst/etykieta

**Sekcja filtrów:**
- Suwak promienia 1–50 km z etykietą wartości
- Opcjonalne: checkboxy kategorii (zwijane, domyślnie schowane)

**Przycisk „Szukaj"** — aktywny tylko gdy punkt X jest wybrany.

**Lista wyników:**
- `LazyColumn` z paginacją (wczytywanie kolejnych stron przy scroll do końca)
- Każdy element listy: nazwa, kategoria z ikoną, odległość, znacznik czy opis już załadowany
- Kliknięcie elementu → nawigacja do `Screen.AttractionDetail`
- Sortowanie: ikona/przycisk zmiany kolejności (odległość / kategoria)

**Stany:**
- Loading: `CircularProgressIndicator`
- Błąd: `Snackbar` lub inline komunikat z `error` z `UiState`
- Pusta lista: komunikat „Brak atrakcji w promieniu X km. Spróbuj zwiększyć promień."
- Stan startowy (brak wyszukania): widok wyboru punktu i filtrów

### 4. Mini-mapa do wyboru punktu

Komponent `LocationPickerMap` (Compose + MapLibre):
- Pełna (lub dialog) mapa MapLibre
- Tap na mapie → ustawia koordynaty punktu X w ViewModelu
- Potwierdzenie przyciskiem „Wybierz"

## Testy

**`feature-search/test/`**:

- `SearchViewModelTest` (JUnit + MockK, TestCoroutineDispatcher):
  - `search()` z pustymi wynikami → `uiState.results = emptyList()`, `isLoading = false`
  - `search()` z błędem z use case → `uiState.error != null`
  - `setLocationFromGps()` gdy `LocationService` zwraca błąd → `uiState.error` ustawiony
  - `setSortOrder(BY_CATEGORY)` → lista posortowana po kategorii
  - `search()` → `isLoading = true` podczas wykonania, `false` po zakończeniu
  - `clearError()` → `uiState.error = null`

## Weryfikacja ukończenia
- `./gradlew :feature-search:test` przechodzi
- Ekran renderuje się na emulatorze
- Wyszukiwanie z GPS i ręcznym punktem działa end-to-end
- Paginacja: scroll do końca ładuje kolejne wyniki
- Sortowanie zmienia kolejność listy
