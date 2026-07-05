# BUG-14: Wolniejsze wyszukiwanie potrafi nadpisać nowsze wyniki

## Problem

`feature-search/.../viewmodel/SearchViewModel.kt:155-182`, `search()` nie trzyma ani nie anuluje żadnego `Job`, w przeciwieństwie do `updateLocationQuery()` w tym samym pliku, które poprawnie robi debounce + cancel poprzedniego joba.

**Scenariusz:** użytkownik zmienia promień wyszukiwania i klika „Szukaj”, zanim poprzednie zapytanie zdąży wrócić (albo zmienia lokalizację i szuka ponownie). Starsze, wolniejsze zapytanie sieciowe kończy się później niż nowsze — nadpisuje `results` w `SearchUiState` przestarzałymi danymi z poprzedniego zapytania, mimo że użytkownik już zainicjował nowsze.

Powiązany, ten sam mechanizm: `filterByQuality()` (linie 184-204) nie jest chronione przed równoległym `search()` — zakończenie filtrowania jakości może nadpisać świeże wyniki wyszukiwania starą, przefiltrowaną listą.

## Cel

Tylko wynik najnowszego wywołania `search()` powinien trafić do stanu UI — starsze, opóźnione odpowiedzi mają być ignorowane/anulowane.

## Zakres zmian

- `feature-search/.../viewmodel/SearchViewModel.kt:155-182` — trzymać referencję do aktywnego `Job` wyszukiwania (analogicznie do `updateLocationQuery`), anulować poprzedni przed uruchomieniem nowego `search()`.
- Zweryfikować interakcję z `filterByQuality()` (linie 184-204) — upewnić się, że wywołanie `search()` w trakcie trwania filtrowania albo anuluje filtrowanie, albo filtrowanie nie nadpisuje wyników nowszego wyszukiwania (np. przez sprawdzenie czy operuje na tej samej „generacji” wyników).

## Weryfikacja

- Szybkie, kolejne wywołania `search()` (np. zmiana promienia + ponowne kliknięcie „Szukaj” przed odpowiedzią) → w UI widoczne są tylko wyniki najnowszego zapytania.
- Wywołanie `filterByQuality()` przerwane/nadpisane przez nowe `search()` nie psuje wyświetlanych wyników.
