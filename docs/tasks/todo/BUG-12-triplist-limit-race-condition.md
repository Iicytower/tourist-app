# BUG-12: Ten sam problem współbieżności co BUG-11, dla list podróży

## Problem

`data/src/.../repository/RoomTripListRepository.kt:48-55`, `addToList()` liczy `getCountForList()`, a potem wykonuje `insert` — bez `@Transaction`, w przeciwieństwie do `removeFromListAndSync` w tym samym pliku, który jest poprawnie transakcyjny.

**Scenariusz:** dwa równoległe dodania do tej samej listy podróży (np. użytkownik dodaje ręcznie i jednocześnie asystent AI dodaje tę samą atrakcję/inną atrakcję do tej samej listy) mogą oba odczytać ten sam licznik przed zapisem obu wierszy — jeśli listy podróży mają limit pozycji analogiczny do Mojej Listy, można go w ten sposób ominąć.

## Cel

Sprawdzenie licznika i insert atomowe w ramach jednej transakcji Room.

## Zakres zmian

- `data/src/.../repository/RoomTripListRepository.kt:48-55` (`addToList()`) — owinąć w `@Transaction` (na poziomie DAO, analogicznie do `removeFromListAndSync`), tak by odczyt liczby pozycji i insert wykonywały się atomowo.

## Weryfikacja

- Dwa równoległe wywołania `addToList()` dla tej samej listy nie prowadzą do niespójnego/przekroczonego stanu licznika.
- Sekwencyjne dodawanie do listy podróży działa bez zmian.
