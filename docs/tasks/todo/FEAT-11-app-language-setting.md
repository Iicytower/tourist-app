# FEAT-11: Ustawienie języka aplikacji (wyszukiwanie, podpowiedzi lokalizacji, asystent)

## Problem

Obecnie jedyne ustawienie językowe to `descriptionLanguage` w `AppSettings`, które steruje **wyłącznie** językiem generowanego przez LLM opisu atrakcji (`GenerateDescriptionUseCase` dokleja `"Język odpowiedzi: ${settings.descriptionLanguage}."` do system promptu). Reszta aplikacji ignoruje to ustawienie:
- Wyszukiwanie lokalizacji (`GeocoderService.suggest()`/`geocode()` — Photon/Nominatim w `data/.../geocoding`) nie przekazuje żadnego parametru języka, mimo że oba API je wspierają (`lang=` w Photon, `accept-language` w Nominatim).
- Asystent czatu (`assistant.txt`) ma statyczną instrukcję *"Pisz po polsku, chyba że użytkownik poprosi o inny język"* — nie reaguje na żadne ustawienie.

## Cel

Jedno ustawienie "Język aplikacji" w Ustawieniach, które steruje spójnie:
1. językiem wyników wyszukiwania (opis atrakcji — już częściowo działa przez `descriptionLanguage`, tu: ujednolicić nazewnictwo/pole),
2. językiem podpowiedzi przy wpisywaniu lokalizacji (geokodowanie/autocomplete),
3. językiem, w jakim odpowiada asystent w rozmowie.

Poza zakresem tego taska: pełne tłumaczenie UI aplikacji (etykiety, przyciski — to osobny temat i18n z `values-en`/`values-de` itd., obecnie nieistniejący w projekcie; nie ruszamy tego tutaj).

## Zakres zmian

### Ustawienia — ujednolicenie pola

- Zdecydować przy implementacji: albo **przemianować** `descriptionLanguage` na `appLanguage` (i zaktualizować wszystkie miejsca użycia + klucz DataStore + ewentualną migrację), albo zostawić `descriptionLanguage` jako alias/synonim tego samego ustawienia z dodanym opisem w UI, że dotyczy całej aplikacji. Rekomendacja: przemianować, żeby nazwa nie myliła (dotyczy dziś tylko opisów, ma zacząć dotyczyć więcej).
- `domain/.../model/AppSettings.kt` — pole `appLanguage: String` (kody: `pl`, `en`, `de`, `fr`, `es` — zgodnie z listą już istniejącą w `SettingsScreen.kt`).
- `domain/.../repository/SettingsRepository.kt` — `updateDescriptionLanguage()` → `updateAppLanguage()` (lub zachować nazwę, do ustalenia).
- `data/.../repository/DataStoreSettingsRepository.kt` — klucz DataStore, `DefaultSettings.kt` — wartość domyślna.
- `feature-settings/.../SettingsScreen.kt` — istniejący dropdown "Jezyk opisow" zmienić etykietę na "Język aplikacji" (lub dodać opis pod spodem wyjaśniający zakres działania).

### Wyszukiwanie lokalizacji (podpowiedzi)

- `domain/.../repository/GeocoderService.kt` — rozszerzyć `suggest(query: String)`/`geocode(query: String)` o parametr `language: String`.
- `data/.../geocoding/NominatimGeocoderService.kt` — przekazać `lang=` do zapytań Photon (`suggest()`) i `accept-language=` do zapytań Nominatim (`geocode()`/`reverseGeocode()`).
- Wywołujący use case/ViewModel (ekran dodawania/edycji lokalizacji z FEAT-07) — pobrać `appLanguage` z `SettingsRepository` i przekazać do wywołań geokodera.

### Asystent

- `app/src/main/assets/agents/assistant.txt` — zamienić statyczne *"Pisz po polsku, chyba że użytkownik poprosi o inny język"* na zmienną wstawianą dynamicznie (podobnie jak w `GenerateDescriptionUseCase`), np. placeholder `{{language}}` zastępowany w kodzie, albo dopisywanie linii `"Język odpowiedzi: ${settings.appLanguage}."` do system promptu analogicznie do generowania opisów.
- `AssistantViewModel.kt` — przy budowaniu system promptu doklejać język z ustawień (tak jak `GenerateDescriptionUseCase` to robi dziś).

### Wyszukiwanie atrakcji / wyniki

- Sprawdzić czy `GenerateDescriptionUseCase` po zmianie nazwy pola nadal działa bez zmian logiki (tylko zmiana nazwy pola w `AppSettings`/`SettingsRepository`).
- Jeśli w międzyczasie powstał task FEAT-10 (lokalne wyszukiwanie w języku kraju atrakcji) — to ustawienie `appLanguage` jest **niezależne**: FEAT-10 dotyczy dodatkowego zapytania w lokalnym języku kraju atrakcji (dla lepszych źródeł), a `appLanguage` z tego taska to język w jakim user chce widzieć/otrzymywać odpowiedzi. Oba mechanizmy mogą współistnieć — LLM i tak tłumaczy finalną odpowiedź na `appLanguage`, niezależnie od języka źródeł.

## Pliki do zmiany

- `domain/src/.../model/AppSettings.kt`
- `domain/src/.../repository/SettingsRepository.kt`
- `domain/src/.../repository/GeocoderService.kt`
- `data/src/.../repository/DataStoreSettingsRepository.kt`
- `data/src/.../local/DefaultSettings.kt` (lub odpowiednik)
- `data/src/.../geocoding/NominatimGeocoderService.kt`
- `domain/src/.../usecase/GenerateDescriptionUseCase.kt` (aktualizacja nazwy pola)
- `feature-settings/src/.../ui/SettingsScreen.kt`
- `feature-settings/src/.../viewmodel/SettingsViewModel.kt`
- `feature-assistant/src/.../viewmodel/AssistantViewModel.kt`
- `app/src/main/assets/agents/assistant.txt`

## Weryfikacja

- Zmiana "Języka aplikacji" w Ustawieniach na `en` → nowo wygenerowany opis atrakcji jest po angielsku (jak dotychczas z `descriptionLanguage`)
- Podpowiedzi przy wpisywaniu lokalizacji (autocomplete) po zmianie na `en`/`de` zwracają nazwy w tym języku (np. "Munich" zamiast "München" przy `en`)
- Rozmowa z asystentem po zmianie ustawienia na `en` odbywa się po angielsku bez konieczności proszenia go o to w wiadomości
- Zmiana ustawienia z powrotem na `pl` przywraca dotychczasowe (polskie) zachowanie we wszystkich trzech miejscach
