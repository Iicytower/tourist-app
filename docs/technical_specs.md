# WanderList – Specyfikacja Techniczna

> **Status:** Draft v0.1
> **Powiązany dokument:** `docs/functionalities_specs.md`

---

## 1. Platforma i technologia

- **Platforma:** Android
- **Język:** Kotlin
- **UI:** Jetpack Compose
- **Min. SDK:** 26 (Android 8.0)
- **Target SDK:** 37
- **Java:** 21
- **Application ID:** `com.iicytower.wanderlist` *(do zmiany przed produkcją)*
- **Nazwa aplikacji:** WanderList

---

## 2. Architektura

- **Pattern:** MVVM (Model-View-ViewModel)
- Widok (Compose) obserwuje stan z ViewModelu
- ViewModel przetwarza logikę i odpytuje warstwę danych
- Oficjalnie rekomendowany pattern dla Jetpack Compose

---

## 3. Dependency Injection

- **Biblioteka:** Koin
- Runtime DI, konfiguracja w Kotlinie bez adnotacji

---

## 4. Nawigacja

- **Biblioteka:** Navigation Compose (Jetpack)
- Route-based, oficjalne wsparcie Google, bez dodatkowych zależności

---

## 5. Lokalne przechowywanie danych

- **Dane strukturalne** (Moja Lista, opisy atrakcji): Room (Jetpack ORM, SQLite)
- **Ustawienia** (klucze API, preferencje użytkownika): DataStore (Jetpack)

---

## 6. Sieć

- **Biblioteka:** Ktor Client
- Kotlin-native, asynchroniczny (Coroutines), używany do wszystkich zewnętrznych API (OpenTripMap, Tavily, OpenRouter, Wikipedia)

**Dostawca wyszukiwania atrakcji:** OpenTripMap
- REST API zbudowane na danych OSM, gotowa kategoryzacja atrakcji turystycznych
- Eliminuje konieczność ręcznego mapowania kategorii na tagi OSM

**Dostawca wyszukiwania internetowego:** Tavily (domyślny, free tier: 1000 zapytań/miesiąc)
- Interfejs `WebSearchService` zdefiniowany w module `domain`
- Implementacja Tavily w module `data` — zamiana na innego dostawcę (np. Brave Search) wymaga tylko nowej implementacji w `data`, bez zmian w `domain` ani `feature-*`

---

## 7. Asynchroniczność

- **Kotlin Coroutines** — operacje jednorazowe (zapytania sieciowe, zapis do bazy)
- **Flow** — strumienie danych (obserwowanie Mojej Listy, streaming odpowiedzi LLM)

---

## 8. Serializacja

- **Biblioteka:** Kotlinx Serialization
- Natywna integracja z Ktor, type-safe, weryfikacja w czasie kompilacji

---

## 9. Mapa

- **Biblioteka:** MapLibre Android SDK
- Open-source, natywna obsługa kafelków OSM, bez kluczy API

---

## 10. Struktura modułów

- **Typ:** Multi-module (możliwość wymiany implementacji bez zmiany interfejsów)

| Moduł | Odpowiedzialność |
|---|---|
| `app` | Punkt wejścia, nawigacja, konfiguracja DI |
| `core` | Wspólne modele, utility, stałe |
| `domain` | Interfejsy repozytoriów, modele biznesowe, use case'y |
| `data` | Implementacje repozytoriów, Room, DataStore, klienty HTTP |
| `feature-search` | Ekran Szukaj |
| `feature-map` | Ekran Mapa |
| `feature-mylist` | Ekran Moja Lista |
| `feature-assistant` | Ekran Asystenta |
| `feature-settings` | Ekran Ustawień |

Zależności: `feature-*` → `domain` → `core`. `data` implementuje interfejsy z `domain`. `app` łączy wszystko.

---

## 11. Integracja z LLM

- **Bramka:** OpenRouter (standardowe API zgodne z OpenAI)
- **Implementacja:** Ktor Client (bez zewnętrznego SDK)
- **Tool calling:** własna implementacja — pole `tools` w zapytaniu, obsługa `tool_use` w odpowiedzi, odesłanie wyniku
- **Streaming:** Server-Sent Events przez Ktor (odpowiedzi asystenta słowo po słowie)
- Interfejs warstwy LLM zdefiniowany w `domain` — wymiana implementacji (np. dodanie SDK) nie wymaga zmian w modułach `feature-*`

---

## 12. Lokalizacja GPS

- **API:** Android Location API (bez Google Play Services)
- Zasada: minimalizacja zależności od Google Play Services wszędzie tam gdzie alternatywa jest wystarczająca

---

## 13. Przechowywanie kluczy API

- **Mechanizm:** EncryptedSharedPreferences (`androidx.security:security-crypto`)
- Klucze szyfrowania w Android Keystore System (bez Google Play Services)
- Szyfrowanie AES-256, aktywne od pierwszej wersji (bez migracji po MVP)

---

## 14. Konfiguracja Koin (multi-module)

- Każdy moduł definiuje własny `KoinModule` z listą zależności
- Moduł `app` ładuje wszystkie moduły przy starcie aplikacji
- Interfejsy z `domain` bindowane z implementacjami z `data` w module `app`

---

## 15. Testowanie

- **Unit testy:** ViewModele, use case'y, logika domenowa — JUnit + MockK
- **Integracyjne:** repozytoria z bazą Room in-memory
- **UI testy:** poza zakresem MVP
- Wszystkie testy uruchamiane lokalnie

---

## 16. Logowanie

- **Biblioteka:** Timber
- Automatycznie wyłączone na release buildzie, tagi generowane z nazwy klasy

---

## 17. Build system

- **Gradle** z Kotlin DSL (`build.gradle.kts`)
- **Version Catalog** (`libs.versions.toml`) — centralny plik wersji wszystkich zależności, współdzielony przez wszystkie moduły

---

## 18. Design System

- **Material 3** (Material You)
- Gotowe komponenty Compose, obsługa ciemnego motywu out-of-the-box
- Theming przez `MaterialTheme`

---

## 19. Wzorce warstwy domenowej

- **Use case'y** jako osobne klasy w module `domain` (np. `GetAttractionsUseCase`, `GenerateDescriptionUseCase`)
- ViewModel wstrzykuje konkretne use case'y, nie repozytoria bezpośrednio
- Logika biznesowa nie wycieka do ViewModeli

---

## 20. Obsługa błędów

- **`Result<T>`** (wbudowany typ Kotlina) jako zwracany typ use case'ów
- ViewModel mapuje `Result` na stan UI
- Bez zewnętrznych bibliotek (Arrow itp.)

---

## 21. Stan UI

- **`StateFlow<UiState>`** w ViewModelu
- `UiState` jako sealed class lub data class trzymający cały stan ekranu (dane, loading, błąd)
- Compose obserwuje przez `collectAsStateWithLifecycle()`

---

## 22. Cachowanie

- Opisy atrakcji cachowane w Room po pierwszym wygenerowaniu — ponowne otwarcie Szczegółu nie odpytuje LLM
- Wyniki wyszukiwania (lista atrakcji) nie są cachowane — zawsze świeże dane z OpenTripMap

---

## 23. Uprawnienia Androida

- Uprawnienia wymagane: `ACCESS_FINE_LOCATION`, `INTERNET`
- Strategia: pytanie o uprawnienia przy pierwszym uruchomieniu aplikacji (nie lazy — nie czekamy aż użytkownik dotrze do funkcji GPS)

---

## 24. Migracje bazy danych (Room)

- Na MVP: brak migracji — destructive migration (baza usuwana i tworzona od nowa przy zmianie schematu)
- Przed wejściem na produkcję: implementacja właściwych migracji

---

## 25. Konwencje nazewnictwa

Zgodne z Clean Code (Uncle Bob), dostosowane do ekosystemu Kotlin/Android:

| Element | Konwencja | Przykład |
|---|---|---|
| Klasy | PascalCase, rzeczowniki | `AttractionRepository` |
| Funkcje | camelCase, czasowniki | `getAttractions()` |
| Zmienne | camelCase, opisowe | `attractionList` |
| Stałe | SCREAMING_SNAKE_CASE | `MAX_RADIUS_KM` |
| Pakiety | lowercase | `com.wanderlist.domain.usecase` |
| Boolean | prefiks `is`/`has`/`can` | `isLoading`, `hasDescription` |
| Interfejsy | bez prefiksu `I` | `AttractionRepository` |
| Implementacje | sufiks technologii | `RoomAttractionRepository` |
| ViewModel | sufiks `ViewModel` | `SearchViewModel` |
| Use case | `[Czasownik][Rzeczownik]UseCase` | `GetAttractionsUseCase` |
| UiState | sufiks `UiState` | `SearchUiState` |

**Stałe aplikacji** (w module `core`):
- `MY_LIST_MAX_SIZE = 50`

Struktura pakietów wewnątrz modułów:
- `domain`: `model/`, `repository/`, `usecase/`
- `data`: `local/`, `remote/`, `repository/`
- `feature-*`: `ui/`, `viewmodel/`

---

## 26. Model danych

> Wstępny model — może ulec zmianie podczas implementacji.

### Baza danych Room

**Tabela `attractions`**

| Pole | Typ | Opis |
|---|---|---|
| `xid` | `String` (PK) | Unikalny ID z OpenTripMap |
| `name` | `String` | Nazwa atrakcji |
| `latitude` | `Double` | Szerokość geograficzna |
| `longitude` | `Double` | Długość geograficzna |
| `category` | `String` | Kategoria biznesowa (np. „Zamki i fortyfikacje") |
| `isInMyList` | `Boolean` | Czy atrakcja jest na Mojej Liście |
| `dateAddedToList` | `Long?` | Timestamp dodania do listy; null jeśli nie na liście |
| `description` | `String?` | Wygenerowany opis; null jeśli nie załadowany |
| `descriptionSources` | `String?` | JSON z listą źródeł `[{name, url}]`; null jeśli brak opisu |
| `isFromLastSearch` | `Boolean` | Czy atrakcja pochodzi z ostatniego wyszukiwania |

Rekord istnieje w bazie gdy spełniony jest co najmniej jeden warunek: `isInMyList = true` LUB `description != null` LUB `isFromLastSearch = true`.

Przy nowym wyszukiwaniu:
1. Wszystkie dotychczasowe `isFromLastSearch = true` → ustaw na `false`
2. Usuń rekordy gdzie `isInMyList = false AND description = null AND isFromLastSearch = false`
3. Wstaw nowe wyniki z `isFromLastSearch = true`

---

### DataStore (ustawienia)

| Klucz | Typ | Opis |
|---|---|---|
| `openrouter_api_key` | `String` | Klucz OpenRouter (szyfrowany) |
| `tavily_api_key` | `String` | Klucz Tavily (szyfrowany) |
| `ai_model` | `String` | Wybrany model AI |
| `default_radius_km` | `Int` | Domyślny promień wyszukiwania |
| `description_language` | `String` | Język generowanych opisów (np. `"pl"`) |
| `user_interests` | `Set<String>` | Wybrane kategorie zainteresowań |
| `system_prompt_description` | `String` | System prompt agenta opisów |
| `system_prompt_assistant` | `String` | System prompt asystenta |
| `tavily_usage_count` | `Int` | Liczba zapytań Tavily w bieżącym miesiącu |
| `tavily_usage_month` | `String` | Miesiąc ostatniego licznika (`"YYYY-MM"`) — reset przy zmianie |
| `last_search_latitude` | `Double?` | Szerokość punktu X ostatniego wyszukiwania |
| `last_search_longitude` | `Double?` | Długość punktu X ostatniego wyszukiwania |
| `last_search_radius_km` | `Int?` | Promień ostatniego wyszukiwania |

---

## 27. Mapowanie kategorii → OpenTripMap `kinds`

> Mapowanie wstępne — do weryfikacji z aktualną dokumentacją OpenTripMap podczas implementacji. Wartości `kinds` są przekazywane jako parametr zapytania do API (przecinkami).

| Kategoria biznesowa | OpenTripMap `kinds` |
|---|---|
| Zamki i fortyfikacje | `castles,fortifications,palaces` |
| Kościoły i obiekty sakralne | `churches,cathedrals,monasteries,mosques,synagogues,temples,other_temples` |
| Muzea i galerie | `museums,art_galleries` |
| Ruiny i stanowiska archeologiczne | `ruins,archaeological_site,other_archaeological_site` |
| Przyroda i parki narodowe | `national_parks,nature_reserves,biosphere_reserves` |
| Punkty widokowe | `view_points` |
| Obiekty militarne | `battlefields,fortifications` |
| Młyny, wiatraki, zabytki techniki | `windmills,watermills,industrial_facilities` |
| Miejsca pamięci i cmentarze | `burial_ground,memorials,monuments` |
| Jaskinie i formacje geologiczne | `caves_and_tunnels,geological_formations,rocks` |

Gdy użytkownik wybierze wiele kategorii, ich `kinds` są łączone w jedno zapytanie (suma). Gdy żadna kategoria nie jest wybrana, zapytanie obejmuje wszystkie powyższe `kinds`.

---

## 28. Definicje narzędzi asystenta (tool calling)

> Schematy narzędzi przekazywanych do LLM przez OpenRouter.

### `search_attractions`
Wyszukuje atrakcje turystyczne w pobliżu wskazanego punktu. Źródło danych: wyłącznie OpenTripMap (ustrukturyzowane dane — lokalizacja, kategoria, nazwa). Nie odpytuje Tavily.

```json
{
  "name": "search_attractions",
  "parameters": {
    "latitude":   { "type": "number" },
    "longitude":  { "type": "number" },
    "radius_km":  { "type": "integer" },
    "categories": { "type": "array", "items": { "type": "string" }, "nullable": true }
  },
  "required": ["latitude", "longitude", "radius_km"]
}
```

### `web_search`
Wyszukuje informacje w internecie. Źródło danych: Tavily. Używane przez asystenta do ogólnych zapytań oraz przez agenta opisów do wzbogacania kontekstu.

```json
{
  "name": "web_search",
  "parameters": {
    "query": { "type": "string" }
  },
  "required": ["query"]
}
```

### `get_my_list`
Zwraca pełną Moją Listę użytkownika (tylko odczyt). Brak parametrów.

```json
{
  "name": "get_my_list",
  "parameters": {}
}
```

---

## 29. Domyślne system prompty

> Wersje robocze — do iteracji po pierwszych testach. Zmienne w nawiasach klamrowych `{...}` są wstrzykiwane przez aplikację w czasie wykonania.

### Agent opisów

```
Jesteś doświadczonym przewodnikiem turystycznym. Twoim zadaniem jest przygotowanie krótkiego, angażującego opisu miejsca na podstawie dostarczonych informacji.

Pisz w języku: {language}

Styl: rzeczowy, ale z charakterem — jakbyś polecał to miejsce znajomemu, który pyta o radę. Unikaj sztywnego, encyklopedycznego tonu i nadmiaru przymiotników.

Użytkownik interesuje się: {interests}

Na podstawie poniższych źródeł przygotuj opis (3–5 zdań):
- Połącz informacje z różnych źródeł naturalnie, jeśli się uzupełniają.
- Zignoruj źródło, jeśli nie wnosi nic nowego.
- Nie wymieniaj nazw źródeł w tekście opisu.
```

### Asystent

```
Jesteś asystentem turystycznym w aplikacji WanderList. Pomagasz użytkownikowi planować wycieczki, szukać atrakcji i odkrywać ciekawe miejsca.

Masz dostęp do narzędzi:
- search_attractions — szukaj atrakcji w okolicy wskazanego punktu
- web_search — szukaj informacji w internecie
- get_my_list — sprawdź miejsca zapisane przez użytkownika

Styl: rozmawiaj jak doradca w biurze podróży — po ludzku, konkretnie, bez sztucznej uprzejmości. Bądź bezpośredni, zadawaj pytania, proponuj alternatywy. Nie zaczynaj odpowiedzi od „Oczywiście!" ani podobnych wtrąceń.

Jeśli lokalizacja potrzebna do wyszukiwania nie wynika jasno z kontekstu, dopytaj użytkownika zamiast zgadywać. Korzystaj z narzędzi aktywnie — nie odpowiadaj z pamięci tam, gdzie możesz sprawdzić na żywo.
```

---

*Dokument: WanderList Tech Spec v0.1 | 2026-06-27*
