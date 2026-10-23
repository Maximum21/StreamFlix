package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for sending password reset email.
 */
class SendPasswordResetEmailUseCase
@Inject
constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Flow<Result<Unit>> {
        return authRepository.sendPasswordResetEmail(email)
    }
}
