# TASK-14: Moduł `feature-map` — ekran Mapa

## Cel
Implementacja pełnoekranowej mapy MapLibre z pinezkami atrakcji, dymkami i przełącznikiem Moja Lista.

## Zależności
- TASK-03 (`GetMyListUseCase`)
- TASK-11 (nawigacja do szczegółu)
- TASK-04 (wyniki ostatniego wyszukiwania z `AttractionRepository`)

## Zakres

### 1. Stan UI (`feature-map/viewmodel/MapUiState.kt`)

```kotlin
data class MapUiState(
    val searchResults: List<Attraction> = emptyList(),   // pinezki z wyszukiwania
    val myList: List<Attraction> = emptyList(),          // pinezki z Mojej Listy
    val showMyListOnly: Boolean = false,
    val searchCenterLocation: Location? = null,          // wyróżniona pinezka punktu X
    val selectedAttraction: Attraction? = null,          // kliknięta pinezka (dymek)
    val userLocation: Location? = null,
    val isLoading: Boolean = false
)
```

### 2. ViewModel (`feature-map/viewmodel/MapViewModel.kt`)

```kotlin
class MapViewModel(
    private val getMyListUseCase: GetMyListUseCase,
    private val attractionRepository: AttractionRepository
) : ViewModel() {
    val uiState: StateFlow<MapUiState>

    fun toggleMyListMode()
    fun selectAttraction(xid: String?)      // null = zamknij dymek
    fun onMapReady()                        // załaduj dane przy starcie
}
```

Przy starcie (`onMapReady()`):
- Załaduj `getMyListUseCase()` jako `Flow` (obserwuje zmiany)
- Załaduj wyniki ostatniego wyszukiwania z `attractionRepository.getLastSearchResults()`

### 3. UI (`feature-map/ui/MapScreen.kt`)

**Mapa MapLibre:**
- Pełnoekranowa, kafelki OSM
- Stan startowy: wyśrodkowana na lokalizacji użytkownika, bez pinezek
- Po wyszukiwaniu: pinezki atrakcji z bieżącego wyszukiwania + wyróżniona pinezka punktu X

**Typy pinezek:**
- Atrakcja z wyszukiwania: standardowy kolor (np. niebieski)
- Atrakcja z Mojej Listy: inny kolor (np. czerwony)
- Punkt X (środek wyszukiwania): wyróżniony marker (np. krzyżyk lub gwiazdka)

**Toggle „Pokaż tylko Moją Listę":**
- `Switch` lub `Chip` w rogu mapy
- Gdy aktywny: pinezki z wyszukiwania zamieniane na pinezki z Mojej Listy

**Dymek po kliknięciu pinezki:**
- Custom composable wyświetlany nad mapą (nie natywny popup MapLibre)
- Zawiera:
  - Nazwa atrakcji
  - Skrót (do 5 słów) — generowany przez LLM lub pierwsze słowa nazwy jako fallback na MVP (opis generowania przez LLM może być dodany w kolejnej iteracji)
  - Przycisk „Więcej →" → nawigacja do `Screen.AttractionDetail` z `showDistance=false`
- Kliknięcie poza dymkiem → zamknięcie dymka

### 4. Integracja MapLibre w Compose

MapLibre nie ma oficjalnego Compose wrappera — używamy `AndroidView` z `MapView`:

```kotlin
AndroidView(
    factory = { context ->
        MapView(context).also { mapView ->
            mapView.getMapAsync { mapboxMap ->
                // konfiguracja mapy
            }
        }
    },
    update = { mapView ->
        // aktualizacja pinezek gdy uiState się zmieni
    }
)
```

Lifecycle integration: `mapView` podpięty do `LocalLifecycleOwner`.

## Testy

**`feature-map/test/`**:

- `MapViewModelTest` (JUnit + MockK):
  - `toggleMyListMode()` → `showMyListOnly` przełącza się
  - `selectAttraction(xid)` → `selectedAttraction` ustawiony
  - `selectAttraction(null)` → `selectedAttraction = null`
  - Przy starcie: `getLastSearchResults()` ładowane
  - `getMyListUseCase()` obserwowany jako Flow (zmiany emitowane do uiState)

## Weryfikacja ukończenia
- `./gradlew :feature-map:test` przechodzi
- Mapa renderuje się na emulatorze
- Pinezki pojawiają się po wyszukiwaniu
- Toggle Moja Lista przełącza widoczne pinezki
- Dymek z nazwą atrakcji i przyciskiem nawigacji do szczegółu działa
