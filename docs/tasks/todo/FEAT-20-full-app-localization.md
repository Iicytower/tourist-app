# FEAT-20: Pełna lokalizacja treści aplikacji do wybranego języka

## Problem

`FEAT-11` (ustawienie "Język aplikacji") i `FEAT-10` (lokalne wyszukiwanie w języku kraju atrakcji) obejmują język generowanych opisów, podpowiedzi lokalizacji i odpowiedzi asystenta — ale świadomie **wykluczają** tłumaczenie samego interfejsu (etykiety, przyciski, nagłówki sekcji), które dziś są zaszyte na stałe po polsku w kodzie Compose (brak `strings.xml`/`values-en` — całość UI to stringi inline w `.kt`). Issue #12 mówi wprost o tym, żeby "wszystko" było w wybranym języku, co obejmuje też ten aspekt.

Dodatkowo: nazwy atrakcji (`Attraction.name`) pochodzą wprost ze źródeł (Overpass/Wikipedia/Wikidata) w ich oryginalnym języku i nie są tłumaczone — to ograniczenie do jawnego udokumentowania, nie do "naprawienia" tłumaczeniem w locie (ryzyko złej jakości/kosztu przy każdym renderze listy wyników).

## Cel

1. Wydzielić wszystkie stringi UI z Compose do zasobów Android (`strings.xml` + `values-en/strings.xml`, `values-de/strings.xml` itd. dla języków już wspieranych przez `appLanguage` z FEAT-11).
2. Ustawienie "Język aplikacji" z FEAT-11 steruje też językiem UI — albo przez `AppCompatDelegate.setApplicationLocales()` (Android 13+ / AppCompat per-app language), albo przez ręczne przełączanie `Locale` w `Context` dla starszych wersji (min SDK 26).
3. Jawnie udokumentować (w UI, np. tooltip/opis pod ustawieniem języka), że nazwy atrakcji pozostają w języku źródła — to nie jest błąd, tylko świadome ograniczenie.

## Zakres zmian

- Każdy moduł `feature-*` — przegląd i wydzielenie stringów Compose (`Text("...")`, `contentDescription`, etc.) do `res/values/strings.xml` per moduł.
- Tłumaczenia na języki z listy w `SettingsScreen.kt` (`pl`, `en`, `de`, `fr`, `es`) — `res/values-en`, `values-de`, `values-fr`, `values-es`.
- `app/.../WanderListApp.kt` lub `MainActivity.kt` — zastosowanie `appLanguage` z ustawień do `AppCompatDelegate.setApplicationLocales()` przy starcie i przy zmianie ustawienia w runtime.
- `feature-settings/.../SettingsScreen.kt` — dopisek pod polem "Język aplikacji" wyjaśniający zakres (UI + opisy + asystent, ale nie oryginalne nazwy atrakcji).

## Pliki do zmiany / stworzenia

- `app/src/main/AndroidManifest.xml` (`android:localeConfig`, jeśli używamy per-app language API)
- `app/src/main/res/xml/locales_config.xml` (nowy, jeśli per-app language API)
- `*/src/main/res/values/strings.xml` (nowe w każdym module `feature-*` + `app`)
- `*/src/main/res/values-en/strings.xml`, `values-de/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml` (nowe)
- `app/src/main/kotlin/.../WanderListApp.kt` lub odpowiednik `MainActivity.kt`
- `feature-settings/src/main/kotlin/.../ui/SettingsScreen.kt`

## Weryfikacja

- Zmiana "Języka aplikacji" na `en` → wszystkie ekrany (Wyszukiwanie, Mapa, Moja Lista, Asystent, Ustawienia) pokazują angielskie etykiety, nie tylko angielskie opisy/odpowiedzi asystenta
- Restart aplikacji zachowuje wybrany język UI
- Nazwy atrakcji nadal są w oryginalnym języku źródła — udokumentowane w UI, nie zgłaszane jako błąd

## Zależności

- Zakłada ukończone `FEAT-11` (pole `appLanguage` w ustawieniach) — ten task go rozszerza o lokalizację UI zamiast duplikować logikę wyboru języka.

<!-- TODO: konsultacja — issue #12 mógł też oznaczać wyłącznie zakres już pokryty przez FEAT-10/FEAT-11 (wyniki/opisy/asystent), a nie pełne i18n UI. Zdecydowałem się opisać różnicę wprost jako osobny task, bo FEAT-11 explicite wyklucza i18n UI ze swojego zakresu — do potwierdzenia/zamknięcia przy priorytetyzacji, jeśli okaże się zbędny. -->
