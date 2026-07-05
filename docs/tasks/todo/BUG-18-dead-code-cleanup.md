# BUG-18: Wyczyścić codebase z nieużywanego kodu

## Problem

Projekt nie ma skonfigurowanego detekt/ktlint ani reguł lint per moduł — martwy kod (nieużywane importy, pola, funkcje, zasoby, moduły DI) narasta niekontrolowanie i był już źródłem realnych problemów (por. `BUG-16`, gdzie cały moduł OpenTripMap okazał się niepodłączony do DI). Pełny przegląd kodu z 2026-07-05 (`notes/code-review-2026-07-05/findings.md`, patrz `docs/tasks/todo/backlog.md`) zidentyfikował już częściowo martwy kod, ale nie było systematycznego przebiegu po całym repo.

Znane, potwierdzone znaleziska (z backlogu, jeszcze nierozbite na taski):

- `feature-map/.../MapUiState.kt:10,13` — pola `searchCenterLocation`/`userLocation` nigdy nie są ustawiane (martwy kod).
- `feature-search/.../SearchScreen.kt:43` — nieużywany import `LaunchedEffect`.
- `feature-search/.../SearchScreen.kt:189,191` — w pełni kwalifikowane referencje `Card`/`CardDefaults` mimo istniejących importów (redundancja, nie martwy kod sensu stricto — pominąć jeśli poza zakresem tego taska).

## Cel

Systematycznie znaleźć i usunąć nieużywany kod w całym repo (wszystkie moduły `app`, `core`, `domain`, `data`, `feature-*`): nieosiągalne klasy/funkcje/pola, nieużywane importy, niepodłączone moduły DI, nieużywane zasoby (`drawable`, `string`, `string.xml` nieużywane), martwe gałęzie kodu.

## Zakres zmian

- Uruchomić `./gradlew lint` na wszystkich modułach i przejrzeć wyniki `UnusedResources` / `unused` — usunąć potwierdzone martwe zasoby.
- Przejść przez Android Studio „Code → Inspect Code” (profil domyślny + „unused declaration”) dla każdego modułu — potwierdzić każdy hit ręcznie (inspekcje IDE dają fałszywe alarmy dla API publicznego/DI-resolved przez Koin runtime, więc **zweryfikować grepem** brak referencji przed usunięciem, dokładnie jak w `BUG-16`).
- Usunąć potwierdzone znaleziska z backlogu: `MapUiState.searchCenterLocation`/`userLocation` (pola), nieużywany import w `SearchScreen.kt:43`.
- Zwrócić szczególną uwagę na moduły Koin (`*Module.kt`) — sprawdzić każdy `val xModule = module { ... }` ma odpowiednik `includes(xModule)` w `WanderListApp.kt` lub innym agregującym module; niepodłączony moduł = martwy kod w całości (wzorzec z `BUG-16`).
- Sprawdzić nieużywane pliki zasobów w `app/src/main/assets/agents/` i `res/` (drawable, strings) — porównać z faktycznymi referencjami w kodzie.
- Usunąć testy jednostkowe odnoszące się wyłącznie do usuwanego martwego kodu.
- **Nie** ruszać elementów już zaplanowanych jako osobne taski w backlogu (np. #10 duplikacja MapLibre boilerplate, #14 martwy streaming SSE w `LlmMapper`/`OpenRouterLlmService` — ten jest zepsuty i wymaga decyzji projektowej, nie prostego usunięcia; zostawić do odrębnego taska).
- Nie usuwać kodu, którego jedyną „referencją” jest test — jeśli produkcyjny kod jest martwy, usunąć oba razem; jeśli kod jest używany tylko w testach celowo (np. fake/test double), zostawić.

## Weryfikacja

- `./gradlew build` przechodzi bez błędów po usunięciu.
- `./gradlew lint` nie zgłasza nowych `UnusedResources`/unused declaration dla usuniętych elementów.
- Grep po nazwach usuniętych klas/funkcji/pól nie zwraca żywych referencji w kodzie (poza dokumentacją, jeśli celowo zachowana).
- Aplikacja buduje się i działa bez regresji (przetestować golden path: wyszukiwanie, mapa, moja lista, asystent, ustawienia).
