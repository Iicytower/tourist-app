# WanderList – Specyfikacja Funkcjonalna

> **Status:** Draft v0.4
> **Platforma:** Android
> **Dane:** lokalne na urządzeniu, bez backendu

---

## 1. Cel i zakres

Aplikacja mobilna do osobistego użytku, wspomagająca planowanie wycieczek i zwiedzanie atrakcji turystycznych. Nie wymaga konta ani połączenia z własnym serwerem. Użytkownik konfiguruje dostęp do zewnętrznych usług (OpenRouter, Tavily) we własnym zakresie.

---

## 2. Nawigacja główna

```
Tab Bar (dolny):
├── 🔍 Szukaj
├── 🗺️  Mapa
├── ❤️  Moja Lista
├── 💬 Asystent
└── ⚙️  Ustawienia
```

---

## 3. Ekrany

### 3.1 Szukaj

Użytkownik określa punkt wyszukiwania (punkt X) jedną z trzech metod:

- **Wpisanie nazwy miejsca** – pole tekstowe z wyszukiwaniem
- **Bieżąca lokalizacja** – przycisk uruchamiający GPS
- **Wybór na mapie** – mini-mapa do ręcznego wskazania punktu

Następnie ustawia promień (suwak 1–50 km) i uruchamia wyszukiwanie.

**Lista wyników** (źródło danych: OpenTripMap) zawiera dla każdej atrakcji:
- Nazwę
- Kategorię (np. Zamek, Muzeum, Rezerwat przyrody)
- Odległość od punktu X (obliczona w momencie wyszukiwania, nie aktualizowana dynamicznie)
- Ikonę kategorii
- Informację czy opis jest już załadowany

Lista domyślnie sortowana wg odległości. Opcja sortowania: wg odległości lub wg kategorii.

Wyniki są paginowane.

Opisy atrakcji **nie są generowane automatycznie** – tylko po kliknięciu przez użytkownika (patrz 3.2 – wzbogacanie wielo-źródłowe).

---

### 3.2 Szczegół Atrakcji

Pojawia się po kliknięciu elementu z listy lub pinezki na mapie.

Zawiera:
- Nazwę atrakcji
- Miniaturę mapy z zaznaczonym miejscem (tylko podgląd; mapa OpenStreetMap)
- Opis (ładowany na żądanie – przycisk „Załaduj opis"; po załadowaniu wyświetla tekst i listę źródeł z linkami, z których powstał)
- Odległość od punktu X (wyświetlana tylko gdy ekran otwarty z wyników wyszukiwania; niewidoczna gdy otwarty z Mojej Listy bez aktywnego wyszukiwania)
- Kategorię
- Przycisk **„Nawiguj do"** – otwiera Google Maps z celem ustawionym na atrakcję; jeśli Google Maps nie jest zainstalowane, otwiera przeglądarkę z odpowiednim URL
- Przycisk **„Dodaj do Mojej Listy"** / „Usuń z Mojej Listy" (akcja wykonywana wyłącznie ręcznie przez użytkownika – patrz 3.5)

#### Generowanie opisu – wzbogacanie wielo-źródłowe

Po kliknięciu „Załaduj opis" aplikacja przygotowuje dla LLM połączony kontekst z trzech niezależnych źródeł:

1. **OpenTripMap** – dane atrakcji pobrane przy wyszukiwaniu
2. **Web Search** – wynik wyszukiwania w internecie (Tavily)
3. **Wikipedia** – artykuł, jeśli istnieje dla danego miejsca

Źródła są od siebie niezależne i wzajemnie się backupują – jeśli jedno nie odpowiada lub nie zwraca wyniku (np. brak artykułu Wikipedia dla niewielkiej atrakcji, limit zapytań do Web Search, timeout), opis jest generowany na bazie pozostałych dostępnych źródeł, bez blokowania całego procesu. Sytuacja, gdy żadne z trzech źródeł nie odpowie, jest obsłużona w Sekcji 5.

Po wygenerowaniu, ekran wskazuje, z których źródeł opis faktycznie powstał, wraz z linkami (transparentność dla użytkownika).

> Uwaga: to dotyczy etapu wzbogacania/opisu konkretnej atrakcji, nie samego wyszukiwania listy w 3.1 – tam jedynym źródłem pozostaje OpenTripMap, bo to ono dostarcza dane wielu obiektów na raz.

---

### 3.3 Mapa

Pełnoekranowa mapa (OpenStreetMap).

Stan domyślny (brak aktywnego wyszukiwania): mapa wyśrodkowana na lokalizacji użytkownika, bez pinezek.

Po wyszukiwaniu mapa zawiera:
- Pinezki wszystkich atrakcji z bieżącego wyszukiwania
- Wyróżnioną pinezkę punktu X
- Toggle „Pokaż tylko Moją Listę" – gdy aktywny, pinezki z wyszukiwania są zastępowane pinezkami z Mojej Listy

Po kliknięciu pinezki pojawia się **dymek** z:
- Nazwą atrakcji
- Skrótem (max 5 słów, generowanym przez LLM)
- Przyciskiem „Więcej →" prowadzącym do Szczegółu

---

### 3.4 Moja Lista

Bucket list atrakcji do odwiedzenia.

Każdy element zawiera:
- Nazwę
- Geolokalizację
- Opis (jeśli był wcześniej załadowany)
- Datę dodania
- Kategorię
- Odległość od bieżącej lokalizacji użytkownika (widoczna tylko gdy GPS aktywny)

Akcje na elementach:
- Kliknięcie → Szczegół Atrakcji (działa offline)
- Długie przytrzymanie → Usuń | Otwórz na mapie

Akcje globalne:
- Sortowanie: data dodania | odległość | nazwa | kategoria

> **Decyzja:** Lista jest edytowalna wyłącznie ręcznie przez użytkownika (dodawanie i usuwanie). Asystent AI ma do niej tylko dostęp odczytu – patrz 3.5.

> Eksport do pliku GPX – funkcja zachowana w specyfikacji, ale przesunięta poza MVP (patrz Sekcja 7).

---

### 3.5 Asystent (czat z AI)

Okno konwersacji z asystentem AI, który ma dostęp do wszystkich narzędzi skonfigurowanych w aplikacji. Służy do swobodnego planowania wycieczek, zadawania pytań o atrakcje, szukania informacji w internecie.

**Narzędzia dostępne dla asystenta:**
- Wyszukiwanie atrakcji w pobliżu wskazanego punktu – użytkownik określa punkt tymi samymi trzema metodami co w 3.1 (wpisanie nazwy, bieżąca lokalizacja, wybór na mapie)
- Wyszukiwanie informacji w internecie (Web Search + Wikipedia)
- **Odczyt Mojej Listy** – asystent może sięgnąć po zapisane atrakcje użytkownika, gdy jest to potrzebne do odpowiedzi (np. „zaplanuj trasę z moich miejsc"). **Asystent nie ma uprawnień do zapisu** – nie może dodawać ani usuwać elementów z Mojej Listy. Te akcje wykonuje wyłącznie użytkownik ręcznie z UI (ekrany 3.2 / 3.4).

**Pamięć rozmowy:**
- Asystent pamięta całą historię konwersacji w ramach sesji (od uruchomienia do zamknięcia aplikacji)
- Przycisk **„Wyczyść czat"** usuwa całą historię rozmowy i zeruje pamięć asystenta
- Po wyczyszczeniu asystent zaczyna rozmowę od nowa

**UI czatu:**
- Dymki wiadomości użytkownika i asystenta
- Odpowiedź asystenta wyświetlana strumieniowo (słowo po słowie)
- Wskaźnik ładowania gdy asystent przetwarza zapytanie
- W przypadku błędu API – komunikat błędu wyświetlany w miejscu odpowiedzi
- Przycisk „Wyczyść czat" w nagłówku ekranu (z potwierdzeniem)

---

### 3.6 Ustawienia

**Klucze API**
- Klucz OpenRouter
- Klucz Tavily API
- Licznik wykorzystania Tavily: „Wykorzystano X / 1000 zapytań w tym miesiącu" (liczony lokalnie)
- Przycisk „Przetestuj połączenie" dla każdego klucza
- *Bezpieczeństwo przechowywania kluczy – odłożone na czas MVP, do ustalenia później (patrz Sekcja 6)*

**Model AI**
- Wybór modelu (pole tekstowe lub dropdown)
- Domyślny model: wybrany pod kątem niskiego kosztu, do rewizji po MVP

**System Prompty**

Każdy agent w aplikacji ma osobny system prompt z możliwością personalizacji i resetowania do domyślnego:

- **Agent opisów** – system prompt używany przy generowaniu opisów atrakcji (ekran 3.2)
- **Asystent** – system prompt używany w ekranie czatu (ekran 3.5)

**Zainteresowania** – lista checkboxów wpływająca na:
1. Filtrowanie wyników wyszukiwania (jakie kategorie atrakcji są pokazywane)
2. Zachowanie AI (asystent i opisy uwzględniają preferencje użytkownika)

> **Uwaga architektoniczna:** Kategorie zainteresowań to pojęcia z warstwy biznesowej (to, co widzi i rozumie użytkownik) i **nie mapują się 1:1 na kategorie OpenTripMap**. Szczegółowe mapowanie jest regułą warstwy infrastrukturalnej/technicznej – niewidoczną dla użytkownika. Zostanie zdefiniowane w specyfikacji technicznej.

Propozycja kategorii:
- [ ] Zamki i fortyfikacje
- [ ] Kościoły i obiekty sakralne
- [ ] Muzea i galerie
- [ ] Ruiny i stanowiska archeologiczne
- [ ] Przyroda i parki narodowe
- [ ] Punkty widokowe
- [ ] Obiekty militarne
- [ ] Młyny, wiatraki, zabytki techniki
- [ ] Miejsca pamięci i cmentarze
- [ ] Jaskinie i formacje geologiczne
*(lista do rozszerzenia)*

**Język opisów**
- Dropdown: Polski | English | Deutsch | Français | Español | *(inne do dodania)*

**Promień domyślny**
- Suwak 1–50 km (wartość wstępna przy otwieraniu ekranu Szukaj)

---

## 4. Tryb offline

| Funkcja | Online | Offline |
|---|---|---|
| Wyszukiwanie nowych atrakcji | ✅ | ❌ |
| Przeglądanie Mojej Listy | ✅ | ✅ |
| Szczegóły z Mojej Listy | ✅ | ✅ (jeśli opis był załadowany) |
| Mapa z pinezkami Mojej Listy | ✅ | ✅ (bez podkładu mapowego) |
| Nawigacja (Google Maps) | ✅ | ⚠️ zależy od Google Maps |
| Asystent AI | ✅ | ❌ |
| Ustawienia | ✅ | ✅ |

---

## 5. Obsługa błędów

- **Brak GPS:** komunikat z opcją ręcznego wyboru punktu lub wpisania nazwy
- **Brak internetu:** komunikat + sugestia przejścia do Mojej Listy (offline)
- **Nieprawidłowy klucz API:** komunikat z linkiem do ekranu Ustawień
- **Brak wyników w promieniu:** komunikat z sugestią zwiększenia promienia
- **Duplikat w Mojej Liście:** informacja „Ta atrakcja jest już na Twojej liście"
- **Limit Mojej Listy przekroczony:** informacja „Twoja lista jest pełna (50/50). Usuń miejsce, żeby dodać nowe."
- **Timeout wyszukiwania:** możliwość ponowienia
- **Częściowa niedostępność źródeł wzbogacania opisu** (Web Search lub Wikipedia nie odpowiada): opis generowany na bazie dostępnych źródeł, UI informuje które źródła faktycznie zostały użyte
- **Całkowita niedostępność wszystkich źródeł wzbogacania opisu** (OpenTripMap + Web Search + Wikipedia): komunikat o niemożności wygenerowania opisu, możliwość ponowienia

---

## 6. Kwestie otwarte (do ustalenia)

- [ ] Ostateczna lista kategorii zainteresowań (warstwa biznesowa)
- [ ] Mapowanie kategorii zainteresowań → kategorie OpenTripMap (warstwa techniczna – specyfikacja techniczna)
- [ ] Model danych / struktury / wybór storage (specyfikacja techniczna – w trakcie ustalania)
- [x] ~~Czy historia poprzednich wyszukiwań (nie listy) jest zachowywana między sesjami~~ — aplikacja cachuje wyłącznie ostatnie wyszukiwanie; nowe wyszukiwanie zastępuje poprzednie
- [ ] Ciemny motyw – od razu czy w późniejszej iteracji
- [x] ~~Limit elementów w Mojej Liście~~ — maksymalnie 50 elementów; próba dodania kolejnego wyświetla komunikat
- [ ] Bezpieczeństwo przechowywania kluczy API – odłożone na czas MVP, do ustalenia później
- [ ] Polityka przy całkowitej niedostępności wszystkich źródeł wzbogacania opisu – domyślnie błąd + retry (do potwierdzenia w testach)

---

## 7. Priorytety implementacji

1. **MVP:** Szukaj → lista atrakcji → Szczegół z opisem (wzbogacanie wielo-źródłowe) → Moja Lista (dodawanie/usuwanie ręczne)
2. **Mapa:** ekran z pinezkami i dymkami
3. **Asystent:** ekran czatu z narzędziami (odczyt) i pamięcią
4. **Ustawienia:** pełna konfiguracja
5. **Eksport GPX**
6. **Polish:** ciemny motyw, animacje, onboarding

---

*Dokument: WanderList Spec v0.5 | 2026-06-27*
