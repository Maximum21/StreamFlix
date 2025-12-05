package com.asadraza.streamflix.feature.auth.email_verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.CheckEmailVerificationUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LogoutUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ResendVerificationEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Email Verification Screen.
 */
@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
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
            is EmailVerificationEvent.OnLogoutClick -> logout()
            is EmailVerificationEvent.OnErrorDismiss -> dismissError()
            else -> {
                //TODO
            }
        }
    }

    /**
     * Observe email verification status in real-time.
     */
    private fun observeEmailVerification() {
        viewModelScope.launch {
            checkEmailVerificationUseCase()
                .onStart {
                    // Show loading when Flow starts
                    _state.update { it.copy(isLoading = true) }
                }
                .catch { exception ->
                    // Handle Flow errors (e.g., network issues)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorType = ErrorType.Unknown(
                                throwable = Throwable(exception.message ?: "Unknown error occurred"),
                            )
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val status = result.data

                            if (status.isVerified) {
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            resendVerificationEmailUseCase.invoke().collect {result->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(
                            EmailVerificationEffect.ShowToast(
                                "Verification email sent. Please check your inbox."
                            )
                        )
                    }

                    is Result.Error -> {
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(
                            EmailVerificationEffect.ShowToast(
                                result.exception.message ?: "Failed to send verification email"
                            )
                        )
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