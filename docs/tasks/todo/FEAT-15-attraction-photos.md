# FEAT-15: Zdjęcia atrakcji (obraz ze źródła lub placeholder per kategoria)

## Cel

Wyświetlać zdjęcie atrakcji tam, gdzie dziś jest tylko tekst/ikona kategorii (karta wyniku wyszukiwania, ekran szczegółów, moja lista, popup pinezki na mapie). Jeśli źródło danych dostarcza obraz — użyć go. Jeśli żadne źródło go nie ma — pokazać placeholder przypisany do kategorii atrakcji.

## Stan obecny (research)

- **Model domenowy** `domain/.../model/Attraction.kt` — brak pola na obraz.
- **Room** `data/.../entity/AttractionEntity.kt` — brak kolumny na obraz; wymagana migracja Room (projekt jest na MVP z `fallbackToDestructiveMigration()`, więc formalna migracja niekonieczna, ale trzeba dopisać pole).
- **Źródła danych — żadne obecnie nie przekazuje URL obrazka dalej, mimo że część już go ma w surowej odpowiedzi:**
  - Overpass: `OverpassElement.tags` (`data/remote/overpass/dto/OverpassDto.kt`) zawiera surowe tagi OSM (`image`, `wikimedia_commons`, `wikipedia`) — `OverpassMapper.toAttraction()` ich nie odczytuje.
  - Wikipedia: REST `/page/summary/` zwraca `thumbnail`/`originalimage`, ale `WikipediaSummaryResponse` (`data/remote/wikipedia/dto/WikipediaDto.kt`) tych pól nie deklaruje, `WikipediaServiceImpl.kt` ich nie parsuje.
  - Wikidata: `WikidataSparqlSource.kt` (SPARQL query) nie pobiera property `P18` (image) — trzeba dodać `?image`/`wdt:P18 ?image` do zapytania.
  - OpenTripMap: **martwy kod, do usunięcia w BUG-16** — nie brać jako źródła obrazu.
- **`CompositeAttractionSource.deduplicate()`** (`data/remote/composite/CompositeAttractionSource.kt`) scala duplikaty po współrzędnych, wybierając rekord z najdłuższą nazwą — brak logiki scalania pól typu `imageUrl`.
- **UI** — żaden ekran nie renderuje dziś obrazków: `SearchScreen.kt` (karta wyniku, ~linia 378), `AttractionDetailScreen.kt` (~linia 130), `MapScreen.kt` (popup pinezki, ~linia 173), `MyListScreen.kt`/`TripListDetailScreen.kt` — wszędzie tylko tekst kategorii + generyczna ikona Material.
- **Kategorie** (`core/.../model/AttractionCategory.kt`) — 10 kategorii (`CASTLES_AND_FORTIFICATIONS`, `CHURCHES_AND_SACRED`, `MUSEUMS_AND_GALLERIES`, `RUINS_AND_ARCHAEOLOGICAL`, `NATURE_AND_PARKS`, `VIEWPOINTS`, `MILITARY`, `MILLS_AND_TECH`, `MEMORIALS_AND_CEMETERIES`, `CAVES_AND_GEOLOGY`) — **brak istniejącego systemu ikon/drawable per kategoria**, trzeba zaprojektować od zera.
- **Biblioteka do ładowania obrazków** — brak w projekcie (brak Coil/Glide w `gradle/libs.versions.toml`). Do dodania (rekomendacja: **Coil** — natywne wsparcie Compose + Ktor engine, spójne ze stackiem projektu). Wymaga zgody użytkownika przed instalacją zależności zgodnie z zasadami projektu (mimo ogólnej autonomii co do commitów/pushy, dodanie nowej zależności JS/Kotlin do propozycji i zatwierdzenia — potwierdzić z użytkownikiem przy starcie implementacji).

## Zakres zmian

1. **Model i baza danych**
   - Dodać `imageUrl: String?` do `Attraction` (domain) i `AttractionEntity` (data), zaktualizować `AttractionMapper.kt`.
2. **Pozyskiwanie obrazu ze źródeł**
   - Wikipedia: rozszerzyć `WikipediaSummaryResponse` o `thumbnail`/`originalimage`, przekazać jako `imageUrl` w `WikipediaResult`.
   - Wikidata: dodać `?image`/`wdt:P18` do zapytania SPARQL w `WikidataSparqlSource.kt`, zmapować na `imageUrl`.
   - Overpass: odczytać `tags["image"]` / `tags["wikimedia_commons"]` (zbudować URL do Wikimedia Commons z tagu) w `OverpassMapper.toAttraction()`.
   - Ustalić i udokumentować priorytet źródeł przy scalaniu (rekomendacja: Wikidata P18 > Wikipedia thumbnail > OSM image/wikimedia_commons — Wikidata/Wikipedia zwykle mają wyższą jakość i trafność niż surowe tagi OSM).
3. **Scalanie w `CompositeAttractionSource`**
   - Rozszerzyć `deduplicate()`, by przy scalaniu duplikatów wybierał pierwszy dostępny `imageUrl` wg ustalonego priorytetu źródeł, zamiast go tracić.
4. **Placeholdery per kategoria**
   - Zaprojektować/dobrać 10 placeholderów (jeden per `AttractionCategory`) — grafika lub Material Icon w formie większego, stylizowanego tile'a (do ustalenia wizualnie, może razem z UX designem karty wyniku).
   - Dodać mapę `AttractionCategory -> DrawableRes` (analogicznie do `CategoryKindsMapping`), użyć jako fallback gdy `imageUrl == null` lub ładowanie się nie powiedzie.
   - **Kategorie są objęte osobnym tematem do dopracowania razem z ustawieniami aplikacji** (`FEAT-13-settings-catchup.md` — sprawdzić czy się pokrywa/nie duplikuje) — jeśli lista/nazwy kategorii ulegną zmianie w ramach tamtego taska, placeholdery muszą nadążyć za finalnym zestawem kategorii. Skoordynować kolejność realizacji z FEAT-13.
5. **UI**
   - Dodać Coil (`AsyncImage`) do modułów `feature-search`, `feature-detail`, `feature-map`, `feature-mylist` (sprawdzić `build.gradle.kts` każdego modułu).
   - Podłączyć obraz/placeholder w: karcie wyniku wyszukiwania (`SearchScreen.kt`), nagłówku ekranu szczegółów (`AttractionDetailScreen.kt`), popupie pinezki mapy (`MapScreen.kt`), liście „Moja lista"/listach podróży (`MyListScreen.kt`, `TripListDetailScreen.kt`).
   - Obsłużyć błąd ładowania obrazu (np. 404, brak sieci) — fallback na placeholder kategorii, bez crasha.

## Poza zakresem (świadomie)

- Cache'owanie/pobieranie obrazów offline poza domyślnym cache Coil.
- Możliwość wgrania własnego zdjęcia przez użytkownika.
- Automatyczna reindeksacja/uzupełnienie `imageUrl` dla atrakcji już zapisanych w lokalnej bazie przed tą zmianą (nowe wyszukiwania będą miały obraz, stare rekordy — nie, dopóki nie zostaną odświeżone).

## Weryfikacja

- Atrakcja z dostępnym obrazem w źródle (np. znany zamek z artykułem na Wikipedii) pokazuje realne zdjęcie na karcie wyniku, w szczegółach, w popupie mapy i w Mojej Liście.
- Atrakcja bez obrazu w żadnym źródle pokazuje poprawny placeholder odpowiadający jej kategorii.
- Błąd sieci przy ładowaniu obrazu (np. wyłączony internet po zapisaniu URL) nie crashuje aplikacji — pokazuje placeholder.
- `./gradlew build` przechodzi bez błędów.
