# FEAT-04 — Propozycja implementacji (zweryfikowana, 2026-07-04)

## Ważna korekta vs task description

Task był pisany z założeniem, że search używa OTM. **W rzeczywistości OTM jest poza pipeline'em** —
`openTripMapModule` używa `FakeOpenTripMapClient` i nie trafia do grafu Koina.
Produkcyjne źródło to `CompositeAttractionSource` = Overpass + Wikipedia GeoSearch + Wikidata SPARQL.
Propozycja poniżej opiera się na rzeczywistej architekturze.

## Diagnoza "śmieciowych" wyników

| Źródło | Stan | Problem |
|---|---|---|
| **Overpass/OSM** | dobry | Dobra whitelist tagów w `OverpassQueryBuilder`, działa |
| **Wikidata SPARQL** | dobry | Whitelist Q-typów (zamki, kościoły, muzea...) — poprawnie filtruje |
| **Wikipedia GeoSearch** | PROBLEM | Zwraca 50 artykułów Wikipedia obok danej lokalizacji bez filtrowania typów. Ulice, dzielnice, rzeki, sportowcy — dostają kategorię `MUSEUMS_AND_GALLERIES` przez catch-all w `inferCategory()` |

## Rozwiązanie: LLM filtr na żądanie

### Co NIE zmieniamy
- Overpass i Wikidata działają dobrze — nie ruszamy
- `WikipediaGeoSearchSource.inferCategory()` — nie tuningujemy słów kluczowych; LLM będzie bramką jakości
- OTM pozostaje poza pipeline'em

### Nowe pliki

**domain:**
```
domain/.../repository/AttractionQualityFilter.kt   ← interfejs
domain/.../usecase/FilterAttractionsByQualityUseCase.kt
```

**data:**
```
data/.../remote/llmfilter/LlmAttractionQualityFilter.kt   ← implementacja przez OpenRouter
data/.../remote/llmfilter/LlmFilterModule.kt              ← Koin binding
```

**feature-search:**
- `SearchUiState` — dodać `isFiltering: Boolean = false`
- `SearchViewModel` — dodać `filterByQuality()`
- `SearchScreen` — przycisk "Przefiltruj wyniki"
- `SearchModule` — wstrzyknąć `FilterAttractionsByQualityUseCase`

### Protokół LLM (kluczowe decyzje projektowe)

**Numerowanie, nie xid-y** — LLM dostaje ponumerowaną listę 1..N. Zwraca indeksy do zachowania.
Xid-y są opaque (`wg12345`, `wdQ234`) i LLM je mangluje. Mapowanie: w kodzie, nie przez LLM.

**Fail-open** — przy błędzie parsowania zwracamy oryginalną listę bez zmian.

**Jeden batch, jeden prompt** — wszystkie atrakcje w jednym `completeChat()` z tool `web_search`.
LLM sam decyduje, dla których niepewnych przypadków wywołać Tavily. Nie pętlamy per-atrakcja.

**Tool calling loop** — taki sam kształt jak `runChatLoop()` w `AssistantViewModel`.

**Wyniki filtru są obserwowalne** — filtr zwraca też listę usuniętych z krótkim uzasadnieniem (do logowania / opcjonalnie UI).

### Model LLM
`google/gemini-flash-2.5` (przez OpenRouter)

### Przykładowy prompt filtru
```
Jesteś filtrem jakości atrakcji turystycznych. Dostałeś listę miejsc znalezionych w okolicy.
Zachowaj TYLKO te, które są realnymi atrakcjami turystycznymi z poniższych kategorii:
[Muzea, Galerie, Parki przyrody, Rezerwaty, Zabytki, ... (50 kategorii)]

Możesz wywołać web_search dla niepewnych przypadków.

Odpowiedz JSON: {"keep": [1, 3, 5], "removed": [{"index": 2, "reason": "ulica, nie atrakcja"}]}

Lista miejsc:
1. Wawel | Zamki | lat: 50.054 lon: 19.936
2. Ulica Floriańska | Muzea i galerie | lat: ...
...
```

### UI
Przycisk "Przefiltruj wyniki" widoczny gdy: `hasSearched && results.isNotEmpty() && !isFiltering`
Podczas filtrowania: `CircularProgressIndicator` + tekst "Analizuję wyniki..."
Po filtrze: lista zaktualizowana w pamięci (Room nie nadpisywany)

## Pliki do modyfikacji (podsumowanie)

| Plik | Typ zmiany |
|---|---|
| `domain/.../repository/AttractionQualityFilter.kt` | NOWY |
| `domain/.../usecase/FilterAttractionsByQualityUseCase.kt` | NOWY |
| `data/.../remote/llmfilter/LlmAttractionQualityFilter.kt` | NOWY |
| `data/.../remote/llmfilter/LlmFilterModule.kt` | NOWY |
| `feature-search/.../SearchUiState.kt` | dodać `isFiltering` |
| `feature-search/.../SearchViewModel.kt` | dodać `filterByQuality()` |
| `feature-search/.../SearchScreen.kt` | przycisk |
| `feature-search/di/SearchModule.kt` | wstrzyknąć use case |
| `app/WanderListApp.kt` | dodać `llmFilterModule` |
