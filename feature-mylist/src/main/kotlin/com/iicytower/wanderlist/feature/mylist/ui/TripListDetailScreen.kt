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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.displayNameRes
import com.iicytower.wanderlist.core.ui.AttractionImage
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.feature.mylist.R
import com.iicytower.wanderlist.feature.mylist.viewmodel.TripListDetailViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListDetailScreen(
    listId: Long,
    onBack: () -> Unit = {},
    onAttractionClick: (String) -> Unit = {},
    onShowPlan: (Long, String) -> Unit = { _, _ -> },
    viewModel: TripListDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listId) { viewModel.load(listId) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.navigateToPlan) {
        if (uiState.navigateToPlan) {
            val list = uiState.tripList ?: return@LaunchedEffect
            viewModel.onPlanNavigated()
            onShowPlan(list.id, list.name)
        }
    }

    if (uiState.confirmRemoveXid != null) {
        val xid = uiState.confirmRemoveXid!!
        val name = uiState.attractions.find { it.xid == xid }?.name ?: xid
        AlertDialog(
            onDismissRequest = { viewModel.cancelRemove() },
            title = { Text(stringResource(R.string.remove_attraction_title)) },
            text = { Text(stringResource(R.string.remove_attraction_message, name)) },
            confirmButton = { TextButton(onClick = { viewModel.confirmRemove() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { viewModel.cancelRemove() }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = uiState.tripList?.name ?: stringResource(R.string.list_fallback)
                    val count = uiState.attractions.size
                    Text("$title ($count/${AppConstants.MY_LIST_MAX_SIZE})")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back)) }
                },
                actions = {
                    if (uiState.isGeneratingPlan) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.onPlanButtonClick() }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.cd_plan_trip))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.attractions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.empty_list),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(uiState.attractions, key = { it.xid }) { attraction ->
                    AttractionListItem(
                        attraction = attraction,
                        onClick = { onAttractionClick(attraction.xid) },
                        onDelete = { viewModel.requestRemove(attraction.xid) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun AttractionListItem(
    attraction: Attraction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttractionImage(
                imageUrl = attraction.imageUrl,
                category = attraction.category,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(attraction.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(attraction.category.displayNameRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
