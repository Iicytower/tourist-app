package com.iicytower.wanderlist.feature.map.ui

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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import timber.log.Timber


@Composable
fun MapScreen(
    onAttractionClick: (String) -> Unit = {},
    targetLat: Double? = null,
    targetLon: Double? = null,
    targetXid: String? = null,
    viewModel: MapViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val currentStyle = styleRef.value
    Timber.tag("MAP").d("compose: currentStyle=%b showMyList=%b search=%d my=%d",
        currentStyle != null, state.showMyListOnly, state.searchResults.size, state.myList.size)

    remember(context) { MapLibre.getInstance(context) }

    LaunchedEffect(Unit) { viewModel.onMapReady() }

    LaunchedEffect(targetLat, targetLon, mapRef.value) {
        if (targetLat != null && targetLon != null) {
            val map = mapRef.value ?: return@LaunchedEffect
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(targetLat, targetLon), 15.0))
            targetXid?.let { viewModel.pinTargetAttraction(it) }
        }
    }

    LaunchedEffect(state.initialCameraPosition, mapRef.value) {
        if (targetLat != null) return@LaunchedEffect
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
                    map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) { style ->
                        Timber.tag("MAP").d("setStyle callback fired — adding source/layers")
                        style.addSource(GeoJsonSource("attractions-source", """{"type":"FeatureCollection","features":[]}"""))
                        style.addLayer(CircleLayer("attractions-layer", "attractions-source").apply {
                            setProperties(
                                PropertyFactory.circleRadius(10f),
                                PropertyFactory.circleColor(android.graphics.Color.parseColor("#FF5722")),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                            )
                        })
                        style.addSource(GeoJsonSource("pinned-source", """{"type":"FeatureCollection","features":[]}"""))
                        style.addLayer(CircleLayer("pinned-layer", "pinned-source").apply {
                            setProperties(
                                PropertyFactory.circleRadius(14f),
                                PropertyFactory.circleColor(android.graphics.Color.parseColor("#1565C0")),
                                PropertyFactory.circleStrokeWidth(3f),
                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                            )
                        })
                        styleRef.value = style
                    }
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
            update = { _ ->
                val style = currentStyle ?: return@AndroidView
                val attractions = if (state.showMyListOnly) state.myList else emptyList()
                fun attractionFeature(a: com.iicytower.wanderlist.domain.model.Attraction): String {
                    val name = a.name.replace("\\", "\\\\").replace("\"", "\\\"")
                    return """{"type":"Feature","geometry":{"type":"Point","coordinates":[${a.longitude},${a.latitude}]},"properties":{"xid":"${a.xid}","name":"$name"}}"""
                }
                (style.getSource("attractions-source") as? GeoJsonSource)?.let { src ->
                    val json = """{"type":"FeatureCollection","features":[${attractions.joinToString(",") { attractionFeature(it) }}]}"""
                    runCatching { src.setGeoJson(json) }.onFailure { Timber.tag("MAP").e(it, "setGeoJson failed") }
                }
                (style.getSource("pinned-source") as? GeoJsonSource)?.let { src ->
                    val pinnedFeature = state.pinnedAttraction?.let { attractionFeature(it) } ?: ""
                    val json = """{"type":"FeatureCollection","features":[$pinnedFeature]}"""
                    runCatching { src.setGeoJson(json) }.onFailure { Timber.tag("MAP").e(it, "pinned setGeoJson failed") }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Card(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Moja Lista", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = state.showMyListOnly, onCheckedChange = { viewModel.toggleMyListMode() })
            }
        }

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
