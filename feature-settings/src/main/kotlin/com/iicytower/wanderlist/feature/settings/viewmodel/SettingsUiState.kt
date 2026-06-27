package com.iicytower.wanderlist.feature.settings.viewmodel

import com.iicytower.wanderlist.domain.model.AppSettings

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILURE }

data class SettingsUiState(
    val settings: AppSettings? = null,
    val isLoading: Boolean = false,
    val openRouterTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val tavilyTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val openRouterKeyVisible: Boolean = false,
    val tavilyKeyVisible: Boolean = false
)
