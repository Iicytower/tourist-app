# FEAT-03: Narzędzie rysowania i planowania trasy

## Cel

Dodać widok trasy w zakładce Mapa, który:
- Rysuje trasę przez atrakcje z Mojej Listy (lub wybrane przez użytkownika)
- Wyświetla kolejność zwiedzania i szacowany czas/dystans
- Umożliwia ręczne przestawianie kolejności punktów

## Zakres

### Wariant MVP (zalecany do wdrożenia)

1. **Przycisk "Zaplanuj trasę"** w `MapScreen` (obok przełącznika pinów)
2. **BottomSheet z listą punktów** — atrakcje z Mojej Listy posortowane według optymalnej kolejności (greedy nearest-neighbor po stronie klienta, bez API)
3. **Rysowanie linii na mapie** — MapLibre `LineLayer` łączący punkty w kolejności
4. **Podsumowanie** — całkowity dystans (odległość euklidesowa po sferze, Haversine) i liczba punktów

### Opcjonalnie (osobny task)
- Integracja z OSRM / Valhalla dla realistycznych tras drogowych
- Drag & drop kolejności punktów

## Architektura

### `domain`
- `RoutePoint(attraction: Attraction, order: Int)`
- `PlanRouteUseCase` — pobiera Moją Listę, oblicza greedy order, zwraca `List<RoutePoint>`

### `feature-map`
- `MapViewModel` — dodać `planRoute()` i `routePoints: StateFlow<List<RoutePoint>>`
- `RouteLayer` — composable nakładający `LineLayer` na `MapLibreMap`
- `RouteSummaryBottomSheet` — lista punktów z numeracją + dystans łączny

### `core`
- `HaversineUtils.kt` — `distanceKm(lat1, lon1, lat2, lon2): Double`

## Pliki do zmiany / stworzenia
- `core/src/.../util/HaversineUtils.kt` (nowy)
- `domain/src/.../model/RoutePoint.kt` (nowy)
- `domain/src/.../usecase/PlanRouteUseCase.kt` (nowy)
- `feature-map/src/.../viewmodel/MapViewModel.kt`
- `feature-map/src/.../ui/MapScreen.kt`
- `feature-map/src/.../ui/RouteLayer.kt` (nowy)
- `feature-map/src/.../ui/RouteSummaryBottomSheet.kt` (nowy)

## Weryfikacja
- Moja Lista zawiera ≥ 2 atrakcje → kliknięcie "Zaplanuj trasę" → linia na mapie + BottomSheet z kolejnością
- Moja Lista pusta → przycisk nieaktywny lub komunikat "Dodaj miejsca do listy"
- Linia znika po zamknięciu BottomSheet / wyłączeniu trybu trasy
