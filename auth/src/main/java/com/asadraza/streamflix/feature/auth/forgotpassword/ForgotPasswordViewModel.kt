package com.asadraza.streamflix.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationResult
import com.asadraza.streamflix.core.ui.text.UiText
import com.asadraza.streamflix.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel
@Inject
constructor(
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>()
    val effect: Flow<ForgotPasswordEffect> = _effect.receiveAsFlow()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.OnEmailChange -> updateEmail(event.email)
            is ForgotPasswordEvent.OnSendResetLinkClick -> sendResetLink()
            is ForgotPasswordEvent.OnBackToLoginClick -> navigateToLogin()
            is ForgotPasswordEvent.OnErrorDismiss -> dismissError()
        }
    }

    private fun updateEmail(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = null,
                emailSent = false
            )
        }
    }

    private fun sendResetLink() {
        if (_state.value.isLoading) return

        val emailValidation = validateEmailUseCase(_state.value.email)

        if (emailValidation is ValidationResult.Invalid) {
            _state.update { it.copy(emailError = emailValidation.error) }
            return // or set isValid = false, matching your existing flow
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }

            sendPasswordResetEmailUseCase.invoke(_state.value.email).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                emailSent = true
                            )
                        }
                        _effect.send(ForgotPasswordEffect.ShowToast(UiText.Resource(R.string.password_reset_sent)))
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorType = result.type
                            )
                        }
                    }
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        viewModelScope.launch {
            _effect.send(ForgotPasswordEffect.NavigateToLogin)
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }
}
