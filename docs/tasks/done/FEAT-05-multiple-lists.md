# FEAT-05: Wiele list użytkownika

## Problem

Aktualnie istnieje tylko jedna globalna "Moja Lista". Użytkownik nie może organizować atrakcji w osobne zestawy (np. "Wycieczka do Warszawy", "Weekendy w Ciechocinie").

## Cel

Umożliwić tworzenie wielu nazwanych list i przypisywanie atrakcji do wybranej listy przy dodawaniu.

## Zakres zmian

### Model danych

- Nowa encja `TripList(id, name, createdAt)` w Room
- Relacja M:N między `Attraction` a `TripList` przez tabelę pośrednią `AttractionListCrossRef(attractionXid, listId)`
- `MY_LIST_MAX_SIZE` (50) stosowany per lista

### Domain

- `TripList` — model domenowy
- `TripListRepository` — interfejs: `getLists()`, `createList(name)`, `deleteList(id)`, `addToList(xid, listId)`, `removeFromList(xid, listId)`, `getAttractionsForList(listId)`
- Use case'y: `GetTripListsUseCase`, `CreateTripListUseCase`, `AddToTripListUseCase`, `RemoveFromTripListUseCase`

### Data

- `TripListDao` i `AttractionListCrossRefDao` w Room
- `RoomTripListRepository` implementuje `TripListRepository`
- Migracja bazy (lub fallbackToDestructiveMigration na MVP)

### UI — flow dodawania do listy

Gdy użytkownik tapnie ikonę serca / "Dodaj do listy" w `AttractionDetailScreen`:
1. Otwiera się bottom sheet z listą dostępnych list + przycisk "Utwórz nową listę"
2. Zaznaczone są listy, do których atrakcja już należy
3. Tap na listę → dodaje/usuwa atrakcję z tej listy
4. "Utwórz nową listę" → dialog z polem tekstowym na nazwę

### UI — zakładka "Moje Listy"

- Ekran główny pokazuje listę list (nazwa, liczba atrakcji)
- Tap w listę → ekran z atrakcjami tej listy (obecny `MyListScreen` staje się widokiem detalu)
- Możliwość usunięcia listy (długie przytrzymanie lub ikona kosza)

### Ikona serca w TopAppBar

Aktualnie `isInMyList` (Bool) — zastąpić stanem: brak na żadnej liście / na przynajmniej jednej liście / na wszystkich listach (opcjonalnie — minimum: jest/nie ma na jakiejkolwiek liście).

## Pliki do zmiany

- `domain/` — nowe modele, interfejsy, use case'y
- `data/` — Room: nowe encje, DAO, migracja, `RoomTripListRepository`
- `feature-mylist/` — przebudowa na dwupoziomową nawigację
- `feature-detail/` — bottom sheet wyboru listy zamiast bezpośredniego toggle
- `app/` — nawigacja (nowy ekran detalu listy)
- `core/` — stała `MY_LIST_MAX_SIZE` pozostaje, stosowana per lista

## Weryfikacja

- Można utworzyć co najmniej dwie listy o różnych nazwach
- Dodając atrakcję wybieramy listę (lub kilka)
- Zakładka "Moje Listy" pokazuje wszystkie listy z liczbą atrakcji
- Wejście w listę pokazuje jej atrakcje
- Usunięcie listy usuwa też powiązania (kaskadowo), nie usuwa atrakcji z bazy
- Limit 50 atrakcji egzekwowany per lista
