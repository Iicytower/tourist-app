package com.iicytower.wanderlist.feature.detail.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.model.displayNameRes
import com.iicytower.wanderlist.core.util.formatDistance
import com.iicytower.wanderlist.feature.detail.R
import com.iicytower.wanderlist.domain.model.TripList
import com.iicytower.wanderlist.feature.detail.viewmodel.AttractionDetailViewModel
import kotlinx.coroutines.launch
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(xid) {
        viewModel.load(xid, showDistance)
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (state.showListSheet) {
        ListSelectionSheet(
            tripLists = state.tripLists,
            selectedIds = state.attractionListIds,
            sheetState = sheetState,
            onToggle = {
                viewModel.toggleList(it)
                scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.closeListSheet() }
            },
            onCreate = { viewModel.createAndAddToList(it) },
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    viewModel.closeListSheet()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.attraction?.name ?: stringResource(R.string.detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back)) }
                }
            )
        },
        floatingActionButton = {
            if (state.attraction != null) {
                FloatingActionButton(onClick = { viewModel.openListSheet() }) {
                    Icon(
                        if (state.attractionListIds.isNotEmpty()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_add_to_list)
                    )
                }
            }
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
                        Text(stringResource(attraction.category.displayNameRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                        if (state.showDistanceFromSearch) {
                            attraction.distanceKm?.let { dist ->
                                Text(stringResource(R.string.distance_label, formatDistance(dist)), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        when {
                            attraction.description != null -> {
                                Text(stringResource(R.string.description_section), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(attraction.description ?: "", style = MaterialTheme.typography.bodyMedium)
                                if (attraction.descriptionSources.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.sources_label), style = MaterialTheme.typography.labelMedium)
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
                                    ) { Text(stringResource(R.string.reload_description)) }
                                }
                            }
                            state.isDescriptionLoading -> {
                                Row {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(R.string.generating_description), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            state.descriptionError != null -> {
                                Text(state.descriptionError!!, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = { viewModel.loadDescription() }) { Text(stringResource(R.string.try_again)) }
                            }
                            else -> {
                                Button(onClick = { viewModel.loadDescription() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.load_description))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

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
                            Text(stringResource(R.string.navigate_to))
                        }

                        OutlinedButton(
                            onClick = {
                                val coords = "${attraction.latitude},${attraction.longitude}"
                                clipboardManager.setText(AnnotatedString(coords))
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.copied, coords))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.copy_location))
                        }

                        onShowOnMap?.let { callback ->
                            OutlinedButton(
                                onClick = { callback(attraction.latitude, attraction.longitude, attraction.xid) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.show_on_map))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSelectionSheet(
    tripLists: List<TripList>,
    selectedIds: Set<Long>,
    sheetState: androidx.compose.material3.SheetState,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.add_to_list_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()

            if (tripLists.isEmpty()) {
                Text(
                    stringResource(R.string.no_lists_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                tripLists.forEach { list ->
                    val isSelected = list.id in selectedIds
                    ListItem(
                        headlineContent = { Text(list.name) },
                        supportingContent = { Text(stringResource(R.string.attractions_count, list.attractionCount)) },
                        leadingContent = {
                            Icon(
                                if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable { onToggle(list.id) }
                    )
                }
            }

            HorizontalDivider()

            if (showCreateField) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text(stringResource(R.string.new_list_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { showCreateField = false; newListName = "" }) { Text(stringResource(R.string.cancel)) }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                onCreate(newListName)
                                showCreateField = false
                                newListName = ""
                            },
                            enabled = newListName.isNotBlank()
                        ) { Text(stringResource(R.string.create_and_add)) }
                    }
                }
            } else {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.create_new_list)) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.clickable { showCreateField = true }
                )
            }
        }
    }
}
