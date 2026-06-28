# BUG-03: Opis atrakcji nie ładuje się (spinner bez końca lub puste pole)

## Problem

Po kliknięciu "Załaduj opis" spinner kręci się ale opis nigdy się nie pojawia. Pole pozostaje puste.

`loadDescription()` wywołuje `generateDescriptionUseCase(xid)`. Ten use case prawdopodobnie:
1. Pobiera dane z Wikipedia/Wikidata
2. Generuje opis przez LLM (streaming — ten sam problem co z asystentem)

## Diagnostyka przed implementacją

Zbadaj `GenerateDescriptionUseCase` i jego zależności:

```
domain/src/.../usecase/GenerateDescriptionUseCase.kt
data/src/.../remote/wikipedia/WikipediaServiceImpl.kt
data/src/.../remote/wikidata/WikidataSparqlSource.kt
```

Sprawdź czy:
1. LLM streaming — ten sam OkHttp fix co BUG-04 (asystent). Jeśli tak, naprawienie asystenta powinno też naprawić opisy.
2. Wikipedia/Wikidata API zwraca dane — dodaj `Timber.d()` przy odpowiedziach
3. `descriptionError` jest ustawiany ale nie wyświetlany — sprawdź przepływ w VM

## Możliwe przyczyny

### A) LLM streaming (najbardziej prawdopodobne)
`GenerateDescriptionUseCase` wywołuje `LlmService.streamResponse()`. Po naprawie OkHttp engine (fix z poprzedniej sesji) powinna zacząć działać. Zweryfikuj na urządzeniu po wgraniu APK z BUG-04.

### B) Wikipedia API zwraca 404/błąd dla danego punktu
Dodaj logging w `WikipediaServiceImpl` — sprawdź czy dane w ogóle przychodzą.

### C) `descriptionError` nie dociera do UI
W `AttractionDetailViewModel.loadDescription()` błąd trafia do `state.descriptionError`. Sprawdź czy UI go wyświetla (powinno — jest kod na `state.descriptionError != null`).

## Plan naprawy

1. **Najpierw wgraj APK z poprawką OkHttp** i sprawdź czy opisy zaczęły działać
2. Jeśli nie — dodaj `Timber.d/e` do `GenerateDescriptionUseCase` i `WikipediaServiceImpl`, wgraj i sprawdź logcat
3. Na podstawie logów zdecyduj co naprawić

## Dodatkowe zabezpieczenia do wdrożenia zawsze

W `AttractionDetailViewModel.loadDescription()` upewnij się że błąd LLM jest propagowany do `descriptionError`:

```kotlin
generateDescriptionUseCase(xid).fold(
    onSuccess = { ... },
    onFailure = { e ->
        Timber.e(e, "generateDescription failed for xid=$xid")
        _uiState.update { it.copy(isDescriptionLoading = false, descriptionError = e.message ?: "Nieznany błąd") }
    }
)
```

## Pliki do zbadania i potencjalnej zmiany
- `domain/src/.../usecase/GenerateDescriptionUseCase.kt`
- `data/src/.../remote/wikipedia/WikipediaServiceImpl.kt`
- `data/src/.../remote/wikidata/WikidataSparqlSource.kt`
- `feature-detail/src/.../viewmodel/AttractionDetailViewModel.kt`

## Weryfikacja
- Kliknięcie "Załaduj opis" → po kilku sekundach pojawia się tekst opisu
- Jeśli błąd → wyświetla się komunikat + przycisk "Spróbuj ponownie"
- `adb logcat -s GenerateDesc Wikipedia Wikidata` nie pokazuje nieobsłużonych wyjątków
