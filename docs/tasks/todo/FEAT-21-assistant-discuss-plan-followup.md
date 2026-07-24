# FEAT-21: Dopracowanie i testy funkcji "Omów z asystentem"

## Kontekst

W PR #17 (branch `release/1.1.0`, commit `546aa02`) naprawiono FEAT-14 w zakresie zgłoszonym w komentarzu:
> "feat14 - nie dziala asystent do obgadania planu. on powinien miec z bomby plan w kontekscie, jak dopytam o cos to on niebma pojecia o co chodzi. asystent nie wie o ktory plan chodzi. po kliknieciu w omow z asystentem to chce zeby od razu asystent mial w kontekscie caly plan i wszystko co potrzebne"

Rozwiązanie: `AssistantViewModel.setContextList(listId)` pobiera w tle plan wycieczki
(`formatTripPlan()`, reużywane też przez narzędzie `get_trip_plan`) i wstrzykuje go
ukrycie do **pierwszej** wiadomości wysyłanej do LLM po wejściu z ekranu planu —
niewidoczne w UI czatu, ale obecne w kontekście modelu od pierwszego pytania
użytkownika. Po wyczyszczeniu czatu (`confirmClearChat()`) kontekst jest
wstrzykiwany ponownie przy kolejnej wiadomości.

Poprawka została zweryfikowana testami jednostkowymi (`AssistantViewModelTest`:
`setContextList_injectsHiddenPlanIntoFirstLlmMessage_butNotIntoVisibleChat`,
`setContextList_planMissing_doesNotBreakSendMessage`), **ale nie testami manualnymi
na urządzeniu/emulatorze** — nie zweryfikowano jak realny model LLM faktycznie
zachowuje się z tym kontekstem w praktyce.

## Cel

1. **Test manualny end-to-end** na emulatorze/urządzeniu:
   - Stwórz listę z planem wycieczki (kilka dni, kilka punktów, notatki).
   - Kliknij "Omów z asystentem" na ekranie planu.
   - Zadaj pytanie odnoszące się do konkretnych punktów planu **bez podawania ID
     listy ani nazwy miejsc** (np. "co proponujesz zmienić w drugim dniu?") —
     sprawdź czy asystent faktycznie odpowiada w kontekście, bez pytania
     zwrotnego "o jaki plan chodzi?" ani zgadywania przez wywołanie `get_trip_lists`.
   - Sprawdź scenariusz z listą bez zapisanego planu (fallback `formatTripPlan()`
     zwraca `null`) — upewnij się, że wysłanie wiadomości nie psuje się cicho.
   - Wyczyść czat (`confirmClearChat()`) i wyślij kolejną wiadomość — potwierdź,
     że kontekst planu wraca (zgodnie z zamierzonym zachowaniem).
2. **Dopracowanie na podstawie wyników testu** — jeśli odpowiedzi modelu są
   nietrafne, doprecyzować format wstrzykiwanego kontekstu (np. jaśniejsze
   wyróżnienie że to dane planu, a nie treść od użytkownika) lub treść promptu
   systemowego asystenta (`app/src/main/assets/agents/assistant.txt`).
3. **Rozważyć (do ustalenia z właścicielem produktu)**: czy kontekst planu
   powinien się odświeżać, jeśli użytkownik zmieni plan w trakcie tej samej
   sesji czatu (obecnie wstrzykiwany tylko raz, przy pierwszej wiadomości po
   wejściu z danej listy / po wyczyszczeniu czatu).

## Pliki

- `feature-assistant/src/main/kotlin/com/iicytower/wanderlist/feature/assistant/viewmodel/AssistantViewModel.kt`
- `feature-assistant/src/test/kotlin/com/iicytower/wanderlist/feature/assistant/AssistantViewModelTest.kt`
- `app/src/main/assets/agents/assistant.txt`

## Weryfikacja

- Manualny test na emulatorze/urządzeniu wg scenariuszy z sekcji "Cel" (1).
- `./gradlew :feature-assistant:test` — zielone po ewentualnych zmianach.
