package com.asadraza.streamflix.feature.auth.login

import androidx.lifecycle.ViewModel
import com.asadraza.streamflix.core.common.result.Result
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnEmailChange -> updateEmail(event.email)
            is LoginEvent.OnPasswordChange -> updatePassword(event.password)
            is LoginEvent.OnPasswordVisibilityToggle -> togglePasswordVisibility()
            is LoginEvent.OnLoginClick -> login()
            is LoginEvent.OnSignUpClick -> navigateToSignUp()
            is LoginEvent.OnForgotPasswordClick -> navigateToForgotPassword()
            is LoginEvent.OnErrorDismiss -> dismissError()
        }
    }

    private fun updateEmail(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
    }

    private fun updatePassword(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null
            )
        }
    }

    private fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun login() {
        // Validate inputs
        val emailValidation = validateEmailUseCase(_state.value.email)
        val passwordValidation = validatePasswordUseCase(_state.value.password)

        if (emailValidation.isFailure || passwordValidation.isFailure) {
            _state.update {
                it.copy(
                    emailError = emailValidation.exceptionOrNull()?.message,
                    passwordError = passwordValidation.exceptionOrNull()?.message
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }
            loginUseCase.invoke(_state.value.email, _state.value.password).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(LoginEffect.NavigateToHome)
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

    private fun navigateToSignUp() {
        viewModelScope.launch {
            _effect.send(LoginEffect.NavigateToSignUp)
        }
    }

    private fun navigateToForgotPassword() {
        viewModelScope.launch {
            _effect.send(LoginEffect.NavigateToForgotPassword)
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }
}
