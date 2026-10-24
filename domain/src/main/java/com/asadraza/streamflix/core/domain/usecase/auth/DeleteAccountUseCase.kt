package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Permanently deletes the account and its data. The password is required because deleting an
 * account is a sensitive operation that the identity provider only accepts after re-authentication.
 */
class DeleteAccountUseCase
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(password: String): Flow<Result<Unit>> = authRepository.deleteAccount(password)
        .onEach { if (it is Result.Success) profileRepository.clearCurrentProfile() }
}
