package com.asadraza.streamflix.core.data.source

import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.Flow

/**
 * Port over the remote profile store for the signed-in user. All members throw
 * [UnauthenticatedException] when nobody is signed in.
 */
interface ProfileDataSource {
    fun observeProfiles(): Flow<List<Profile>>

    fun observeProfile(profileId: String): Flow<Profile?>

    /** Creates a profile with a server-generated id; throws [ProfileLimitReachedException] past the limit. */
    suspend fun create(profile: Profile): Profile

    suspend fun update(profile: Profile)

    /** Throws [LastProfileException] if it is the only profile. */
    suspend fun delete(profileId: String)

    companion object {
        const val MAX_PROFILES = Profile.MAX_PROFILES
    }
}
