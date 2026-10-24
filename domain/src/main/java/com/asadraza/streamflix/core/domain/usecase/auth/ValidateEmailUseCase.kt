package com.asadraza.streamflix.core.domain.usecase.auth

import javax.inject.Inject

/**
 * Use case for email validation.
 * Returns a ValidationResult: Valid, or Invalid with the first rule that failed.
 */
class ValidateEmailUseCase
@Inject
constructor() {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    operator fun invoke(email: String): ValidationResult = when {
        email.isBlank() -> ValidationResult.Invalid(ValidationError.EMPTY_EMAIL)
        !emailRegex.matches(email) -> ValidationResult.Invalid(ValidationError.INVALID_EMAIL)
        else -> ValidationResult.Valid
    }
}
