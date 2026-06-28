package com.iicytower.wanderlist.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iicytower.wanderlist.core.constant.DefaultSettings
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val llmService: LlmService,
    private val webSearchService: WebSearchService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun updateOpenRouterKey(key: String) {
        viewModelScope.launch { settingsRepository.updateOpenRouterApiKey(key) }
    }

    fun updateTavilyKey(key: String) {
        viewModelScope.launch { settingsRepository.updateTavilyApiKey(key) }
    }

    fun updateAiModel(model: String) {
        viewModelScope.launch { settingsRepository.updateAiModel(model) }
    }

    fun updateDefaultRadius(radiusKm: Int) {
        viewModelScope.launch { settingsRepository.updateDefaultRadius(radiusKm) }
    }

    fun updateDescriptionLanguage(language: String) {
        viewModelScope.launch { settingsRepository.updateDescriptionLanguage(language) }
    }

    fun updateInterests(interests: Set<AttractionCategory>) {
        viewModelScope.launch { settingsRepository.updateUserInterests(interests) }
    }

    fun updateSystemPromptDescription(prompt: String) {
        viewModelScope.launch { settingsRepository.updateSystemPromptDescription(prompt) }
    }

    fun updateSystemPromptAssistant(prompt: String) {
        viewModelScope.launch { settingsRepository.updateSystemPromptAssistant(prompt) }
    }

    fun resetSystemPromptDescription() {
        updateSystemPromptDescription(DefaultSettings.SYSTEM_PROMPT_DESCRIPTION)
    }

    fun resetSystemPromptAssistant() {
        updateSystemPromptAssistant(DefaultSettings.SYSTEM_PROMPT_ASSISTANT)
    }

    fun toggleOpenRouterKeyVisibility() {
        _uiState.update { it.copy(openRouterKeyVisible = !it.openRouterKeyVisible) }
    }

    fun toggleTavilyKeyVisibility() {
        _uiState.update { it.copy(tavilyKeyVisible = !it.tavilyKeyVisible) }
    }

    fun testOpenRouterConnection(currentKeyInField: String) {
        _uiState.update { it.copy(openRouterTestState = ConnectionTestState.TESTING, openRouterTestError = null) }
        viewModelScope.launch {
            if (currentKeyInField.isNotBlank()) settingsRepository.updateOpenRouterApiKey(currentKeyInField)
            val result = llmService.testConnection()
            _uiState.update {
                it.copy(
                    openRouterTestState = if (result.isSuccess) ConnectionTestState.SUCCESS else ConnectionTestState.FAILURE,
                    openRouterTestError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun testTavilyConnection(currentKeyInField: String) {
        _uiState.update { it.copy(tavilyTestState = ConnectionTestState.TESTING, tavilyTestError = null) }
        viewModelScope.launch {
            if (currentKeyInField.isNotBlank()) settingsRepository.updateTavilyApiKey(currentKeyInField)
            val result = webSearchService.search("test")
            _uiState.update {
                it.copy(
                    tavilyTestState = if (result.isSuccess) ConnectionTestState.SUCCESS else ConnectionTestState.FAILURE,
                    tavilyTestError = result.exceptionOrNull()?.message ?: if (result.isFailure) "Nieznany blad Tavily" else null
                )
            }
        }
    }

    fun clearOpenRouterTestError() {
        _uiState.update { it.copy(openRouterTestError = null, openRouterTestState = ConnectionTestState.IDLE) }
    }

    fun clearTavilyTestError() {
        _uiState.update { it.copy(tavilyTestError = null, tavilyTestState = ConnectionTestState.IDLE) }
    }
}
