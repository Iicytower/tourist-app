# BUG-02: Pinezki z Mojej Listy nie wyświetlają się na mapie

## Problem

W `MapScreen.kt:82-86` blok `update` AndroidView jest pusty — pinezki nigdy nie są renderowane:

```kotlin
update = { mv ->
    mv.getMapAsync { map ->
        map.getStyle { style ->
            val attractions = if (state.showMyListOnly) state.myList else state.searchResults
            // Prosta implementacja pinezek — w pełnej wersji można użyć SymbolManager
        }
    }
},
```

Przełącznik "Moja Lista" zmienia `state.showMyListOnly` i wybiera odpowiednią listę, ale nic z nią nie robi.

## Rozwiązanie

Zaimplementuj renderowanie pinezek używając **Circle layers** MapLibre (prostsze niż SymbolManager, nie wymaga ikon).

### Podejście: GeoJSON Source + Circle Layer

```kotlin
update = { mv ->
    mv.getMapAsync { map ->
        map.getStyle { style ->
            val attractions = if (state.showMyListOnly) state.myList else state.searchResults

            // Usuń poprzednie warstwy/źródła
            style.removeLayer("attractions-layer")
            style.removeSource("attractions-source")

            // Zbuduj GeoJSON
            val features = attractions.map { a ->
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[${a.longitude},${a.latitude}]},"properties":{"xid":"${a.xid}","name":"${a.name}"}}"""
            }
            val geojson = """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

            val source = GeoJsonSource("attractions-source", geojson)
            style.addSource(source)

            val layer = CircleLayer("attractions-layer", "attractions-source").apply {
                setProperties(
                    PropertyFactory.circleRadius(10f),
                    PropertyFactory.circleColor(Color.parseColor("#FF5722")),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleStrokeColor(Color.WHITE)
                )
            }
            style.addLayer(layer)
        }
    }
}
```

### Kliknięcie w pinezkę

Listener `map.addOnMapClickListener` już istnieje i woła `viewModel.selectAttraction(null)`. Rozszerz go:

```kotlin
map.addOnMapClickListener { latLng ->
    val point = map.projection.toScreenLocation(latLng)
    val features = map.queryRenderedFeatures(point, "attractions-layer")
    val xid = features.firstOrNull()?.getStringProperty("xid")
    viewModel.selectAttraction(xid)
    xid != null
}
```

### Importy

```kotlin
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
```

## Uwagi

- Blok `update` wywołuje się przy każdej zmianie `state` — usuwanie i dodawanie source/layer na nowo jest OK dla MVP
- `removeLayer`/`removeSource` mogą rzucić wyjątek jeśli nie istnieją — użyj `style.getLayer("attractions-layer") != null` przed usunięciem lub `runCatching`
- Dymek po kliknięciu pinezki (`state.selectedAttraction`) już jest zaimplementowany w UI

## Pliki do zmiany
- `feature-map/src/.../ui/MapScreen.kt`

## Weryfikacja
- Wyniki wyszukiwania → zakładka Mapa → widoczne pomarańczowe kropki na mapie
- Kliknięcie w kropkę → dymek z nazwą i przyciskiem "Więcej →"
- Włączenie przełącznika "Moja Lista" → wyświetlają się tylko punkty z listy
- Kliknięcie poza pinezką → dymek znika
