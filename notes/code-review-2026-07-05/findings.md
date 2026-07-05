# Pełny przegląd kodu WanderList (2026-07-05)

Przegląd całej bazy kodu (nie diff) — 10 modułów, ~11 000 linii, wykonany przez 6 równoległych agentów (core+domain, data/local, data/remote, app+feature-detail+feature-settings, feature-search+feature-map, feature-mylist+feature-assistant).

Artefakt z wizualną wersją: https://claude.ai/code/artifact/814ca9b2-5cee-49b6-bd03-276bb96d8f7a

## Krytyczne

1. **`data/remote/.../HttpClientProvider.kt:18-23`** — Klucz API OpenRouter (Bearer token) trafia do logów, także w release. `Logging { level = LogLevel.HEADERS }` bez `sanitizeHeader` i bez gate'a na `BuildConfig.DEBUG`.

2. **`data/local/.../RoomAttractionRepository.kt:41`, `AttractionDao.kt:23-52`** — Ponowne wyszukanie kasuje opis atrakcji i wypina ją ze wszystkich list. `upsertAll` z `OnConflictStrategy.REPLACE` robi DELETE+INSERT, nadpisując wiersz „pustymi” danymi ze zdalnego mappera; FK CASCADE kasuje wpisy w `attraction_list_crossref`.

3. **`data/local/.../AndroidLocationService.kt:45`** — Wybór lokalizacji z GPS bierze najgorszy odczyt zamiast najlepszego. `tryGetLastKnown()` robi `maxByOrNull { it.accuracy }`, a mniejsza wartość `accuracy` = lepsza precyzja.

4. **`feature-map/.../MapScreen.kt:136`** — Wyniki wyszukiwania nigdy nie pojawiają się na mapie. `val attractions = if (state.showMyListOnly) state.myList else emptyList()` — `state.searchResults` nigdzie nie jest użyte.

5. **`feature-settings/.../SettingsScreen.kt:330-341`** — Klucze API zawsze widoczne w plaintext. Infrastruktura maskowania (`openRouterKeyVisible`, `toggleOpenRouterKeyVisibility()`) istnieje w ViewModelu, ale żaden composable jej nie używa.

6. **`feature-assistant/.../AssistantViewModel.kt:186-236`** — Asystent może kasować listy i nadpisywać plany podróży bez potwierdzenia użytkownika. `remove_from_list`/`update_trip_plan` wykonują się natychmiast; treść z `web_search` trafia do kontekstu LLM (ryzyko prompt injection).

## Poważne

7. **`domain/.../AddToMyListUseCase.kt:11-17`** — Limit 50 pozycji w Mojej Liście można ominąć współbieżnym dodaniem (TOCTOU, brak transakcji).

8. **`feature-mylist/.../RoomTripListRepository.kt:48-55`** — Ten sam problem TOCTOU dla list podróży — `addToList()` bez `@Transaction`.

9. **`domain/.../GenerateTripPlanUseCase.kt:45-65`, `data/remote/.../LlmAttractionQualityFilter.kt:52-75`** — Pętle tool-call LLM bez górnego limitu iteracji — model może zawiesić generowanie planu/filtrowanie bez timeoutu.

10. **`feature-map/.../MapScreen.kt:84-93`, `feature-search/.../SearchScreen.kt:290-321`** — MapView nigdy nie jest zwalniany (`DisposableEffect` z pustym `onDispose`) — wyciek natywnych zasobów GL przy każdym otwarciu ekranu/pickera.

11. **`feature-search/.../SearchViewModel.kt:155-182`** — `search()` nie anuluje poprzedniego joba — wolniejsze, starsze zapytanie może nadpisać świeższe wyniki.

12. **`data/local/.../TripListDao.kt:37-43`** — `getListsForAttraction` na sztywno zwraca `attractionCount = 0` zamiast realnego joina.

13. **`data/remote/.../CompositeAttractionSource.kt:21`** + Nominatim/Wikipedia/Wikidata bez sprawdzania statusu HTTP — błędy sieciowe (429, timeout) znikają cicho jako „brak wyników”.

14. **`data/remote/.../OpenTripMapModule.kt`, `OverpassModule.kt`** — Cała integracja OpenTripMap to martwy kod niepodłączony do DI; `RealOpenTripMapClient` ma klucz API w URL (ten sam wzorzec ryzyka co #1), ale jest nieosiągalny.

15. **`data/remote/.../LlmMapper.kt:84-93`, `OpenRouterLlmService.kt:69`** — Ścieżka SSE streaming jest martwa i zepsuta, gdyby ją włączyć (parser zakłada kompletny JSON per chunk, timeout ustawiony na `Long.MAX_VALUE`).

16. **`feature-settings/.../SettingsScreen.kt:227-234`** — Szybkie zaznaczanie kilku zainteresowań gubi wcześniejsze wybory (lost update na bazie nieodświeżonego stanu).

## Do poprawy

17. `feature-detail/.../AttractionDetailViewModel.kt:33-58` — kolektory się nie anulują przy ponownym `load()`.
18. `app/.../WanderListNavGraph.kt:145,149` — nazwa listy w route nawigacji nie jest kodowana URL.
19. `feature-search/.../SearchViewModel.kt:72-85` — sugestie lokalizacji mogą wrócić po zamknięciu listy (brak anulowania `suggestJob`).
20. `feature-map/.../MapViewModel.kt:24-46` — `onMapReady()` bez zabezpieczenia przed wielokrotnym wywołaniem, resetuje kamerę.
21. `data/local/.../AttractionDao.kt:32` — osierocone atrakcje z opisem nigdy nie są czyszczone (niekontrolowany wzrost DB).
22. `feature-settings/.../SettingsViewModel.kt:84-96` — przycisk „Przetestuj połączenie” po cichu zapisuje klucz API.
23. `data/remote/.../WikipediaServiceImpl.kt:17,25` — `URLEncoder` użyty w segmencie ścieżki zamiast query string (możliwe 404 dla wieloczłonowych tytułów).
24. `data/local/.../DataStoreSettingsRepository.kt:25-41` — deszyfrowanie kluczy API przy każdej, nawet niepowiązanej zmianie ustawień.
25. `domain/.../GenerateDescriptionUseCase.kt:35-62` — brak timeoutu na równoległe zapytania web search / Wikipedia.
26. `core/.../CategoryKindsMapping.kt:6,12` — kind „fortifications” zmapowany na dwie różne kategorie.
27. `domain/.../SearchAttractionsUseCase.kt:10-11` — promień wyszukiwania nie jest walidowany na granicy domain.
28. `feature-assistant/.../AssistantViewModel.kt:74-92` — pętla czatu bez globalnego `try/catch`.
29. `feature-mylist/.../AssistantToolDefs.kt`, `CreateTripListUseCase.kt` — brak górnego limitu liczby list podróży.
30. `feature-detail/.../AttractionDetailViewModel.kt:92-106` — odczyt stanu przed zapisem przy przełączaniu przynależności do listy.
31. `feature-search/.../SearchViewModel.kt:184-204` — filtrowanie jakości i wyszukiwanie mogą się wzajemnie nadpisać.
32. `domain/.../GenerateDescriptionUseCase.kt`, `GenerateTripPlanUseCase.kt` — bezpośrednia zależność domain → Timber.
33. `domain/.../SettingsRepository.kt:19`, `AppSettings.kt`, `TripListRepository.kt:19` — nienazwane `Triple`/`Pair` na granicy domain (ryzyko pomyłki kolejności lat/lon/zoom).
34. `domain/.../DefaultSettings.kt`, `AppSettings.kt`, `GenerateTripPlanUseCase.kt:46` — prompt planu wycieczki pomija warstwę ustawień użytkownika (niemodyfikowalny, w przeciwieństwie do promptów opisu/asystenta).
35. `data/local/.../SecureKeyStorage.kt:9-15` — przestarzałe API `EncryptedSharedPreferences`/`MasterKeys`.
36. `data/local/.../RoomAttractionRepository.kt:29-45` — `lastSearchStats` jako stan mutowalny poza transakcją.
37. `data/local/.../AttractionMapper.kt:27` — błędna kategoria w bazie cicho zamienia się na domyślną (`valueOf` + `runCatching` bez logowania).
38. `feature-map/.../MapScreen.kt:137-149` — ręcznie budowany GeoJSON nie ucieka wszystkich znaków specjalnych (nazwa ze znakiem kontrolnym cicho usuwa pinezkę).
39. `feature-map/.../MapScreen.kt`, `feature-search/.../SearchScreen.kt` — zduplikowany boilerplate MapLibre między dwoma ekranami (ten sam wyciek w dwóch miejscach).
40. `core/.../DistanceUtils.kt:16` — reimplementacja `roundToInt()` mimo importu `kotlin.math.*`.
41. `feature-map/.../MapUiState.kt:10,13` — pola `searchCenterLocation`/`userLocation` nigdy nie są ustawiane (martwy kod).
42. `feature-search/.../SearchScreen.kt:43` — nieużywany import `LaunchedEffect`.
43. `feature-search/.../SearchScreen.kt:189,191` — w pełni kwalifikowane referencje `Card`/`CardDefaults` mimo istniejących importów.
44. `feature-settings/.../SettingsScreen.kt:221` — `AttractionCategory.values()` zamiast `.entries`.

## Sugerowana kolejność napraw

1. Bezpieczeństwo kluczy API — wyłączyć logowanie nagłówków w Ktor + podłączyć istniejące maskowanie w Ustawieniach.
2. Utrata danych w Room — zamienić `REPLACE` na merge/`@Update` w `upsertAll`.
3. Odwrócona logika GPS — `maxByOrNull` → `minByOrNull`.
4. Wyniki wyszukiwania na mapie — podłączyć `state.searchResults`.
5. Potwierdzenia w asystencie — jawne potwierdzenie przed `remove_from_list`/`update_trip_plan`.
6. Reszta „Poważnych” i „Do poprawy” — rozbić na osobne taski w `docs/tasks/todo/` wg wzorca BUG-06/BUG-07/FEAT-13.
