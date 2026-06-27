# TASK-02: Moduł `core`

## Cel
Implementacja modułu `core` zawierającego stałe, wspólne utility i modele używane przez wszystkie inne moduły.

## Zależności
- TASK-01 (projekt musi istnieć)

## Zakres

### 1. Stałe aplikacji (`core/constant/AppConstants.kt`)
```kotlin
object AppConstants {
    const val MY_LIST_MAX_SIZE = 50
    const val MIN_SEARCH_RADIUS_KM = 1
    const val MAX_SEARCH_RADIUS_KM = 50
    const val DEFAULT_SEARCH_RADIUS_KM = 10
}
```

### 2. Model wspólny — kategorie atrakcji (`core/model/AttractionCategory.kt`)
Enum lub sealed class reprezentujący kategorie biznesowe (widoczne dla użytkownika):

```kotlin
enum class AttractionCategory(val displayName: String) {
    CASTLES_AND_FORTIFICATIONS("Zamki i fortyfikacje"),
    CHURCHES_AND_SACRED("Kościoły i obiekty sakralne"),
    MUSEUMS_AND_GALLERIES("Muzea i galerie"),
    RUINS_AND_ARCHAEOLOGICAL("Ruiny i stanowiska archeologiczne"),
    NATURE_AND_PARKS("Przyroda i parki narodowe"),
    VIEWPOINTS("Punkty widokowe"),
    MILITARY("Obiekty militarne"),
    MILLS_AND_TECH("Młyny, wiatraki, zabytki techniki"),
    MEMORIALS_AND_CEMETERIES("Miejsca pamięci i cmentarze"),
    CAVES_AND_GEOLOGY("Jaskinie i formacje geologiczne")
}
```

### 3. Mapowanie kategorii → OpenTripMap kinds (`core/constant/CategoryKindsMapping.kt`)
Mapa `AttractionCategory → List<String>` zgodna z sekcją 27 specyfikacji technicznej:

| Kategoria | kinds |
|---|---|
| CASTLES_AND_FORTIFICATIONS | `castles`, `fortifications`, `palaces` |
| CHURCHES_AND_SACRED | `churches`, `cathedrals`, `monasteries`, `mosques`, `synagogues`, `temples`, `other_temples` |
| MUSEUMS_AND_GALLERIES | `museums`, `art_galleries` |
| RUINS_AND_ARCHAEOLOGICAL | `ruins`, `archaeological_site`, `other_archaeological_site` |
| NATURE_AND_PARKS | `national_parks`, `nature_reserves`, `biosphere_reserves` |
| VIEWPOINTS | `view_points` |
| MILITARY | `battlefields`, `fortifications` |
| MILLS_AND_TECH | `windmills`, `watermills`, `industrial_facilities` |
| MEMORIALS_AND_CEMETERIES | `burial_ground`, `memorials`, `monuments` |
| CAVES_AND_GEOLOGY | `caves_and_tunnels`, `geological_formations`, `rocks` |

Funkcja pomocnicza: `fun Set<AttractionCategory>.toKindsParam(): String` — zwraca przecinkami połączone kinds dla wybranych kategorii. Gdy zbiór pusty — zwraca wszystkie kinds.

### 4. Utility — obliczanie odległości (`core/util/DistanceUtils.kt`)
```kotlin
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
```
Implementacja wzorem Haversine. Bez zewnętrznych bibliotek.

### 5. Utility — formatowanie odległości (`core/util/DistanceUtils.kt`)
```kotlin
fun formatDistance(distanceKm: Double): String
// Przykłady: "0,8 km", "12 km", "120 m"
```

## Testy
**`core/test/`** (JUnit, bez MockK — czysta logika):
- `CategoryKindsMappingTest`: pusta selekcja → wszystkie kinds, jedna kategoria → jej kinds, kilka kategorii → suma kinds (bez duplikatów)
- `DistanceUtilsTest`: znane pary koordynat z oczekiwaną odległością (tolerancja ±0.1 km), formatowanie: wartości poniżej 1 km, powyżej 1 km

## Weryfikacja ukończenia
- `./gradlew :core:test` przechodzi
- Mapowanie wszystkich 10 kategorii zdefiniowane
- Brak zależności od `domain`, `data` ani `feature-*`
