package com.asadraza.streamflix.feature.auth.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.SignUpUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateNameUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidatePasswordUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateNameUseCase: ValidateNameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEffect>()
    val effect: Flow<SignUpEffect> = _effect.receiveAsFlow()

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnNameChange -> updateName(event.name)
            is SignUpEvent.OnEmailChange -> updateEmail(event.email)
            is SignUpEvent.OnPasswordChange -> updatePassword(event.password)
            is SignUpEvent.OnConfirmPasswordChange -> updateConfirmPassword(event.password)
            is SignUpEvent.OnPasswordVisibilityToggle -> togglePasswordVisibility()
            is SignUpEvent.OnConfirmPasswordVisibilityToggle -> toggleConfirmPasswordVisibility()
            is SignUpEvent.OnTermsAcceptedChange -> updateTermsAccepted(event.accepted)
            is SignUpEvent.OnSignUpClick -> signUp()
            is SignUpEvent.OnLoginClick -> navigateToLogin()
            is SignUpEvent.OnErrorDismiss -> dismissError()
        }
    }

    private fun updateName(name: String) {
        _state.update {
            it.copy(
                name = name,
                nameError = null
            )
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

    private fun updateConfirmPassword(password: String) {
        _state.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = null
            )
        }
    }

    private fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun toggleConfirmPasswordVisibility() {
        _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    private fun updateTermsAccepted(accepted: Boolean) {
        _state.update { it.copy(acceptedTerms = accepted) }
    }

    private fun signUp() {
        var isValid = true
        val currentState = _state.value

        val nameResult = validateNameUseCase(_state.value.name)
        if (nameResult is ValidationResult.Invalid) {
            _state.update { it.copy(nameError = nameResult.error) }
            isValid = false
        }

        val emailResult = validateEmailUseCase(_state.value.email)
        if (emailResult is ValidationResult.Invalid) {
            _state.update { it.copy(emailError = emailResult.error) }
            isValid = false
        }

        val passwordResult = validatePasswordUseCase(_state.value.password)
        if (passwordResult is ValidationResult.Invalid) {
            _state.update { it.copy(passwordError = passwordResult.error) }
            isValid = false
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }

        if (!currentState.acceptedTerms) {
            viewModelScope.launch {
                _effect.send(SignUpEffect.ShowToast("Please accept terms and conditions"))
            }
            isValid = false
        }

        if (!isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }

            signUpUseCase.invoke(
                name = currentState.name,
                email = currentState.email,
                password = currentState.password
            ).collect {result->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(SignUpEffect.NavigateToEmailVerification)
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
            _effect.send(SignUpEffect.NavigateToLogin)
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }
}
