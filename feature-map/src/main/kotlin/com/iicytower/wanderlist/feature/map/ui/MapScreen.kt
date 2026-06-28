package com.iicytower.wanderlist.feature.map.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private fun createPinBitmap(): Bitmap {
    val w = 64
    val h = 80
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = w / 2f
    val r = w / 2f

    paint.color = android.graphics.Color.parseColor("#FF5722")
    canvas.drawCircle(cx, r, r, paint)

    val tail = Path().apply {
        moveTo(cx - r * 0.45f, r + r * 0.55f)
        lineTo(cx + r * 0.45f, r + r * 0.55f)
        lineTo(cx, h.toFloat())
        close()
    }
    canvas.drawPath(tail, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, r, r * 0.42f, paint)

    return bitmap
}

@Composable
fun MapScreen(
    onAttractionClick: (String) -> Unit = {},
    viewModel: MapViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    // Odczyt w scopie composable — ustanawia obserwację; zmiana styleRef wyzwala rekompozyację
    val currentStyle = styleRef.value

    remember(context) { MapLibre.getInstance(context) }

    LaunchedEffect(Unit) { viewModel.onMapReady() }

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
                    map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) { style ->
                        style.addImage("pin-icon", createPinBitmap())
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
                val attractions = if (state.showMyListOnly) state.myList else state.searchResults

                runCatching { style.removeLayer("attractions-labels") }
                runCatching { style.removeLayer("attractions-layer") }
                runCatching { style.removeSource("attractions-source") }

                if (attractions.isEmpty()) return@AndroidView

                val featuresJson = attractions.joinToString(",") { a ->
                    val name = a.name.replace("\\", "\\\\").replace("\"", "\\\"")
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${a.longitude},${a.latitude}]},"properties":{"xid":"${a.xid}","name":"$name"}}"""
                }
                style.addSource(GeoJsonSource("attractions-source", """{"type":"FeatureCollection","features":[$featuresJson]}"""))
                style.addLayer(SymbolLayer("attractions-layer", "attractions-source").apply {
                    setProperties(
                        PropertyFactory.iconImage("pin-icon"),
                        PropertyFactory.iconSize(0.9f),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        PropertyFactory.iconAllowOverlap(true)
                    )
                })
                style.addLayer(SymbolLayer("attractions-labels", "attractions-source").apply {
                    setProperties(
                        PropertyFactory.textField(Expression.get("name")),
                        PropertyFactory.textSize(11f),
                        PropertyFactory.textColor(android.graphics.Color.BLACK),
                        PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
                        PropertyFactory.textHaloWidth(2f),
                        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                        PropertyFactory.textOffset(arrayOf(0f, 0.3f)),
                        PropertyFactory.textMaxWidth(8f),
                        PropertyFactory.textOptional(true)
                    )
                })
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
