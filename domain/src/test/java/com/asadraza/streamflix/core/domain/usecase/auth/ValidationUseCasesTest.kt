package com.asadraza.streamflix.core.domain.usecase.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationUseCasesTest {
    private val validateEmail = ValidateEmailUseCase()
    private val validatePassword = ValidatePasswordUseCase()
    private val validateName = ValidateNameUseCase()

    private fun invalid(error: ValidationError) = ValidationResult.Invalid(error)

    @Test
    fun `email rules`() {
        assertEquals(invalid(ValidationError.EMPTY_EMAIL), validateEmail("  "))
        assertEquals(invalid(ValidationError.INVALID_EMAIL), validateEmail("user@"))
        assertEquals(invalid(ValidationError.INVALID_EMAIL), validateEmail("user@example"))
        assertEquals(ValidationResult.Valid, validateEmail("first.last+tag@example.co.uk"))
    }

    @Test
    fun `password rules are checked in order`() {
        assertEquals(invalid(ValidationError.EMPTY_PASSWORD), validatePassword(""))
        assertEquals(invalid(ValidationError.PASSWORD_TOO_SHORT), validatePassword("Ab1"))
        assertEquals(invalid(ValidationError.PASSWORD_NO_UPPERCASE), validatePassword("lowercase1"))
        assertEquals(invalid(ValidationError.PASSWORD_NO_LOWERCASE), validatePassword("UPPERCASE1"))
        assertEquals(invalid(ValidationError.PASSWORD_NO_DIGIT), validatePassword("NoDigitsHere"))
        assertEquals(ValidationResult.Valid, validatePassword("Password1"))
    }

    @Test
    fun `name rules`() {
        assertEquals(invalid(ValidationError.EMPTY_NAME), validateName(""))
        assertEquals(invalid(ValidationError.NAME_TOO_SHORT), validateName("A"))
        assertEquals(invalid(ValidationError.NAME_TOO_LONG), validateName("A".repeat(51)))
        assertEquals(ValidationResult.Valid, validateName("Asad"))
        assertEquals(ValidationResult.Valid, validateName("A".repeat(50)))
    }
}
