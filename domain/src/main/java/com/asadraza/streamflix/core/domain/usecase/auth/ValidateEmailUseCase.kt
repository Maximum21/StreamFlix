package com.asadraza.streamflix.core.domain.usecase.auth

import javax.inject.Inject

/**
 * Use case for email validation.
 * Returns Result with validation error or success.
 */
class ValidateEmailUseCase @Inject constructor() {
    operator fun invoke(email: String): Result<Unit> {
        return when {
            email.isBlank() -> {
                Result.failure(Exception("Email cannot be empty"))
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Result.failure(Exception("Invalid email format"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
}
