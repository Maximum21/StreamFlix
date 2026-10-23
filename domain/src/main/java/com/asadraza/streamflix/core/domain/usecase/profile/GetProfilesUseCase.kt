package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing all user profiles with real-time updates.
 *
 * Returns Flow for reactive updates when profiles change.
 * Follows Single Responsibility Principle - only gets profiles.
 */
class GetProfilesUseCase
@Inject
constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<Result<List<Profile>>> {
        return profileRepository.getProfiles()
    }
}
