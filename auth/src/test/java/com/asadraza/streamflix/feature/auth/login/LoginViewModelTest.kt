package com.asadraza.streamflix.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val loginUseCase = mockk<LoginUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(loginUseCase, ValidateEmailUseCase(), analytics, handle)

    private fun LoginViewModel.fill(email: String = "user@example.com", password: String = "secret") {
        onEvent(LoginEvent.OnEmailChange(email))
        onEvent(LoginEvent.OnPasswordChange(password))
    }

    @Test
    fun `invalid email is reported without calling the use case`() {
        val viewModel = createViewModel()
        viewModel.fill(email = "not-an-email")

        viewModel.onEvent(LoginEvent.OnLoginClick)

        assertEquals(ValidationError.INVALID_EMAIL, viewModel.state.value.emailError)
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `empty password is reported without calling the use case`() {
        val viewModel = createViewModel()
        viewModel.fill(password = "")

        viewModel.onEvent(LoginEvent.OnLoginClick)

        assertEquals(ValidationError.EMPTY_PASSWORD, viewModel.state.value.passwordError)
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `existing passwords that fail the sign up rules can still sign in`() = runTest {
        coEvery { loginUseCase("user@example.com", "old") } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()
        viewModel.fill(password = "old")

        viewModel.effect.test {
            viewModel.onEvent(LoginEvent.OnLoginClick)
            assertEquals(LoginEffect.NavigateToHome, awaitItem())
        }
        verify { analytics.log(AnalyticsEvent.Login) }
    }

    @Test
    fun `failure is exposed as an error type and stops loading`() {
        val type = ErrorType.Authorization(AuthReason.InvalidCredentials)
        coEvery { loginUseCase(any(), any()) } returns flowOf(Result.Error(Exception(), type))
        val viewModel = createViewModel()
        viewModel.fill()

        viewModel.onEvent(LoginEvent.OnLoginClick)

        assertEquals(type, viewModel.state.value.errorType)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a second click while signing in is ignored`() {
        coEvery { loginUseCase(any(), any()) } returns flow { awaitCancellation() }
        val viewModel = createViewModel()
        viewModel.fill()

        viewModel.onEvent(LoginEvent.OnLoginClick)
        viewModel.onEvent(LoginEvent.OnLoginClick)

        coVerify(exactly = 1) { loginUseCase(any(), any()) }
    }

    @Test
    fun `email is restored from saved state but the password is not stored`() {
        val handle = SavedStateHandle()
        createViewModel(handle).fill()

        val restored = createViewModel(handle)

        assertEquals("user@example.com", restored.state.value.email)
        assertEquals("", restored.state.value.password)
    }
}
