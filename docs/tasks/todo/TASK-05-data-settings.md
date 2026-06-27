# TASK-05: Moduł `data` — ustawienia (DataStore + szyfrowanie kluczy API)

## Cel
Implementacja przechowywania ustawień aplikacji: klucze API szyfrowane przez EncryptedSharedPreferences (Android Keystore), pozostałe ustawienia w DataStore Preferences.

## Zależności
- TASK-03 (interfejs `SettingsRepository`)
- TASK-02 (modele `AttractionCategory`)

## Zakres

### 1. Szyfrowane przechowywanie kluczy API (`data/local/SecureKeyStorage.kt`)

Klucze API (OpenRouter, Tavily) przechowywane w `EncryptedSharedPreferences`:

```kotlin
class SecureKeyStorage(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(key: String, value: String)
    fun getKey(key: String): String
    fun clearKey(key: String)
}
```

Biblioteka: `androidx.security:security-crypto` (bez Google Play Services — klucze w Android Keystore System).

### 2. DataStore Preferences (`data/local/AppDataStore.kt`)

DataStore przechowuje wszystkie ustawienia **poza** kluczami API:

```kotlin
object PreferencesKeys {
    val AI_MODEL = stringPreferencesKey("ai_model")
    val DEFAULT_RADIUS_KM = intPreferencesKey("default_radius_km")
    val DESCRIPTION_LANGUAGE = stringPreferencesKey("description_language")
    val USER_INTERESTS = stringSetPreferencesKey("user_interests")     // Set<String> nazw enum
    val SYSTEM_PROMPT_DESCRIPTION = stringPreferencesKey("system_prompt_description")
    val SYSTEM_PROMPT_ASSISTANT = stringPreferencesKey("system_prompt_assistant")
    val TAVILY_USAGE_COUNT = intPreferencesKey("tavily_usage_count")
    val TAVILY_USAGE_MONTH = stringPreferencesKey("tavily_usage_month")   // format "YYYY-MM"
    val LAST_SEARCH_LATITUDE = floatPreferencesKey("last_search_latitude")    // Double jako Float (DataStore)
    val LAST_SEARCH_LONGITUDE = floatPreferencesKey("last_search_longitude")
    val LAST_SEARCH_RADIUS_KM = intPreferencesKey("last_search_radius_km")
}
```

### 3. Domyślne wartości ustawień (`data/local/DefaultSettings.kt`)

```kotlin
object DefaultSettings {
    const val AI_MODEL = "google/gemini-2.5-flash-lite"   // tanie, szybkie; do rewizji po MVP
    const val DEFAULT_RADIUS_KM = 10
    const val DESCRIPTION_LANGUAGE = "pl"
    // System prompty — dosłownie z sekcji 29 specyfikacji technicznej
    const val SYSTEM_PROMPT_DESCRIPTION = """..."""
    const val SYSTEM_PROMPT_ASSISTANT = """..."""
}
```

### 4. Implementacja repozytorium (`data/repository/DataStoreSettingsRepository.kt`)

Implementuje `SettingsRepository` z modułu `domain`.

- `getSettings()` — łączy wartości z DataStore i `SecureKeyStorage` w jeden `Flow<AppSettings>`
- Klucze API odczytywane z `SecureKeyStorage` za każdym razem gdy Flow emituje (DataStore nie przechowuje kluczy)
- `incrementTavilyUsage()` — atomicznie inkrementuje licznik; przed inkrementacją wywołuje `resetTavilyUsageIfNewMonth()`
- `resetTavilyUsageIfNewMonth()` — odczytuje `TAVILY_USAGE_MONTH`, porównuje z aktualnym miesiącem (`"YYYY-MM"`), jeśli różny — zeruje licznik i aktualizuje miesiąc
- `userInterests` serializowane jako `Set<String>` (nazwy enum `AttractionCategory`) → deserializowane przez `enumValueOf<AttractionCategory>()`

### 5. Koin module (`data/local/SettingsModule.kt`)

```kotlin
val settingsModule = module {
    single { SecureKeyStorage(androidContext()) }
    single { androidContext().dataStore }   // extension property na Context
    single<SettingsRepository> { DataStoreSettingsRepository(get(), get()) }
}
```

## Testy

**`data/test/`**:

- `DataStoreSettingsRepositoryTest`:
  - Odczyt wartości domyślnych gdy DataStore pusty
  - `updateDefaultRadius(25)` → `getSettings()` emituje `AppSettings` z `defaultRadiusKm = 25`
  - `incrementTavilyUsage()` wielokrotnie → poprawny licznik
  - `resetTavilyUsageIfNewMonth()` przy zmianie miesiąca → licznik zerowany
  - Serializacja/deserializacja `userInterests` (Set<AttractionCategory>)

Uwaga: `SecureKeyStorage` trudny do testowania bez urządzenia (Android Keystore) — testy pokrywają tylko warstwę DataStore, `SecureKeyStorage` mockowane przez MockK.

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi (testy DataStore)
- `DataStoreSettingsRepository` implementuje cały interfejs `SettingsRepository`
- Klucze API nie są przechowywane w DataStore (tylko w `EncryptedSharedPreferences`)
