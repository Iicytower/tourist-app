# FEAT-11: Poprawa promptu filtra LLM — odrzucanie pospolitych obiektów

## Problem

`LlmAttractionQualityFilter` za rzadko odrzuca obiekty turystycznie nieinteresujące. System prompt zachowuje "miejsca kultu religijnego" jako kategorię bez rozróżnienia — w efekcie do wyników trafiają zwykłe osiedlowe kościoły, kaplice bez historii, małe figury przydrożne. Podobnie: pomniki bez kontekstu (np. tablica pamiątkowa na bloku), nieoznaczone punkty widokowe, targowiska codzienne zamiast historycznych bazarów.

## Cel

Zaostrzyć kryteria filtra tak, żeby LLM odrzucał obiekty "standardowe" — istniejące w każdym mieście i nieinteresujące z perspektywy turysty przyjezdnego — a zachowywał tylko te z realną wartością turystyczną.

## Zakres zmian

Wyłącznie `data/.../remote/llmfilter/LlmAttractionQualityFilter.kt` — zmiana `SYSTEM_PROMPT`.

### Nowe zasady w prompcie

**Kościoły i obiekty sakralne** — zachowaj TYLKO gdy spełnia co najmniej jedno z:
- jest katedrą, bazyliką, sanktuarium lub kościołem o znaczeniu krajowym/regionalnym
- ma wiek > 150 lat i jest wpisany do rejestru zabytków
- jest wyjątkowy architektonicznie (styl gotycki, barokowy, romański itp.)
- jest celem pielgrzymek lub figuruje w przewodnikach turystycznych
- Odrzucaj: typowe parafialne kościoły bez historii, kaplice osiedlowe, figury przydrożne, krzyże

**Pomniki i tablice** — zachowaj TYLKO gdy:
- jest pomnikiem ogólnopolskim lub regionalnym (np. Pomnik Powstania Warszawskiego)
- upamiętnia wydarzenie historyczne o znaczeniu wykraczającym poza lokalną dzielnicę
- Odrzucaj: tablice pamiątkowe na budynkach, małe lokalne obeliski, krzyże katyńskie przy osiedlach

**Parki i tereny zielone** — zachowaj TYLKO gdy:
- ma status parku krajobrazowego, narodowego lub zabytkowego ogrodu
- jest wyjątkowy (ogród japoński, różany, dendrologiczny)
- Odrzucaj: zwykłe parki miejskie, skwery, zieleńce osiedlowe

**Rynki i place** — zachowaj TYLKO:
- historyczne rynki starówkowe z zabudową zabytkową
- Odrzucaj: place bez zabudowy historycznej, współczesne place miejskie

**Ogólna zasada priorytetu**: jeśli dany obiekt można znaleźć w każdym średnim mieście (kościół parafialny, park osiedlowy, tablica na bloku), odrzuć go. Zachowuj to, po co turysta celowo przyjeżdża.

### Weryfikacja przez web_search

Gdy obiekt jest niejednoznaczny (np. nazwa kościoła bez kategorii "zabytek"), wywołaj `web_search` żeby sprawdzić czy figuruje w źródłach turystycznych lub rejestrze zabytków.

## Plik do zmiany

- `data/src/main/kotlin/com/iicytower/wanderlist/data/remote/llmfilter/LlmAttractionQualityFilter.kt`
  - tylko stała `SYSTEM_PROMPT`

## Weryfikacja

- Wyszukaj atrakcje w centrum dużego miasta (np. Warszawa, Kraków)
- Kliknij "Przefiltruj"
- Sprawdź logi (`adb logcat -s LlmAttractionQualityFilter`) — zwykłe kościoły parafialne i pomniki lokalne powinny trafić na listę REMOVED
- Muzea, katedry, zamki, zabytkowe kościoły powinny pozostać
