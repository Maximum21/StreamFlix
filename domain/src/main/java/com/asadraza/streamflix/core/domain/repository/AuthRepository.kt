package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import kotlinx.coroutines.flow.Flow
import com.asadraza.streamflix.core.common.result.Result

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    /**
     * Login with email and password.
     *
     * @param email User's email address
     * @param password User's password
     */
    suspend fun login(email: String, password: String): Flow<Result<Unit>>

    /**
     * Sign up new user and send verification email.
     *
     * @param name User's display name
     * @param email User's email address
     * @param password User's password
     */
    suspend fun signUp(name: String, email: String, password: String): Flow<Result<Unit>>

    /**
     * Logout current user.
     */
    suspend fun logout()

    /**
     * Send password reset email.
     *
     * @param email User's email address
     */
    suspend fun sendPasswordResetEmail(email: String): Flow<Result<Unit>>

    /**
     * Observe email verification status in real-time.
     */
    fun observeEmailVerification(): Flow<Result<EmailVerificationStatus>>

    /**
     * Resend verification email to current user.
     */
    suspend fun resendVerificationEmail(): Flow<Result<Unit>>

    /**
     * Observe current user state in real-time.
     */
    fun getCurrentUser(): Flow<User?>
}