package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.source.CurrentProfileStore
import com.asadraza.streamflix.core.data.source.ProfileDataSource
import com.asadraza.streamflix.core.data.util.asResultFlow
import com.asadraza.streamflix.core.data.util.operationFlow
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

/**
 * Profiles are stored remotely per user; which one is active is a per-device choice kept locally.
 */
class ProfileRepositoryImpl
@Inject
constructor(
    private val profileDataSource: ProfileDataSource,
    private val currentProfileStore: CurrentProfileStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {
    override fun getProfiles(): Flow<Result<List<Profile>>> = profileDataSource.observeProfiles()
        .asResultFlow()
        .flowOn(ioDispatcher)

    override fun saveProfile(profile: Profile): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        if (profile.id == Profile.NEW_ID) {
            profileDataSource.create(profile)
        } else {
            profileDataSource.update(profile)
        }
        Result.Success(Unit)
    }

    override fun deleteProfile(profileId: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        profileDataSource.delete(profileId)
        if (currentProfileStore.currentProfileId.first() == profileId) {
            currentProfileStore.clear()
        }
        Result.Success(Unit)
    }

    override fun setCurrentProfile(profileId: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        currentProfileStore.set(profileId)
        Result.Success(Unit)
    }

    override suspend fun clearCurrentProfile() {
        currentProfileStore.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentProfile(): Flow<Result<Profile?>> = currentProfileStore.currentProfileId
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            if (profileId == null) {
                flowOf(Result.Success(null))
            } else {
                profileDataSource.observeProfile(profileId).asResultFlow()
            }
        }
        .flowOn(ioDispatcher)
}
