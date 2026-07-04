# FEAT-09: Generowanie planu wycieczki dla listy przez asystenta AI

## Problem

Użytkownik ma named listy atrakcji (FEAT-05) i może rozmawiać o nich z asystentem (FEAT-08), ale nie ma dedykowanej funkcji, która na podstawie zawartości listy wygeneruje konkretny plan zwiedzania (kolejność, dni, notatki). Plan trzeba dziś układać ręcznie albo prosić asystenta w wolnej rozmowie, bez trwałego powiązania wyniku z listą.

## Cel

Dodać przycisk "Zaplanuj wycieczkę" przy liście, który generuje (przez LLM z pełną pętlą tool-calling, tak jak asystent) ustrukturyzowany plan wycieczki i trwale przypisuje go do tej listy. Plan jest widoczny na dedykowanym ekranie, można go edytować (notatka tekstowa), regenerować przez asystenta i usunąć.

## Zakres zmian

### Model danych

- `domain/.../model/TripPlan.kt` (nowy) — struktura JSON: lista dni, każdy dzień lista punktów w kolejności (odwołanie do `xid` atrakcji + ewentualny czas/uwaga wygenerowana przez LLM), plus jedno pole `notes: String?` na ręczną notatkę usera.
- `domain/.../model/TripList.kt` — dodać `tripPlan: TripPlan?` (lub `hasTripPlan: Boolean` w wariancie listowym + osobne pobieranie planu dla ekranu szczegółów, do ustalenia przy implementacji pod kątem wydajności `getLists()`).
- `data/.../local/entity/TripListEntity.kt` — nowa kolumna `tripPlanJson: String?` (serializowany JSON) + `tripPlanNotes: String?`. Wymaga migracji Room (aktualnie `fallbackToDestructiveMigration()` na MVP — zgodnie z CLAUDE.md można to wykorzystać, ale odnotować że migracja będzie potrzebna przed produkcją).
- `data/.../local/dao/TripListDao.kt` — `updateTripPlan(id: Long, planJson: String?)`, `updateTripPlanNotes(id: Long, notes: String?)`, uwzględnić kasowanie planu w istniejącej logice usuwania listy (cascade — plan znika automatycznie razem z rekordem `trip_lists`, nic dodatkowego nie trzeba robić poza usunięciem wiersza).

### `domain`

- `TripListRepository` — dodać `suspend fun saveTripPlan(listId: Long, plan: TripPlan): Result<Unit>`, `suspend fun updateTripPlanNotes(listId: Long, notes: String?): Result<Unit>`, `suspend fun deleteTripPlan(listId: Long): Result<Unit>`.
- Nowy use case `GenerateTripPlanUseCase` — buduje prompt z atrakcjami listy, uruchamia pętlę LLM z narzędziami asystenta (`web_search`, itp. — reużyć `AssistantToolDefs.ALL`/logikę `executeTool` z `AssistantViewModel`, rozważyć wydzielenie wspólnej pętli tool-calling do współdzielonej klasy, żeby nie duplikować kodu), parsuje odpowiedź do `TripPlan`, zapisuje przez repo.
- `SaveTripPlanNotesUseCase`, `DeleteTripPlanUseCase` (proste wrappery na repo).

### `feature-mylist`

- `TripListDetailScreen` — przycisk "Zaplanuj wycieczkę" w `TopAppBar`/jako akcja:
  - Jeśli lista **nie ma** planu → generuje plan (loading state) i nawiguje/pokazuje dedykowany ekran planu po zakończeniu.
  - Jeśli lista **ma już** plan → pokazuje `AlertDialog` ("Ta lista ma już plan — przejść do asystenta aby go omówić?" [Tak/Anuluj]); po potwierdzeniu nawigacja do ekranu asystenta z kontekstem listy (bez auto-wysłanej wiadomości, user sam pisze).
- Nowy ekran `TripPlanScreen` (+ `TripPlanViewModel`) — wyświetla dni/punkty planu (nazwy atrakcji rozwiązane przez `xid`), pole tekstowe na notatkę usera (edytowalne, zapis przez `SaveTripPlanNotesUseCase`), przycisk "Usuń plan" (z potwierdzeniem), przycisk "Omów z asystentem" (nawigacja do czatu z kontekstem listy).
- `TripListDetailViewModel`/`UiState` — flaga czy lista ma plan (do warunkowego dialogu).

### `feature-assistant`

- Kontekst listy przekazywany do asystenta (już istnieje przez `get_trip_lists`/`get_list_attractions`) rozszerzyć o dołączanie istniejącego planu listy, gdy rozmowa dotyczy konkretnej listy — system prompt lub narzędzie powinno informować asystenta o istnieniu i treści planu.
- Nowe narzędzie w `AssistantToolDefs` — `update_trip_plan` (parametry: `list_id`, nowa struktura JSON planu) — pozwala asystentowi modyfikować istniejący plan w trakcie rozmowy.
- `AssistantViewModel.executeTool()` — obsługa `update_trip_plan` (parsowanie, zapis przez `GenerateTripPlanUseCase`/repo, walidacja że `list_id` istnieje).
- System prompt (ustawienia) — dopisać informację o możliwości odczytu/modyfikacji planu wycieczki przypisanego do listy.

### Usuwanie

- Usunięcie całej listy (`deleteList`) usuwa też plan — zapewnione automatycznie przez usunięcie wiersza `trip_lists` (kolumny `tripPlanJson`/`tripPlanNotes` znikają razem z rekordem), zweryfikować że nie ma osobnej tabeli wymagającej ręcznego czyszczenia.
- Usunięcie samego planu (bez usuwania listy) — z ekranu `TripPlanScreen`, ustawia `tripPlanJson = null`, `tripPlanNotes = null`.

## Pliki do zmiany / stworzenia

- `domain/src/.../model/TripPlan.kt` (nowy)
- `domain/src/.../model/TripList.kt`
- `domain/src/.../repository/TripListRepository.kt`
- `domain/src/.../usecase/GenerateTripPlanUseCase.kt` (nowy)
- `domain/src/.../usecase/SaveTripPlanNotesUseCase.kt` (nowy)
- `domain/src/.../usecase/DeleteTripPlanUseCase.kt` (nowy)
- `data/src/.../local/entity/TripListEntity.kt`
- `data/src/.../local/dao/TripListDao.kt`
- `data/src/.../repository/RoomTripListRepository.kt` (lub odpowiednik nazwy)
- `feature-mylist/src/.../ui/TripListDetailScreen.kt`
- `feature-mylist/src/.../ui/TripPlanScreen.kt` (nowy)
- `feature-mylist/src/.../viewmodel/TripPlanViewModel.kt` (nowy)
- `feature-mylist/src/.../viewmodel/TripListDetailViewModel.kt`
- `feature-assistant/src/.../AssistantToolDefs.kt`
- `feature-assistant/src/.../viewmodel/AssistantViewModel.kt`
- `app/.../navigation` — dodać trasę do `TripPlanScreen` i przekazywanie kontekstu listy przy nawigacji do asystenta
- `data/local/DefaultSettings.kt` (lub odpowiednik) — aktualizacja system promptu

## Otwarte pytania / do ustalenia przy implementacji

- Dokładny format `TripPlan` JSON (nazwy pól dni/punktów) — ustalić przy pisaniu promptu dla LLM, tak by dało się to łatwo sparsować i ewentualnie później wykorzystać w FEAT-03 (trasa na mapie).
- Czy `getLists()` (lista wszystkich list) powinna zwracać info "ma plan" bez ładowania całego JSON-a (osobna kolumna `hasTripPlan` albo `SELECT tripPlanJson IS NOT NULL`) — dla wydajności listy list.
- Wspólna pętla tool-calling między `AssistantViewModel` a `GenerateTripPlanUseCase` — rozważyć wydzielenie do współdzielonej klasy (np. w `domain` lub nowym module), żeby nie duplikować logiki obsługi `LlmEvent`/`executeTool`.

## Weryfikacja

- Lista bez planu → kliknięcie "Zaplanuj wycieczkę" → generowanie (z użyciem narzędzi asystenta) → zapisany plan widoczny na `TripPlanScreen`
- Lista z planem → kliknięcie "Zaplanuj wycieczkę" → dialog potwierdzenia → po akceptacji nawigacja do asystenta z kontekstem listy
- Asystent w rozmowie o liście widzi istniejący plan i może go zmodyfikować przez `update_trip_plan`
- User edytuje notatkę tekstową na `TripPlanScreen` i zapis się utrwala
- User usuwa plan z `TripPlanScreen` — plan znika, lista zostaje
- User usuwa całą listę — plan usuwany razem z nią, brak osieroconych danych
