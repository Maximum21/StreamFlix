package com.asadraza.streamflix.feature.auth.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.SignUpUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateNameUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidatePasswordUseCase
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
        // Validate inputs
        val nameValidation = validateNameUseCase(_state.value.name)
        val emailValidation = validateEmailUseCase(_state.value.email)
        val passwordValidation = validatePasswordUseCase(_state.value.password)

        var hasError = false
        val currentState = _state.value

        if (nameValidation.isFailure) {
            _state.update { it.copy(nameError = nameValidation.exceptionOrNull()?.message) }
            hasError = true
        }

        if (emailValidation.isFailure) {
            _state.update { it.copy(emailError = emailValidation.exceptionOrNull()?.message) }
            hasError = true
        }

        if (passwordValidation.isFailure) {
            _state.update { it.copy(passwordError = passwordValidation.exceptionOrNull()?.message) }
            hasError = true
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(confirmPasswordError = "Passwords do not match") }
            hasError = true
        }

        if (!currentState.acceptedTerms) {
            viewModelScope.launch {
                _effect.send(SignUpEffect.ShowToast("Please accept terms and conditions"))
            }
            hasError = true
        }

        if (hasError) return

        Log.e("signupscreen","before")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }

            signUpUseCase.invoke(
                name = currentState.name,
                email = currentState.email,
                password = currentState.password
            ).collect {result->
                Log.e("signupscreen","after")
                when (result) {
                    is Result.Success -> {
                        Log.e("signupscreen","success")
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(SignUpEffect.NavigateToEmailVerification)
                    }
                    is Result.Error -> {
                        Log.e("signupscreen","error")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorType = result.type
                            )
                        }
                    }
                    is Result.Loading -> {
                        Log.e("signupscreen","loading")
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
