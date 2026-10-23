package com.asadraza.streamflix.core.domain.usecase.auth

import javax.inject.Inject

/**
 * Use case for name validation.
 */
class ValidateNameUseCase
@Inject
constructor() {
    operator fun invoke(name: String): ValidationResult = when {
        name.isBlank() -> ValidationResult.Invalid(ValidationError.EMPTY_NAME)
        name.length < 2 -> ValidationResult.Invalid(ValidationError.NAME_TOO_SHORT)
        name.length > 50 -> ValidationResult.Invalid(ValidationError.NAME_TOO_LONG)
        else -> ValidationResult.Valid
    }
}
