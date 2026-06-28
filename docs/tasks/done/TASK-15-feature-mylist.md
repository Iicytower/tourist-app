# TASK-15: Moduł `feature-mylist` — ekran Moja Lista

## Cel
Implementacja ekranu listy zapisanych atrakcji (bucket list): przeglądanie, sortowanie, usuwanie, dostęp offline.

## Zależności
- TASK-03 (`GetMyListUseCase`, `RemoveFromMyListUseCase`)
- TASK-10 (`LocationService` — do sortowania wg odległości)
- TASK-11 (nawigacja do szczegółu)

## Zakres

### 1. Stan UI (`feature-mylist/viewmodel/MyListUiState.kt`)

```kotlin
data class MyListUiState(
    val attractions: List<Attraction> = emptyList(),
    val sortOrder: MyListSortOrder = MyListSortOrder.DATE_ADDED,
    val isLoading: Boolean = false,
    val userLocation: Location? = null,              // null gdy GPS niedostępny
    val itemCount: Int = 0                           // do wyświetlenia "X / 50"
)

enum class MyListSortOrder {
    DATE_ADDED,
    DISTANCE,       // wymaga userLocation
    NAME,
    CATEGORY
}
```

### 2. ViewModel (`feature-mylist/viewmodel/MyListViewModel.kt`)

```kotlin
class MyListViewModel(
    private val getMyListUseCase: GetMyListUseCase,
    private val removeFromMyListUseCase: RemoveFromMyListUseCase,
    private val locationService: LocationService
) : ViewModel() {
    val uiState: StateFlow<MyListUiState>

    fun setSortOrder(order: MyListSortOrder)
    fun removeAttraction(xid: String)
    fun refreshUserLocation()
}
```

`getMyListUseCase()` zwraca `Flow<List<Attraction>>` — ViewModel obserwuje automatycznie. Sortowanie nakładane po stronie ViewModel (nie w Room) — dane z Flow sortowane zgodnie z `sortOrder` przed ustawieniem w `uiState`.

Sortowanie wg odległości: obliczane przez `DistanceUtils.calculateDistanceKm()` (z `core`) między lokalizacją użytkownika a koordynatami atrakcji.

### 3. UI (`feature-mylist/ui/MyListScreen.kt`)

**Nagłówek:**
- Tytuł „Moja Lista"
- Licznik „X / 50"
- Dropdown lub ikona zmiany sortowania

**Lista (`LazyColumn`):**
Każdy element:
- Nazwa atrakcji
- Kategoria z ikoną
- Data dodania (sformatowana: „Dodano 15 czerwca 2026")
- Opis (skrócony do 2 linii, jeśli był załadowany)
- Odległość od bieżącej lokalizacji — widoczna tylko gdy `userLocation != null` i GPS aktywny

**Akcje:**
- Kliknięcie elementu → `Screen.AttractionDetail` z `showDistance=false`
- Długie przytrzymanie → `DropdownMenu` z opcjami:
  - „Usuń z listy" → `removeAttraction(xid)` z potwierdzeniem (AlertDialog)
  - „Otwórz na mapie" → nawigacja do `Screen.Map` (i zaznaczenie pinezki — do ustalenia przy implementacji)

**Stan pusty:**
- Komunikat „Twoja lista jest pusta. Zacznij od wyszukiwania atrakcji."

**Tryb offline:**
- Lista wyświetla się bez internetu (dane z Room)
- Opis wyświetla się bez internetu (jeśli był załadowany wcześniej)
- Brak internetu: baner informacyjny na górze (subtelny, nie blokujący)

## Testy

**`feature-mylist/test/`**:

- `MyListViewModelTest` (JUnit + MockK):
  - `getMyListUseCase()` emituje listę → `uiState.attractions` zaktualizowane
  - `setSortOrder(NAME)` → lista posortowana alfabetycznie
  - `setSortOrder(DISTANCE)` gdy `userLocation = null` → brak sortowania (fallback do DATE_ADDED) lub info o braku GPS
  - `removeAttraction(xid)` → wywołuje `removeFromMyListUseCase`
  - `removeAttraction(xid)` gdy `removeFromMyListUseCase` zwraca błąd → błąd w uiState
  - Licznik `itemCount` zgodny z długością listy

## Weryfikacja ukończenia
- `./gradlew :feature-mylist:test` przechodzi
- Lista działa offline (dane z Room)
- Usuwanie z listy z potwierdzeniem dialogiem działa
- Sortowanie wszystkimi czterema kryteriami działa
- Odległość widoczna tylko gdy GPS aktywny
