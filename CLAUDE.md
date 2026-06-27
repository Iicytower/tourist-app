# WanderList — CLAUDE.md

Osobista aplikacja turystyczna Android. Wyszukiwanie atrakcji, generowanie opisów przez LLM, asystent AI z tool calling.

## Dokumentacja

- Spec funkcjonalna: `docs/functionalities_specs.md`
- Spec techniczna: `docs/technical_specs.md`
- Taski implementacji: `docs/tasks/todo/` (TASK-01 … TASK-17, wykonuj w kolejności)

## Stack techniczny (skrót)

| Co | Czym |
|---|---|
| Język | Kotlin, Java 21 |
| UI | Jetpack Compose, Material 3 |
| Architektura | MVVM |
| DI | Koin (runtime, bez adnotacji) |
| Nawigacja | Navigation Compose |
| Baza lokalna | Room (SQLite) |
| Ustawienia | DataStore Preferences |
| Klucze API | EncryptedSharedPreferences (Android Keystore) |
| Sieć | Ktor Client |
| Serializacja | Kotlinx Serialization |
| Mapa | MapLibre Android SDK (kafelki OSM) |
| LLM | OpenRouter (OpenAI-compatible, SSE streaming) |
| Wyszukiwanie atrakcji | OpenTripMap REST API |
| Web search | Tavily (interfejs `WebSearchService` — wymienny) |
| GPS | Android Location API (bez Google Play Services) |
| Logowanie | Timber |
| Build | Gradle + Kotlin DSL + Version Catalog |
| Testy | JUnit + MockK + Room in-memory |

Min SDK: 26 · Target SDK: 37 · Application ID: `com.iicytower.wanderlist`

## Struktura modułów

```
app          ← punkt wejścia, DI, nawigacja
core         ← stałe, utility, enum kategorii, mapowanie kinds
domain       ← modele, interfejsy repozytoriów, use case'y
data         ← Room, DataStore, klienty Ktor, implementacje repozytoriów
feature-search
feature-map
feature-mylist
feature-assistant
feature-settings
```

**Reguła zależności:** `feature-*` → `domain` → `core`. `data` implementuje interfejsy z `domain`. `feature-*` NIE importuje z `data`.

## Konwencje nazewnictwa

- Klasy: `PascalCase` · Funkcje/zmienne: `camelCase` · Stałe: `SCREAMING_SNAKE_CASE`
- Interfejsy: bez prefiksu `I` (np. `AttractionRepository`)
- Implementacje: sufiks technologii (np. `RoomAttractionRepository`)
- ViewModele: sufiks `ViewModel` · Use case'y: `[Verb][Noun]UseCase` · Stany UI: sufiks `UiState`
- Pakiety: `lowercase`

## Kluczowe ograniczenia

- **Bez Google Play Services** — GPS przez `android.location.*`, klucze przez Android Keystore
- **Tavily** za interfejsem `WebSearchService` w `domain` — zamiana providera = nowa klasa w `data`, zero zmian w `domain`/`feature-*`
- **Room na MVP: `fallbackToDestructiveMigration()`** — migracje przed produkcją
- **Limit Mojej Listy: 50** (`MY_LIST_MAX_SIZE` w `core`)
- **Klucze API nigdy w kodzie** — przez `EncryptedSharedPreferences`; klucz deweloperski w `.env` (nie commitować)

## Komendy

```bash
# Build
./gradlew assembleDebug

# Testy wszystkich modułów
./gradlew test

# Testy konkretnego modułu
./gradlew :domain:test

# Lint
./gradlew lint

# Instalacja na emulatorze/urządzeniu
./gradlew installDebug

# Testy instrumentalne (wymagają urządzenia)
./gradlew connectedAndroidTest

# Lista dostępnych AVD
emulator -list-avds

# Uruchomienie emulatora
emulator -avd <NAME> -no-audio -no-boot-anim
```

## Linki do API

- OpenTripMap docs: https://dev.opentripmap.org/docs
- OpenTripMap kinds (kategorie): https://dev.opentripmap.org/catalog
- OpenRouter API (OpenAI-compatible): https://openrouter.ai/docs
- OpenRouter modele: https://openrouter.ai/models
- Tavily API: https://docs.tavily.com
- MapLibre Android SDK: https://maplibre.org/maplibre-native/android/api/
- Wikipedia REST API: https://en.wikipedia.org/api/rest_v1/
