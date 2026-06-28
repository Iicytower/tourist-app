package com.iicytower.wanderlist.feature.mylist.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.util.formatDistance
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.feature.mylist.viewmodel.MyListSortOrder
import com.iicytower.wanderlist.feature.mylist.viewmodel.MyListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onAttractionClick: (String) -> Unit = {},
    viewModel: MyListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.confirmDeleteXid != null) {
        val xid = uiState.confirmDeleteXid!!
        val name = uiState.attractions.find { it.xid == xid }?.name ?: xid
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Usun atrakcje") },
            text = { Text("Czy na pewno chcesz usunac \"$name\" z listy?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) { Text("Usun") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Moja Lista (${uiState.attractions.size}/${AppConstants.MY_LIST_MAX_SIZE})")
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sortuj")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            MyListSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(sortOrderLabel(order)) },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.attractions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Twoja lista jest pusta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(uiState.attractions, key = { it.xid }) { attraction ->
                    MyListItem(
                        attraction = attraction,
                        onClick = { onAttractionClick(attraction.xid) },
                        onDelete = { viewModel.requestDelete(attraction.xid) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun MyListItem(
    attraction: Attraction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(attraction.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    attraction.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attraction.distanceKm != null) {
                    Text(
                        formatDistance(attraction.distanceKm ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usun",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun sortOrderLabel(order: MyListSortOrder) = when (order) {
    MyListSortOrder.DATE_ADDED -> "Data dodania"
    MyListSortOrder.DISTANCE -> "Odleglosc"
    MyListSortOrder.NAME -> "Nazwa"
    MyListSortOrder.CATEGORY -> "Kategoria"
}
