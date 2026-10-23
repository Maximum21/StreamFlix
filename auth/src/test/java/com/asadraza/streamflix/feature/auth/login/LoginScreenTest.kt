package com.asadraza.streamflix.feature.auth.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val loginUseCase = mockk<LoginUseCase>()
    private var signUpClicks = 0

    private fun setScreen() {
        val viewModel = LoginViewModel(loginUseCase, ValidateEmailUseCase(), mockk(relaxed = true), SavedStateHandle())
        composeRule.setContent {
            LoginScreen(
                onNavigateToHome = {},
                onNavigateToSignUp = { signUpClicks++ },
                onNavigateToForgotPassword = {},
                viewModel = viewModel
            )
        }
    }

    @Test
    fun `submitting an empty form shows validation errors and does not sign in`() {
        setScreen()

        composeRule.onNodeWithText("Login").performClick()

        composeRule.onNodeWithText("Email cannot be empty").assertIsDisplayed()
        composeRule.onNodeWithText("Password cannot be empty").assertIsDisplayed()
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `a malformed email is rejected`() {
        setScreen()

        composeRule.onNodeWithText("Email").performTextInput("not-an-email")
        composeRule.onNodeWithText("Login").performClick()

        composeRule.onNodeWithText("Invalid email format").assertIsDisplayed()
    }

    @Test
    fun `the sign up link navigates`() {
        setScreen()

        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.waitForIdle()

        assertEquals(1, signUpClicks)
    }
}
