package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDataErrorType
import com.asadraza.streamflix.core.data.source.AuthDataSource
import com.asadraza.streamflix.core.data.util.operationFlow
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl
@Inject
constructor(
    private val authDataSource: AuthDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {
    override fun login(email: String, password: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        val user = authDataSource.signIn(email, password)
        if (user.isEmailVerified) {
            Result.Success(Unit)
        } else {
            // An unverified account must not stay signed in
            authDataSource.signOut()
            Result.Error(
                exception = IllegalStateException("Email not verified"),
                type = ErrorType.Authorization(AuthReason.EmailNotVerified)
            )
        }
    }

    override fun signUp(name: String, email: String, password: String): Flow<Result<Unit>> =
        operationFlow(ioDispatcher) {
            authDataSource.register(name, email, password)
            Result.Success(Unit)
        }

    override suspend fun logout() {
        authDataSource.signOut()
    }

    override fun sendPasswordResetEmail(email: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        authDataSource.sendPasswordReset(email)
        Result.Success(Unit)
    }

    override fun observeEmailVerification(): Flow<Result<EmailVerificationStatus>> = authDataSource
        .observeEmailVerification()
        .map<EmailVerificationStatus, Result<EmailVerificationStatus>> { Result.Success(it) }
        .catch { emit(Result.Error(it, it.toDataErrorType())) }
        .flowOn(ioDispatcher)

    override fun resendVerificationEmail(): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        authDataSource.resendVerificationEmail()
        Result.Success(Unit)
    }

    override fun deleteAccount(password: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        authDataSource.deleteAccount(password)
        Result.Success(Unit)
    }

    override fun getCurrentUser(): Flow<User?> = authDataSource.observeCurrentUser()
}
