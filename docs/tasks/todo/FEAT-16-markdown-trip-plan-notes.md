# FEAT-16: Obsługa Markdown w notatkach i planie wycieczki

## Problem

Plan wycieczki (`TripPlan`, generowany przez [[FEAT-09]]) oraz notatki użytkownika do planu są dziś czystym tekstem:

- `TripPoint.note` — krótka uwaga LLM per punkt (np. "otwarty tylko do 17:00") — wyświetlana zwykłym `Text()` w `TripPlanScreen.kt` (`DayCard`), bez żadnego formatowania.
- `uiState.notes` — swobodne notatki użytkownika do całego planu, edytowane w `OutlinedTextField` (`TripPlanScreen.kt` ok. linia 116-124) i zapisywane przez `viewModel.saveNotes()`. Też czysty tekst.
- Prompt generujący plan (`app/src/main/assets/agents/trip-plan.txt`) instruuje LLM, żeby zwracał plain-textowe `note`, bez wskazówki dot. formatowania.

Efekt: LLM nie może w notatce użyć list, pogrubień, nagłówków itp., a nawet gdyby użył składni Markdown "z automatu", i tak wyświetliłaby się jako surowy tekst (`**ważne**`, `- punkt 1` zamiast realnego formatowania).

## Cel

1. Plan wycieczki generowany przez LLM ma używać Markdown w treści notatek (`note` per punkt), żeby dało się przekazać listy, pogrubienia itp. zamiast płaskiego zdania.
2. Notatki użytkownika do planu (`uiState.notes`) też mają wspierać Markdown.
3. UI ma renderować ten Markdown z prawdziwym formatowaniem (listy, pogrubienie, nagłówki), nie jako surowy tekst ze znakami `*`/`#`/`-`.

## Zakres zmian (do zweryfikowania przy implementacji)

- `app/src/main/assets/agents/trip-plan.txt` — zaktualizować instrukcję dot. pola `note`, żeby jawnie zezwalała/zachęcała do Markdown (pogrubienia, listy) tam gdzie to poprawia czytelność, bez zmiany wymogu "reszta odpowiedzi to czysty JSON" (Markdown ma być tylko *wewnątrz* wartości stringowej `note`, JSON jako całość musi zostać parsowalny).
- `domain/.../model/TripPlan.kt` — prawdopodobnie bez zmian strukturalnych (`note: String?` już przyjmie Markdown jako zwykły string) — do potwierdzenia, że nic nie escapuje/sanityzuje tej wartości w drodze do UI.
- `feature-mylist/.../ui/TripPlanScreen.kt`:
  - `DayCard`/`point.note` — renderować przez komponent Markdown zamiast `Text()`.
  - Sekcja notatek użytkownika — obecnie edycja w `OutlinedTextField` i wyświetlanie tylko podczas edycji (nie ma osobnego trybu podglądu). Do ustalenia: czy dodać podgląd renderowanego Markdown obok/zamiast pola edycji (edycja zawsze jako plain text/Markdown source, podgląd jako osobny widok pod spodem lub po zapisaniu).
- Wybór biblioteki do renderowania Markdown w Compose — w projekcie nie ma dziś żadnej (`gradle/libs.versions.toml` bez wpisu). Do researchu/decyzji przy implementacji (np. lekka biblioteka Compose Markdown lub własny prosty renderer dla podzbioru składni faktycznie używanego przez LLM: pogrubienie, listy punktowane, nagłówki — nie trzeba pełnego CommonMark). Wymaga zgody na instalację pakietu zgodnie z zasadami projektu.

## Pytania otwarte do ustalenia przy implementacji

- Czy notatki użytkownika (`uiState.notes`) mają mieć osobny tryb "edycja" / "podgląd", czy renderować Markdown na żywo pod polem edycji?
- Czy istniejące, już zapisane plany/notatki (sprzed tej zmiany, czysty tekst) wymagają jakiejkolwiek migracji — odpowiedź: nie, czysty tekst jest poprawnym (trywialnym) Markdownem, więc będzie renderował się tak jak dziś.

## Weryfikacja

- Nowo wygenerowany plan wycieczki może zawierać w `note` np. listę punktowaną lub pogrubienie, i wyświetla się to jako realne formatowanie (nie surowe znaki `-`/`**`) w `TripPlanScreen`.
- Notatki użytkownika wpisane z składnią Markdown (np. `- zabrać parasol`, `**ważne**: zamknięte w poniedziałki`) renderują się z formatowaniem.
- Plan bez żadnej składni Markdown w notatkach nadal wyświetla się poprawnie (zwykły tekst, bez artefaktów).
