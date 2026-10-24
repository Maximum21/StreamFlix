package com.asadraza.streamflix.feature.auth.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.auth.SignUpUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateNameUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidatePasswordUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel
@Inject
constructor(
    private val signUpUseCase: SignUpUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateNameUseCase: ValidateNameUseCase,
    private val analytics: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Only non-sensitive fields survive process death; passwords are never persisted
    private val _state = MutableStateFlow(
        SignUpState(
            name = savedStateHandle[KEY_NAME] ?: "",
            email = savedStateHandle[KEY_EMAIL] ?: ""
        )
    )
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
        savedStateHandle[KEY_NAME] = name
        _state.update {
            it.copy(
                name = name,
                nameError = null
            )
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
        _state.update { it.copy(acceptedTerms = accepted, termsError = null) }
    }

    private fun signUp() {
        val current = _state.value
        if (current.isLoading) return

        val nameResult = validateNameUseCase(current.name)
        val emailResult = validateEmailUseCase(current.email)
        val passwordResult = validatePasswordUseCase(current.password)
        val mismatch = ValidationError.PASSWORDS_DO_NOT_MATCH.takeIf { current.password != current.confirmPassword }
        val termsMissing = ValidationError.TERMS_NOT_ACCEPTED.takeUnless { current.acceptedTerms }

        val nameError = (nameResult as? ValidationResult.Invalid)?.error
        val emailError = (emailResult as? ValidationResult.Invalid)?.error
        val passwordError = (passwordResult as? ValidationResult.Invalid)?.error

        if (listOf(nameError, emailError, passwordError, mismatch, termsMissing).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = mismatch,
                    termsError = termsMissing
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }

            signUpUseCase(name = current.name, email = current.email, password = current.password)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _state.update { it.copy(isLoading = false) }
                            analytics.log(AnalyticsEvent.SignUp)
                            _effect.send(SignUpEffect.NavigateToEmailVerification)
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

    private fun navigateToLogin() {
        viewModelScope.launch {
            _effect.send(SignUpEffect.NavigateToLogin)
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
    }
}
