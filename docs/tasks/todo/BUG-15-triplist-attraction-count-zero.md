# BUG-15: Liczba atrakcji na liście podróży zawsze pokazuje 0

## Problem

`data/src/.../local/dao/TripListDao.kt:37-43`, `getListsForAttraction(...)` na sztywno wstawia `0 as attractionCount` w zapytaniu SQL, zamiast policzyć realną liczbę atrakcji na liście przez join/podzapytanie do `attraction_list_crossref`.

`TripList.attractionCount`/`TripListWithCount` jest używane w UI m.in. w arkuszu wyboru listy (`AttractionDetailScreen.ListSelectionSheet`, `"${list.attractionCount} atrakcji"`) — dla wyników tej konkretnej metody zawsze pokaże „0 atrakcji”, niezależnie od faktycznej zawartości listy.

## Cel

`getListsForAttraction` ma zwracać realną liczbę atrakcji na każdej liście.

## Zakres zmian

- `data/src/main/kotlin/com/iicytower/wanderlist/data/local/dao/TripListDao.kt:37-43` — zamienić `0 as attractionCount` na podzapytanie/join liczący wiersze w `attraction_list_crossref` dla danego `listId` (wzorem innych metod w tym samym pliku, jeśli już liczą to poprawnie gdzie indziej).

## Weryfikacja

- Lista z ≥1 atrakcją → arkusz wyboru listy w szczegółach atrakcji pokazuje poprawną, niezerową liczbę atrakcji.
- Pusta lista nadal pokazuje 0.
