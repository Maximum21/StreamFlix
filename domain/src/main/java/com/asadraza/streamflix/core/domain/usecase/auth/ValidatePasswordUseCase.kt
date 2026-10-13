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
    operator fun invoke(password: String): ValidationResult = when {
        password.isBlank() -> ValidationResult.Invalid(ValidationError.EMPTY_PASSWORD)
        password.length < 8 -> ValidationResult.Invalid(ValidationError.PASSWORD_TOO_SHORT)
        !password.any { it.isUpperCase() } -> ValidationResult.Invalid(ValidationError.PASSWORD_NO_UPPERCASE)
        !password.any { it.isLowerCase() } -> ValidationResult.Invalid(ValidationError.PASSWORD_NO_LOWERCASE)
        !password.any { it.isDigit() } -> ValidationResult.Invalid(ValidationError.PASSWORD_NO_DIGIT)
        else -> ValidationResult.Valid
    }
}