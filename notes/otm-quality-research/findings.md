# OpenTripMap API — Wyniki researchu (2026-07-04)

## Zweryfikowane fakty (głosowanie 3-0)

### Parametr `rate`
- Skala: **1–3** (nie 0–7), dostępne wartości: `1`, `2`, `3`, `1h`, `2h`, `3h`
- Sufiks `h` oznacza obiekt dziedzictwa kulturowego (cultural heritage)
- Można podać jako **parametr zapytania do endpointu /radius** — filtruje po stronie API
- Przykład z oficjalnej dokumentacji: `rate=2` (w URL zapytania /radius)
- Przykład z gista: `rate=3` — zwraca tylko najwyżej oceniane obiekty
- Filtrowanie po `rate` działa po stronie serwera → mniej danych do pobrania

### Limity API (plan darmowy)
- **5 000 requestów / dzień**
- Synthesis step failnął przez session limit — informacje o rate limit per-second i ograniczeniu do non-commercial nie zostały zweryfikowane

### Sorting
- Brak potwierdzenia dla `order_by=rate` — nie zweryfikowane w tej sesji

## Źródła
- https://dev.opentripmap.org/docs (primary)
- https://dev.opentripmap.org/examples (primary)
- https://dev.opentripmap.org/price (primary)
- https://dev.opentripmap.org/product (primary)
- https://gist.github.com/srdelarosa/67cc731fe90a64675d8e6c9ff1fdec09 (forum)

## Wnioski dla implementacji

1. **Dodać `rate=2` do parametrów /radius** — filtruje śmieciowe obiekty już na poziomie API
2. Wartości `rate=2` i `rate=3` = obiekty o średniej i wysokiej popularności
3. `1h`, `2h`, `3h` = dziedzictwo kulturowe — wartościowe dla aplikacji turystycznej
4. Whitelist `kinds` już działająca w kodzie pokrywa wartościowe kategorie
5. 5000 req/dzień to wystarczający limit dla aplikacji niekomercyjnej

## Pominięte przez session limit
- Weryfikacja rate limit per-second
- Weryfikacja non-commercial restriction
- Weryfikacja `order_by=rate`
- Kinds catalog — wartościowe vs śmieciowe (nie zbadane w tym researchu)
