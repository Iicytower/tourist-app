package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateOpenRouterApiKey(key: String)
    suspend fun updateTavilyApiKey(key: String)
    suspend fun updateAiModel(model: String)
    suspend fun updateDefaultRadius(radiusKm: Int)
    suspend fun updateDescriptionLanguage(language: String)
    suspend fun updateUserInterests(interests: Set<AttractionCategory>)
    suspend fun updateSystemPromptDescription(prompt: String)
    suspend fun updateSystemPromptAssistant(prompt: String)
    suspend fun incrementTavilyUsage()
    suspend fun resetTavilyUsageIfNewMonth()
}
