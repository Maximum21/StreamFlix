package com.asadraza.streamflix.feature.auth.forgot_password

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError

// Forgot Password Screen
data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val emailError: ValidationError? = null,
    val emailSent: Boolean = false
)

sealed class ForgotPasswordEvent {
    data class OnEmailChange(val email: String) : ForgotPasswordEvent()
    data object OnSendResetLinkClick : ForgotPasswordEvent()
    data object OnBackToLoginClick : ForgotPasswordEvent()
    data object OnErrorDismiss : ForgotPasswordEvent()
}

sealed class ForgotPasswordEffect {
    data object NavigateToLogin : ForgotPasswordEffect()
    data class ShowToast(val message: String) : ForgotPasswordEffect()
}