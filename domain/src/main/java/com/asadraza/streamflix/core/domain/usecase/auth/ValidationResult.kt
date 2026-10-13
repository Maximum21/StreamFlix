package com.asadraza.streamflix.core.domain.usecase.auth

/**
 * Result of synchronous field validation.
 * Carries a message KEY (not a hardcoded string) so the UI can localize it.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val error: ValidationError) : ValidationResult
}

enum class ValidationError {
    EMPTY_EMAIL, INVALID_EMAIL,
    EMPTY_PASSWORD, PASSWORD_TOO_SHORT,
    PASSWORD_NO_UPPERCASE, PASSWORD_NO_LOWERCASE, PASSWORD_NO_DIGIT,
    EMPTY_NAME, NAME_TOO_SHORT, NAME_TOO_LONG
}