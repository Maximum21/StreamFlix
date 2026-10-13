package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing the currently active profile.
 *
 * Emits updates when:
 * - Current profile selection changes
 * - Profile data is updated in Firestore
 *
 * Returns Flow<Result<Profile?>> - null if no profile selected.
 */
class GetCurrentProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<Result<Profile?>> {
        return profileRepository.getCurrentProfile()
    }
}