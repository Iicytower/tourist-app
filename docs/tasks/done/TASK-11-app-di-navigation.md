# TASK-11: Moduł `app` — DI (Koin) i nawigacja (Navigation Compose)

## Cel
Podłączenie wszystkich modułów Koin, definicja grafu nawigacji i konfiguracja `MainActivity`. Punkt wejścia aplikacji, który łączy wszystkie wcześniejsze taski.

## Zależności
- TASK-01 (projekt)
- TASK-04, TASK-05, TASK-06, TASK-07, TASK-08, TASK-09, TASK-10 (wszystkie Koin modules z `data`)
- TASK-03 (use case'y z `domain`)

## Zakres

### 1. Koin modules dla use case'ów i ViewModeli (`app/di/`)

Use case'y i ViewModele definiowane w module `app` (lub w osobnych plikach per feature — do decyzji podczas implementacji):

```kotlin
val useCaseModule = module {
    factory { SearchAttractionsUseCase(get()) }
    factory { GetMyListUseCase(get()) }
    factory { AddToMyListUseCase(get()) }
    factory { RemoveFromMyListUseCase(get()) }
    factory { GenerateDescriptionUseCase(get(), get(), get(), get()) }
    factory { GetSettingsUseCase(get()) }
    factory { GetAttractionDetailUseCase(get()) }
    factory { SendChatMessageUseCase(get()) }
}
```

Alternatywa: każdy moduł `feature-*` definiuje własny `KoinModule` z ViewModelami — moduł `app` ładuje listę modułów. Preferowane jeśli moduły feature mają własne zależności.

**Kolejność ładowania w `WanderListApp`:**
```kotlin
startKoin {
    androidContext(this@WanderListApp)
    modules(
        databaseModule,
        attractionRepositoryModule,
        settingsModule,
        openTripMapModule,
        webSearchModule,
        wikipediaModule,
        llmModule,
        locationModule,
        useCaseModule,
        // viewModelModules z feature-*
    )
}
```

### 2. Graf nawigacji (`app/navigation/WanderListNavGraph.kt`)

Navigation Compose z route-based navigation:

```kotlin
sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Map : Screen("map")
    object MyList : Screen("mylist")
    object Assistant : Screen("assistant")
    object Settings : Screen("settings")
    object AttractionDetail : Screen("attraction/{xid}") {
        fun createRoute(xid: String) = "attraction/$xid"
    }
}
```

`NavHost` ze wszystkimi composable:
- Bottom navigation bar (tab bar) z 5 zakładkami: Szukaj, Mapa, Moja Lista, Asystent, Ustawienia
- Ekran szczegółu (`AttractionDetail`) dostępny ze wszystkich zakładek przez nawigację push (bez bottom bar)
- Argument `xid` jako `NavType.StringType`

### 3. `MainActivity.kt`

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermissionIfNeeded()
        setContent {
            WanderListTheme {
                WanderListNavGraph()
            }
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        // ActivityResultContracts.RequestPermission()
        // Pytanie przy pierwszym uruchomieniu (nie lazy)
    }
}
```

### 4. Motyw Material 3 (`app/ui/theme/`)

`WanderListTheme.kt` — wrapper na `MaterialTheme` z:
- `ColorScheme` z dynamicznymi kolorami Material You (Android 12+) lub fallback
- `Typography` z dopasowaną czcionką (domyślna system font na MVP)
- Ciemny motyw: obsługa `isSystemInDarkTheme()` — implementacja odłożona (TASK rozszerzający przed produkcją)

## Testy

Brak automatycznych testów dla nawigacji i DI na MVP (UI testy poza zakresem MVP).

Weryfikacja manualna:
- Aplikacja startuje bez wyjątków Koin
- Nawigacja po wszystkich 5 zakładkach działa
- Przejście do szczegółu atrakcji i powrót działa

## Weryfikacja ukończenia
- `./gradlew assembleDebug` przechodzi
- Aplikacja startuje na emulatorze
- Wszystkie moduły Koin załadowane bez błędów (`KoinApplication` w logach)
- Nawigacja działa dla wszystkich ekranów
