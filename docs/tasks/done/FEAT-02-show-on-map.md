# FEAT-02: Przycisk "Pokaż na mapie" w szczegółach atrakcji

## Cel

Dodać przycisk "Pokaż na mapie" w ekranie szczegółów atrakcji. Kliknięcie nawiguje do zakładki Mapa i centruje ją na lokalizacji wybranej atrakcji, z zaznaczonym punktem.

## Implementacja

### 1. `Screen.kt` — rozszerzenie trasy Mapy o opcjonalne parametry

```kotlin
sealed class Screen(val route: String) {
    // ...
    object Map : Screen("map")
    object MapWithTarget : Screen("map?lat={lat}&lon={lon}&xid={xid}") {
        fun createRoute(lat: Double, lon: Double, xid: String) =
            "map?lat=$lat&lon=$lon&xid=$xid"
    }
}
```

Alternatywnie (prostsze): użyj jednego route z opcjonalnymi argumentami:
```
"map?lat={lat}&lon={lon}&xid={xid}"
```
i zarejestruj go jako ZASTĘPSTWO dla `Screen.Map.route` z `defaultValue`.

### 2. `WanderListNavGraph.kt`

Zmień rejestrację ekranu Mapy na przyjmowanie opcjonalnych parametrów:

```kotlin
composable(
    route = "map?lat={lat}&lon={lon}&xid={xid}",
    arguments = listOf(
        navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
        navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = null },
        navArgument("xid") { type = NavType.StringType; nullable = true; defaultValue = null }
    )
) { backStackEntry ->
    val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
    val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
    val xid = backStackEntry.arguments?.getString("xid")
    MapScreen(
        onAttractionClick = { navController.navigate(Screen.AttractionDetail.createRoute(it)) },
        targetLat = lat,
        targetLon = lon,
        targetXid = xid
    )
}
```

Zachowaj backward-compatible route dla bottom nav (bez parametrów).

### 3. `MapScreen.kt`

Dodaj parametry:
```kotlin
@Composable
fun MapScreen(
    onAttractionClick: (String) -> Unit = {},
    targetLat: Double? = null,
    targetLon: Double? = null,
    targetXid: String? = null,
    viewModel: MapViewModel = koinViewModel()
)
```

Jeśli `targetLat != null && targetLon != null`, po załadowaniu mapy wycentruj na docelowej pozycji:
```kotlin
LaunchedEffect(targetLat, targetLon) {
    if (targetLat != null && targetLon != null) {
        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(targetLat, targetLon), 15.0))
        targetXid?.let { viewModel.selectAttraction(it) }
    }
}
```

Przechowaj referencję do `MapLibreMap` jako `remember { mutableStateOf<MapLibreMap?>(null) }` w `MapScreen`.

### 4. `AttractionDetailScreen.kt`

Dodaj callback `onShowOnMap`:
```kotlin
@Composable
fun AttractionDetailScreen(
    xid: String,
    onBack: () -> Unit = {},
    onShowOnMap: ((lat: Double, lon: Double, xid: String) -> Unit)? = null,
    showDistance: Boolean = false,
    viewModel: AttractionDetailViewModel = koinViewModel()
)
```

Przycisk w UI (obok "Nawiguj do"):
```kotlin
onShowOnMap?.let { callback ->
    OutlinedButton(
        onClick = { callback(attraction.latitude, attraction.longitude, attraction.xid) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Pokaż na mapie")
    }
}
```

### 5. `WanderListNavGraph.kt` — podłącz callback

```kotlin
composable(...) { backStackEntry ->
    val xid = ...
    AttractionDetailScreen(
        xid = xid,
        onBack = { navController.popBackStack() },
        onShowOnMap = { lat, lon, targetXid ->
            navController.navigate("map?lat=$lat&lon=$lon&xid=$targetXid")
        }
    )
}
```

## Pliki do zmiany
- `app/src/.../navigation/Screen.kt` (lub gdzie są definicje tras)
- `app/src/.../navigation/WanderListNavGraph.kt`
- `feature-map/src/.../ui/MapScreen.kt`
- `feature-detail/src/.../ui/AttractionDetailScreen.kt`

## Weryfikacja
- Wejście w szczegóły atrakcji → widoczny przycisk "Pokaż na mapie"
- Kliknięcie → zakładka Mapa otwiera się, kamera centruje się na atrakcji (zoom ~15)
- Dymek z nazwą atrakcji jest widoczny automatycznie
- Nawigacja bottom bar do Mapy bez parametrów → normalne działanie (bez wymuszania pozycji)
