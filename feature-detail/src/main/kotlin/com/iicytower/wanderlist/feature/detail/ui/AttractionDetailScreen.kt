package com.iicytower.wanderlist.feature.detail.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.util.formatDistance
import com.iicytower.wanderlist.feature.detail.viewmodel.AttractionDetailViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionDetailScreen(
    xid: String,
    onBack: () -> Unit = {},
    onShowOnMap: ((lat: Double, lon: Double, xid: String) -> Unit)? = null,
    showDistance: Boolean = false,
    viewModel: AttractionDetailViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(xid) {
        viewModel.load(xid, showDistance)
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.attraction?.name ?: "Szczegóły") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Wróć") }
                },
                actions = {
                    state.attraction?.let { attraction ->
                        IconButton(onClick = { viewModel.toggleMyList() }) {
                            Icon(
                                if (attraction.isInMyList) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (attraction.isInMyList) "Usuń z listy" else "Dodaj do listy"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(16.dp))
            state.attraction != null -> {
                val attraction = state.attraction!!
                LazyColumn(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                    item {
                        Text(attraction.name, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(attraction.category.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                        if (state.showDistanceFromSearch) {
                            attraction.distanceKm?.let { dist ->
                                Text("Odległość: ${formatDistance(dist)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Sekcja opisu
                        when {
                            attraction.description != null -> {
                                Text("Opis", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(attraction.description ?: "", style = MaterialTheme.typography.bodyMedium)
                                if (attraction.descriptionSources.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Źródła:", style = MaterialTheme.typography.labelMedium)
                                    attraction.descriptionSources.forEach { source ->
                                        TextButton(onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                            context.startActivity(intent)
                                        }) {
                                            Text(source.name, textDecoration = TextDecoration.Underline)
                                        }
                                    }
                                }
                                if (!state.isDescriptionLoading) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadDescription(force = true) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Przeładuj opis") }
                                }
                            }
                            state.isDescriptionLoading -> {
                                Row {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                    Text("Generuję opis...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            state.descriptionError != null -> {
                                Text(state.descriptionError!!, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = { viewModel.loadDescription() }) { Text("Spróbuj ponownie") }
                            }
                            else -> {
                                Button(onClick = { viewModel.loadDescription() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Załaduj opis")
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Nawigacja
                        Button(
                            onClick = {
                                val uri = Uri.parse("google.navigation:q=${attraction.latitude},${attraction.longitude}")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    val fallback = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${attraction.latitude},${attraction.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, fallback))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nawiguj do")
                        }

                        OutlinedButton(
                            onClick = {
                                val coords = "${attraction.latitude},${attraction.longitude}"
                                clipboardManager.setText(AnnotatedString(coords))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Skopiowano: $coords")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kopiuj lokalizację")
                        }

                        onShowOnMap?.let { callback ->
                            OutlinedButton(
                                onClick = { callback(attraction.latitude, attraction.longitude, attraction.xid) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pokaż na mapie")
                            }
                        }
                    }
                }
            }
            state.error != null -> {
                Text(state.error!!, modifier = Modifier.padding(innerPadding).padding(16.dp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
