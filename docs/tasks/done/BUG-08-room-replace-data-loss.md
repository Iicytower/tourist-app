# BUG-08: Ponowne wyszukanie kasuje opis atrakcji i wypina ją ze wszystkich list

## Problem

`RoomAttractionRepository.replaceSearchResults()` (`RoomAttractionRepository.kt:41`) wywołuje `AttractionDao.upsertAll()` (`AttractionDao.kt:23-27,48-52`), które używa `@Insert(onConflict = OnConflictStrategy.REPLACE)`. SQLite realizuje `REPLACE` jako `DELETE` + `INSERT` starego wiersza.

Wszystkie mappery zdalne (`OtmMapper.kt:16-19`, Overpass, Wikidata, Wikipedia) budują `Attraction` ze świeżo pobranych danych, gdzie `isInMyList = false`, `description = null`, `dateAddedToList = null` — bo mapper zdalny nic nie wie o lokalnym stanie atrakcji.

**Scenariusz:** Atrakcja X jest w „Mojej Liście”, ma zapisany, wygenerowany opis. Użytkownik ponownie wyszukuje w tej samej okolicy → X ponownie pojawia się w wynikach zdalnego źródła → `upsertAll` REPLACE-uje wiersz X „pustymi” danymi → opis i status „w liście” znikają. Ponieważ `REPLACE` fizycznie usuwa i wstawia wiersz na nowo (nowy `rowid`), **FK CASCADE kasuje też wpisy w `attraction_list_crossref`** — atrakcja wypada ze wszystkich list podróży bez ostrzeżenia.

## Cel

Zachować dane lokalne (`isInMyList`, `description`, `descriptionSources`, `dateAddedToList`) i przynależność do list podróży, gdy atrakcja ponownie pojawia się w wynikach wyszukiwania — jednocześnie odświeżając pola pochodzące ze zdalnego źródła (nazwa, kategoria, współrzędne, itp.).

## Zakres zmian

- `data/src/.../local/dao/AttractionDao.kt` — zamienić `OnConflictStrategy.REPLACE` na strategię, która nie kasuje wiersza: np. `@Update` z ręcznym mergem pól (odczytać istniejący wiersz, nadpisać tylko pola „zdalne”, zachować pola „lokalne”), albo `@Insert(onConflict = IGNORE)` + osobny `@Query("UPDATE ... SET name=:name, category=:category, ... WHERE xid=:xid")` pomijający kolumny `isInMyList`/`description`/`descriptionSources`/`dateAddedToList`.
- `data/src/.../repository/RoomAttractionRepository.kt` — dostosować `replaceSearchResults`/`upsertAll` do nowego mechanizmu mergowania.
- Sprawdzić `data/src/.../local/mapper/AttractionMapper.kt` — czy potrzebny jest osobny mapper „merge remote + local” zamiast pełnego nadpisania encji.

## Weryfikacja

- Atrakcja w „Mojej Liście” z zapisanym opisem → ponowne wyszukanie w tej samej okolicy (atrakcja ponownie w wynikach) → opis, `isInMyList` i przynależność do wszystkich list podróży **pozostają bez zmian**.
- Pola pochodzące ze zdalnego źródła (nazwa, kategoria, współrzędne) nadal się odświeżają przy ponownym wyszukaniu.
- Atrakcja bez lokalnych danych (nigdy nie dodana do listy, bez opisu) nadal poprawnie się aktualizuje/wstawia jak dotychczas.
