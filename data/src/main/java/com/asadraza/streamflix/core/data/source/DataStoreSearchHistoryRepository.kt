package com.asadraza.streamflix.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asadraza.streamflix.core.domain.repository.SearchHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keeps the latest [MAX_ENTRIES] distinct queries in one preference, newest first. */
class DataStoreSearchHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SearchHistoryRepository {
    override val history: Flow<List<String>> = dataStore.data.map { it.decode() }

    override suspend fun add(query: String) {
        val cleaned = query.trim().replace(SEPARATOR, ' ')
        if (cleaned.isEmpty()) return
        dataStore.edit { prefs ->
            val updated = listOf(cleaned) + prefs.decode().filterNot { it.equals(cleaned, ignoreCase = true) }
            prefs[KEY_HISTORY] = updated.take(MAX_ENTRIES).joinToString(SEPARATOR.toString())
        }
    }

    override suspend fun remove(query: String) {
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = prefs.decode().filterNot { it == query }.joinToString(SEPARATOR.toString())
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY_HISTORY) }
    }

    private fun Preferences.decode(): List<String> = this[KEY_HISTORY].orEmpty().split(
        SEPARATOR
    ).filter { it.isNotEmpty() }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("search_history")
        const val SEPARATOR = '\n'
        const val MAX_ENTRIES = 10
    }
}
