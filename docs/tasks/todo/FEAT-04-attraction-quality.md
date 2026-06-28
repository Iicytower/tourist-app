# FEAT-04: Poprawa jakości atrakcji turystycznych

## Problem

Obecne wyniki wyszukiwania przez OpenTripMap zawierają dużo "śmieci" — mało interesujących lub nieistotnych obiektów. Użytkownik widzi setki słabych wyników zamiast garści naprawdę wartych uwagi miejsc.

## Cel

Sprawić, żeby aplikacja pokazywała tylko atrakcje wysokiej jakości — znane, warte odwiedzenia, z sensownymi metadanymi.

## Etap 1 — Analiza i projekt rozwiązania

Przed implementacją należy zbadać dostępne opcje i wybrać najlepsze podejście. Pytania do rozstrzygnięcia:

- Jakie parametry filtrowania oferuje OpenTripMap API? (`rate`, `kinds`, limity, sortowanie)
- Czy lepiej filtrować po stronie API (mniej danych) czy po stronie klienta (większa kontrola)?
- Czy warto zastąpić lub uzupełnić OpenTripMap innym źródłem? (np. Overpass API / OSM, Wikipedia nearby, Google Places-like alternatywy open source)
- Jak wygląda rozkład ocen (`rate`) w danych OTM — od jakiego progu wyniki są sensowne?
- Czy LLM może służyć jako filtr/ranker (reranking wyników przed pokazaniem użytkownikowi)?

Wynikiem tego etapu jest decyzja: które mechanizmy filtrowania/rankowania wdrożyć, w jakiej kolejności.

## Etap 2 — Implementacja

Do ustalenia po Etapie 1. Potencjalne kierunki:

- **Filtr po `rate`** — OTM zwraca pole `rate` (0–3); odrzucać obiekty poniżej progu (np. `rate >= 2`)
- **Ograniczenie `kinds`** — wyselekcjonować tylko wartościowe kategorie, wykluczyć śmieciowe (np. `parking`, `atm`, `bench`)
- **Limit i sortowanie** — OTM wspiera `limit` i `order_by=rate`; pobierać mniej, ale lepiej posortowanych
- **Drugi filtr po stronie klienta** — odrzucać obiekty bez nazwy, bez współrzędnych, duplikaty
- **Alternatywne źródło** — jeśli OTM okaże się niewystarczający, dodać drugi provider za interfejsem `AttractionRepository` / nowy `AttractionSource`

## Pliki do zbadania w Etapie 1

- `data/src/.../remote/opentripmap/` — klient OTM, parametry zapytań
- `domain/src/.../model/SearchParams.kt` — co już przekazujemy
- `docs/technical_specs.md` — architektura źródeł danych
- `core/src/.../constant/` — stałe (limity, kategorie)

## Weryfikacja końcowa

- Wyniki wyszukiwania w tym samym miejscu są wyraźnie bardziej interesujące niż przed zmianą
- Brak obiektów bez nazwy lub z trywialną nazwą (ławka, parking, bankomat)
- Czas odpowiedzi nie pogarsza się istotnie

------------------------------------------------------------------------------
notatka, pomysły do zbadania:
# Filtrowanie i Źródła Danych POI w Aplikacji Turystycznej

Dokument zawiera strategię odsiewania "szumu" (nieatrakcyjnych punktów POI) oraz alternatywne, darmowe źródła danych do wykorzystania w niekomercyjnym projekcie aplikacji turystycznej.

---

## 1. Filtrowanie i Optymalizacja Obecnych Źródeł (OSM & Wiki)

Aby pozbyć się mało atrakcyjnych obiektów (np. stacji kolejowych, wiat przystankowych) i zapobiec timeoutom API, należy wdrożyć rygorystyczne kryteria selekcji.

### OpenStreetMap (Overpass API)
* **Zawężenie tagów:** Zamiast pobierać ogólne `tourism=*`, należy jawnie zdefiniować białą listę wartościowych tagów:
  * `tourism=attraction` (atrakcje)
  * `tourism=museum` (muzea)
  * `tourism=viewpoint` (punkty widokowe)
  * `historic=castle` / `historic=monument` / `historic=ruins` (zamki, pomniki, ruiny)
* **Czarna lista (Ignorowane):** `tourism=hotel`, `tourism=guest_house`, `tourism=picnic_site`, `tourism=camp_site`.
* **Optymalizacja zapytań:** 
  * Ograniczenie promienia wyszukiwania (maksymalnie 5–10 km).
  * Wprowadzenie jawnego parametru `[timeout:10]` w skrypcie Overpass.
  * *Alternatywa:* Pobranie gotowych paczek danych dla regionu/kraju (np. z serwisu Geofabrik) i przetworzenie ich lokalnie do bazy SQLite (SpatiaLite).

### Wikipedia i Wikidata
* **Filtrowanie przez SPARQL (Wikidata Query Service):** Zamiast ogólnego Geosearch z Wikipedii, lepiej odpytywać Wikidatę o obiekty w danym promieniu, filtrując po właściwości `P31` (jest to / instance of), wybierając tylko konkretne klasy (np. *zabytek, muzeum, park narodowy*).
* **Wskaźnik atrakcyjności (Sitelinks):** Dobrą heurystyką "ważności" obiektu jest liczba powiązanych artykułów w różnych językach. Jeśli obiekt posiada artykuł tylko w języku polskim, jego priorytet jest niski. Jeśli ma artykuły w 10+ językach, jest to kluczowa atrakcja.

---

## 2. Alternatywne, Darmowe Źródła Danych (Non-Profit / Self-Hosted)

Gdy publiczne API są przeciążone lub komercyjne rozwiązania (Google Places, TripAdvisor) odpadają ze względu na koszty, warto wykorzystać otwarte zbiory danych do lokalnego przetworzenia.

### Publiczne Dane Rządowe i Regionalne (Open Data)
* **Dane.gov.pl:** Krajowy portal otwartych danych w Polsce. Zawiera oficjalne, zweryfikowane rejestry publiczne, w tym:
  * Krajowy rejestr zabytków.
  * Oficjalne wykazy muzeów i instytucji kultury.
  * Bazy danych punktów informacji turystycznej.
* **Regionalne portale GIS (Wojewódzkie bazy danych):** Urzędy marszałkowskie często udostępniają darmowe, wysokiej jakości warstwy geodezyjne (formaty SHP, KML, GeoJSON) zawierające szlaki turystyczne, ścieżki rowerowe, pomniki przyrody oraz lokalne atrakcje.

### Overture Maps Foundation
* **Charakterystyka:** Nowoczesna, darmowa alternatywa dla komercyjnych baz POI, rozwijana m.in. przez Amazon, Microsoft i Meta.
* **Zastosowanie:** Agreguje dane m.in. z OSM, ale ich warstwa **POI (Places)** jest znacznie lepiej ustrukturyzowana, zweryfikowana i łatwiejsza do przefiltrowania pod kątem turystyki niż surowy dump OSM. Dane są udostępniane jako pliki Parquet, idealne do pobrania i zaimportowania do własnej bazy danych.