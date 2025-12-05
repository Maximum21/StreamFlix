package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.Flow
import com.asadraza.streamflix.core.common.result.Result

/**
 * Repository interface for profile management.
 * Implementation uses Firebase Firestore for cloud storage
 * and DataStore for local current profile selection.
 *
 * All operations return Flow<Result<T>> for:
 * - Reactive updates
 * - Consistent error handling
 * - Loading states
 * - Stale data support
 */
interface ProfileRepository {
    /**
     * Observe all profiles for the current user from Firestore.
     * Emits updates when profiles change in real-time.
     *
     * @return Flow emitting Result with profile list, loading, or error states
     */
    fun getProfiles(): Flow<Result<List<Profile>>>

    /**
     * Save or update a profile in Firestore.
     *
     * @param profile Profile to save
     * @return Flow emitting Result with success or error
     */
    fun saveProfile(profile: Profile): Flow<Result<Unit>>

    /**
     * Delete a profile from Firestore.
     *
     * @param profileId ID of profile to delete
     * @return Flow emitting Result with success or error
     */
    fun deleteProfile(profileId: String): Flow<Result<Unit>>

    /**
     * Set the currently active profile (stored in DataStore).
     *
     * @param profileId ID of profile to set as current
     * @return Flow emitting Result with success or error
     */
    fun setCurrentProfile(profileId: String): Flow<Result<Unit>>

    /**
     * Observe the currently active profile.
     * Emits updates when current profile changes or profile data updates.
     *
     * @return Flow emitting Result with current profile or null if none selected
     */
    fun getCurrentProfile(): Flow<Result<Profile?>>
}