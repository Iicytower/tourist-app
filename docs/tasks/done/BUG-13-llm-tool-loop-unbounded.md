# BUG-13: Pętle tool-call LLM bez górnego limitu iteracji

## Problem

Dwa miejsca implementują pętlę „wywołaj LLM → jeśli zwrócił tool call, wykonaj narzędzie i wywołaj LLM ponownie” bez żadnego górnego limitu iteracji:

- `domain/.../usecase/GenerateTripPlanUseCase.kt:45-65` — `while (continueLoop)`.
- `data/.../llmfilter/LlmAttractionQualityFilter.kt:52-75` — analogiczna pętla `while (continueLoop)`.

Jeśli model uporczywie zwraca kolejne tool calle (np. przez niejednoznaczny prompt, pętlę w rozumowaniu modelu, albo błąd w obsłudze wyniku narzędzia powodujący, że model wciąż prosi o to samo), metoda nigdy się nie kończy — generowanie planu wycieczki lub filtrowanie jakości atrakcji zawiesza się bez możliwości przerwania poza anulowaniem całego ekranu przez użytkownika. Dodatkowo każda iteracja to płatne wywołanie OpenRouter (i czasem Tavily) — brak limitu to też brak ograniczenia kosztów.

## Cel

Ograniczyć liczbę iteracji pętli tool-call do rozsądnej stałej, z kontrolowanym zakończeniem (błąd/best-effort) po przekroczeniu limitu.

## Zakres zmian

- `core/` — nowa stała, np. `MAX_TOOL_CALL_ITERATIONS` (rozważyć wartość np. 8-10 — wystarczająco dużo dla typowego przepływu narzędzi, ale ograniczające patologiczne pętle).
- `domain/.../usecase/GenerateTripPlanUseCase.kt:45-65` — dodać licznik iteracji, przy przekroczeniu limitu przerwać pętlę i zwrócić błąd/częściowy wynik z komunikatem dla użytkownika (np. „Nie udało się wygenerować pełnego planu — spróbuj ponownie”).
- `data/.../llmfilter/LlmAttractionQualityFilter.kt:52-75` — analogicznie, z fallbackiem (np. zwrócić atrakcje bez filtrowania jakości zamiast zawieszenia).

## Weryfikacja

- Symulacja/test z modelem zwracającym tool call w kółko (mock LLM service) → pętla kończy się po ustalonym limicie iteracji z kontrolowanym błędem, a nie zawiesza się w nieskończoność.
- Normalny przepływ (1-3 tool calle, typowe dla obecnych promptów) działa bez zmian.
