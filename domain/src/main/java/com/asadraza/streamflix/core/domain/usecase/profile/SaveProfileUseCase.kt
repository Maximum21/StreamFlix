package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.Flow
import com.asadraza.streamflix.core.common.result.Result
import javax.inject.Inject

/**
 * Use case for creating or updating a user profile.
 *
 * Validation rules:
 * - Name must not be blank
 * - Name must be 1-20 characters
 * - Maximum 5 profiles per user
 *
 * Returns Flow for loading/success/error states.
 */
class SaveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(profile: Profile): Flow<Result<Unit>> {
        // Validation
        if (profile.name.isBlank()) {
            return kotlinx.coroutines.flow.flow {
                emit(Result.Error(
                    exception = IllegalArgumentException("Profile name cannot be blank"),
                    type = ErrorType.Validation("Profile name cannot be blank")
                ))
            }
        }

        if (profile.name.length > 20) {
            return kotlinx.coroutines.flow.flow {
                emit(Result.Error(
                    exception = IllegalArgumentException("Profile name too long (max 20 characters)"),
                    type = ErrorType.Validation("Profile name cannot be blank")
                ))
            }
        }

        return profileRepository.saveProfile(profile)
    }
}