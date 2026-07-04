# BUG-05: Zła pozycja arkusza wyboru listy względem ikony "dodaj do listy"

## Problem

Na ekranie szczegółów atrakcji (`AttractionDetailScreen`) ikona "dodaj do listy" (serce, `Favorite`/`FavoriteBorder`) znajduje się w `TopAppBar` — na górze ekranu. Po kliknięciu otwiera się `ListSelectionSheet` jako `ModalBottomSheet` — czyli wysuwa się od dołu ekranu. Palec użytkownika w momencie kliknięcia jest na górze ekranu, a lista list do wyboru pojawia się na dole, poza zasięgiem kciuka — słaby UX, zwłaszcza na dużych telefonach (trudno dosięgnąć bez zmiany chwytu).

## Cel

Przybliżyć miejsce interakcji (ikona) i miejsce wyboru (lista) do siebie. Dwa warianty do rozważenia — **wybrać jeden przy implementacji**:

### Wariant A: Przenieść listę wyboru na górę
- Zamienić `ModalBottomSheet` na komponent wysuwany/wyświetlany od góry (np. `DropdownMenu` zakotwiczony przy ikonie serca w `TopAppBar`, albo customowy overlay przy górnej krawędzi).
- Zaleta: ikona zostaje w naturalnym miejscu (`TopAppBar`, spójne z resztą akcji ekranu — np. przyciski nawigacji).
- Wada: `DropdownMenu` gorzej się skaluje przy długiej liście list + polu "utwórz nową listę" (może wymagać przewijania w małym menu zamiast pełnego bottom sheetu).

### Wariant B: Przenieść ikonę na dół
- Przenieść przycisk "dodaj do listy" z `TopAppBar` do dolnej części ekranu (np. `BottomAppBar`/`FloatingActionButton`), blisko miejsca gdzie i tak wysuwa się `ModalBottomSheet`.
- Zaleta: zachowuje obecny, sprawdzony `ModalBottomSheet` z pełną funkcjonalnością (checkboxy, tworzenie nowej listy) bez przepisywania.
- Wada: zmiana układu całego ekranu szczegółów atrakcji, trzeba przemyśleć spójność z innymi akcjami w `TopAppBar` (czy zostają, czy też się przenoszą).

## Plik do zmiany

- `feature-detail/src/main/kotlin/com/iicytower/wanderlist/feature/detail/ui/AttractionDetailScreen.kt` — przycisk `IconButton`/`Icons.Default.Favorite` w `actions` `TopAppBar` (obecnie ok. linia z `viewModel.openListSheet()`) oraz `ListSelectionSheet` (linia ~229-308).

## Weryfikacja

- Kliknięcie ikony "dodaj do listy" otwiera wybór listy w miejscu bliskim ikonie (bez konieczności przesuwania kciuka na drugi koniec ekranu)
- Funkcjonalność bez zmian: zaznaczanie/odznaczanie list przez checkbox, tworzenie nowej listy, zamykanie arkusza/menu
