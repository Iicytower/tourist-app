# FEAT-17: Dopracowanie narzędzia planowania wycieczki na bazie listy

## Kontekst

FEAT-09 (generowanie planu wycieczki przez LLM na bazie listy) jest ukończone i działa, ale w obecnym kształcie ma kilka braków UX/jakościowych, niezależnych od rozbudowy transportowo-tras owej z FEAT-12 (która wymaga FEAT-03 i jest odłożona na później). Ten task **nie** dotyka środka transportu, liczby osób, posiłków ani trasy na mapie — to zakres FEAT-12. Skupia się na dopracowaniu tego, co już istnieje.

## Problemy do naprawienia

1. **Brak regeneracji planu z `TripPlanScreen`** (`feature-mylist/.../ui/TripPlanScreen.kt`, `viewmodel/TripPlanViewModel.kt`) — jedyna droga do nowej wersji planu to dialog z ekranu listy → rozmowa z asystentem (zgodnie z FEAT-09). Brak prostego przycisku "Wygeneruj ponownie" bezpośrednio na `TripPlanScreen` dla usera, który chce po prostu nową wersję bez rozmowy.
2. **Brak ręcznej zmiany kolejności punktów** — plan da się tylko obejrzeć; przestawienie punktu w dniu (np. drag&drop) wymaga dziś przejścia przez asystenta.
3. **Słaba obsługa błędów generowania** (`domain/.../usecase/GenerateTripPlanUseCase.kt:32,50,87`) — generyczne komunikaty (`"Lista jest pusta..."`, `"Nie udało się wygenerować pełnego planu..."`, `"LLM nie zwrócił odpowiedzi"`) bez rozróżnienia przyczyny (sieć, limit iteracji, brak/zły JSON) i bez przycisku retry w UI — user dostaje tylko snackbar.
4. **Parsowanie JSON z odpowiedzi LLM jest kruche** (`GenerateTripPlanUseCase.parsePlan`, linia ~88) — usuwa tylko dokładne markery ```` ```json ```` / ```` ``` ````; model czasem owija JSON dodatkowym tekstem wyjaśniającym przed/po bloku kodu albo używa innego formatowania — wtedy parsowanie wybucha z nieczytelnym błędem zamiast spróbować wyciągnąć sam blok JSON (np. przez wyszukanie pierwszego `{` i dopasowanego ostatniego `}`).
5. **Brak walidacji planu po sparsowaniu** — nie sprawdza się, że `plan.days` nie jest puste ani że `xid` punktów odpowiada atrakcjom faktycznie znajdującym się na liście (możliwa halucynacja LLM) — cichy błąd, który ujawni się dopiero przy integracji z mapą (FEAT-12).
6. **Brak informacji zwrotnej podczas generowania** — pętla tool-callingu (`web_search`, do `MAX_TOOL_CALL_ITERATIONS`) może trwać długo, a UI pokazuje tylko jeden generyczny `CircularProgressIndicator` bez informacji co się dzieje (np. "Szukam informacji o: ...", "Iteracja 2/5").
7. **Prompt planu ignoruje ustawienia użytkownika** (`app/src/main/assets/agents/trip-plan.txt`, `GenerateTripPlanUseCase.kt`) — w przeciwieństwie do `description.txt`/`assistant.txt`, prompt generowania planu nie uwzględnia zainteresowań/preferencji z `AppSettings` (backlog #34) — plan nie jest personalizowany mimo że dane już istnieją w aplikacji.

## Zakres zmian (do zweryfikowania przy implementacji)

- `feature-mylist/.../ui/TripPlanScreen.kt` — przycisk "Wygeneruj ponownie" z potwierdzeniem (`AlertDialog`, plan zostanie nadpisany), stan ładowania z bardziej opisowym komunikatem postępu.
- `feature-mylist/.../viewmodel/TripPlanViewModel.kt` / `TripPlanUiState.kt` — akcja `regenerate()`, stan `isRegenerating`/progress, obsługa błędów z rozróżnieniem przyczyny.
- `domain/.../usecase/GenerateTripPlanUseCase.kt` — bardziej odporne wyciąganie bloku JSON z odpowiedzi (fallback: szukanie `{...}` gdy markery ```` ``` ```` nie pasują), walidacja niepustych `days` i (best-effort) zgodności `xid` z listą wejściowych atrakcji, przekazanie zainteresowań usera do promptu.
- `app/src/main/assets/agents/trip-plan.txt` — dopisanie sekcji z placeholderem na zainteresowania/preferencje usera (wzorem `description.txt`/`assistant.txt`).
- Reorder punktów w dniu — rozważyć czy to osobna, mniejsza funkcja (lokalna zmiana kolejności zapisywana bezpośrednio przez `TripListRepository.saveTripPlan`, bez wołania LLM) czy do przesunięcia do backlogu, jeśli okaże się zbyt złożone na ten task.

## Poza zakresem (patrz FEAT-12)

- Środek transportu, liczba osób, rezerwacje grupowe
- Przerwy na posiłki/odpoczynek
- Wyznaczanie i rysowanie trasy na mapie

## Weryfikacja

- Na `TripPlanScreen` z istniejącym planem da się wygenerować nową wersję bez przechodzenia przez asystenta, po potwierdzeniu w dialogu
- Błąd generowania (np. pusta lista, przekroczony limit iteracji, zły JSON z LLM) pokazuje czytelny komunikat i pozwala spróbować ponownie
- Odpowiedź LLM owinięta dodatkowym tekstem wokół bloku JSON nadal poprawnie się parsuje
- Wygenerowany plan uwzględnia zainteresowania ustawione w Ustawieniach (widoczne w treści `note`/doborze punktów)
- Podczas generowania user widzi więcej niż goły spinner (informacja o trwającej operacji)
