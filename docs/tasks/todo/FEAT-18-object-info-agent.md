# FEAT-18: Dedykowany agent do wyszukiwania informacji o konkretnym obiekcie

## Problem

Obecnie jedyny mechanizm dociągania wiedzy o atrakcji to `GenerateDescriptionUseCase` — wykonuje jedno stałe zapytanie `web_search` (`"${attraction.name} atrakcja turystyczna"`) i generuje jeden akapit opisu. Nie ma sposobu, żeby użytkownik dopytał o konkretny aspekt obiektu (godziny otwarcia, bilety, historia, jak dojechać, ciekawostki) bez ręcznego wyszukiwania poza aplikacją. Ogólny asystent (`AssistantViewModel`) potrafi użyć `web_search`, ale nie ma kontekstu "jestem teraz na szczegółach tej konkretnej atrakcji" ani ukierunkowanego promptu do dogłębnego researchu jednego obiektu.

## Cel

Dedykowany agent LLM (osobny system prompt w `app/src/main/assets/agents/`, wzorem `description.txt`/`quality-filter.txt`), wywoływany z ekranu szczegółów atrakcji, który:
1. Otrzymuje kontekst wybranej atrakcji (nazwa, kategoria, lokalizacja, istniejący opis).
2. Pozwala użytkownikowi zadać dowolne pytanie o ten obiekt (np. "ile kosztuje bilet", "czy warto iść z dzieckiem", "jak długo zwiedzać").
3. Iteracyjnie korzysta z `WebSearchService` (i opcjonalnie `WikipediaService`) — wielokrotne zapytania w pętli tool-calling (wzorem `GenerateTripPlanUseCase`/`LlmAttractionQualityFilter`, z limitem `AppConstants.MAX_TOOL_CALL_ITERATIONS`), zamiast jednego sztywnego zapytania.
4. Zwraca odpowiedź w języku ustawionym w `AppSettings` (spójnie z FEAT-11, jeśli już zaimplementowane).

## Zakres zmian

- `app/src/main/assets/agents/object-info.txt` (nowy) — system prompt: rola dogłębnego researchera pojedynczego obiektu turystycznego, z instrukcją korzystania z narzędzi wyszukiwania i cytowania źródeł.
- `domain/.../usecase/AskAboutAttractionUseCase.kt` (nowy) — pętla tool-calling analogiczna do `GenerateTripPlanUseCase`, przyjmuje `Attraction` + pytanie użytkownika + historię konwersacji, zwraca `Result<String>` (odpowiedź) lub `ChatMessage`, jeśli ma być rozmowa wieloturowa.
- `feature-detail/.../AttractionDetailScreen.kt` — nowa sekcja/przycisk "Zapytaj o to miejsce" (pole tekstowe + historia pytań/odpowiedzi w obrębie ekranu szczegółów).
- `feature-detail/.../AttractionDetailViewModel.kt` — stan pytań/odpowiedzi, wywołanie use case'a.
- Rozważyć limit liczby pytań na sesję / koszt zapytań LLM+Tavily (analogicznie do ograniczeń w FEAT-14 dot. bezpieczeństwa/kosztów asystenta).

## Pliki do zmiany / stworzenia

- `app/src/main/assets/agents/object-info.txt` (nowy)
- `domain/src/main/kotlin/.../usecase/AskAboutAttractionUseCase.kt` (nowy)
- `domain/src/test/kotlin/.../AskAboutAttractionUseCaseTest.kt` (nowy)
- `feature-detail/src/main/kotlin/.../ui/AttractionDetailScreen.kt`
- `feature-detail/src/main/kotlin/.../viewmodel/AttractionDetailViewModel.kt`

## Weryfikacja

- Na ekranie szczegółów atrakcji użytkownik zadaje pytanie ("jakie są godziny otwarcia?") → agent wykonuje wyszukiwanie i zwraca konkretną odpowiedź, nie ogólnikowy opis
- Pytanie bez jednoznacznej odpowiedzi w źródłach → agent przyznaje niepewność zamiast zmyślać
- Limit iteracji tool-calling działa (agent nie zapętla się w nieskończoność przy niejednoznacznych wynikach wyszukiwania)
- Odpowiedź jest w języku ustawionym w Ustawieniach

<!-- TODO: konsultacja — do ustalenia przy implementacji: czy to ma być osobny ekran/panel czy rozszerzenie istniejącego ogólnego asystenta o kontekst "aktualnie oglądana atrakcja" (mniej kodu, ale gorsze UX niż dedykowany, zawężony agent). Zdecydowałem się opisać jako dedykowany agent zgodnie z dosłownym brzmieniem issue #9. -->
