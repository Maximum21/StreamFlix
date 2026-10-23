package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Signs the user out and forgets the active profile so the next account starts clean.
 */
class LogoutUseCase
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
        profileRepository.clearCurrentProfile()
    }
}
