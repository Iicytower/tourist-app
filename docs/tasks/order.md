# Kolejność realizacji tasków

Ustalona kolejność wykonywania tasków z `docs/tasks/todo/`. Priorytet: szybkie i widoczne poprawki bugów → integralność danych → porządki w kodzie → bezpieczeństwo asystenta → fundamenty (Ustawienia/język) → polish istniejących funkcji → duże nowe funkcje z zależnościami.

Przy dodawaniu nowego taska do `docs/tasks/todo/` dopisz go tutaj we właściwym miejscu kolejki (z czasem/trudnością/ważnością) zamiast zostawiać poza tym plikiem.

| # | Task | Opis | Czas | Trudność | Ważność |
|---|---|---|---|---|---|
| 1 | BUG-10 | Wyniki wyszukiwania nigdy nie pojawiają się na mapie poza trybem „Moja Lista” | 2-3 h | Średnia | Wysoka |
| 2 | BUG-12 | Ten sam problem współbieżności co BUG-11, dla list podróży | 1 h | Niska | Średnia |
| 3 | BUG-14 | Wolniejsze wyszukiwanie potrafi nadpisać nowsze wyniki (brak anulowania joba) | 1-2 h | Średnia | Średnia |
| 4 | BUG-16 | Usunąć martwą integrację OpenTripMap i zdublowany moduł Overpass | 1-2 h | Niska | Średnia |
| 5 | BUG-18 | Systematyczne czyszczenie martwego kodu w całym repo | 3-5 h | Średnia | Średnia |
| 6 | FEAT-14 | Asystent musi potwierdzać destrukcyjne zmiany (dialog Tak/Nie) + cofnięcie planu | 3-4 h | Średnia/Wysoka | Wysoka (bezpieczeństwo) |
| 7 | BUG-07 | Przeprojektowanie pickera lokalizacji na mapie (long-press zamiast pan-under-pin) | 3-4 h | Średnia/Wysoka | Średnia |
| 8 | FEAT-13 | Dogonienie ekranu Ustawień + weryfikacja wpływu zainteresowań na wyszukiwanie | 4-6 h | Średnia | Wysoka (fundament) |
| 9 | FEAT-11 | Jedno ustawienie „Język aplikacji” (wyszukiwanie, podpowiedzi lokalizacji, asystent) | 3-4 h | Średnia | Średnia/Wysoka |
| 10 | FEAT-10 | Wyszukiwanie informacji o atrakcji w lokalnym języku kraju | 3-4 h | Średnia | Niska/Średnia |
| 11 | FEAT-17 | Dopracowanie planowania wycieczki (regeneracja, reorder, błędy, walidacja planu) | 6-8 h | Wysoka | Średnia |
| 12 | FEAT-16 | Obsługa Markdown w notatkach i planie wycieczki | 2 h | Niska | Niska |
| 13 | FEAT-06 | Godziny otwarcia atrakcji | 4-6 h | Średnia | Średnia |
| 14 | FEAT-15 | Zdjęcia atrakcji (obraz ze źródła lub placeholder per kategoria) | 6-8 h | Wysoka | Średnia |
| 15 | FEAT-03 | Narzędzie rysowania i planowania trasy (fundament pod FEAT-12) | 8-10 h | Wysoka | Wysoka |
| 16 | FEAT-12 | Rozbudowa planowania wycieczki (transport, liczba osób, rezerwacje, trasa) | Duże, wymaga brainstormu | Bardzo wysoka | Wysoka (odłożone do czasu ukończenia FEAT-03) |

## Uzasadnienie

1. **Szybkie, wysokowartościowe poprawki bugów** (1) — widoczne dla użytkownika, mały koszt zmiany (BUG-09, BUG-15, BUG-17, BUG-11 z tej grupy już ukończone).
2. **Race conditions / integralność danych** (2-3) — ten sam wzorzec (`@Transaction`), mniej widoczne, ale realne ryzyko utraty spójności danych.
3. **Porządki w kodzie** (4-5) — zanim dojdzie więcej nowych funkcji na te same moduły.
4. **Bezpieczeństwo asystenta** (6) — przed dalszą rozbudową asystenta o nowe narzędzia.
5. **UX wymagający więcej pracy, ale izolowany** (7).
6. **Fundamenty Ustawień/języka** (8-10) — zanim dobudujemy na nich kolejne funkcje.
7. **Polish istniejących funkcji** (11-14) — niski/średni koszt, brak zależności.
8. **Duże funkcje z zależnościami** (15-16) — FEAT-12 wymaga ukończenia FEAT-03 i wspólnej burzy mózgów przed implementacją.
