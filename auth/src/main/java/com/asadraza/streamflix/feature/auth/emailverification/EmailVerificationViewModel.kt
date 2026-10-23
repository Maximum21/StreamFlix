package com.asadraza.streamflix.feature.auth.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.CheckEmailVerificationUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LogoutUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ResendVerificationEmailUseCase
import com.asadraza.streamflix.core.ui.text.UiText
import com.asadraza.streamflix.core.ui.text.toUiText
import com.asadraza.streamflix.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Email Verification Screen.
 */
@HiltViewModel
class EmailVerificationViewModel
@Inject
constructor(
    private val checkEmailVerificationUseCase: CheckEmailVerificationUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    // State: Represents current UI state
    private val _state = MutableStateFlow(EmailVerificationState())
    val state: StateFlow<EmailVerificationState> = _state.asStateFlow()

    // Effect: One-time events like navigation, toasts
    private val _effect = Channel<EmailVerificationEffect>()
    val effect: Flow<EmailVerificationEffect> = _effect.receiveAsFlow()

    // Separate jobs: resending must not cancel the verification listener
    private var verificationJob: Job? = null
    private var navigatedHome = false
    private var resendJob: Job? = null

    init {
        // Start observing email verification status on ViewModel creation
        observeEmailVerification()
    }

    /**
     * Handle UI events.
     */
    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.OnResendVerification -> resendVerification()
            is EmailVerificationEvent.OnCheckVerification -> observeEmailVerification()
            is EmailVerificationEvent.OnLogoutClick -> logout()
            is EmailVerificationEvent.OnErrorDismiss -> dismissError()
        }
    }

    /**
     * Observe email verification status in real-time.
     */
    private fun observeEmailVerification() {
        if (verificationJob?.isActive == true) {
            verificationJob?.cancel()
        }
        verificationJob =
            viewModelScope.launch {
                checkEmailVerificationUseCase()
                    .catch { exception ->
                        // Handle Flow errors (e.g., network issues)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorType = ErrorType.Unknown(throwable = exception)
                            )
                        }
                    }
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val status = result.data
                                if (status.isVerified && navigatedHome) {
                                    // Already navigated; later re-emissions must not navigate again
                                } else if (status.isVerified) {
                                    navigatedHome = true
                                    // Email verified - navigate to home
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isVerified = true,
                                            email = status.email
                                        )
                                    }
                                    _effect.send(EmailVerificationEffect.NavigateToHome)
                                } else {
                                    // Email not verified yet - show waiting screen
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isVerified = false,
                                            email = status.email,
                                            errorType = null
                                        )
                                    }
                                }
                            }

                            is Result.Error -> {
                                // Handle verification check error
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        errorType = result.type
                                    )
                                }
                            }

                            is Result.Loading -> {
                                // Handle loading state (if repository emits it)
                                _state.update { it.copy(isLoading = true) }
                            }
                        }
                    }
            }
    }

    /**
     * Resend verification email.
     *
     * User can manually request a new verification email.
     * Shows feedback via toast messages.
     */
    private fun resendVerification() {
        if (resendJob?.isActive == true) return
        resendJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                resendVerificationEmailUseCase.invoke().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(
                                EmailVerificationEffect.ShowToast(
                                    UiText.Resource(R.string.verification_email_sent)
                                )
                            )
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(EmailVerificationEffect.ShowToast(result.type.toUiText()))
                        }

                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                }
            }
    }

    /**
     * Logout current user.
     *
     * Signs out and navigates back to login screen.
     */
    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effect.send(EmailVerificationEffect.NavigateToLogin)
        }
    }

    /**
     * Dismiss error message.
     *
     * Clears error state when user acknowledges the error.
     */
    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }
}
