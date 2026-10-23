package com.asadraza.streamflix.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginUseCase: LoginUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val analytics: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Only the email survives process death; the password is never persisted
    private val _state = MutableStateFlow(LoginState(email = savedStateHandle[KEY_EMAIL] ?: ""))
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
        savedStateHandle[KEY_EMAIL] = email
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
        if (_state.value.isLoading) return

        val email = _state.value.email
        val password = _state.value.password
        val emailValidation = validateEmailUseCase(email)
        // Sign-in only needs a value; the complexity rules apply when choosing a password
        val passwordError = ValidationError.EMPTY_PASSWORD.takeIf { password.isEmpty() }

        if (emailValidation is ValidationResult.Invalid || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = (emailValidation as? ValidationResult.Invalid)?.error,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }
            loginUseCase(email, password).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        analytics.log(AnalyticsEvent.Login)
                        _effect.send(LoginEffect.NavigateToHome)
                    }
                    is Result.Error -> {
                        _state.update { it.copy(isLoading = false, errorType = result.type) }
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

    private companion object {
        const val KEY_EMAIL = "email"
    }
}
