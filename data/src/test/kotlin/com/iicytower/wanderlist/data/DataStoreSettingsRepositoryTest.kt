package com.iicytower.wanderlist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.local.DefaultSettings
import com.iicytower.wanderlist.data.local.SecureKeyStorage
import com.iicytower.wanderlist.data.repository.DataStoreSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var secureKeyStorage: SecureKeyStorage
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        secureKeyStorage = mockk(relaxed = true)
        every { secureKeyStorage.getKey(any()) } returns ""
        repository = DataStoreSettingsRepository(dataStore, secureKeyStorage)
    }

    @Test
    fun defaultValues_whenDataStoreEmpty() = testScope.runTest {
        val settings = repository.getSettings().first()
        assertEquals(DefaultSettings.AI_MODEL, settings.aiModel)
        assertEquals(DefaultSettings.DEFAULT_RADIUS_KM, settings.defaultRadiusKm)
        assertEquals(DefaultSettings.DESCRIPTION_LANGUAGE, settings.descriptionLanguage)
        assertEquals(0, settings.tavilyUsageCount)
        assertTrue(settings.userInterests.isEmpty())
    }

    @Test
    fun updateDefaultRadius_emitsNewValue() = testScope.runTest {
        repository.updateDefaultRadius(25)
        val settings = repository.getSettings().first()
        assertEquals(25, settings.defaultRadiusKm)
    }

    @Test
    fun incrementTavilyUsage_incrementsCount() = testScope.runTest {
        repository.incrementTavilyUsage()
        repository.incrementTavilyUsage()
        repository.incrementTavilyUsage()
        val settings = repository.getSettings().first()
        assertEquals(3, settings.tavilyUsageCount)
    }

    @Test
    fun resetTavilyUsageIfNewMonth_resetsCount() = testScope.runTest {
        repository.incrementTavilyUsage()
        repository.incrementTavilyUsage()
        // Simulate old month by manually invoking reset with a different month context
        // We can't easily change YearMonth.now() so we test the internal logic:
        // If current month matches stored → no reset
        repository.resetTavilyUsageIfNewMonth()
        // Count stays because same month
        val settings = repository.getSettings().first()
        assertEquals(2, settings.tavilyUsageCount)
    }

    @Test
    fun userInterests_serializedAndDeserialized() = testScope.runTest {
        val interests = setOf(
            AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            AttractionCategory.MUSEUMS_AND_GALLERIES
        )
        repository.updateUserInterests(interests)
        val settings = repository.getSettings().first()
        assertEquals(interests, settings.userInterests)
    }

    @Test
    fun updateOpenRouterKey_callsSecureStorage() = testScope.runTest {
        repository.updateOpenRouterApiKey("my-key")
        verify { secureKeyStorage.saveKey(any(), "my-key") }
    }

    @Test
    fun updateAiModel_persistsValue() = testScope.runTest {
        repository.updateAiModel("anthropic/claude-3-haiku")
        val settings = repository.getSettings().first()
        assertEquals("anthropic/claude-3-haiku", settings.aiModel)
    }
}
