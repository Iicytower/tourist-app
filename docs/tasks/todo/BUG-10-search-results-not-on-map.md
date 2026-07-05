# BUG-10: Wyniki wyszukiwania nigdy nie pojawiają się na mapie

## Problem

`MapScreen.kt:136`:

```kotlin
val attractions = if (state.showMyListOnly) state.myList else emptyList()
```

Poza trybem „Moja Lista” warstwa pinezek (`attractions-layer`) jest zawsze pusta. `state.searchResults` (o ile w ogóle istnieje w `MapUiState`) nigdzie nie jest użyte. Użytkownik wyszukuje atrakcje w zakładce Szukaj, przechodzi do zakładki Mapa — mapa jest pusta, mimo że wyniki wyszukiwania istnieją.

Dodatkowo w tym samym pliku (`MapScreen.kt:121-127`) `addOnMapClickListener` odpytuje tylko `"attractions-layer"` — skoro ta warstwa jest pusta poza trybem „Moja Lista”, kliknięcie w pinezkę wyniku wyszukiwania nigdy nie zaznacza atrakcji (ten sam root cause).

## Cel

Domyślny widok mapy (poza trybem „Moja Lista”) ma pokazywać ostatnie wyniki wyszukiwania.

## Zakres zmian — do zbadania przy implementacji

Zależność `feature-map` → `domain` (bez `data`) oznacza, że `MapViewModel` nie może bezpośrednio odpytać encji Room ani stanu `SearchViewModel` (inny moduł, inny ViewModel). Do ustalenia, skąd `MapViewModel` ma wziąć wyniki ostatniego wyszukiwania — kandydaci:

1. **Wspólne źródło przez Room** — atrakcje z ostatniego wyszukiwania są już oznaczane flagą `isFromLastSearch` w `AttractionEntity`/`AttractionDao` (używaną też przy `deleteOrphans`). `MapViewModel` może odpytać use case zwracający atrakcje z `isFromLastSearch = true`, analogicznie do tego jak `SearchViewModel` je czyta.
2. **Współdzielony stan między modułami** — jeśli wyszukiwanie i mapa mają być zsynchronizowane w czasie rzeczywistym (a nie tylko „ostatnie zapisane wyszukanie”), rozważyć wspólny use case/repository jako źródło prawdy zamiast per-ViewModel state.

Zalecenie: zacząć od opcji 1 (najmniejsza zmiana, wykorzystuje istniejący mechanizm `isFromLastSearch`), o ile use case do odczytu takich atrakcji już istnieje w `domain` (sprawdzić) lub dodać go.

- `feature-map/src/.../viewmodel/MapViewModel.kt` — dodać pobieranie wyników ostatniego wyszukiwania, wystawić w `MapUiState`.
- `feature-map/src/.../ui/MapScreen.kt:136` — użyć nowego pola zamiast `emptyList()`.
- `feature-map/src/.../ui/MapScreen.kt:121-127` — upewnić się, że `addOnMapClickListener` trafia też w pinezki wyników wyszukiwania.
- `domain/src/.../usecase/` — nowy use case, jeśli nie istnieje odpowiedni do odczytu atrakcji `isFromLastSearch`.

## Weryfikacja

- Wyszukanie atrakcji w zakładce Szukaj → przejście do zakładki Mapa (bez trybu „Moja Lista”) → pinezki wyników widoczne na mapie.
- Kliknięcie pinezki wyniku wyszukiwania zaznacza atrakcję (tak jak już działa dla „Mojej Listy”).
- Włączenie trybu „Moja Lista” nadal pokazuje tylko listę, bez zmian w dotychczasowym zachowaniu.
