package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for setting the currently active profile.
 * Stores selection in local DataStore for quick access.
 *
 * Returns Flow for loading/success/error states.
 */
class SetCurrentProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(profileId: String): Flow<Result<Unit>> {
        return profileRepository.setCurrentProfile(profileId)
    }
}