# Backlog — pozostałe znaleziska z pełnego przeglądu kodu (2026-07-05)

Znaleziska z pełnego przeglądu kodu (`notes/code-review-2026-07-05/findings.md`), które nie dostały jeszcze własnego taska. Numeracja zgodna z tym plikiem. Rozbić na osobne taski (`BUG-XX`/`FEAT-XX`) w miarę priorytetyzacji — wzorem `BUG-08`..`BUG-17`, `FEAT-14`.

## Krytyczne

1. **`data/remote/.../HttpClientProvider.kt:18-23`** — klucz API OpenRouter (Bearer token) w logach Ktor (`LogLevel.HEADERS` bez `sanitizeHeader`), także w release.
5. **`feature-settings/.../SettingsScreen.kt:330-341`** — klucze API zawsze widoczne w plaintext w Ustawieniach; istniejąca infrastruktura maskowania (`toggleOpenRouterKeyVisibility()`) nigdzie nie podłączona.

## Poważne

10. **`feature-map/.../MapScreen.kt:84-93`, `feature-search/.../SearchScreen.kt:290-321`** — `MapView` nigdy nie jest zwalniany (`DisposableEffect` z pustym `onDispose`) — wyciek natywnych zasobów GL przy każdym otwarciu ekranu mapy/pickera lokalizacji. *(wyjaśnione w rozmowie — patrz kontekst; do zaplanowania jako osobny task)*
13. **`data/remote/.../CompositeAttractionSource.kt:21`** + Nominatim/Wikipedia/Wikidata bez sprawdzania statusu HTTP — błędy sieciowe (429, timeout) znikają cicho jako „brak wyników”.
15. **`data/remote/.../LlmMapper.kt:84-93`, `OpenRouterLlmService.kt:69`** — ścieżka SSE streaming (`streamResponse`) jest martwym kodem, ale zepsutym, gdyby ją kiedyś podłączono (parser tool-call zakłada kompletny JSON per chunk, timeout `Long.MAX_VALUE`). *(wyjaśnione w rozmowie; obecnie bez ryzyka, bo nieużywane)*

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
31. `feature-search/.../SearchViewModel.kt:184-204` — filtrowanie jakości i wyszukiwanie mogą się wzajemnie nadpisać (powiązane z BUG-14, zweryfikować czy naprawa BUG-14 to też rozwiązuje).
32. `domain/.../GenerateDescriptionUseCase.kt`, `GenerateTripPlanUseCase.kt` — bezpośrednia zależność domain → Timber.
33. `domain/.../SettingsRepository.kt:19`, `AppSettings.kt`, `TripListRepository.kt:19` — nienazwane `Triple`/`Pair` na granicy domain (ryzyko pomyłki kolejności lat/lon/zoom).
34. `domain/.../DefaultSettings.kt`, `AppSettings.kt`, `GenerateTripPlanUseCase.kt:46` — prompt planu wycieczki pomija warstwę ustawień użytkownika (niemodyfikowalny, w przeciwieństwie do promptów opisu/asystenta).
35. `data/local/.../SecureKeyStorage.kt:9-15` — przestarzałe API `EncryptedSharedPreferences`/`MasterKeys`.
36. `data/local/.../RoomAttractionRepository.kt:29-45` — `lastSearchStats` jako stan mutowalny poza transakcją.
37. `data/local/.../AttractionMapper.kt:27` — błędna kategoria w bazie cicho zamienia się na domyślną (`valueOf` + `runCatching` bez logowania).
38. `feature-map/.../MapScreen.kt:137-149` — ręcznie budowany GeoJSON nie ucieka wszystkich znaków specjalnych (nazwa ze znakiem kontrolnym cicho usuwa pinezkę).
39. `feature-map/.../MapScreen.kt`, `feature-search/.../SearchScreen.kt` — zduplikowany boilerplate MapLibre między dwoma ekranami (ten sam wyciek w dwóch miejscach — powiązane z #10).
40. `core/.../DistanceUtils.kt:16` — reimplementacja `roundToInt()` mimo importu `kotlin.math.*`.
41. `feature-map/.../MapUiState.kt:10,13` — pola `searchCenterLocation`/`userLocation` nigdy nie są ustawiane (martwy kod).
42. `feature-search/.../SearchScreen.kt:43` — nieużywany import `LaunchedEffect`.
43. `feature-search/.../SearchScreen.kt:189,191` — w pełni kwalifikowane referencje `Card`/`CardDefaults` mimo istniejących importów.
44. `feature-settings/.../SettingsScreen.kt:221` — `AttractionCategory.values()` zamiast `.entries`.

## Już przekształcone w taski (dla porządku, nie duplikować)

2 → BUG-08, 3 → BUG-09, 4 → BUG-10, 6 → FEAT-14, 7 → BUG-11, 8 → BUG-12, 9 → BUG-13, 11 → BUG-14, 12 → BUG-15, 14 → BUG-16, 16 → BUG-17.
