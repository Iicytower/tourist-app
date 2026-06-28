package com.iicytower.wanderlist.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.local.DefaultSettings
import com.iicytower.wanderlist.data.local.PreferencesKeys
import com.iicytower.wanderlist.data.local.SecureKeyStorage
import com.iicytower.wanderlist.domain.model.AppSettings
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

private const val KEY_OPENROUTER = "openrouter_api_key"
private const val KEY_TAVILY = "tavily_api_key"

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val secureKeyStorage: SecureKeyStorage
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            openRouterApiKey = secureKeyStorage.getKey(KEY_OPENROUTER),
            tavilyApiKey = secureKeyStorage.getKey(KEY_TAVILY),
            aiModel = prefs[PreferencesKeys.AI_MODEL] ?: DefaultSettings.AI_MODEL,
            defaultRadiusKm = prefs[PreferencesKeys.DEFAULT_RADIUS_KM] ?: DefaultSettings.DEFAULT_RADIUS_KM,
            descriptionLanguage = prefs[PreferencesKeys.DESCRIPTION_LANGUAGE] ?: DefaultSettings.DESCRIPTION_LANGUAGE,
            userInterests = prefs[PreferencesKeys.USER_INTERESTS]
                ?.mapNotNull { runCatching { AttractionCategory.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet(),
            systemPromptDescription = prefs[PreferencesKeys.SYSTEM_PROMPT_DESCRIPTION] ?: DefaultSettings.SYSTEM_PROMPT_DESCRIPTION,
            systemPromptAssistant = prefs[PreferencesKeys.SYSTEM_PROMPT_ASSISTANT] ?: DefaultSettings.SYSTEM_PROMPT_ASSISTANT,
            tavilyUsageCount = prefs[PreferencesKeys.TAVILY_USAGE_COUNT] ?: 0,
            tavilyUsageMonth = prefs[PreferencesKeys.TAVILY_USAGE_MONTH] ?: currentMonth()
        )
    }

    override suspend fun updateOpenRouterApiKey(key: String) {
        secureKeyStorage.saveKey(KEY_OPENROUTER, key)
        dataStore.edit { prefs -> prefs[PreferencesKeys.API_KEYS_VERSION] = (prefs[PreferencesKeys.API_KEYS_VERSION] ?: 0) + 1 }
    }

    override suspend fun updateTavilyApiKey(key: String) {
        secureKeyStorage.saveKey(KEY_TAVILY, key)
        dataStore.edit { prefs -> prefs[PreferencesKeys.API_KEYS_VERSION] = (prefs[PreferencesKeys.API_KEYS_VERSION] ?: 0) + 1 }
    }

    override suspend fun updateAiModel(model: String) {
        dataStore.edit { it[PreferencesKeys.AI_MODEL] = model }
    }

    override suspend fun updateDefaultRadius(radiusKm: Int) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_RADIUS_KM] = radiusKm }
    }

    override suspend fun updateDescriptionLanguage(language: String) {
        dataStore.edit { it[PreferencesKeys.DESCRIPTION_LANGUAGE] = language }
    }

    override suspend fun updateUserInterests(interests: Set<AttractionCategory>) {
        dataStore.edit { it[PreferencesKeys.USER_INTERESTS] = interests.map { c -> c.name }.toSet() }
    }

    override suspend fun updateSystemPromptDescription(prompt: String) {
        dataStore.edit { it[PreferencesKeys.SYSTEM_PROMPT_DESCRIPTION] = prompt }
    }

    override suspend fun updateSystemPromptAssistant(prompt: String) {
        dataStore.edit { it[PreferencesKeys.SYSTEM_PROMPT_ASSISTANT] = prompt }
    }

    override suspend fun incrementTavilyUsage() {
        resetTavilyUsageIfNewMonth()
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TAVILY_USAGE_COUNT] = (prefs[PreferencesKeys.TAVILY_USAGE_COUNT] ?: 0) + 1
        }
    }

    override suspend fun resetTavilyUsageIfNewMonth() {
        dataStore.edit { prefs ->
            val stored = prefs[PreferencesKeys.TAVILY_USAGE_MONTH]
            val current = currentMonth()
            if (stored != current) {
                prefs[PreferencesKeys.TAVILY_USAGE_COUNT] = 0
                prefs[PreferencesKeys.TAVILY_USAGE_MONTH] = current
            }
        }
    }

    override suspend fun getLastMapPosition(): Triple<Double, Double, Double>? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        val lat = prefs[PreferencesKeys.MAP_LAST_LAT] ?: return null
        val lon = prefs[PreferencesKeys.MAP_LAST_LON] ?: return null
        val zoom = prefs[PreferencesKeys.MAP_LAST_ZOOM] ?: return null
        return Triple(lat, lon, zoom)
    }

    override suspend fun updateLastMapPosition(lat: Double, lon: Double, zoom: Double) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.MAP_LAST_LAT] = lat
            prefs[PreferencesKeys.MAP_LAST_LON] = lon
            prefs[PreferencesKeys.MAP_LAST_ZOOM] = zoom
        }
    }

    private fun currentMonth(): String = YearMonth.now().toString()
}
