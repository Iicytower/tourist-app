# BUG-17: Szybkie zaznaczanie kilku zainteresowań gubi wcześniejsze wybory

## Problem

`feature-settings/.../ui/SettingsScreen.kt:227-234` (checkbox listy zainteresowań) liczy nowy zbiór jako `currentInterests + category` / `currentInterests - category`, gdzie `currentInterests` pochodzi z bieżącego `settings?.userInterests` w `uiState`. `SettingsViewModel` odświeża `uiState` asynchronicznie — przez `getSettingsUseCase()` obserwujący Flow z DataStore (`DataStoreSettingsRepository`).

**Scenariusz:** użytkownik szybko zaznacza kilka kategorii pod rząd (np. trzy checkboxy w krótkim odstępie czasu). Każde kliknięcie oblicza nowy zbiór na bazie `uiState`, który może jeszcze nie odzwierciedlać zapisu z poprzedniego kliknięcia (DataStore + Flow re-emitują asynchronicznie) — kolejne zapisy nadpisują się nawzajem, część wcześniej zaznaczonych kategorii znika po zakończeniu serii kliknięć, bez żadnego komunikatu o błędzie.

## Cel

Zaznaczanie/odznaczanie wielu kategorii pod rząd (nawet szybko) nie może gubić wcześniejszych wyborów.

## Zakres zmian

- `feature-settings/.../viewmodel/SettingsViewModel.kt` — zmienić sposób aktualizacji zainteresowań tak, by kolejne przełączenia nie bazowały na potencjalnie nieaktualnym `uiState`. Warianty do rozważenia przy implementacji:
  - Serializować zapisy (np. przez `Mutex`/kolejkowanie zmian w ViewModelu) tak, by każde kolejne przełączenie czekało na zakończenie poprzedniego zapisu i operowało na jego wyniku, zamiast na starym `uiState`.
  - Utrzymywać lokalny, natychmiast aktualizowany stan zaznaczeń w ViewModelu (źródło prawdy dla UI) i zapisywać do DataStore w tle (debounce), zamiast polegać wyłącznie na re-emisji z Flow jako źródle prawdy dla kolejnego toggle.
- `feature-settings/.../ui/SettingsScreen.kt:216-241` — dostosować do nowego API ViewModelu, jeśli sygnatura `updateInterests`/`toggleInterest` się zmieni.

## Weryfikacja

- Szybkie zaznaczenie 3-4 kategorii pod rząd (np. programowo w teście UI, symulując kliknięcia bez czekania na przebudowę stanu) → wszystkie zaznaczone kategorie są zapisane po zakończeniu serii kliknięć.
- Pojedyncze, wolne zaznaczanie/odznaczanie działa jak dotychczas.
