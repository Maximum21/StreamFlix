package com.asadraza.streamflix.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreCurrentProfileStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : CurrentProfileStore {
    override val currentProfileId: Flow<String?> = dataStore.data.map { it[KEY_CURRENT_PROFILE] }

    override suspend fun set(profileId: String) {
        dataStore.edit { it[KEY_CURRENT_PROFILE] = profileId }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY_CURRENT_PROFILE) }
    }

    private companion object {
        val KEY_CURRENT_PROFILE = stringPreferencesKey("current_profile_id")
    }
}
