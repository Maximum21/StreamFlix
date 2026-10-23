package com.asadraza.streamflix.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.GetCurrentUserUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetCurrentProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetAnalyticsConsentUseCase
import com.asadraza.streamflix.core.model.auth.User
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the session decision for the shell: where the app starts, and where to go if the user
 * signs out while it is running (the auth guard).
 */
@HiltViewModel
class AppViewModel
@Inject
constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getCurrentProfile: GetCurrentProfileUseCase,
    observeAnalyticsConsent: ObserveAnalyticsConsentUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setAnalyticsConsent: SetAnalyticsConsentUseCase
) : ViewModel() {
    private val _startDestination = MutableStateFlow<Screen?>(null)

    /** Null while the session is being resolved; the splash screen stays up until it is set. */
    val startDestination: StateFlow<Screen?> = _startDestination.asStateFlow()

    private val _redirects = Channel<Screen>(Channel.BUFFERED)

    /** One-off destinations that replace the back stack, e.g. Login after sign-out. */
    val redirects: Flow<Screen> = _redirects.receiveAsFlow()

    /** True until the user has answered the analytics prompt; false while the stored answer loads. */
    val showConsentPrompt: StateFlow<Boolean> = observeAnalyticsConsent()
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The chosen colour scheme; follows the system until the stored value loads. */
    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    init {
        observeSession()
    }

    fun onConsentAnswered(granted: Boolean) {
        viewModelScope.launch { setAnalyticsConsent(granted) }
    }

    private fun observeSession() {
        viewModelScope.launch {
            var resolved = false
            getCurrentUser().collect { user ->
                if (!resolved) {
                    _startDestination.value = resolveStartDestination(user)
                    resolved = true
                } else if (user == null) {
                    _redirects.send(Screen.Login)
                }
            }
        }
    }

    private suspend fun resolveStartDestination(user: User?): Screen = when {
        user == null -> Screen.Login
        !user.isEmailVerified -> Screen.EmailVerification
        hasSelectedProfile() -> Screen.Home
        else -> Screen.ProfileSelection
    }

    // A profile lookup that cannot complete (offline with no cache) must not hold the splash screen
    private suspend fun hasSelectedProfile(): Boolean {
        val result = withTimeoutOrNull(PROFILE_LOOKUP_TIMEOUT_MS) {
            getCurrentProfile().first { it !is Result.Loading }
        }
        return result is Result.Success && result.data != null
    }

    private companion object {
        const val PROFILE_LOOKUP_TIMEOUT_MS = 3_000L
    }
}
