# BUG-06: Usunąć debugowy widok wyników wyszukiwania + zbadać brak wyników z Overpass/OSM

## Problem

1. `SearchScreen` zawsze renderuje kartę `"[DEBUG] Wyniki per zrodlo:"` (gdy `state.debugSourceStats` nie jest puste), bez guardu na build variant. To zostało w kodzie z fazy developmentu i trafia też do usera na buildzie release.
2. Overpass API (jedno ze źródeł w `CompositeAttractionSource` obok Wikipedia GeoSearch i Wikidata SPARQL) w praktyce nie zwraca wyników — przyczyna nieznana, wymaga zbadania.

## Zakres zmian

### Usunięcie debug UI

- `feature-search/.../ui/SearchScreen.kt` (linie ok. 188-204) — usunąć `Card` z `"[DEBUG] Wyniki per zrodlo:"`, albo opakować w `if (BuildConfig.DEBUG)` jeśli statystyki per źródło mają zostać przydatne deweloperowi (do ustalenia przy implementacji — prościej po prostu usunąć, skoro i tak wymaga śledztwa poniżej).
- `feature-search/.../viewmodel/SearchUiState.kt` — pole `debugSourceStats` i wypełnianie go w `SearchViewModel.search()` (linia ok. 155-182) usunąć razem z UI, o ile nie jest wykorzystywane nigdzie indziej.

### Śledztwo: dlaczego Overpass/OSM nie zwraca wyników

- `data/src/.../remote/overpass/OverpassApiClient.kt` — sprawdzić realną odpowiedź HTTP z `https://overpass-api.de/api/interpreter` (status, body) dla przykładowego zapytania — obecnie błąd jest cicho łykany: `CompositeAttractionSource.kt` (linia ok. 21) robi `.getOrElse { emptyList() }`, więc np. `429 Too Many Requests`, timeout, czy błąd parsowania nie są widoczne nigdzie (brak loga przez Timber).
- Sprawdzić `OverpassQueryBuilder.build(params)` — czy generowane zapytanie Overpass QL jest poprawne (przetestować ręcznie na `overpass-api.de` lub `overpass-turbo.eu` z tymi samymi parametrami).
- Sprawdzić `OverpassMapper.toAttraction` — filtruje elementy bez `name`/`name:pl`/`name:en` i bez współrzędnych (`return null`); zweryfikować czy to nie odrzuca zbyt dużo realnych wyników (np. węzły bez tagu `name` w ogóle, tylko `name:en`).
- Sprawdzić czy brakuje nagłówka `User-Agent` w requeście — publiczne API Overpass czasem throttluje/blokuje zapytania bez niego.
- Zweryfikować martwy, niepodłączony `data/.../di/OverpassModule.kt` (`overpassModule`) — nie jest ładowany w `WanderListApp.kt` (tylko `attractionSourceModule` jest), więc nie wpływa na problem, ale to zbędny duplikat do usunięcia przy okazji, żeby nie mylił przy kolejnym debugowaniu.
- Po ustaleniu przyczyny: dodać logowanie błędów przez Timber w `CompositeAttractionSource` zamiast cichego `getOrElse { emptyList() }`, żeby przyszłe podobne problemy było widać w logach.

## Pliki do zmiany

- `feature-search/src/.../ui/SearchScreen.kt`
- `feature-search/src/.../viewmodel/SearchUiState.kt`
- `feature-search/src/.../viewmodel/SearchViewModel.kt`
- `data/src/.../remote/overpass/OverpassApiClient.kt`
- `data/src/.../remote/overpass/OverpassQueryBuilder.kt`
- `data/src/.../remote/overpass/OverpassMapper.kt`
- `data/src/.../remote/composite/CompositeAttractionSource.kt`
- `data/src/.../di/OverpassModule.kt` (do usunięcia, jeśli potwierdzone jako martwy kod)

## Weryfikacja

- Widok wyszukiwania nie pokazuje żadnej karty "[DEBUG] ..." w żadnym buildzie
- Wyszukiwanie w miejscu z dobrym pokryciem OSM (np. duże miasto) zwraca wyniki z Overpass (widoczne w statystykach/logach deweloperskich, nie w UI usera)
- Błędy sieciowe/API z Overpass są logowane przez Timber zamiast cicho ginąć
