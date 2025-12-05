package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for user login with email and password.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Flow<Result<Unit>> {
        return authRepository.login(email, password)
    }
}