package com.iicytower.wanderlist.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.util.formatDistance
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.feature.search.viewmodel.SearchViewModel
import com.iicytower.wanderlist.feature.search.viewmodel.SortOrder
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAttractionClick: (String) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Wyszukaj atrakcje", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            // Pole lokalizacji z dropdownem sugestii
            Box {
                OutlinedTextField(
                    value = state.locationQuery,
                    onValueChange = { viewModel.updateLocationQuery(it) },
                    label = { Text("Szukaj miejsca (miasto, adres...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = { viewModel.searchLocationByName(state.locationQuery) },
                                enabled = state.locationQuery.isNotBlank() && !state.isLoading
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Szukaj miejsca")
                            }
                            IconButton(onClick = { viewModel.openLocationPicker() }) {
                                Icon(Icons.Default.Map, contentDescription = "Wybierz na mapie")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.dismissSuggestions()
                            viewModel.searchLocationByName(state.locationQuery)
                        }
                    )
                )

                DropdownMenu(
                    expanded = state.locationSuggestions.isNotEmpty(),
                    onDismissRequest = { viewModel.dismissSuggestions() }
                ) {
                    state.locationSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion.displayName, style = MaterialTheme.typography.bodySmall) },
                            onClick = { viewModel.selectSuggestion(suggestion) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.searchLocationLabel.ifBlank { "Wybierz punkt wyszukiwania" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.searchLocation == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { viewModel.setLocationFromGps() },
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "Moja lokalizacja (GPS)")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Promień
            Text("Promień: ${state.radiusKm} km", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = state.radiusKm.toFloat(),
                onValueChange = { viewModel.setRadius(it.toInt()) },
                valueRange = AppConstants.MIN_SEARCH_RADIUS_KM.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Przycisk szukaj
            Button(
                onClick = { viewModel.search() },
                enabled = state.searchLocation != null && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Szukaj")
            }

            Spacer(Modifier.height(8.dp))

            // Wyniki
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.hasSearched && state.results.isEmpty() && state.error == null) {
                Text(
                    "Brak atrakcji w promieniu ${state.radiusKm} km. Spróbuj zwiększyć promień.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (state.results.isNotEmpty()) {
                if (state.debugSourceStats.isNotEmpty()) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("[DEBUG] Wyniki per zrodlo:", style = MaterialTheme.typography.labelSmall)
                            state.debugSourceStats.forEach { (source, count) ->
                                Text("  $source: $count", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("  Po dedup: ${state.results.size}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Znaleziono: ${state.results.size}", style = MaterialTheme.typography.bodySmall)
                        state.filterRemovedCount?.let { count ->
                            Text(
                                "Odfiltrowano: $count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isFiltering) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.filterByQuality() },
                                enabled = !state.isLoading
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Przefiltruj")
                            }
                        }
                        OutlinedButton(onClick = {
                            viewModel.setSortOrder(
                                if (state.sortOrder == SortOrder.BY_DISTANCE) SortOrder.BY_CATEGORY else SortOrder.BY_DISTANCE
                            )
                        }) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(if (state.sortOrder == SortOrder.BY_DISTANCE) "Odległość" else "Kategoria")
                        }
                    }
                }
                LazyColumn {
                    items(state.results) { attraction ->
                        AttractionListItem(
                            attraction = attraction,
                            onClick = { onAttractionClick(attraction.xid) }
                        )
                    }
                }
            }
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
            ) { Text(error) }
        }
    }

    // Location picker bottom sheet
    if (state.showLocationPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissLocationPicker() },
            sheetState = sheetState
        ) {
            LocationPickerContent(
                initialLat = state.searchLocation?.latitude ?: 52.2297,
                initialLon = state.searchLocation?.longitude ?: 21.0122,
                onPick = { lat, lon -> viewModel.onLocationPicked(lat, lon) },
                onCancel = { viewModel.dismissLocationPicker() }
            )
        }
    }
}

@Composable
private fun LocationPickerContent(
    initialLat: Double,
    initialLon: Double,
    onPick: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pickedLat = remember { mutableStateOf(initialLat) }
    val pickedLon = remember { mutableStateOf(initialLon) }

    remember(context) { MapLibre.getInstance(context) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Wybierz lokalizację", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Przesuń mapę, aby wybrać punkt", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
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
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(initialLat, initialLon))
                                .zoom(12.0)
                                .build()
                            map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty"))
                            map.addOnCameraIdleListener {
                                val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                                pickedLat.value = target.latitude
                                pickedLon.value = target.longitude
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Pin w centrum
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "%.5f, %.5f".format(pickedLat.value, pickedLon.value),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Anuluj")
            }
            Button(
                onClick = { onPick(pickedLat.value, pickedLon.value) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Wybierz tę lokalizację")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AttractionListItem(attraction: Attraction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(attraction.name, style = MaterialTheme.typography.titleMedium)
            Text(
                attraction.category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            attraction.distanceKm?.let { dist ->
                Text(
                    formatDistance(dist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (attraction.description != null) {
                Text("• Opis dostępny", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
