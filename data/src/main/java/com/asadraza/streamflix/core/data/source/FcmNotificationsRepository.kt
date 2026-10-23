package com.asadraza.streamflix.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDataErrorType
import com.asadraza.streamflix.core.domain.repository.NotificationsRepository
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Opting in subscribes this install to a topic; the server sends announcements to the topic. */
class FcmNotificationsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val messaging: FirebaseMessaging
) : NotificationsRepository {
    override val enabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] == true }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = try {
        if (enabled) {
            messaging.subscribeToTopic(TOPIC_NEW_RELEASES).await()
        } else {
            messaging.unsubscribeFromTopic(TOPIC_NEW_RELEASES).await()
        }
        dataStore.edit { it[KEY_ENABLED] = enabled }
        Result.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Result.Error(failure, failure.toDataErrorType())
    }

    companion object {
        const val TOPIC_NEW_RELEASES = "new_releases"
        private val KEY_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
