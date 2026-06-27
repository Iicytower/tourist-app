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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.util.formatDistance
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.feature.search.viewmodel.SearchViewModel
import com.iicytower.wanderlist.feature.search.viewmodel.SortOrder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAttractionClick: (String) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Wyszukaj atrakcje", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            // Wyszukiwanie miejsca po nazwie + GPS
            OutlinedTextField(
                value = state.locationQuery,
                onValueChange = { viewModel.updateLocationQuery(it) },
                label = { Text("Szukaj miejsca (miasto, adres...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.searchLocationByName(state.locationQuery) },
                        enabled = state.locationQuery.isNotBlank() && !state.isLoading
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj miejsca")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.searchLocationByName(state.locationQuery) }
                )
            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Znaleziono: ${state.results.size}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        viewModel.setSortOrder(
                            if (state.sortOrder == SortOrder.BY_DISTANCE) SortOrder.BY_CATEGORY else SortOrder.BY_DISTANCE
                        )
                    }) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (state.sortOrder == SortOrder.BY_DISTANCE) "Odległość" else "Kategoria")
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
