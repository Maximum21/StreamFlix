package com.asadraza.streamflix.feature.auth.forgotpassword

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val sendPasswordReset = mockk<SendPasswordResetEmailUseCase>()

    private fun createViewModel() = ForgotPasswordViewModel(sendPasswordReset, ValidateEmailUseCase())

    @Test
    fun `invalid email is reported without sending`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ForgotPasswordEvent.OnEmailChange("nope"))

        viewModel.onEvent(ForgotPasswordEvent.OnSendResetLinkClick)

        assertEquals(ValidationError.INVALID_EMAIL, viewModel.state.value.emailError)
        coVerify(exactly = 0) { sendPasswordReset(any()) }
    }

    @Test
    fun `successful request marks the email as sent and shows a message`() = runTest {
        coEvery { sendPasswordReset("a@b.com") } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()
        viewModel.onEvent(ForgotPasswordEvent.OnEmailChange("a@b.com"))

        viewModel.effect.test {
            viewModel.onEvent(ForgotPasswordEvent.OnSendResetLinkClick)
            assertTrue(awaitItem() is ForgotPasswordEffect.ShowToast)
        }
        assertTrue(viewModel.state.value.emailSent)
    }
}
