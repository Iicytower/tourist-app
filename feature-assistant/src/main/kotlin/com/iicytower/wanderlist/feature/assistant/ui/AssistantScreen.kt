package com.iicytower.wanderlist.feature.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.feature.assistant.viewmodel.AssistantViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        val itemCount = uiState.messages.size + if (uiState.isProcessing) 1 else 0
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (uiState.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearConfirmation() },
            title = { Text("Wyczysc czat") },
            text = { Text("Czy na pewno chcesz wyczysc historie rozmowy?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearChat() }) { Text("Wyczysc") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearConfirmation() }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asystent") },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        enabled = uiState.messages.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Wyczysc czat")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }
                if (uiState.isProcessing) {
                    if (uiState.streamingText.isNotEmpty()) {
                        item { AssistantBubble(uiState.streamingText + " |") }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.currentInput,
                    onValueChange = { viewModel.updateInput(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Napisz wiadomosc...") },
                    enabled = !uiState.isProcessing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.currentInput.isNotBlank() && !uiState.isProcessing
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Wyslij")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message) {
        is ChatMessage.User -> UserBubble(message.text)
        is ChatMessage.Assistant -> AssistantBubble(message.text)
        is ChatMessage.Error -> ErrorBubble(message.message)
        is ChatMessage.ToolResult -> Unit
        is ChatMessage.AssistantWithToolCalls -> Unit
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                )
                .padding(12.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .padding(12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = "Blad: $text",
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
