package com.asadraza.streamflix.feature.auth.signup

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError

data class SignUpState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val nameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: String? = null,
    val acceptedTerms: Boolean = false
)

sealed class SignUpEvent {
    data class OnNameChange(val name: String) : SignUpEvent()
    data class OnEmailChange(val email: String) : SignUpEvent()
    data class OnPasswordChange(val password: String) : SignUpEvent()
    data class OnConfirmPasswordChange(val password: String) : SignUpEvent()
    data object OnPasswordVisibilityToggle : SignUpEvent()
    data object OnConfirmPasswordVisibilityToggle : SignUpEvent()
    data class OnTermsAcceptedChange(val accepted: Boolean) : SignUpEvent()
    data object OnSignUpClick : SignUpEvent()
    data object OnLoginClick : SignUpEvent()
    data object OnErrorDismiss : SignUpEvent()
}

sealed class SignUpEffect {
    data object NavigateToLogin : SignUpEffect()
    data object NavigateToEmailVerification : SignUpEffect()
    data class ShowToast(val message: String) : SignUpEffect()
}

