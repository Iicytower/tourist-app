# FEAT-04: Poprawa jakości atrakcji turystycznych

## Problem

Obecne wyniki wyszukiwania przez OpenTripMap zawierają dużo "śmieci" — mało interesujących lub nieistotnych obiektów. Użytkownik widzi setki słabych wyników zamiast garści naprawdę wartych uwagi miejsc.

## Cel

Sprawić, żeby aplikacja pokazywała tylko atrakcje wysokiej jakości — znane, warte odwiedzenia, z sensownymi metadanymi.

## Etap 1 — Analiza i projekt rozwiązania

Przed implementacją należy zbadać dostępne opcje i wybrać najlepsze podejście. Pytania do rozstrzygnięcia:

- Jakie parametry filtrowania oferuje OpenTripMap API? (`rate`, `kinds`, limity, sortowanie)
- Czy lepiej filtrować po stronie API (mniej danych) czy po stronie klienta (większa kontrola)?
- Czy warto zastąpić lub uzupełnić OpenTripMap innym źródłem? (np. Overpass API / OSM, Wikipedia nearby, Google Places-like alternatywy open source)
- Jak wygląda rozkład ocen (`rate`) w danych OTM — od jakiego progu wyniki są sensowne?
- Czy LLM może służyć jako filtr/ranker (reranking wyników przed pokazaniem użytkownikowi)?

Wynikiem tego etapu jest decyzja: które mechanizmy filtrowania/rankowania wdrożyć, w jakiej kolejności.

## Etap 2 — Implementacja

Do ustalenia po Etapie 1. Potencjalne kierunki:

- **Filtr po `rate`** — OTM zwraca pole `rate` (0–3); odrzucać obiekty poniżej progu (np. `rate >= 2`)
- **Ograniczenie `kinds`** — wyselekcjonować tylko wartościowe kategorie, wykluczyć śmieciowe (np. `parking`, `atm`, `bench`)
- **Limit i sortowanie** — OTM wspiera `limit` i `order_by=rate`; pobierać mniej, ale lepiej posortowanych
- **Drugi filtr po stronie klienta** — odrzucać obiekty bez nazwy, bez współrzędnych, duplikaty
- **Alternatywne źródło** — jeśli OTM okaże się niewystarczający, dodać drugi provider za interfejsem `AttractionRepository` / nowy `AttractionSource`

## Pliki do zbadania w Etapie 1

- `data/src/.../remote/opentripmap/` — klient OTM, parametry zapytań
- `domain/src/.../model/SearchParams.kt` — co już przekazujemy
- `docs/technical_specs.md` — architektura źródeł danych
- `core/src/.../constant/` — stałe (limity, kategorie)

## Weryfikacja końcowa

- Wyniki wyszukiwania w tym samym miejscu są wyraźnie bardziej interesujące niż przed zmianą
- Brak obiektów bez nazwy lub z trywialną nazwą (ławka, parking, bankomat)
- Czas odpowiedzi nie pogarsza się istotnie
