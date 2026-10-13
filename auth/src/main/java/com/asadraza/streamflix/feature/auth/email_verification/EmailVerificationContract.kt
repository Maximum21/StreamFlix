package com.asadraza.streamflix.feature.auth.email_verification

import com.asadraza.streamflix.core.common.result.ErrorType

data class EmailVerificationState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val isVerified: Boolean = false
)

sealed class EmailVerificationEvent {
    data object OnCheckVerification : EmailVerificationEvent()
    data object OnResendVerification : EmailVerificationEvent()
    data object OnLogoutClick : EmailVerificationEvent()
    data object OnErrorDismiss : EmailVerificationEvent()
}

sealed class EmailVerificationEffect {
    data object NavigateToHome : EmailVerificationEffect()
    data object NavigateToLogin : EmailVerificationEffect()
    data class ShowToast(val message: String) : EmailVerificationEffect()
}