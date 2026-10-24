package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for deleting a user profile.
 *
 * The rule that the last profile cannot be deleted is enforced where the data lives, in the profile
 * data source (it throws LastProfileException), and the management screen also hides the option.
 *
 * Returns Flow for loading/success/error states.
 */
class DeleteProfileUseCase
@Inject
constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(profileId: String): Flow<Result<Unit>> {
        return profileRepository.deleteProfile(profileId)
    }
}
