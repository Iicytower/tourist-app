package com.iicytower.wanderlist.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object PreferencesKeys {
    val AI_MODEL = stringPreferencesKey("ai_model")
    val DEFAULT_RADIUS_KM = intPreferencesKey("default_radius_km")
    val DESCRIPTION_LANGUAGE = stringPreferencesKey("description_language")
    val USER_INTERESTS = stringSetPreferencesKey("user_interests")
    val SYSTEM_PROMPT_DESCRIPTION = stringPreferencesKey("system_prompt_description")
    val SYSTEM_PROMPT_ASSISTANT = stringPreferencesKey("system_prompt_assistant")
    val TAVILY_USAGE_COUNT = intPreferencesKey("tavily_usage_count")
    val TAVILY_USAGE_MONTH = stringPreferencesKey("tavily_usage_month")
    val LAST_SEARCH_LATITUDE = floatPreferencesKey("last_search_latitude")
    val LAST_SEARCH_LONGITUDE = floatPreferencesKey("last_search_longitude")
    val LAST_SEARCH_RADIUS_KM = intPreferencesKey("last_search_radius_km")
}
