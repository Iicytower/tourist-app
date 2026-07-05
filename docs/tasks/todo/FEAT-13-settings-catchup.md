# FEAT-13: Aktualizacja ekranu ustawień — dogonić resztę aplikacji + zweryfikować realny wpływ zainteresowań

## Problem

Ekran ustawień (`feature-settings`) nie był aktualizowany równolegle z rozwojem reszty aplikacji i pozostał w tyle. Dwa konkretne, potwierdzone braki:

1. **Zainteresowania (`userInterests`) nie wpływają na wyszukiwanie**, mimo że `docs/functionalities_specs.md` (linie ok. 176-177) wprost obiecuje, że zainteresowania mają wpływać na: (1) filtrowanie wyników wyszukiwania i (2) zachowanie AI. W kodzie działa tylko #2:
   - Jedyny konsument `userInterests` to `GenerateDescriptionUseCase.kt` (linie ok. 69-71) — dopisuje zainteresowania do promptu LLM przy generowaniu opisu atrakcji.
   - `SearchViewModel.kt` (`feature-search`) w ogóle nie odwołuje się do `SettingsRepository`/`userInterests` — ma własny, niezależny, doraźny wybór kategorii per wyszukiwanie (`setCategories()`, `selectedCategories` w `SearchUiState`), niepowiązany z zapisanym ustawieniem zainteresowań.
2. Brakuje ustawienia dla filtra jakości atrakcji (FEAT-04/FEAT-11, `LlmAttractionQualityFilter`) — funkcja istnieje w aplikacji, ale nie ma żadnej reprezentacji w `SettingsScreen.kt`/`SettingsUiState.kt`.

## Cel

1. Zweryfikować z użytkownikiem/projektowo, czy zainteresowania **mają** faktycznie filtrować/sortować wyniki wyszukiwania (zgodnie ze spec) — jeśli tak, podłączyć `userInterests` z `SettingsRepository` do `SearchViewModel`/`SearchAttractionsUseCase` (np. jako domyślne pre-zaznaczone kategorie przy starcie wyszukiwania, i/lub jako czynnik sortowania/rankingu wyników). Jeśli decyzja projektowa jest inna (zainteresowania mają wpływać tylko na AI) — zaktualizować `docs/functionalities_specs.md`, żeby nie obiecywał funkcji, której nie ma.
2. Przegląd całego ekranu ustawień pod kątem kompletności względem aktualnego stanu aplikacji — dodać brakujące opcje (np. filtr jakości atrakcji) i usunąć/zaktualizować te, które są nieaktualne.
3. Ocenić UX/organizację ekranu ustawień (obecnie: API keys, model AI, system prompty, zainteresowania, język opisu, promień wyszukiwania) — czy sekcje są logicznie pogrupowane, czy coś wymaga przeniesienia bliżej powiązanych funkcji (np. spójność z `FEAT-11-app-language-setting.md`, który opisuje podobny, już zidentyfikowany brak dla ustawienia języka).

## Pliki do zmiany

- `feature-settings/src/main/kotlin/com/iicytower/wanderlist/feature/settings/ui/SettingsScreen.kt`
- `feature-settings/src/main/kotlin/com/iicytower/wanderlist/feature/settings/viewmodel/SettingsViewModel.kt`
- `feature-settings/src/main/kotlin/com/iicytower/wanderlist/feature/settings/viewmodel/SettingsUiState.kt`
- `feature-search/src/main/kotlin/com/iicytower/wanderlist/feature/search/viewmodel/SearchViewModel.kt` (jeśli decyzja: podłączyć zainteresowania do wyszukiwania)
- `domain/src/main/kotlin/com/iicytower/wanderlist/domain/usecase/SearchAttractionsUseCase.kt` (jeśli filtrowanie/ranking po stronie use case'u)
- `docs/functionalities_specs.md` (jeśli decyzja: zainteresowania nie mają filtrować wyszukiwania — doprecyzować spec)

## Powiązane taski

- `docs/tasks/todo/FEAT-11-app-language-setting.md` — analogiczny, już zidentyfikowany brak spójności ustawień z resztą aplikacji (język).
- `docs/tasks/done/FEAT-04-attraction-quality.md`, `docs/tasks/done/FEAT-11-filter-quality.md` — funkcja filtra jakości bez odpowiadającego ustawienia.

## Weryfikacja

- Jasna, udokumentowana decyzja: czy zainteresowania filtrują/sortują wyniki wyszukiwania — zaimplementowana zgodnie z decyzją, albo spec zaktualizowany żeby nie obiecywał funkcji, która nie istnieje.
- Ekran ustawień zawiera wszystkie aktywne, konfigurowalne funkcje aplikacji (w tym filtr jakości, jeśli ma być konfigurowalny).
- Brak rozbieżności między `docs/functionalities_specs.md` sekcja 3.6 a rzeczywistym zachowaniem ekranu ustawień.
