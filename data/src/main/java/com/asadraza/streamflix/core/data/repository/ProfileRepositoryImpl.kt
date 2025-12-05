package com.asadraza.streamflix.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asadraza.streamflix.core.common.result.ErrorMapper
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import com.asadraza.streamflix.core.common.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.compareTo
import kotlin.text.get

/**
 * Implementation of ProfileRepository using Firebase Firestore for storage.
 *
 * Architecture:
 * - Firestore: Cloud storage for profile data with real-time updates
 * - DataStore: Local storage for current profile selection
 * - Auth: User authentication for multi-user support
 *
 * Collection structure:
 * /users/{userId}/profiles/{profileId}
 *
 * CONSISTENT PATTERN: All methods return Flow<Result<T>> for:
 * - Real-time updates via Firestore snapshots
 * - Loading states during operations
 * - Error handling with ErrorType
 * - Stale data support (cached data during errors)
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>
) : ProfileRepository {
    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_PROFILES = "profiles"
        const val MAX_PROFILES = 5
        val KEY_CURRENT_PROFILE = stringPreferencesKey("current_profile_id")
    }

    /**
     * Observes all profiles with real-time Firestore updates.
     *
     * Flow emissions:
     * 1. Loading(null) - Initial load
     * 2. Loading(cachedData) - If cached data available
     * 3. Success(profiles) - Profiles loaded
     * 4. Error(exception, cachedData) - Error with stale data if available
     */
    override fun getProfiles(): Flow<Result<List<Profile>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            send(Result.Error(
                exception = IllegalStateException("User not authenticated"),
                type = ErrorType.Authorization("User not authenticated")
            ))
            close()
            return@callbackFlow
        }

        // Send loading state
        send(Result.Loading(null))

        var lastKnownProfiles: List<Profile>? = null
        var listenerRegistration: ListenerRegistration? = null

        try {
            // Set up real-time listener
            listenerRegistration = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Send error with stale data if available
                        trySend(Result.Error(
                            exception = error,
                            type = ErrorMapper.map(error),
                            data = lastKnownProfiles
                        ))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val profiles = snapshot.documents.mapNotNull { doc ->
                            try {
                                Profile(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    avatarUrl = doc.getString("avatarUrl") ?: "",
                                    isKidsProfile = doc.getBoolean("isKidsProfile") ?: false,
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null // Skip malformed profiles
                            }
                        }.sortedBy { it.createdAt }

                        lastKnownProfiles = profiles
                        trySend(Result.Success(profiles))
                    }
                }
        } catch (e: Exception) {
            send(Result.Error(
                exception = e,
                type = ErrorMapper.map(e),
                data = lastKnownProfiles
            ))
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Saves a profile with loading and error states.
     *
     * Flow emissions:
     * 1. Loading(null) - Save in progress
     * 2. Success(Unit) - Profile saved successfully
     * 3. Error(exception) - Save failed
     */
    override fun saveProfile(profile: Profile): Flow<Result<Unit>> = flow {
        emit(Result.Loading(null))

        val userId = auth.currentUser?.uid
        if (userId == null) {
            emit(Result.Error(
                exception = IllegalStateException("User not authenticated"),
                type = ErrorType.Authorization("User not authenticated")
            ))
            return@flow
        }

        try {
            // Check max profiles limit
            val currentProfilesSnapshot = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .get()
                .await()

            val isNewProfile = currentProfilesSnapshot.documents.none { it.id == profile.id }
            if (isNewProfile && currentProfilesSnapshot.size() >= MAX_PROFILES) {
                emit(Result.Error(
                    exception = IllegalStateException("Maximum $MAX_PROFILES profiles reached"),
                    type = ErrorType.Validation("Maximum $MAX_PROFILES profiles reached")
                ))
                return@flow
            }

            // Save profile
            val profileData = hashMapOf(
                "name" to profile.name,
                "avatarUrl" to profile.avatarUrl,
                "isKidsProfile" to profile.isKidsProfile,
                "createdAt" to profile.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .document(profile.id)
                .set(profileData)
                .await()

            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }
    /**
     * Observes the current profile with real-time updates.
     *
     * Flow emissions:
     * 1. Loading(null) - Initial load
     * 2. Success(profile) - Current profile loaded
     * 3. Success(null) - No profile selected
     * 4. Error(exception) - Load failed
     *
     * Updates automatically when:
     * - Current profile selection changes (DataStore)
     * - Profile data changes (Firestore)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentProfile(): Flow<Result<Profile?>> = dataStore.data
        .map { it[KEY_CURRENT_PROFILE] }
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            if (profileId == null) {
                flowOf(Result.Success(null))
            } else {
                observeProfileById(profileId)
            }
        }


    /**
     * Observes a specific profile by ID with real-time Firestore updates.
     */
    private fun observeProfileById(profileId: String): Flow<Result<Profile?>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            send(Result.Error(
                exception = IllegalStateException("User not authenticated"),
                type = ErrorType.Authorization("User not authenticated")
            ))
            close()
            return@callbackFlow
        }

        send(Result.Loading(null))

        var listenerRegistration: ListenerRegistration? = null

        try {
            listenerRegistration = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .document(profileId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(
                            exception = error,
                            type = ErrorMapper.map(error)
                        ))
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val profile = Profile(
                                id = snapshot.id,
                                name = snapshot.getString("name") ?: "",
                                avatarUrl = snapshot.getString("avatarUrl") ?: "",
                                isKidsProfile = snapshot.getBoolean("isKidsProfile") ?: false,
                                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                            trySend(Result.Success(profile))
                        } catch (e: Exception) {
                            trySend(Result.Error(
                                exception = e,
                                type = ErrorMapper.map(e)
                            ))
                        }
                    } else {
                        // Profile deleted, clear selection
                        trySend(Result.Success(null))
                    }
                }
        } catch (e: Exception) {
            send(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Deletes a profile with loading and error states.
     *
     * Flow emissions:
     * 1. Loading(null) - Delete in progress
     * 2. Success(Unit) - Profile deleted successfully
     * 3. Error(exception) - Delete failed
     */
    override fun deleteProfile(profileId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading(null))

        val userId = auth.currentUser?.uid
        if (userId == null) {
            emit(Result.Error(
                exception = IllegalStateException("User not authenticated"),
                type = ErrorType.Authorization("User not authenticated")
            ))
            return@flow
        }

        try {
            // Check if it's the last profile
            val profilesSnapshot = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .get()
                .await()

            if (profilesSnapshot.size() <= 1) {
                emit(Result.Error(
                    exception = IllegalStateException("Cannot delete last profile"),
                    type = ErrorType.Validation("Cannot delete last profile")
                ))
                return@flow
            }

            // Delete profile
            firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .document(profileId)
                .delete()
                .await()

            // Clear current profile if it was deleted
            val currentProfileId = dataStore.data
                .map { it[KEY_CURRENT_PROFILE] }
                .first()

            if (currentProfileId == profileId) {
                dataStore.edit { it.remove(KEY_CURRENT_PROFILE) }
            }

            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }

    /**
     * Sets current profile with loading and error states.
     *
     * Flow emissions:
     * 1. Loading(null) - Setting in progress
     * 2. Success(Unit) - Profile set successfully
     * 3. Error(exception) - Set failed
     */
    override fun setCurrentProfile(profileId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading(null))

        try {
            dataStore.edit { preferences ->
                preferences[KEY_CURRENT_PROFILE] = profileId
            }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }
}

