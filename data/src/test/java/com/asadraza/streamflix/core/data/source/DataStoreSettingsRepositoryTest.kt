package com.asadraza.streamflix.core.data.source

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.asadraza.streamflix.core.model.settings.ThemeMode
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository() = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "settings.preferences_pb") }
    )

    @Test
    fun `consent is unknown until the user answers`() = runTest(UnconfinedTestDispatcher()) {
        assertNull(repository().analyticsConsent.first())
    }

    @Test
    fun `the answer is stored and can be changed`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        repository.setAnalyticsConsent(true)
        assertEquals(true, repository.analyticsConsent.first())

        repository.setAnalyticsConsent(false)
        assertEquals(false, repository.analyticsConsent.first())
    }

    @Test
    fun `the theme follows the system until chosen and is then stored`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        assertEquals(ThemeMode.System, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, repository.themeMode.first())
    }
}
