package com.iicytower.wanderlist.feature.settings.ui

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.iicytower.wanderlist.core.constant.AppConstants
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.feature.settings.viewmodel.ConnectionTestState
import com.iicytower.wanderlist.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    val view = LocalView.current
    SideEffect {
        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }

    uiState.openRouterTestError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearOpenRouterTestError() },
            title = { Text("Blad polaczenia OpenRouter") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearOpenRouterTestError() }) { Text("OK") }
            }
        )
    }

    uiState.tavilyTestError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearTavilyTestError() },
            title = { Text("Blad polaczenia Tavily") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearTavilyTestError() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ustawienia") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // === API Keys ===
            item { SectionHeader("Klucze API") }

            item {
                ApiKeySection(
                    label = "OpenRouter API Key",
                    value = settings?.openRouterApiKey ?: "",
                    testState = uiState.openRouterTestState,
                    testError = uiState.openRouterTestError,
                    onValueChange = { viewModel.updateOpenRouterKey(it) },
                    onTest = { viewModel.testOpenRouterConnection(it) }
                )
            }

            item {
                ApiKeySection(
                    label = "Tavily API Key",
                    value = settings?.tavilyApiKey ?: "",
                    testState = uiState.tavilyTestState,
                    testError = uiState.tavilyTestError,
                    onValueChange = { viewModel.updateTavilyKey(it) },
                    onTest = { viewModel.testTavilyConnection(it) }
                )
                if (settings != null) {
                    Text(
                        "Wykorzystano ${settings.tavilyUsageCount} / 1000 zapytan w tym miesiacu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // === AI Model ===
            item { SectionHeader("Model AI") }

            item {
                var modelInput by remember(settings?.aiModel) { mutableStateOf(settings?.aiModel ?: "") }
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("Model AI") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Modele dostepne na openrouter.ai/models") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                if (modelInput != settings?.aiModel && modelInput.isNotBlank()) {
                    Button(onClick = { viewModel.updateAiModel(modelInput) }) {
                        Text("Zapisz model")
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // === System Prompts ===
            item { SectionHeader("System Prompty") }

            item {
                var descPrompt by remember(settings?.systemPromptDescription) {
                    mutableStateOf(settings?.systemPromptDescription ?: "")
                }
                Column {
                    OutlinedTextField(
                        value = descPrompt,
                        onValueChange = { descPrompt = it },
                        label = { Text("Agent opisow") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (descPrompt != settings?.systemPromptDescription) {
                            Button(onClick = { viewModel.updateSystemPromptDescription(descPrompt) }) {
                                Text("Zapisz")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.resetSystemPromptDescription() }) {
                            Text("Przywroc domyslny")
                        }
                    }
                }
            }

            item {
                var assistantPrompt by remember(settings?.systemPromptAssistant) {
                    mutableStateOf(settings?.systemPromptAssistant ?: "")
                }
                Column {
                    OutlinedTextField(
                        value = assistantPrompt,
                        onValueChange = { assistantPrompt = it },
                        label = { Text("Asystent") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (assistantPrompt != settings?.systemPromptAssistant) {
                            Button(onClick = { viewModel.updateSystemPromptAssistant(assistantPrompt) }) {
                                Text("Zapisz")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.resetSystemPromptAssistant() }) {
                            Text("Przywroc domyslny")
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // === Interests ===
            item { SectionHeader("Zainteresowania") }

            item {
                val currentInterests = settings?.userInterests ?: emptySet()
                Column {
                    AttractionCategory.values().forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = category in currentInterests,
                                onCheckedChange = { checked ->
                                    val newSet = if (checked) {
                                        currentInterests + category
                                    } else {
                                        currentInterests - category
                                    }
                                    viewModel.updateInterests(newSet)
                                }
                            )
                            Text(category.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // === Preferences ===
            item { SectionHeader("Preferencje") }

            item {
                val languages = listOf("pl" to "Polski", "en" to "English", "de" to "Deutsch", "fr" to "Francais", "es" to "Espanol")
                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = languages.find { it.first == settings?.descriptionLanguage }?.second ?: "Polski"
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jezyk opisow") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.updateDescriptionLanguage(code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                val currentRadius = settings?.defaultRadiusKm?.toFloat() ?: AppConstants.DEFAULT_SEARCH_RADIUS_KM.toFloat()
                var sliderValue by remember(settings?.defaultRadiusKm) { mutableStateOf(currentRadius) }
                Column {
                    Text(
                        "Domyslny promien: ${sliderValue.toInt()} km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { viewModel.updateDefaultRadius(sliderValue.toInt()) },
                        valueRange = AppConstants.MIN_SEARCH_RADIUS_KM.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
                        steps = AppConstants.MAX_SEARCH_RADIUS_KM - AppConstants.MIN_SEARCH_RADIUS_KM - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ApiKeySection(
    label: String,
    value: String,
    testState: ConnectionTestState,
    testError: String? = null,
    onValueChange: (String) -> Unit,
    onTest: (String) -> Unit
) {
    // Lokalny stan — klucz API trafia do EncryptedSharedPreferences, nie do DataStore,
    // więc getSettings() flow nie re-emituje po zapisie. Bezpośrednie bindowanie value=
    // do settings cofałoby każdy keystroke. Wzorzec taki jak pole AI Model: edytuj lokalnie,
    // zapisz przyciskiem.
    var localValue by remember(value) { mutableStateOf(value) }
    val isDirty = localValue != value

    Column {
        OutlinedTextField(
            value = localValue,
            onValueChange = { localValue = it },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            if (isDirty) {
                Button(onClick = { onValueChange(localValue) }) {
                    Text("Zapisz")
                }
            }
            OutlinedButton(
                onClick = { onTest(localValue) },
                enabled = testState != ConnectionTestState.TESTING && localValue.isNotBlank()
            ) {
                Text("Przetestuj")
            }
            when (testState) {
                ConnectionTestState.TESTING -> CircularProgressIndicator(
                    modifier = Modifier.height(24.dp)
                )
                ConnectionTestState.SUCCESS -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Polaczenie OK",
                    tint = MaterialTheme.colorScheme.primary
                )
                ConnectionTestState.FAILURE -> Icon(
                    Icons.Default.Close,
                    contentDescription = "Blad polaczenia",
                    tint = MaterialTheme.colorScheme.error
                )
                ConnectionTestState.IDLE -> Unit
            }
        }
        if (testState == ConnectionTestState.FAILURE && testError != null) {
            Text(
                text = testError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

