# FEAT-10: Wyszukiwanie informacji o atrakcji w lokalnym języku kraju

## Problem

`GenerateDescriptionUseCase` wysyła zapytanie do `WebSearchService` zawsze w stałym, polskim stringu (`"${attraction.name} atrakcja turystyczna"`), niezależnie od tego, gdzie znajduje się atrakcja. Dla miejsc poza obszarem polsko-/anglojęzycznym (np. Japonia, Korea, Grecja) skutkuje to gorszymi wynikami wyszukiwania — lokalne źródła (najczęściej najbogatsze w szczegóły) są pisane w lokalnym języku i nie trafiają w wyniki wyszukiwania po polsku/angielsku.

## Cel

Gdy atrakcja znajduje się w kraju, którego język da się ustalić, dodatkowo wykonać wyszukiwanie w tym lokalnym języku i uwzględnić wyniki w kontekście przekazywanym do LLM przy generowaniu opisu.

## Zakres zmian

### Kraj atrakcji

> Zaktualizowano po BUG-16 (usunięcie martwej integracji OpenTripMap) — faktycznym źródłem atrakcji jest dziś `CompositeAttractionSource` (Overpass + Wikipedia geosearch + Wikidata SPARQL), nie OpenTripMap.

- `domain/.../model/Attraction.kt` — dodać pole `countryCode: String?` (ISO 3166-1 alpha-2).
- `data/remote/overpass/dto/OverpassDto.kt` — Overpass zwraca tagi OSM per element; sprawdzić dostępność `tags["addr:country"]` (ISO alpha-2, gdy w ogóle obecny — w praktyce rzadko wypełniony dla POI, częściej trzeba by reverse-geocodingu).
- `data/remote/overpass/mapper/OverpassMapper.kt` — uzupełnić `toAttraction()` o `countryCode = tags["addr:country"]`.
- `data/remote/wikidata/...` — sprawdzić czy odpowiedź SPARQL z Wikidata (źródło `WikidataSparqlSource`) da się rozszerzyć o `wdt:P17` (kraj) w zapytaniu — Wikidata ma to pole bardziej niezawodnie niż tagi OSM.
- Skoro żadne z trzech źródeł nie gwarantuje kraju dla każdej atrakcji, rozważyć fallback: reverse-geocoding przez istniejący `GeocoderService`/Nominatim (`reverseGeocode()` z FEAT-07) po współrzędnych, tylko gdy `countryCode` z Overpass/Wikidata jest `null` — kosztowniejsze (dodatkowe zapytanie), więc ograniczyć do atrakcji faktycznie dodawanych do opisu (on-demand w `GenerateDescriptionUseCase`, nie przy każdym wyniku wyszukiwania).
- Jeśli żadne źródło nie da kraju, pole zostaje `null` i logika lokalizacji języka jest pomijana (fallback do obecnego zachowania).

### Mapowanie kraj → język

- `core/.../util/CountryLanguage.kt` (nowy) — funkcja `languageForCountryCode(countryCode: String): String?` zwracająca kod języka (np. `"ja"` dla `"JP"`, `"ko"` dla `"KR"`). Wykorzystać `java.util.Locale` tam gdzie to możliwe (`Locale("", countryCode).displayLanguage` nie daje kodu języka wprost, więc potrzebna prosta tabela dla najczęstszych krajów turystycznych + fallback na `null` dla nieobsłużonych).

### Web search

- `domain/repository/WebSearchService.kt` — rozszerzyć `search(query: String)` o opcjonalny parametr, np. `suspend fun search(query: String, language: String? = null): Result<String>`.
- `data/remote/tavily/TavilyWebSearchService.kt` i `TavilySearchDto.kt` — przekazać `language` jako `search_lang` (lub równoważne pole wspierane przez Tavily API) w `TavilySearchRequest`, gdy podane.

### Generowanie opisu

- `GenerateDescriptionUseCase` — gdy `attraction.countryCode` mapuje się na język różny od `descriptionLanguage` z ustawień, wykonać **dodatkowe** zapytanie `webSearchService.search(attraction.name, language = lokalnyJęzyk)` równolegle z istniejącym, i dołączyć oba wyniki do kontekstu przekazywanego w prompt do LLM (LLM i tak tłumaczy finalną odpowiedź na `descriptionLanguage` z ustawień).
- Zapytanie samo w sobie zostaje w nazwie atrakcji (nie trzeba tłumaczyć treści zapytania) — Tavily/lokalne wyszukiwarki zwrócą lokalne źródła przez parametr języka/regionu, nie przez tłumaczenie query.

## Pliki do zmiany / stworzenia

- `domain/src/.../model/Attraction.kt`
- `domain/src/.../repository/WebSearchService.kt`
- `domain/src/.../usecase/GenerateDescriptionUseCase.kt`
- `data/src/.../remote/overpass/dto/OverpassDto.kt`
- `data/src/.../remote/overpass/mapper/OverpassMapper.kt`
- `data/src/.../remote/wikidata/...` (mapper/DTO dla SPARQL — dodanie `wdt:P17`, jeśli używane jako drugie źródło kraju)
- `data/src/.../remote/tavily/TavilyWebSearchService.kt`
- `data/src/.../remote/tavily/dto/TavilySearchDto.kt`
- `core/src/.../util/CountryLanguage.kt` (nowy)

## Weryfikacja

- Atrakcja w Japonii (np. Fushimi Inari, kraj `JP`) → generowanie opisu wykonuje dodatkowe zapytanie z `language="ja"`, w logach/kontekście widać wyniki z japońskich źródeł
- Atrakcja w Polsce/kraju bez mapowania język ≠ `descriptionLanguage` → zachowanie bez zmian (jedno zapytanie, jak dotychczas)
- Finalny opis nadal jest w języku ustawionym w `descriptionLanguage`, niezależnie od języka źródeł
