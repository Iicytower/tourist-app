# FEAT-12: Rozbudowa planowania wycieczki — środek transportu, liczba osób, rezerwacje, posiłki/odpoczynek, trasa na mapie

## Zależność

**Wymaga ukończenia FEAT-03 (narzędzie rysowania i planowania trasy)** — ten task korzysta z mechanizmu wyznaczania/rysowania trasy na mapie zbudowanego w FEAT-03. Nie zaczynać przed jego zakończeniem.

Rozbudowuje też [[FEAT-09]] (generowanie planu wycieczki przez LLM przypisanego do listy) — ten task zakłada, że FEAT-09 już istnieje i dodaje mu kolejne wymiary.

## Status

**Wymaga burzy mózgów przed implementacją.** Poniższe punkty to zebrane wymagania od użytkownika, nie gotowa specyfikacja — architektura, dokładny kształt promptu, format danych i UI do wspólnego ustalenia przed napisaniem kodu.

## Zebrane wymagania (surowe, do doprecyzowania)

1. **Pytanie o środek transportu przed generowaniem planu** — piechota, komunikacja miejska, rower, samochód. Wpływa na:
   - realistyczny dobór odległości/kolejności punktów w ciągu dnia (piechota = mniej punktów dziennie niż samochód),
   - sposób wyznaczania trasy na mapie (inny profil routingu pieszy/rowerowy/samochodowy/transportu publicznego — do sprawdzenia czy FEAT-03 i ewentualny routing przez OSRM/Valhalla, wspomniany tam jako opcjonalne rozszerzenie, wspiera różne profile).
2. **Pytanie o liczbę osób** — wpływa na:
   - uwzględnienie w planie miejsc, gdzie **grupowa rezerwacja może być wymagana** (np. restauracje, atrakcje z limitem wejść) — LLM powinien o tym wspomnieć w planie jako notatkę/ostrzeżenie, nie musi automatycznie rezerwować.
3. **Realizm dnia zwiedzania** — plan ma uwzględniać:
   - przerwy na posiłki w ciągu dnia (śniadanie/obiad/kolacja w rozsądnych porach),
   - przerwy na odpoczynek/kawę/herbatę między atrakcjami,
   - nie układać punktów "plecami do siebie" bez luzu czasowego.
4. **Wyznaczenie i pokazanie trasy na mapie** — po wygenerowaniu planu, punkty z planu (w ustalonej kolejności per dzień) mają być narysowane na mapie z wykorzystaniem mechanizmu z FEAT-03 (np. `RouteLayer`/`PlanRouteUseCase` — do zweryfikowania i ewentualnego rozszerzenia o kolejność zadaną przez plan LLM zamiast tylko greedy nearest-neighbor).

## Pytania otwarte do burzy mózgów

- Czy pytania o transport/liczbę osób pojawiają się jako krótki formularz **przed** wywołaniem LLM (deterministyczne dane wejściowe do promptu), czy jako pierwsze pytania **zadawane przez asystenta w rozmowie** (LLM samo pyta, zanim zbierze dość informacji do wygenerowania planu)? Wpływa na UX i na to, gdzie te dane są przechowywane (czy to pola w `TripPlan`/`TripList`, czy tylko kontekst jednej generacji).
- Czy środek transportu i liczba osób są **przypisane do planu** (i zmieniają się przy regeneracji) czy **do listy** (stałe, niezależne od tego ile razy plan jest generowany)?
- Jak dokładnie plan (z FEAT-09, format JSON dni/punktów) rozszerzyć o: pola czasu (godziny poszczególnych punktów/przerw), pole "wymaga rezerwacji" per punkt, środek transportu per dzień (czy per cała wycieczka)?
- Czy trasa rysowana na mapie ma być per dzień (osobna trasa/kolor na dzień) czy cała wycieczka na raz?
- Czy routing ma pozostać euklidesowy/Haversine (jak w MVP FEAT-03) mimo różnych środków transportu, czy to jest moment, żeby jednak wdrożyć realny routing (OSRM/Valhalla) wspomniany w FEAT-03 jako opcja — inaczej "samochód" i "piechota" dają tę samą linię prostą na mapie, co jest mylące.
- Co się dzieje z istniejącym planem (bez tych danych) po wdrożeniu tego taska — czy stare plany dostają wartości domyślne, czy wymagają regeneracji?

## Prawdopodobny zakres plików (do zweryfikowania po burzy mózgów)

- `domain/.../model/TripPlan.kt` (z FEAT-09) — rozszerzenie o transport, liczbę osób, pola czasowe, flagi rezerwacji
- `domain/.../usecase/GenerateTripPlanUseCase.kt` (z FEAT-09) — prompt rozszerzony o nowe parametry wejściowe i instrukcje dot. posiłków/odpoczynku/rezerwacji
- `feature-mylist/.../ui/TripPlanScreen.kt` (z FEAT-09) — formularz wstępny (transport, liczba osób) przed generowaniem
- `feature-map` — integracja z `PlanRouteUseCase`/`RouteLayer` z FEAT-03, zasilenie kolejnością punktów z planu zamiast (lub obok) greedy nearest-neighbor
- `app/src/main/assets/agents/` — nowy lub rozszerzony prompt dla generowania planu

## Weryfikacja (wstępna, do doprecyzowania po burzy mózgów)

- Generowanie planu pyta o środek transportu i liczbę osób przed wygenerowaniem
- Wygenerowany plan zawiera przerwy na posiłki i odpoczynek, nie tylko listę atrakcji jedna po drugiej
- Plan dla dużej grupy zawiera wzmianki o miejscach wymagających rezerwacji
- Po wygenerowaniu planu trasa jest widoczna na mapie (zakładka Mapa) w kolejności zgodnej z planem
