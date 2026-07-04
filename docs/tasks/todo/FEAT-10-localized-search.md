# FEAT-10: Wyszukiwanie informacji o atrakcji w lokalnym języku kraju

## Problem

`GenerateDescriptionUseCase` wysyła zapytanie do `WebSearchService` zawsze w stałym, polskim stringu (`"${attraction.name} atrakcja turystyczna"`), niezależnie od tego, gdzie znajduje się atrakcja. Dla miejsc poza obszarem polsko-/anglojęzycznym (np. Japonia, Korea, Grecja) skutkuje to gorszymi wynikami wyszukiwania — lokalne źródła (najczęściej najbogatsze w szczegóły) są pisane w lokalnym języku i nie trafiają w wyniki wyszukiwania po polsku/angielsku.

## Cel

Gdy atrakcja znajduje się w kraju, którego język da się ustalić, dodatkowo wykonać wyszukiwanie w tym lokalnym języku i uwzględnić wyniki w kontekście przekazywanym do LLM przy generowaniu opisu.

## Zakres zmian

### Kraj atrakcji

- `domain/.../model/Attraction.kt` — dodać pole `countryCode: String?` (ISO 3166-1 alpha-2).
- `data/remote/opentripmap/dto/OtmAttractionDetailDto.kt` — sprawdzić czy OpenTripMap w odpowiedzi detali zwraca `address.country_code` (dostępne w API OTM); jeśli tak, zmapować.
- `data/remote/opentripmap/OtmMapper.kt` — uzupełnić `toDomain()` o `countryCode`.
- Jeśli OTM nie zwraca kraju dla danej atrakcji (część odpowiedzi może nie mieć `address`), pole zostaje `null` i logika lokalizacji języka jest pomijana (fallback do obecnego zachowania).

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
- `data/src/.../remote/opentripmap/dto/OtmAttractionDetailDto.kt`
- `data/src/.../remote/opentripmap/OtmMapper.kt`
- `data/src/.../remote/tavily/TavilyWebSearchService.kt`
- `data/src/.../remote/tavily/dto/TavilySearchDto.kt`
- `core/src/.../util/CountryLanguage.kt` (nowy)

## Weryfikacja

- Atrakcja w Japonii (np. Fushimi Inari, kraj `JP`) → generowanie opisu wykonuje dodatkowe zapytanie z `language="ja"`, w logach/kontekście widać wyniki z japońskich źródeł
- Atrakcja w Polsce/kraju bez mapowania język ≠ `descriptionLanguage` → zachowanie bez zmian (jedno zapytanie, jak dotychczas)
- Finalny opis nadal jest w języku ustawionym w `descriptionLanguage`, niezależnie od języka źródeł
