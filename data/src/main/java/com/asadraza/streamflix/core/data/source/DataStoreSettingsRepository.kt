package com.asadraza.streamflix.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asadraza.streamflix.core.domain.repository.SettingsRepository
import com.asadraza.streamflix.core.model.settings.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    override val analyticsConsent: Flow<Boolean?> = dataStore.data.map { it[KEY_ANALYTICS_CONSENT] }

    override suspend fun setAnalyticsConsent(granted: Boolean) {
        dataStore.edit { it[KEY_ANALYTICS_CONSENT] = granted }
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        // An unknown stored value (e.g. from a newer version) falls back to following the system
        ThemeMode.entries.firstOrNull { it.name == prefs[KEY_THEME_MODE] } ?: ThemeMode.System
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ANALYTICS_CONSENT = booleanPreferencesKey("analytics_consent")
    }
}
