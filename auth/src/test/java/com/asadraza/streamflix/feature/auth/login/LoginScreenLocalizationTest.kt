package com.asadraza.streamflix.feature.auth.login

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.SavedStateHandle
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ValidateEmailUseCase
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LoginScreenLocalizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var layoutDirection: LayoutDirection? = null

    private fun setScreen(direction: LayoutDirection? = null) {
        val viewModel = LoginViewModel(
            mockk<LoginUseCase>(),
            ValidateEmailUseCase(),
            mockk(relaxed = true),
            SavedStateHandle()
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides (direction ?: LocalLayoutDirection.current)) {
                layoutDirection = LocalLayoutDirection.current
                LoginScreen(
                    onNavigateToHome = {},
                    onNavigateToSignUp = {},
                    onNavigateToForgotPassword = {},
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "es-w400dp-h1200dp")
    fun `the screen is shown in Spanish`() {
        setScreen()

        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        composeRule.onNodeWithText("¿Olvidaste tu contraseña?").assertIsDisplayed()
        composeRule.onNodeWithText("Registrarse").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w400dp-h1200dp")
    fun `a right-to-left layout renders the whole screen`() {
        setScreen(direction = LayoutDirection.Rtl)

        assertEquals(LayoutDirection.Rtl, layoutDirection)
        composeRule.onNodeWithText("Login").assertIsDisplayed()
        composeRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }
}
