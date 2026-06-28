# FEAT-06: Godziny otwarcia atrakcji

## Problem

Ekran szczegółów atrakcji nie pokazuje godzin otwarcia. Użytkownik nie wie, czy atrakcja jest teraz otwarta, ani kiedy można ją odwiedzić.

## Cel

Pobierać i wyświetlać godziny otwarcia dla atrakcji, które je posiadają (muzea, galerie, zamki, kościoły itd.).

## Źródła danych

### Opcja A — OpenStreetMap Overpass API
- Tag `opening_hours` w OSM jest szeroko stosowany i pokrywa większość typów atrakcji
- Format: `Mo-Fr 09:00-17:00; Sa 10:00-14:00; Su off`
- Parser: biblioteka `opening-hours-parser` (JVM) lub własna implementacja uproszczona
- Zapytanie Overpass: `node/way/relation[opening_hours]` przy pobieraniu szczegółów obiektu

### Opcja B — Wikipedia / Wikidata
- Wikidata property `P3025` (schedule) lub `P6477` (opening hours)
- Pokrycie słabsze niż OSM dla mniejszych atrakcji

### Opcja C — Google Places API
- Najlepsza jakość danych, ale wymaga klucza i płatności po przekroczeniu limitu
- Niezgodna z założeniem braku Google Play Services w projekcie

**Rekomendacja MVP: Opcja A (Overpass)**. Projekt już używa Overpass API do wyszukiwania, więc rozszerzenie o `opening_hours` jest naturalne.

## Zakres zmian

### Domain

- Nowy model `OpeningHours(raw: String, parsedSlots: List<TimeSlot>?, isOpenNow: Boolean?)`
- `TimeSlot(dayOfWeek: Int, openTime: LocalTime, closeTime: LocalTime)`
- Pole `openingHours: OpeningHours?` w `Attraction`

### Data

- `OverpassApiClient`: przy pobieraniu szczegółów obiektu uwzględnić tag `opening_hours`
- `OsmOpeningHoursParser`: parser wyrażeń OSM → lista `TimeSlot` + flaga `isOpenNow`
  - MVP: obsługa podstawowych wzorców (`Mo-Fr`, `Mo,We,Fr`, `24/7`, `off`)
  - Nieznane formaty → przechować `raw`, nie parsować

### Data — Room

- Nowa kolumna `openingHoursRaw: String?` w `AttractionEntity` (DB version bump)
- Zapis i odczyt przez `AttractionDao`

### UI — AttractionDetailScreen

- Sekcja "Godziny otwarcia" po nazwie/kategorii:
  - Chip "Otwarte teraz" / "Zamknięte" (zielony/czerwony) jeśli można ustalić
  - Lista dni tygodnia z godzinami (np. `Pon–Pt  9:00–17:00`)
  - Jeśli tylko `raw` bez parsera: wyświetl surowy string szaro
- Brak danych → sekcja ukryta

## Pliki do zmiany

- `domain/model/Attraction.kt` — nowe pole
- `domain/model/OpeningHours.kt` — nowy model
- `data/remote/overpass/` — rozszerzenie zapytania i mappera
- `data/local/entity/AttractionEntity.kt` — nowa kolumna
- `data/local/dao/AttractionDao.kt` — zapis/odczyt
- `data/local/OsmOpeningHoursParser.kt` — nowy parser
- `feature-detail/ui/AttractionDetailScreen.kt` — UI sekcji

## Weryfikacja

- Dla muzeum z danymi w OSM (`opening_hours`) wyświetla się harmonogram
- Chip "Otwarte teraz" / "Zamknięte" jest poprawny relative do aktualnej godziny
- Dla atrakcji bez tagu godziny nie wyświetlają się
- Brak parsowania nieznanego formatu nie crashuje aplikacji
