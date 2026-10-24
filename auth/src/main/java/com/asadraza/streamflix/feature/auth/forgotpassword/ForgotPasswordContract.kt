package com.asadraza.streamflix.feature.auth.forgotpassword

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.ui.text.UiText

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

    data class ShowToast(val message: UiText) : ForgotPasswordEffect()
}
