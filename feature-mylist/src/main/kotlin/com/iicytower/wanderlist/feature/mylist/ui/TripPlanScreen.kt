package com.iicytower.wanderlist.feature.mylist.ui

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.domain.model.TripDay
import com.iicytower.wanderlist.feature.mylist.R
import com.iicytower.wanderlist.feature.mylist.viewmodel.TripPlanViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlanScreen(
    listId: Long,
    listName: String,
    onBack: () -> Unit,
    onDiscussWithAssistant: () -> Unit,
    viewModel: TripPlanViewModel = koinViewModel()
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

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.delete_plan_title)) },
            text = { Text(stringResource(R.string.delete_plan_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete(onBack) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_title, listName)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back)) }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val plan = uiState.plan
        if (plan == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_plan), style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            items(plan.days) { day ->
                DayCard(day)
            }

            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text(stringResource(R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                Button(
                    onClick = { viewModel.saveNotes() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.save_notes)) }
            }

            if (uiState.canRevert) {
                item {
                    Button(
                        onClick = { viewModel.revertPlan() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.revert_plan)) }
                }
            }

            item {
                Button(
                    onClick = onDiscussWithAssistant,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.discuss_with_assistant)) }
            }

            item {
                Button(
                    onClick = { viewModel.requestDelete() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text(stringResource(R.string.delete_plan_title)) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DayCard(day: TripDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(day.label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            day.points.forEachIndexed { index, point ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${index + 1}. ${point.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!point.note.isNullOrBlank()) {
                    Text(
                        point.note ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}
