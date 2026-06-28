# BUG-04: Asystent nie wyświetla odpowiedzi

## Problem

Po wysłaniu wiadomości czat nie wyświetla żadnej odpowiedzi. `streamingText` pozostaje puste, `isProcessing` wraca do false bez dodania wiadomości asystenta.

## Diagnoza

`AssistantViewModel.runChatLoop()` kolekcjonuje eventy z `sendChatMessageUseCase` → `LlmService.streamResponse()`.

Wcześniej Ktor Android engine (HttpURLConnection) nie obsługiwał SSE. **Zamiana na OkHttp engine jest już wdrożona** (fix z poprzedniej sesji, w `HttpClientProvider.kt`).

## Co zrobić

### 1. Weryfikacja na urządzeniu

Wgraj nowy APK i wyślij wiadomość na czacie. Sprawdź logcat:

```bash
adb logcat -s OpenRouter Ktor
```

Oczekiwane: linie `data: {...}` pojawiają się w logach → streaming działa.

### 2. Jeśli brak odpowiedzi mimo OkHttp — zbadaj kolejność eventów

Problem może być w tym, że OpenRouter przesyła eventy w specyficznej kolejności. Sprawdź czy `LlmEvent.Done` jest odbierany bez żadnych `LlmEvent.TextChunk`.

Dodaj tymczasowy logging w `OpenRouterLlmService.streamResponse()`:

```kotlin
is LlmEvent.TextChunk -> {
    Timber.tag("OpenRouter").d("TextChunk: ${event.text}")
    accumulatedText.append(event.text)
    ...
}
is LlmEvent.Done -> {
    Timber.tag("OpenRouter").d("Done, accumulated=${accumulatedText.length} chars")
    ...
}
is LlmEvent.Error -> {
    Timber.tag("OpenRouter").e("Error: ${event.message}")
    ...
}
```

### 3. Jeśli streaming działa ale UI nie aktualizuje się

Sprawdź czy `_uiState.update` w `runChatLoop()` jest wywoływany na właściwym dispatcher. `viewModelScope.launch` domyślnie używa `Dispatchers.Main.immediate` — OK.

### 4. Możliwy problem: brak obsługi `finish_reason: tool_calls` bez tool call body

Niektóre modele (np. Gemini przez OpenRouter) mogą wysłać chunk z `finish_reason` ale bez `delta.content`. Upewnij się, że taki chunk jest poprawnie ignorowany (zwraca `null` z `toLlmEvent()` — już tak jest).

### 5. Jeśli problem leży w modelu AI

Sprawdź czy model ustawiony w Ustawieniach (`settings.aiModel`) jest poprawny i obsługuje streaming przez OpenRouter. Przetestuj z modelem `google/gemini-2.5-flash-lite`.

## Pliki do zmiany (jeśli potrzeba)
- `data/src/.../remote/openrouter/OpenRouterLlmService.kt` — logging
- `feature-assistant/src/.../viewmodel/AssistantViewModel.kt` — ewentualny debugging

## Weryfikacja
- Wysłanie wiadomości → odpowiedź pojawia się na czacie (streaming token po tokenie lub cała naraz)
- `adb logcat -s OpenRouter` pokazuje `TextChunk` eventy
- Narzędzia (tool calls) działają — asystent może wyszukać atrakcje
