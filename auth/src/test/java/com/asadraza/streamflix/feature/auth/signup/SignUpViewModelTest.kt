package com.asadraza.streamflix.feature.auth.signup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.auth.SignUpUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateNameUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidatePasswordUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val signUpUseCase = mockk<SignUpUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) = SignUpViewModel(
        signUpUseCase,
        ValidateEmailUseCase(),
        ValidatePasswordUseCase(),
        ValidateNameUseCase(),
        analytics,
        handle
    )

    private fun SignUpViewModel.fillValid(confirm: String = PASSWORD, acceptTerms: Boolean = true) {
        onEvent(SignUpEvent.OnNameChange("Asad"))
        onEvent(SignUpEvent.OnEmailChange("asad@example.com"))
        onEvent(SignUpEvent.OnPasswordChange(PASSWORD))
        onEvent(SignUpEvent.OnConfirmPasswordChange(confirm))
        onEvent(SignUpEvent.OnTermsAcceptedChange(acceptTerms))
    }

    @Test
    fun `mismatched confirmation is reported`() {
        val viewModel = createViewModel()
        viewModel.fillValid(confirm = "Different1")

        viewModel.onEvent(SignUpEvent.OnSignUpClick)

        assertEquals(ValidationError.PASSWORDS_DO_NOT_MATCH, viewModel.state.value.confirmPasswordError)
        coVerify(exactly = 0) { signUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `terms must be accepted`() {
        val viewModel = createViewModel()
        viewModel.fillValid(acceptTerms = false)

        viewModel.onEvent(SignUpEvent.OnSignUpClick)

        assertEquals(ValidationError.TERMS_NOT_ACCEPTED, viewModel.state.value.termsError)
        coVerify(exactly = 0) { signUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `all field errors are reported together`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SignUpEvent.OnSignUpClick)

        val state = viewModel.state.value
        assertEquals(ValidationError.EMPTY_NAME, state.nameError)
        assertEquals(ValidationError.EMPTY_EMAIL, state.emailError)
        assertEquals(ValidationError.EMPTY_PASSWORD, state.passwordError)
        assertEquals(ValidationError.TERMS_NOT_ACCEPTED, state.termsError)
    }

    @Test
    fun `accepting the terms clears the terms error`() {
        val viewModel = createViewModel()
        viewModel.onEvent(SignUpEvent.OnSignUpClick)

        viewModel.onEvent(SignUpEvent.OnTermsAcceptedChange(true))

        assertNull(viewModel.state.value.termsError)
    }

    @Test
    fun `successful sign up navigates to email verification`() = runTest {
        coEvery { signUpUseCase("Asad", "asad@example.com", PASSWORD) } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()
        viewModel.fillValid()

        viewModel.effect.test {
            viewModel.onEvent(SignUpEvent.OnSignUpClick)
            assertEquals(SignUpEffect.NavigateToEmailVerification, awaitItem())
        }
        verify { analytics.log(AnalyticsEvent.SignUp) }
    }

    @Test
    fun `failure is exposed as an error type`() {
        val type = ErrorType.Authorization(AuthReason.EmailAlreadyInUse)
        coEvery { signUpUseCase(any(), any(), any()) } returns flowOf(Result.Error(Exception(), type))
        val viewModel = createViewModel()
        viewModel.fillValid()

        viewModel.onEvent(SignUpEvent.OnSignUpClick)

        assertEquals(type, viewModel.state.value.errorType)
    }

    @Test
    fun `name and email are restored from saved state but passwords are not`() {
        val handle = SavedStateHandle()
        createViewModel(handle).fillValid()

        val state = createViewModel(handle).state.value

        assertEquals("Asad", state.name)
        assertEquals("asad@example.com", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
    }

    private companion object {
        const val PASSWORD = "Password1"
    }
}
