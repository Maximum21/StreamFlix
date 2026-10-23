package com.asadraza.streamflix.core.data.source

import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import kotlinx.coroutines.flow.Flow

/**
 * Port over the identity provider. Implementations throw provider exceptions; the repository
 * turns them into [com.asadraza.streamflix.core.common.result.ErrorType] values.
 */
interface AuthDataSource {
    suspend fun signIn(email: String, password: String): User

    /** Creates the account, sets the display name, sends the verification email and stores the user document. */
    suspend fun register(name: String, email: String, password: String)

    suspend fun signOut()

    suspend fun sendPasswordReset(email: String)

    suspend fun resendVerificationEmail()

    /** Re-authenticates, erases the user's stored data and deletes the account. */
    suspend fun deleteAccount(password: String)

    fun observeCurrentUser(): Flow<User?>

    /** Completes with [UnauthenticatedException] when nobody is signed in. */
    fun observeEmailVerification(): Flow<EmailVerificationStatus>
}
