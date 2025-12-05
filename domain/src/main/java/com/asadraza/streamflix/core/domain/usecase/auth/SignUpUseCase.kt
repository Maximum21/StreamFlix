package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for user registration.
 * Creates account and sends verification email.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): Flow<Result<Unit>> {
        return authRepository.signUp(name, email, password)
    }
}
