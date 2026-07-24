# BUG-09: Odwrócona logika wyboru najlepszej lokalizacji GPS

## Problem

`AndroidLocationService.kt:45`, `tryGetLastKnown()`:

```kotlin
providers.mapNotNull { locationManager.getLastKnownLocation(it) }
    .filter { /* świeżość */ }
    .maxByOrNull { it.accuracy }
```

W Android Location API `Location.getAccuracy()` zwraca promień błędu w metrach — **mniejsza wartość oznacza lepszą precyzję**. `maxByOrNull { it.accuracy }` wybiera więc odczyt z **największym** promieniem błędu, czyli systematycznie najgorszą dostępną pozycję spośród wszystkich providerów (GPS, sieć).

## Cel

Wybierać odczyt o najmniejszym promieniu błędu (najdokładniejszy).

## Zakres zmian

- `data/src/main/kotlin/com/iicytower/wanderlist/data/local/location/AndroidLocationService.kt:45` — zamienić `maxByOrNull { it.accuracy }` na `minByOrNull { it.accuracy }`.

## Weryfikacja

- Przy dostępnych odczytach z kilku providerów o różnej `accuracy`, `tryGetLastKnown()` zwraca ten o najmniejszej wartości `accuracy` (najdokładniejszy).
- Test jednostkowy (jeśli istnieje pokrycie dla tej klasy) potwierdzający wybór minimalnej `accuracy` spośród świeżych odczytów.
