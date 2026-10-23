package com.asadraza.streamflix.feature.auth.emailverification

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.CheckEmailVerificationUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LogoutUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ResendVerificationEmailUseCase
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val verification = MutableSharedFlow<Result<EmailVerificationStatus>>(extraBufferCapacity = 8)
    private val checkEmailVerification = mockk<CheckEmailVerificationUseCase>()
    private val resendVerificationEmail = mockk<ResendVerificationEmailUseCase>()
    private val logout = mockk<LogoutUseCase>(relaxed = true)

    private fun createViewModel(): EmailVerificationViewModel {
        every { checkEmailVerification() } returns verification
        return EmailVerificationViewModel(checkEmailVerification, resendVerificationEmail, logout)
    }

    @Test
    fun `verified status navigates to home`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            verification.emit(Result.Success(EmailVerificationStatus("a@b.com", isVerified = true)))
            assertEquals(EmailVerificationEffect.NavigateToHome, awaitItem())
        }
        assertTrue(viewModel.state.value.isVerified)
    }

    @Test
    fun `a verified status that is reported again does not navigate twice`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            verification.emit(Result.Success(EmailVerificationStatus("a@b.com", isVerified = true)))
            verification.emit(Result.Success(EmailVerificationStatus("a@b.com", isVerified = true)))

            assertEquals(EmailVerificationEffect.NavigateToHome, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `resending keeps the verification listener alive`() = runTest {
        coEvery { resendVerificationEmail() } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(EmailVerificationEvent.OnResendVerification)
            assertTrue(awaitItem() is EmailVerificationEffect.ShowToast)

            verification.emit(Result.Success(EmailVerificationStatus("a@b.com", isVerified = true)))
            assertEquals(EmailVerificationEffect.NavigateToHome, awaitItem())
        }
    }

    @Test
    fun `resend failure is reported with a mapped message`() = runTest {
        val error = Result.Error<Unit>(Exception("raw message"), ErrorType.Unknown(null))
        coEvery { resendVerificationEmail() } returns flowOf(error)
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(EmailVerificationEvent.OnResendVerification)
            assertTrue(awaitItem() is EmailVerificationEffect.ShowToast)
        }
    }

    @Test
    fun `logout signs out and returns to login`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(EmailVerificationEvent.OnLogoutClick)
            assertEquals(EmailVerificationEffect.NavigateToLogin, awaitItem())
        }
        coVerify { logout() }
    }
}
