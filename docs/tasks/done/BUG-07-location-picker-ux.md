# BUG-07: Przeprojektować UX pickera lokalizacji na mapie (long-press zamiast pinezki pod środkiem)

## Problem

`LocationPickerContent` w `SearchScreen.kt` (linie 278-366) implementuje wybór punktu w stylu "przesuń mapę pod stałą pinezkę": ikona `Icons.Default.Map` jest zawsze wyśrodkowana na ekranie (linie 333-339), a `map.addOnCameraIdleListener` (322-326) odczytuje `map.cameraPosition.target` po każdym zatrzymaniu kamery i zapisuje jako wybrany punkt. Użytkownicy zgłaszają, że mapa "nie przesuwa się wygodnie" — ten wzorzec (pan-under-pin) wymaga precyzyjnego przeciągania całej mapy żeby trafić w konkretny punkt, co jest niewygodne zwłaszcza przy gęściej rozmieszczonych atrakcjach.

## Cel

Zmienić model interakcji na: **wybór punktu przez przytrzymanie (long-press) bezpośrednio na mapie**, a nie przesuwanie mapy pod stały znacznik. Użytkownik swobodnie przesuwa/zooming mapę (bez wpływu na wybór), a punkt wskazuje przytrzymując palec w konkretnym miejscu — tam pojawia się pinezka/marker.

## Zakres zmian

- `feature-search/src/main/kotlin/com/iicytower/wanderlist/feature/search/ui/SearchScreen.kt`
  - `LocationPickerContent` (linie 278-366): usunąć `addOnCameraIdleListener` + stały `Icon` na środku (322-326, 333-339).
  - Dodać obsługę long-press na `MapView`/`MapboxMap` (MapLibre nie ma gotowego `addOnMapLongClickListener` jak zwykły `addOnMapClickListener` — zweryfikować dostępne API MapLibre Android SDK dla long-press; w razie braku wbudowanego listenera, obsłużyć przez `GestureDetector`/`OnTouchListener` na widoku mapy).
  - Po long-press: dodać/przenieść marker (np. `Symbol`/`Annotation` z MapLibre Annotation Plugin, albo Compose overlay pozycjonowany przez `map.projection.toScreenLocation(latLng)`) w miejscu wskazanym palcem, zapisać `pickedLat`/`pickedLon`.
  - Przycisk "Wybierz tę lokalizację" (357-362) zostaje jako explicit confirm, ale powinien być aktywny/widoczny dopiero po tym jak użytkownik cokolwiek wybrał long-pressem (przed pierwszym long-pressem brak wybranego punktu — rozważyć stan początkowy, np. domyślnie `initialLat/initialLon` jak dotychczas, z możliwością nadpisania long-pressem).
- Sprawdzić `feature-map/src/main/kotlin/com/iicytower/wanderlist/feature/map/ui/MapScreen.kt` (linia 121-127, `addOnMapClickListener`) — to inny przypadek użycia (tap = wybór istniejącej pinezki atrakcji, nie wybór dowolnego punktu), nie ruszać, ale można się wzorować na ogólnym wzorcu obsługi kliknięć na `MapLibreMap` w tym pliku.

## Weryfikacja

- Przesuwanie/zoomowanie mapy w pickerze nie zmienia wybranego punktu.
- Przytrzymanie palca w dowolnym miejscu mapy ustawia tam marker i aktualizuje wybrany punkt.
- Przycisk "Wybierz tę lokalizację" nadal zatwierdza wybrany punkt (`onPick(lat, lon)`), "Anuluj" nadal działa bez zmian.
- Zachowanie na różnych poziomach zoomu — long-press musi trafiać we właściwe współrzędne niezależnie od aktualnego zoomu/rotacji mapy.
