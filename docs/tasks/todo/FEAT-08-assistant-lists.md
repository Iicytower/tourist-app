# FEAT-08: Dopracowanie list w kontekście asystenta AI

## Problem

Po wdrożeniu FEAT-05 (wiele list) asystent nadal używa narzędzia `get_my_list`, które zwraca wszystkie atrakcje z flagą `isInMyList=1` — bez rozróżnienia na listy. Asystent nie wie, że użytkownik ma kilka named list, nie może ich wyliczać, czytać ani modyfikować.

## Cel

Dać asystentowi pełną wiedzę o listach użytkownika i możliwość operowania na nich przez tool calling.

## Zakres zmian

### Nowe narzędzia (AssistantToolDefs)

Zastąpić `get_my_list` zestawem:

| Narzędzie | Opis | Parametry |
|---|---|---|
| `get_trip_lists` | Zwraca wszystkie listy z liczbą atrakcji | — |
| `get_list_attractions` | Zwraca atrakcje z wybranej listy | `list_id: integer` |
| `add_to_list` | Dodaje atrakcję do listy | `xid: string`, `list_id: integer` |
| `remove_from_list` | Usuwa atrakcję z listy | `xid: string`, `list_id: integer` |
| `create_list` | Tworzy nową listę | `name: string` |

### AssistantViewModel

- Wstrzyknąć `TripListRepository` (lub use case'y: `GetTripListsUseCase`, `GetAttractionsForListUseCase`, `AddToTripListUseCase`, `RemoveFromTripListUseCase`, `CreateTripListUseCase`)
- Usunąć `GetMyListUseCase` z tego ViewModelu (jeśli nie jest używany inaczej)
- Rozszerzyć `executeTool()` o obsługę nowych narzędzi
- Stary `get_my_list` można zachować jako alias dla kompatybilności wstecznej (opcjonalnie)

### System prompt

Dodać do system promptu asystenta (w ustawieniach) informację:
- Użytkownik może mieć wiele nazwanych list wycieczek
- Narzędzie `get_trip_lists` zwraca listę list; każda ma `id`, `name`, `attractionCount`
- Przed dodaniem/usuwaniem atrakcji asystent powinien zapytać, do której listy

### Format wyników narzędzi

`get_trip_lists`:
```
Lista 1 (id=1): Wycieczka do Warszawy — 5 atrakcji
Lista 2 (id=2): Weekendy w Ciechocinie — 2 atrakcje
```

`get_list_attractions(list_id=1)`:
```
Nazwa: Zamek Królewski | Kategoria: Zamki | Lat: 52.24 | Lon: 21.01
...
```

## Pliki do zmiany

- `feature-assistant/AssistantToolDefs.kt` — nowe definicje narzędzi
- `feature-assistant/viewmodel/AssistantViewModel.kt` — nowe use case'y, obsługa narzędzi
- `feature-assistant/di/AssistantModule.kt` — aktualizacja wstrzyknięć
- `data/local/DefaultSettings.kt` (lub odpowiednik) — zaktualizować system prompt

## Weryfikacja

- Asystent zapytany "jakie mam listy?" wywołuje `get_trip_lists` i poprawnie wymienia listy
- Asystent zapytany "co mam na liście Wycieczka do Warszawy?" wywołuje `get_list_attractions`
- Asystent może dodać znalezioną atrakcję do wybranej listy przez `add_to_list`
- Asystent może utworzyć nową listę przez `create_list`
