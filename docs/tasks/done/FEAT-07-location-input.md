# FEAT-07: Ulepszone wprowadzanie lokalizacji do wyszukiwania

## Problem

Pole "lokalizacja" w SearchScreen wymaga wpisania pełnej nazwy miejscowości i potwierdzenia. Brak podpowiedzi podczas pisania sprawia, że użytkownik nie wie, czy jego fraza zostanie rozpoznana. Nie ma też możliwości wskazania punktu wyszukiwania bezpośrednio na mapie.

## Cel

1. **Podpowiedzi geokodowania** — podczas wpisywania nazwy pojawiają się sugestie adresów, tap na sugestię ustawia lokalizację.
2. **Wybór punktu na mapie** — przycisk otwiera mini-mapę (MapLibre), tap na niej ustawia lokalizację do wyszukiwania i wraca do SearchScreen.

## Zakres zmian

### Podpowiedzi (autocomplete)

Źródło: **Nominatim** (`/search?q=...&format=json&limit=5`) — projekt już go używa do geokodowania.

- `GeocoderService` (domain) ma `geocode(query)` → rozszerzyć o `suggest(query): List<GeocodeSuggestion>`
  - Model `GeocodeSuggestion(displayName: String, lat: Double, lon: Double)`
- `NominatimGeocoderService` implementuje `suggest()`: `GET /search?q={query}&format=json&limit=5&addressdetails=0`
- `SearchViewModel`: po wpisaniu ≥ 3 znaków w polu lokalizacji, po debounce 400 ms, wywołuje `suggest()`; wynik trafia do `SearchUiState.locationSuggestions`
- `SearchScreen`: pod polem lokalizacji `DropdownMenu` / `LazyColumn` z sugestiami; tap → `viewModel.selectSuggestion(suggestion)`

### Wybór punktu na mapie

Nowy ekran `LocationPickerScreen` (lub `LocationPickerDialog` — ModalBottomSheet z mapą):
- Wyświetla `MapView` (MapLibre) z pinem w centrum
- Użytkownik przesuwa mapę — pin pozostaje na środku (floating pin, ruchoma mapa)
- Przycisk "Wybierz tę lokalizację" → odczytuje `map.cameraPosition.target`, wywołuje callback z `(lat, lon)`
- Przycisk "Anuluj"
- Tryb wejścia: otwierany z ostatniej pozycji mapy lub bieżącej lokalizacji GPS

### Nawigacja

Opcja A (bottom sheet): `LocationPickerScreen` otwierany przez `ModalBottomSheet` wewnątrz `SearchScreen` — nie wymaga nowej trasy.
Opcja B (osobna trasa): `Screen.LocationPicker` → `SearchScreen` przekazuje wynik przez SharedViewModel lub `navController.previousBackStackEntry`.

**Rekomendacja: Opcja A** — prostsze, bez modyfikacji grafu nawigacji.

### Integracja z SearchViewModel

- Nowe pole `locationSuggestions: List<GeocodeSuggestion>` w `SearchUiState`
- Nowe pole `showLocationPicker: Boolean`
- `selectSuggestion(s)` → ustawia `searchLocation`, `searchLocationLabel`, czyści sugestie
- `openLocationPicker()` / `onLocationPicked(lat, lon)` → reverse geocode przez Nominatim, ustawia `searchLocationLabel`

## Pliki do zmiany

- `domain/repository/GeocoderService.kt` — nowa metoda `suggest()`
- `domain/model/GeocodeSuggestion.kt` — nowy model
- `data/remote/nominatim/NominatimGeocoderService.kt` — implementacja `suggest()`
- `feature-search/viewmodel/SearchUiState.kt` — nowe pola
- `feature-search/viewmodel/SearchViewModel.kt` — debounce suggest, picker flow
- `feature-search/ui/SearchScreen.kt` — dropdown z sugestiami, przycisk mapy, bottom sheet pickera

## Weryfikacja

- Wpisanie ≥ 3 znaków w polu lokalizacji powoduje pojawienie się sugestii po ~400 ms
- Tap na sugestię ustawia lokalizację i zamyka dropdown
- Przycisk ikony mapy przy polu lokalizacji otwiera picker
- Przesunięcie mapy i "Wybierz" ustawia lokalizację (reverse geocode jako etykieta)
- Wyszukiwanie działa poprawnie po wyborze przez obie metody
