package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.model.auth.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing current user state.
 * Returns Flow that emits user or null.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return authRepository.getCurrentUser()
    }
}
