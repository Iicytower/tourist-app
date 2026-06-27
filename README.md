# WanderList

Osobista aplikacja turystyczna na Android. Wyszukiwanie atrakcji w okolicy, generowanie opisów przez LLM (wzbogacanie z OpenTripMap + Wikipedia + web search), asystent AI z tool calling i mapa z pinezkami.

Bez konta, bez własnego backendu — dane lokalne na urządzeniu.

## Funkcje

- **Szukaj** — wyszukiwanie atrakcji wg lokalizacji GPS, nazwy miejsca lub punktu na mapie; filtrowanie wg kategorii i promienia (1–50 km)
- **Mapa** — pinezki wyników wyszukiwania i Mojej Listy na mapie MapLibre (kafelki OSM)
- **Moja Lista** — bucket list do 50 atrakcji, dostępna offline, sortowanie wg daty / odległości / nazwy / kategorii
- **Asystent** — czat z AI, streaming odpowiedzi, tool calling (wyszukiwanie atrakcji, web search, dostęp do Mojej Listy)
- **Opisy** — generowane na żądanie przez LLM na podstawie danych z OpenTripMap, Wikipedii i wyszukiwarki

## Stack

| Co | Czym |
|---|---|
| Język | Kotlin, Java 21 |
| UI | Jetpack Compose, Material 3 |
| Architektura | MVVM |
| DI | Koin |
| Baza lokalna | Room (SQLite) |
| Sieć | Ktor Client |
| Mapa | MapLibre Android SDK |
| LLM | OpenRouter (SSE streaming) |
| Wyszukiwanie atrakcji | OpenTripMap REST API |
| Web search | Tavily |
| GPS | Android Location API (bez Google Play Services) |

Min SDK: 26 · Target SDK: 37

## Wymagania

- Klucz API [OpenRouter](https://openrouter.ai) — do generowania opisów i asystenta AI
- Klucz API [Tavily](https://tavily.com) — do web search (opcjonalnie; limit 1000 zapytań/mies. na planie darmowym)

Klucze wprowadza się w ekranie Ustawień — nigdy nie są przechowywane w kodzie ani plikach konfiguracyjnych.

## Uruchamianie

```bash
# Build
./gradlew assembleDebug

# Instalacja na emulatorze / urządzeniu
./gradlew installDebug

# Testy
./gradlew test
```
