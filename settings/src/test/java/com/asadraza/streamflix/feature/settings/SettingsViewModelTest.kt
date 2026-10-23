package com.asadraza.streamflix.feature.settings

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.DeleteAccountUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.GetCurrentUserUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LogoutUseCase
import com.asadraza.streamflix.core.domain.usecase.flags.ObserveNotificationsEnabledUseCase
import com.asadraza.streamflix.core.domain.usecase.flags.SetNotificationsEnabledUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ClearCacheUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetThemeModeUseCase
import com.asadraza.streamflix.core.model.auth.User
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val user = MutableStateFlow<User?>(User("u1", "me@example.com", "Me", null, true))
    private val consent = MutableStateFlow<Boolean?>(null)
    private val getCurrentUser = mockk<GetCurrentUserUseCase>().also { every { it() } returns user }
    private val observeConsent = mockk<ObserveAnalyticsConsentUseCase>().also { every { it() } returns consent }
    private val setConsent = mockk<SetAnalyticsConsentUseCase>(relaxed = true)
    private val notifications = MutableStateFlow(false)
    private val observeNotifications = mockk<ObserveNotificationsEnabledUseCase>().also {
        every { it() } returns notifications
    }
    private val setNotifications = mockk<SetNotificationsEnabledUseCase>()
    private val theme = MutableStateFlow(ThemeMode.System)
    private val observeTheme = mockk<ObserveThemeModeUseCase>().also { every { it() } returns theme }
    private val setTheme = mockk<SetThemeModeUseCase>(relaxed = true)
    private val clearCache = mockk<ClearCacheUseCase>(relaxed = true)
    private val logout = mockk<LogoutUseCase>(relaxed = true)
    private val deleteAccount = mockk<DeleteAccountUseCase>()

    private fun createViewModel() = SettingsViewModel(
        getCurrentUser,
        SettingsPreferences(
            observeConsent,
            setConsent,
            observeNotifications,
            setNotifications,
            observeTheme,
            setTheme
        ),
        clearCache,
        logout,
        deleteAccount
    )

    @Test
    fun `shows the signed in email and treats unanswered consent as off`() {
        val state = createViewModel().state.value

        assertEquals("me@example.com", state.email)
        assertFalse(state.analyticsEnabled)
    }

    @Test
    fun `consent changes are persisted and reflected`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsEvent.OnAnalyticsToggle(true))
        coVerify { setConsent(true) }

        consent.value = true
        assertTrue(viewModel.state.value.analyticsEnabled)
    }

    @Test
    fun `turning notifications on stores the choice`() {
        coEvery { setNotifications(true) } returns Result.Success(Unit)
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsEvent.OnNotificationsToggle(true))
        notifications.value = true

        coVerify { setNotifications(true) }
        assertTrue(viewModel.state.value.notificationsEnabled)
    }

    @Test
    fun `a failed subscription is reported and the switch stays off`() = runTest {
        coEvery { setNotifications(true) } returns Result.Error(Exception(), ErrorType.Http(500))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(SettingsEvent.OnNotificationsToggle(true))
            assertTrue(awaitItem() is SettingsEffect.ShowMessage)
        }
        assertFalse(viewModel.state.value.notificationsEnabled)
    }

    @Test
    fun `the selected theme is stored and reflected`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsEvent.OnThemeSelect(ThemeMode.Light))
        coVerify { setTheme(ThemeMode.Light) }

        theme.value = ThemeMode.Light
        assertEquals(ThemeMode.Light, viewModel.state.value.themeMode)
    }

    @Test
    fun `clearing the cache confirms with a message`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(SettingsEvent.OnClearCacheClick)
            assertTrue(awaitItem() is SettingsEffect.ShowMessage)
        }
        coVerify { clearCache() }
    }

    @Test
    fun `sign out calls the use case`() {
        createViewModel().onEvent(SettingsEvent.OnSignOutClick)

        coVerify { logout() }
    }

    @Test
    fun `delete dialog opens and closes`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsEvent.OnDeleteAccountClick)
        assertTrue(viewModel.state.value.showDeleteDialog)

        viewModel.onEvent(SettingsEvent.OnDeleteDismiss)
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `a wrong password keeps the dialog open with the error`() {
        val type = ErrorType.Authorization(AuthReason.InvalidCredentials)
        coEvery { deleteAccount("wrong") } returns flowOf(Result.Loading(), Result.Error(Exception(), type))
        val viewModel = createViewModel()
        viewModel.onEvent(SettingsEvent.OnDeleteAccountClick)

        viewModel.onEvent(SettingsEvent.OnDeleteConfirm("wrong"))

        val state = viewModel.state.value
        assertTrue(state.showDeleteDialog)
        assertEquals(type, state.deleteError)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `a successful deletion closes the dialog`() {
        coEvery { deleteAccount("pw") } returns flowOf(Result.Loading(), Result.Success(Unit))
        val viewModel = createViewModel()
        viewModel.onEvent(SettingsEvent.OnDeleteAccountClick)

        viewModel.onEvent(SettingsEvent.OnDeleteConfirm("pw"))

        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `the dialog cannot be dismissed or resubmitted while deleting`() {
        coEvery { deleteAccount(any()) } returns flow {
            emit(Result.Loading())
            awaitCancellation()
        }
        val viewModel = createViewModel()
        viewModel.onEvent(SettingsEvent.OnDeleteAccountClick)
        viewModel.onEvent(SettingsEvent.OnDeleteConfirm("pw"))
        assertTrue(viewModel.state.value.isDeleting)

        viewModel.onEvent(SettingsEvent.OnDeleteDismiss)
        viewModel.onEvent(SettingsEvent.OnDeleteConfirm("pw"))

        assertTrue(viewModel.state.value.showDeleteDialog)
        coVerify(exactly = 1) { deleteAccount(any()) }
    }
}
