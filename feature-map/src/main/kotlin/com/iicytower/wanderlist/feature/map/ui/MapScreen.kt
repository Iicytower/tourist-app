package com.iicytower.wanderlist.feature.map.ui

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.iicytower.wanderlist.feature.map.viewmodel.MapViewModel
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

@Composable
fun MapScreen(
    onAttractionClick: (String) -> Unit = {},
    viewModel: MapViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

    // Must be called before MapView is created (remember runs synchronously during composition)
    remember(context) { MapLibre.getInstance(context) }

    LaunchedEffect(Unit) {
        viewModel.onMapReady()
    }

    LaunchedEffect(state.initialCameraPosition, mapRef.value) {
        val pos = state.initialCameraPosition ?: return@LaunchedEffect
        val map = mapRef.value ?: return@LaunchedEffect
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(pos.first, pos.second))
            .zoom(pos.third)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val mapView = remember { MapView(context) }

        DisposableEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) = mapView.onStart()
                override fun onResume(owner: androidx.lifecycle.LifecycleOwner) = mapView.onResume()
                override fun onPause(owner: androidx.lifecycle.LifecycleOwner) = mapView.onPause()
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) = mapView.onStop()
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) = mapView.onDestroy()
            })
            onDispose { }
        }

        AndroidView(
            factory = { mapView.apply {
                getMapAsync { map ->
                    mapRef.value = map
                    map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty"))
                    map.addOnMapClickListener { latLng ->
                        val point = map.projection.toScreenLocation(latLng)
                        val features = map.queryRenderedFeatures(point, "attractions-layer")
                        val xid = features.firstOrNull()?.getStringProperty("xid")
                        viewModel.selectAttraction(xid)
                        xid != null
                    }
                    map.addOnCameraIdleListener {
                        val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                        viewModel.saveMapPosition(target.latitude, target.longitude, map.cameraPosition.zoom)
                    }
                }
            }},
            update = { mv ->
                mv.getMapAsync { map ->
                    map.getStyle { style ->
                        val attractions = if (state.showMyListOnly) state.myList else state.searchResults

                        runCatching { style.removeLayer("attractions-layer") }
                        runCatching { style.removeSource("attractions-source") }

                        if (attractions.isEmpty()) return@getStyle

                        val featuresJson = attractions.joinToString(",") { a ->
                            val name = a.name.replace("\\", "\\\\").replace("\"", "\\\"")
                            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${a.longitude},${a.latitude}]},"properties":{"xid":"${a.xid}","name":"$name"}}"""
                        }
                        style.addSource(GeoJsonSource("attractions-source", """{"type":"FeatureCollection","features":[$featuresJson]}"""))
                        style.addLayer(CircleLayer("attractions-layer", "attractions-source").apply {
                            setProperties(
                                PropertyFactory.circleRadius(10f),
                                PropertyFactory.circleColor(android.graphics.Color.parseColor("#FF5722")),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                            )
                        })
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Toggle Moja Lista
        Card(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Moja Lista", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = state.showMyListOnly, onCheckedChange = { viewModel.toggleMyListMode() })
            }
        }

        // Dymek po kliknięciu pinezki
        state.selectedAttraction?.let { attraction ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(attraction.name, style = MaterialTheme.typography.titleMedium)
                    Text(attraction.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { onAttractionClick(attraction.xid) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Więcej →")
                    }
                }
            }
        }
    }
}
