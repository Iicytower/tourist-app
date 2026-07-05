# BUG-11: Limit 50 pozycji w Mojej Liście można ominąć współbieżnym dodaniem

## Problem

`domain/.../usecase/AddToMyListUseCase.kt:11-17` sprawdza rozmiar listy (`getMyList().first()`), a dopiero potem wykonuje zapis (`addToMyList(xid)`) — dwie oddzielne operacje, bez transakcji.

**Scenariusz (TOCTOU):** dwa niemal równoczesne wywołania dodania do listy (np. szybkie podwójne kliknięcie „Dodaj do listy” na dwóch różnych atrakcjach, albo dodanie przez UI i przez asystenta AI w tym samym momencie) mogą oba odczytać `currentSize = 49`, oba przejść walidację `currentSize >= MY_LIST_MAX_SIZE`, i oba zapisać — dając 51 pozycji zamiast twardego limitu 50 (`MY_LIST_MAX_SIZE` w `core`).

## Cel

Sprawdzenie limitu i zapis muszą być atomowe — limit 50 musi być egzekwowany także przy współbieżnych wywołaniach.

## Zakres zmian

- Warstwa `data` (Room) — dodać metodę DAO/repozytorium wykonującą sprawdzenie liczby pozycji i insert w jednej transakcji `@Transaction`, zwracającą błąd/`false` gdy limit osiągnięty, zamiast osobnego odczytu i zapisu z poziomu use case'u.
- `domain/.../usecase/AddToMyListUseCase.kt` — dostosować do nowego, atomowego API repozytorium (usunąć osobny odczyt rozmiaru przed zapisem, polegać na wyniku transakcji).
- `domain/.../repository/` (interfejs repozytorium Mojej Listy) — zaktualizować sygnaturę, jeśli się zmienia (np. `addToMyList(xid): Result<Unit>` zwracające błąd przy przekroczeniu limitu).

## Weryfikacja

- Dwa równoległe wywołania dodania do Mojej Listy przy 49 istniejących pozycjach → tylko jedno się powiedzie, druga zwraca błąd limitu — finalny rozmiar listy nie przekracza 50.
- Standardowe, sekwencyjne dodawanie do 50 pozycji nadal działa bez zmian.
