package com.asadraza.streamflix.core.domain.usecase.auth

import javax.inject.Inject

/**
 * Use case for password validation.
 *
 * Rules:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 */
class ValidatePasswordUseCase @Inject constructor() {
    operator fun invoke(password: String): Result<Unit> {
        return when {
            password.isBlank() -> {
                Result.failure(Exception("Password cannot be empty"))
            }
            password.length < 8 -> {
                Result.failure(Exception("Password must be at least 8 characters"))
            }
            !password.any { it.isUpperCase() } -> {
                Result.failure(Exception("Password must contain uppercase letter"))
            }
            !password.any { it.isLowerCase() } -> {
                Result.failure(Exception("Password must contain lowercase letter"))
            }
            !password.any { it.isDigit() } -> {
                Result.failure(Exception("Password must contain a digit"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
}