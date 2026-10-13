package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for deleting a user profile.
 *
 * Business rules enforced:
 * - Cannot delete last remaining profile
 *
 * Returns Flow for loading/success/error states.
 */
class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(profileId: String): Flow<Result<Unit>> {
        return profileRepository.deleteProfile(profileId)
    }
}