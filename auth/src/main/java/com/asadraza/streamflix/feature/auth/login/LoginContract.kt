package com.asadraza.streamflix.feature.auth.login

import com.asadraza.streamflix.core.common.result.ErrorType

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val emailError: String? = null,
    val passwordError: String? = null
)

sealed class LoginEvent {
    data class OnEmailChange(val email: String) : LoginEvent()
    data class OnPasswordChange(val password: String) : LoginEvent()
    data object OnPasswordVisibilityToggle : LoginEvent()
    data object OnLoginClick : LoginEvent()
    data object OnSignUpClick : LoginEvent()
    data object OnForgotPasswordClick : LoginEvent()
    data object OnErrorDismiss : LoginEvent()
}

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()
    data object NavigateToSignUp : LoginEffect()
    data object NavigateToForgotPassword : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}