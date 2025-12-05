package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user logout.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}