# FEAT-19: Agent samodzielnie przygotowujący listę i plan wycieczki

## Problem

Obecnie zbudowanie listy atrakcji i planu wycieczki wymaga ręcznej pracy użytkownika: przeszukania okolicy, ręcznego dodawania atrakcji do listy, a potem osobnego wywołania generowania planu (`GenerateTripPlanUseCase`) na już zebranej liście. Ogólny asystent (`AssistantViewModel`) ma narzędzia do tworzenia list i pobierania/aktualizowania planu, ale nie prowadzi użytkownika przez ustrukturyzowany wywiad przed zbudowaniem czegokolwiek — użytkownik musi sam wiedzieć, o co poprosić i w jakiej kolejności.

## Cel

Dedykowany tryb/agent, który:
1. Zadaje użytkownikowi serię pytań doprecyzowujących wycieczkę (lokalizacja/region, liczba dni, zainteresowania — jeśli nie wynikają już z `AppSettings.userInterests`, tempo zwiedzania, ewentualne ograniczenia typu budżet/dostępność).
2. Na podstawie odpowiedzi samodzielnie wyszukuje atrakcje (`SearchAttractionsUseCase`/`AttractionRepository`), filtruje jakość (wzorem `LlmAttractionQualityFilter`), tworzy nową listę wycieczek (`CreateTripListUseCase`) i dodaje do niej wybrane atrakcje.
3. Generuje plan wycieczki dla utworzonej listy (`GenerateTripPlanUseCase`).
4. Prezentuje użytkownikowi wynik (listę + plan) do przejrzenia/edycji, bez konieczności ręcznego przechodzenia przez wszystkie kroki osobno.

## Zakres zmian

- `app/src/main/assets/agents/trip-planner-agent.txt` (nowy) — system prompt: rola planisty prowadzącego wywiad, jasno określona kolejność pytań i kryteria przejścia do fazy wyszukiwania/budowy listy.
- `domain/.../usecase/PlanTripInteractivelyUseCase.kt` (nowy, nazwa robocza) — orkiestracja: wywiad (seria pytań w konwersacji) → `SearchAttractionsUseCase` → filtr jakości → `CreateTripListUseCase`/`AddToTripListUseCase` (pętla) → `GenerateTripPlanUseCase`. Prawdopodobnie zaimplementowane jako rozszerzenie pętli tool-calling asystenta o nowe narzędzia (`ask_clarifying_question` jest zbędne — pytania to zwykłe tury czatu; realne nowe narzędzia to raczej `search_and_curate_attractions`, `build_trip_list_and_plan`) niż całkiem osobny use case — do ustalenia przy projektowaniu.
- `feature-assistant/.../AssistantToolDefs.kt` — nowe narzędzia dla tego trybu (albo osobny ekran/wejście "Zaplanuj wycieczkę dla mnie" uruchamiający dedykowaną konwersację z innym system promptem niż domyślny `assistant.txt`).
- `feature-assistant/.../ui/AssistantScreen.kt` — punkt wejścia do trybu (np. przycisk/skrót rozpoczynający konwersację z promptem `trip-planner-agent.txt` zamiast `assistant.txt`).
- Uwzględnić istniejące ograniczenia bezpieczeństwa z FEAT-14 (potwierdzanie destrukcyjnych zmian) — tworzenie nowej listy i masowe dodawanie atrakcji nie jest destrukcyjne (nic nie nadpisuje), ale generowanie planu na już istniejącej liście z planem powinno przechodzić przez te same zabezpieczenia.
- Uwzględnić limit list podróży (`AssistantToolDefs`/`CreateTripListUseCase` — patrz backlog #29: brak górnego limitu liczby list) — ten agent tworzy nowe listy automatycznie, więc limit staje się bardziej istotny; jeśli #29 nie jest jeszcze zaimplementowany, rozważyć przy tej okazji.

## Pliki do zmiany / stworzenia

- `app/src/main/assets/agents/trip-planner-agent.txt` (nowy)
- `domain/src/main/kotlin/.../usecase/PlanTripInteractivelyUseCase.kt` (nowy, nazwa robocza)
- `feature-assistant/src/main/kotlin/.../AssistantToolDefs.kt`
- `feature-assistant/src/main/kotlin/.../viewmodel/AssistantViewModel.kt`
- `feature-assistant/src/main/kotlin/.../ui/AssistantScreen.kt`

## Weryfikacja

- Użytkownik uruchamia tryb "Zaplanuj wycieczkę dla mnie" → agent zadaje pytania o lokalizację/liczbę dni/zainteresowania zamiast od razu zgadywać
- Po udzieleniu odpowiedzi agent samodzielnie tworzy nową listę, wypełnia ją pasującymi atrakcjami i generuje plan — bez dodatkowych ręcznych kroków użytkownika
- Wynikowa lista i plan są widoczne i edytowalne na standardowych ekranach (`TripListDetailScreen`/`TripPlanScreen`)
- Agent nie tworzy duplikatów list przy powtórnym uruchomieniu tej samej rozmowy

<!-- TODO: konsultacja — zakres tego taska jest szeroki (orkiestracja wielu use case'ów + nowy tryb konwersacji); przy planowaniu warto rozważyć rozbicie na mniejsze taski (np. osobno: wywiad z pytaniami, osobno: automatyczne wyszukanie+dodanie do listy, osobno: spięcie z generowaniem planu) zamiast jednego dużego PR. -->
