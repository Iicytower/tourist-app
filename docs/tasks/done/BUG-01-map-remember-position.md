# BUG-01: Mapa zawsze centruje się na Kraków

## Problem

W `MapScreen.kt:69-73` pozycja kamery jest hardcoded:
```kotlin
map.cameraPosition = CameraPosition.Builder()
    .target(LatLng(50.06, 19.94))
    .zoom(11.0)
    .build()
```
Po każdym uruchomieniu mapa wraca do Krakowa, ignorując gdzie użytkownik był ostatnio.

## Rozwiązanie

### 1. Nowe klucze w `AppDataStore.kt`

```kotlin
val MAP_LAST_LAT = doublePreferencesKey("map_last_lat")
val MAP_LAST_LON = doublePreferencesKey("map_last_lon")
val MAP_LAST_ZOOM = doublePreferencesKey("map_last_zoom")
```

### 2. Metody w `SettingsRepository` (domain interface)

```kotlin
suspend fun updateLastMapPosition(lat: Double, lon: Double, zoom: Double)
```

### 3. Implementacja w `DataStoreSettingsRepository`

Zapisz lat/lon/zoom do DataStore.

### 4. `AppSettings` lub oddzielny odczyt w `MapViewModel`

Opcja A (prosta): Nowy use case `GetLastMapPositionUseCase` zwracający `Triple<Double, Double, Double>?`.

Opcja B (prostsza): `MapViewModel` wstrzykuje `SettingsRepository` bezpośrednio i wczytuje pozycję przez `dataStore.data.map { ... }.first()`.

Wybierz opcję B — mniej kodu, wystarczy na potrzeby mapy.

### 5. `MapViewModel`

```kotlin
fun onMapReady(onInitialPosition: (lat: Double, lon: Double, zoom: Double) -> Unit) {
    viewModelScope.launch {
        val (lat, lon, zoom) = settingsRepository.getLastMapPosition() 
            ?: Triple(50.06, 19.94, 11.0)  // fallback Kraków
        onInitialPosition(lat, lon, zoom)
        // ... reszta logiki ładowania
    }
}

fun saveMapPosition(lat: Double, lon: Double, zoom: Double) {
    viewModelScope.launch { settingsRepository.updateLastMapPosition(lat, lon, zoom) }
}
```

### 6. `MapScreen.kt`

- W `getMapAsync` callback: odczytaj pozycję z VM zamiast hardcode
- Dodaj listener `map.addOnCameraIdleListener` → wywołaj `viewModel.saveMapPosition(lat, lon, zoom)`

## Pliki do zmiany
- `data/src/.../local/AppDataStore.kt`
- `domain/src/.../repository/SettingsRepository.kt`
- `data/src/.../repository/DataStoreSettingsRepository.kt`
- `feature-map/src/.../viewmodel/MapViewModel.kt`
- `feature-map/src/.../ui/MapScreen.kt`

## Weryfikacja
- Ustaw widok mapy na inne miasto, wyjdź z aplikacji (lub zmień zakładkę i wróć)
- Mapa wraca do ostatnio oglądanego miejsca
- Przy pierwszym uruchomieniu (brak zapisanej pozycji) fallback = Kraków
