package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for resending email verification.
 */
class ResendVerificationEmailUseCase
@Inject
constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Flow<Result<Unit>> {
        return authRepository.resendVerificationEmail()
    }
}
