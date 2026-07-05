# FEAT-14: Asystent musi potwierdzać destrukcyjne zmiany (dialog Tak/Nie) + opcja cofnięcia

## Problem

`AssistantViewModel.kt:186-236` (`executeTool`) wykonuje narzędzia takie jak `remove_from_list` i `update_trip_plan` **natychmiast**, bez pytania użytkownika o potwierdzenie — w przeciwieństwie do UI, gdzie te same operacje zawsze idą przez `AlertDialog` (`TripListDetailViewModel.confirmRemove()`, `MyListViewModel.confirmDelete()`).

Treść zwrócona przez narzędzie `web_search` trafia do kontekstu przekazywanego LLM — spreparowana strona (prompt injection) mogłaby nakłonić model, żeby w pętli wywołał `remove_from_list` dla wszystkich atrakcji zwróconych przez `get_list_attractions`, albo nadpisał `plan_json` dowolnej listy przez `update_trip_plan`, bez wiedzy i zgody użytkownika.

## Cel

Przed wykonaniem zmiany inicjowanej przez asystenta (usunięcie z listy, nadpisanie planu wycieczki) pokazać użytkownikowi dialog z pytaniem Tak/Nie opisującym dokładnie, co asystent chce zrobić. Dodatkowo dla `update_trip_plan` — po zatwierdzeniu zmiany, umożliwić cofnięcie do poprzedniej wersji planu.

## Zakres narzędzi wymagających potwierdzenia

Na podstawie przeglądu, narzędzia z realnym ryzykiem nieodwracalnej/niepożądanej zmiany:
- `remove_from_list` — usunięcie atrakcji z listy.
- `update_trip_plan` — nadpisanie całego planu wycieczki dla danej listy.

Pozostałe narzędzia (`add_to_list`, `create_list`, `get_list_attractions`, itp.) zostają bez zmian — dodanie czegoś do listy jest łatwo odwracalne i nie wymaga dialogu (do potwierdzenia przy implementacji, czy `create_list` też powinno wymagać potwierdzenia — na razie poza zakresem, zgłoszone osobno jako brak limitu liczby list w backlogu).

## Zakres zmian

### Stan oczekującego potwierdzenia

- `feature-assistant/.../viewmodel/AssistantViewModel.kt` — gdy `executeTool` napotka narzędzie z listy wymagającej potwierdzenia, **nie wykonywać go od razu**: zapisać wywołanie (nazwę narzędzia, argumenty, czytelny dla użytkownika opis akcji) w nowym polu stanu, np. `pendingConfirmation: PendingToolConfirmation?`, i wstrzymać dalsze przetwarzanie pętli czatu do czasu decyzji użytkownika.
- `feature-assistant/.../viewmodel/AssistantUiState.kt` — dodać `pendingConfirmation: PendingToolConfirmation?` (data class z czytelnym opisem akcji, np. „Asystent chce usunąć „Wawel” z listy „Kraków 2026”. Potwierdzić?”).
- Dodać `confirmPendingAction()` / `rejectPendingAction()` w `AssistantViewModel` — potwierdzenie wykonuje odłożone narzędzie i kontynuuje pętlę czatu z wynikiem; odrzucenie zwraca do LLM wynik narzędzia informujący, że użytkownik odmówił (żeby model mógł to uwzględnić w dalszej odpowiedzi, zamiast zawiesić rozmowę).

### UI dialogu

- `feature-assistant/.../ui/AssistantScreen.kt` — gdy `state.pendingConfirmation != null`, pokazać `AlertDialog` z opisem akcji i przyciskami „Tak”/„Nie”, analogicznie do istniejących wzorców potwierdzeń w `TripListDetailScreen`/`MyListScreen`.

### Revert dla `update_trip_plan`

- Przed nadpisaniem planu (po potwierdzeniu użytkownika) zachować poprzednią wersję `plan_json` — najprościej jako stan w `AssistantViewModel`/`TripPlanViewModel` (np. `previousPlanJson: String?` per `listId`), żywy przez czas trwania sesji (bez trwałej historii wersji — to świadome uproszczenie zakresu, pełna historia wersji planu to osobny temat, jeśli okaże się potrzebny).
- Po zastosowaniu zmiany planu pokazać użytkownikowi możliwość cofnięcia (np. przycisk „Cofnij zmianę planu” widoczny przez najbliższą interakcję/w tym samym ekranie planu wycieczki) — kliknięcie przywraca `previousPlanJson` przez istniejący use case aktualizacji planu.
- Sprawdzić `domain/.../usecase/` dot. planu wycieczki (`GenerateTripPlanUseCase`, use case aktualizacji planu) — czy istnieje już metoda `updatePlan(listId, planJson)`, którą można wykorzystać zarówno do zastosowania zmiany, jak i do revertu (przywrócenie to po prostu wywołanie tej samej metody ze starą wartością).

## Weryfikacja

- Asystent proponuje usunięcie atrakcji z listy → pojawia się dialog z pytaniem, zawierający nazwę atrakcji i listy → „Nie” nie usuwa niczego i informuje asystenta o odmowie; „Tak” usuwa i kontynuuje rozmowę.
- Asystent proponuje nadpisanie planu wycieczki → dialog z pytaniem → „Tak” stosuje zmianę i udostępnia opcję cofnięcia do poprzedniej wersji planu w tej samej sesji.
- `add_to_list`/`create_list`/pozostałe narzędzia działają bez zmian (bez dialogu).
- Próba prompt injection przez `web_search` (np. treść strony sugerująca modelowi wywołanie `remove_from_list`) nie prowadzi do usunięcia niczego bez jawnego kliknięcia „Tak” przez użytkownika.
