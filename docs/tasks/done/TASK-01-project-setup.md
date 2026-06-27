# TASK-01: Konfiguracja projektu Android (multi-module)

## Cel
Stworzenie szkieletu projektu Android z pełną strukturą multi-module, systemem budowania i narzędziami developerskimi. To fundament — wszystkie kolejne taski zależą od tego.

## Zakres

### 1. Inicjalizacja projektu Android
- Utwórz projekt Android w Android Studio (Empty Activity z Jetpack Compose)
- Application ID: `com.iicytower.wanderlist`
- Min SDK: 26, Target SDK: 37, Java: 21
- Język: Kotlin
- Nazwa aplikacji: WanderList

### 2. Struktura modułów Gradle
Utwórz następujące moduły (każdy jako `com.android.library` z wyjątkiem `app`):

```
app/                    (com.android.application)
core/                   (com.android.library)
domain/                 (com.android.library)
data/                   (com.android.library)
feature-search/         (com.android.library)
feature-map/            (com.android.library)
feature-mylist/         (com.android.library)
feature-assistant/      (com.android.library)
feature-settings/       (com.android.library)
```

Każdy moduł: pusty `build.gradle.kts` z odpowiednim pluginem, pustą strukturą pakietów (`src/main/kotlin/...`) i plikiem `AndroidManifest.xml` jeśli wymagany.

Struktura pakietów wewnątrz modułów:
- `core`: `model/`, `util/`, `constant/`
- `domain`: `model/`, `repository/`, `usecase/`
- `data`: `local/`, `remote/`, `repository/`
- `feature-*`: `ui/`, `viewmodel/`

### 3. Version Catalog (`libs.versions.toml`)
Centralny plik wersji w `gradle/libs.versions.toml`. Musi zawierać wersje i definicje dla:

```toml
[versions]
kotlin = "..."
agp = "..."
compose-bom = "..."
koin = "..."
ktor = "..."
room = "..."
datastore = "..."
navigation-compose = "..."
security-crypto = "..."
maplibre = "..."
kotlinx-serialization = "..."
timber = "..."
junit = "..."
mockk = "..."
coroutines = "..."

[libraries]
# Compose
# Koin
# Ktor (client-core, client-android, client-content-negotiation, client-logging, serialization-json)
# Room (runtime, ktx, compiler)
# DataStore (preferences)
# Navigation Compose
# Security Crypto
# MapLibre
# Kotlinx Serialization
# Timber
# Coroutines (core, android)
# JUnit
# MockK

[plugins]
android-application = "..."
android-library = "..."
kotlin-android = "..."
kotlin-compose = "..."
kotlin-serialization = "..."
ksp = "..."
```

### 4. Konfiguracja zależności między modułami
W `settings.gradle.kts` — wszystkie moduły zarejestrowane.

Zależności (tylko te wymagane strukturalnie):
- `app` → wszystkie moduły
- `feature-*` → `domain`, `core`
- `data` → `domain`, `core`
- `domain` → `core`

**Ważne:** `feature-*` NIE zależy bezpośrednio od `data`.

### 5. Claude Skills w `CLAUDE.md` projektu
Uzupełnij plik `CLAUDE.md` w katalogu głównym repozytorium o skille umożliwiające Claude Code pracę z projektem:

```markdown
## Skills

### Build
Buduje aplikację (debug):
`./gradlew assembleDebug`

### Test
Uruchamia wszystkie testy jednostkowe:
`./gradlew test`

Uruchamia testy dla konkretnego modułu (np. domain):
`./gradlew :domain:test`

### Lint
`./gradlew lint`

### Emulator
Lista dostępnych AVD:
`emulator -list-avds`

Uruchomienie emulatora (zastąp NAME nazwą AVD):
`emulator -avd NAME -no-audio -no-boot-anim`

### Install
Instalacja debug apk na podłączonym urządzeniu/emulatorze:
`./gradlew installDebug`

### Connected tests (instrumentacja)
`./gradlew connectedAndroidTest`

### Room schema export
Eksport schematu Room do pliku JSON:
`./gradlew :data:kspDebugKotlin`
```

Skille powinny być też faktycznie zarejestrowane jako slash commands w `.claude/commands/` (pliki `.md` z komendą) — jeden plik per skill: `build.md`, `test.md`, `lint.md`, `install.md`.

### 6. Konfiguracja Timber w module `app`
- Klasa `WanderListApp : Application()`
- W `onCreate()`: `if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())`
- Zarejestrowanie w `AndroidManifest.xml`

### 7. Konfiguracja Koin (szkielet)
- W `WanderListApp.onCreate()`: `startKoin { androidContext(this@WanderListApp) }`
- Na razie bez żadnych modułów (zostaną dodane w kolejnych taskach)

## Testy
Brak testów dla tego tasku — same pliki konfiguracyjne i boilerplate.

## Weryfikacja ukończenia
- `./gradlew build` przechodzi bez błędów
- Wszystkie moduły widoczne w Android Studio
- `libs.versions.toml` zawiera wszystkie wymagane wersje
- `CLAUDE.md` zawiera skille z poprawnymi komendami
- Aplikacja instaluje się na emulatorze i uruchamia (pusta MainActivity)
