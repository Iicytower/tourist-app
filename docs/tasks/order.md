# Kolejność realizacji tasków

Ustalona kolejność wykonywania tasków z `docs/tasks/todo/`. Priorytet: szybkie i widoczne poprawki bugów → integralność danych → porządki w kodzie → bezpieczeństwo asystenta → fundamenty (Ustawienia/język) → polish istniejących funkcji → duże nowe funkcje z zależnościami.

Przy dodawaniu nowego taska do `docs/tasks/todo/` dopisz go tutaj we właściwym miejscu kolejki (z czasem/trudnością/ważnością) zamiast zostawiać poza tym plikiem.

| # | Task | Opis | Czas | Trudność | Ważność |
|---|---|---|---|---|---|
| 1 | BUG-09 | Odwrócona logika wyboru najlepszej lokalizacji GPS (`maxByOrNull` zamiast `minByOrNull` po `accuracy`) | 15 min | Bardzo niska | Wysoka |
| 2 | BUG-15 | Liczba atrakcji na liście podróży zawsze pokazuje 0 (sztywne `0 as attractionCount` w SQL) | 30 min | Bardzo niska | Średnia |
| 3 | BUG-10 | Wyniki wyszukiwania nigdy nie pojawiają się na mapie poza trybem „Moja Lista” | 2-3 h | Średnia | Wysoka |
| 4 | BUG-17 | Szybkie zaznaczanie kilku zainteresowań w Ustawieniach gubi wcześniejsze wybory (stale state) | 1-2 h | Niska/Średnia | Średnia |
| 5 | BUG-11 | Limit 50 w Mojej Liście do ominięcia przy współbieżnym dodaniu (brak `@Transaction`) | 1-2 h | Niska/Średnia | Średnia |
| 6 | BUG-12 | Ten sam problem współbieżności co BUG-11, dla list podróży | 1 h | Niska | Średnia |
| 7 | BUG-14 | Wolniejsze wyszukiwanie potrafi nadpisać nowsze wyniki (brak anulowania joba) | 1-2 h | Średnia | Średnia |
| 8 | BUG-16 | Usunąć martwą integrację OpenTripMap i zdublowany moduł Overpass | 1-2 h | Niska | Średnia |
| 9 | BUG-18 | Systematyczne czyszczenie martwego kodu w całym repo | 3-5 h | Średnia | Średnia |
| 10 | FEAT-14 | Asystent musi potwierdzać destrukcyjne zmiany (dialog Tak/Nie) + cofnięcie planu | 3-4 h | Średnia/Wysoka | Wysoka (bezpieczeństwo) |
| 11 | BUG-07 | Przeprojektowanie pickera lokalizacji na mapie (long-press zamiast pan-under-pin) | 3-4 h | Średnia/Wysoka | Średnia |
| 12 | FEAT-13 | Dogonienie ekranu Ustawień + weryfikacja wpływu zainteresowań na wyszukiwanie | 4-6 h | Średnia | Wysoka (fundament) |
| 13 | FEAT-11 | Jedno ustawienie „Język aplikacji” (wyszukiwanie, podpowiedzi lokalizacji, asystent) | 3-4 h | Średnia | Średnia/Wysoka |
| 14 | FEAT-10 | Wyszukiwanie informacji o atrakcji w lokalnym języku kraju | 3-4 h | Średnia | Niska/Średnia |
| 15 | FEAT-17 | Dopracowanie planowania wycieczki (regeneracja, reorder, błędy, walidacja planu) | 6-8 h | Wysoka | Średnia |
| 16 | FEAT-16 | Obsługa Markdown w notatkach i planie wycieczki | 2 h | Niska | Niska |
| 17 | FEAT-06 | Godziny otwarcia atrakcji | 4-6 h | Średnia | Średnia |
| 18 | FEAT-15 | Zdjęcia atrakcji (obraz ze źródła lub placeholder per kategoria) | 6-8 h | Wysoka | Średnia |
| 19 | FEAT-03 | Narzędzie rysowania i planowania trasy (fundament pod FEAT-12) | 8-10 h | Wysoka | Wysoka |
| 20 | FEAT-12 | Rozbudowa planowania wycieczki (transport, liczba osób, rezerwacje, trasa) | Duże, wymaga brainstormu | Bardzo wysoka | Wysoka (odłożone do czasu ukończenia FEAT-03) |

## Uzasadnienie

1. **Szybkie, wysokowartościowe poprawki bugów** (1-4) — widoczne dla użytkownika, mały koszt zmiany.
2. **Race conditions / integralność danych** (5-7) — ten sam wzorzec (`@Transaction`), mniej widoczne, ale realne ryzyko utraty spójności danych.
3. **Porządki w kodzie** (8-9) — zanim dojdzie więcej nowych funkcji na te same moduły.
4. **Bezpieczeństwo asystenta** (10) — przed dalszą rozbudową asystenta o nowe narzędzia.
5. **UX wymagający więcej pracy, ale izolowany** (11).
6. **Fundamenty Ustawień/języka** (12-14) — zanim dobudujemy na nich kolejne funkcje.
7. **Polish istniejących funkcji** (15-18) — niski/średni koszt, brak zależności.
8. **Duże funkcje z zależnościami** (19-20) — FEAT-12 wymaga ukończenia FEAT-03 i wspólnej burzy mózgów przed implementacją.
