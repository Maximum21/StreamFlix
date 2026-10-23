package com.asadraza.streamflix.core.domain.usecase.profile

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Validates and saves a profile. A profile with [Profile.NEW_ID] is created; the profile limit
 * is enforced by the data layer, where it can be checked atomically.
 */
class SaveProfileUseCase
@Inject
constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(profile: Profile): Flow<Result<Unit>> {
        val violation = when {
            profile.name.isBlank() -> ValidationReason.NameBlank
            profile.name.length > Profile.MAX_NAME_LENGTH -> ValidationReason.NameTooLong
            else -> null
        }
        return if (violation != null) {
            flowOf(
                Result.Error(
                    exception = IllegalArgumentException("Invalid profile: $violation"),
                    type = ErrorType.Validation(violation)
                )
            )
        } else {
            profileRepository.saveProfile(profile)
        }
    }
}
