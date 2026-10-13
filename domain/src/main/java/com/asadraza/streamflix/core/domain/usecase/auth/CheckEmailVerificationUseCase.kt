package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing email verification status.
 */
class CheckEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Observe email verification status.
     *
     * Returns a Flow that continuously emits verification status updates.
     * @return Flow of Result containing EmailVerificationStatus
     */
    operator fun invoke(): Flow<Result<EmailVerificationStatus>> {
        return authRepository.observeEmailVerification()
    }
}