# BUG-16: Usunąć martwą integrację OpenTripMap i zdublowany moduł Overpass

## Problem

Cała integracja OpenTripMap jest martwym kodem, niepodłączonym do DI:

- `data/src/.../remote/opentripmap/di/OpenTripMapModule.kt` (`openTripMapModule`) nie jest ładowany w `WanderListApp.kt` (ładowany jest tylko `attractionSourceModule`, `httpClientModule` itd.).
- W konsekwencji `OpenTripMapClient` (interfejs), `FakeOpenTripMapClient`, `RealOpenTripMapClient` są nieosiągalne — żaden kod produkcyjny ich nie rozwiązuje przez Koin.
- `RealOpenTripMapClient.kt:31` wysyła `apikey` jako parametr URL (ten sam wzorzec ryzyka co finding o Bearer tokenie w logach — gdyby ten klient był kiedyś aktywny) — ale jest martwy, więc obecnie nieszkodliwy, za to myli przy debugowaniu (dokładnie ten problem zidentyfikowany wcześniej w BUG-06 dla samego Overpass).
- `data/src/.../remote/overpass/di/OverpassModule.kt` (`overpassModule`) jest zduplikowanym, również nieużywanym odpowiednikiem bindingu `OverpassApiClient`, który faktycznie jest już zarejestrowany w `AttractionSourceModule.kt:10` (`attractionSourceModule`).

Projekt aktywnie korzysta z Overpass/Wikipedia/Wikidata przez `CompositeAttractionSource` — OpenTripMap (mimo że wymieniony w `CLAUDE.md` jako część stacku) nie jest już częścią realnego przepływu wyszukiwania.

## Cel

Usunąć nieosiągalny kod, żeby nie mylił przy przyszłym debugowaniu (dokładnie ta sytuacja co doprowadziła do potrzeby zbadania „dlaczego Overpass nie zwraca wyników” w BUG-06 — martwy, zdublowany moduł obok prawdziwego, aktywnego bindingu).

## Zakres zmian

- Usunąć cały katalog `data/src/main/kotlin/com/iicytower/wanderlist/data/remote/opentripmap/` (klient, DTO, mapper, moduł DI) — **potwierdzić wcześniej brak jakichkolwiek żywych referencji** (grep po `OpenTripMapClient`, `OpenTripMapModule`, `openTripMapModule` w całym repo, poza plikami do usunięcia i dokumentacją).
- Usunąć `data/src/main/kotlin/com/iicytower/wanderlist/data/remote/overpass/di/OverpassModule.kt` (zdublowany moduł) — zostawić jedyny, aktywny binding `OverpassApiClient` w `AttractionSourceModule.kt`.
- Sprawdzić `CLAUDE.md` / `docs/technical_specs.md` — czy wzmianka o OpenTripMap jako źródle wyszukiwania atrakcji jest nadal aktualna; jeśli nie, zaktualizować dokumentację, żeby nie wprowadzała w błąd (OpenTripMap occupies miejsce w tabeli stacku technicznego w `CLAUDE.md` jako „Wyszukiwanie atrakcji”).
- Sprawdzić testy jednostkowe odnoszące się do usuwanych klas — usunąć razem z kodem produkcyjnym.

## Weryfikacja

- `./gradlew build` przechodzi bez błędów po usunięciu.
- Grep po `opentripmap`/`OpenTripMap` (poza ewentualną wzmianką w dokumentacji, jeśli celowo zachowana) nie zwraca żadnych żywych referencji w kodzie.
- Wyszukiwanie atrakcji w aplikacji działa bez zmian (Overpass/Wikipedia/Wikidata przez `CompositeAttractionSource`).
