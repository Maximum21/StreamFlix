package com.asadraza.streamflix.app

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.GetCurrentUserUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetCurrentProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetAnalyticsConsentUseCase
import com.asadraza.streamflix.core.model.auth.User
import com.asadraza.streamflix.core.model.profile.Profile
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import com.asadraza.streamflix.navigation.Screen
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val users = MutableStateFlow<User?>(null)
    private val getCurrentUser: GetCurrentUserUseCase = mockk { every { this@mockk() } returns users }
    private val getCurrentProfile: GetCurrentProfileUseCase = mockk()

    private val verifiedUser = User("uid", "a@b.c", "A", null, isEmailVerified = true)

    private val consent = MutableStateFlow<Boolean?>(true)
    private val observeConsent: ObserveAnalyticsConsentUseCase = mockk { every { this@mockk() } returns consent }
    private val setConsent: SetAnalyticsConsentUseCase = mockk(relaxed = true)
    private val theme = MutableStateFlow(ThemeMode.System)
    private val observeTheme: ObserveThemeModeUseCase = mockk { every { this@mockk() } returns theme }

    private fun viewModel() = AppViewModel(getCurrentUser, getCurrentProfile, observeConsent, observeTheme, setConsent)

    @Test
    fun `the consent prompt shows only while the question is unanswered`() = runTest {
        consent.value = null
        val viewModel = viewModel()
        assertTrue(viewModel.showConsentPrompt.value)

        consent.value = false
        assertFalse(viewModel.showConsentPrompt.value)
    }

    @Test
    fun `the theme follows the stored preference`() = runTest {
        val viewModel = viewModel()
        assertEquals(ThemeMode.System, viewModel.themeMode.value)

        theme.value = ThemeMode.Dark

        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
    }

    @Test
    fun `answering the prompt stores the choice`() = runTest {
        val viewModel = viewModel()

        viewModel.onConsentAnswered(true)

        coVerify { setConsent(true) }
    }

    @Test
    fun `signed out users start on login`() = runTest {
        users.value = null

        assertEquals(Screen.Login, viewModel().startDestination.value)
    }

    @Test
    fun `unverified users start on email verification`() = runTest {
        users.value = verifiedUser.copy(isEmailVerified = false)

        assertEquals(Screen.EmailVerification, viewModel().startDestination.value)
    }

    @Test
    fun `verified users with a selected profile start on home`() = runTest {
        users.value = verifiedUser
        every { getCurrentProfile() } returns flowOf(Result.Loading(), Result.Success(Profile("p1", "Ana", "")))

        assertEquals(Screen.Home, viewModel().startDestination.value)
    }

    @Test
    fun `verified users without a selected profile start on profile selection`() = runTest {
        users.value = verifiedUser
        every { getCurrentProfile() } returns flowOf(Result.Success(null))

        assertEquals(Screen.ProfileSelection, viewModel().startDestination.value)
    }

    @Test
    fun `the start destination is unresolved until the session is known`() = runTest {
        every { getCurrentUser() } returns emptyFlow()

        assertNull(viewModel().startDestination.value)
    }

    @Test
    fun `a profile lookup that never completes does not block startup`() = runTest {
        users.value = verifiedUser
        every { getCurrentProfile() } returns kotlinx.coroutines.flow.flow { kotlinx.coroutines.awaitCancellation() }

        val viewModel = viewModel()
        assertNull(viewModel.startDestination.value)
        advanceTimeBy(3_001)

        assertEquals(Screen.ProfileSelection, viewModel.startDestination.value)
    }

    @Test
    fun `signing out while running redirects to login`() = runTest {
        users.value = verifiedUser
        every { getCurrentProfile() } returns flowOf(Result.Success(Profile("p1", "Ana", "")))
        val viewModel = viewModel()

        viewModel.redirects.test {
            users.value = null

            assertEquals(Screen.Login, awaitItem())
        }
    }

    @Test
    fun `staying signed in does not redirect`() = runTest {
        users.value = verifiedUser
        every { getCurrentProfile() } returns flowOf(Result.Success(Profile("p1", "Ana", "")))
        val viewModel = viewModel()

        viewModel.redirects.test {
            users.value = verifiedUser.copy(displayName = "Renamed")

            expectNoEvents()
        }
    }
}
