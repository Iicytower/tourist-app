package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()
}
