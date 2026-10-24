package com.asadraza.streamflix.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.auth.DeleteAccountUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.GetCurrentUserUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LogoutUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ClearCacheUseCase
import com.asadraza.streamflix.core.ui.text.UiText
import com.asadraza.streamflix.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Signing out or deleting the account ends the session, and the app shell redirects to Login when
 * that happens, so neither action navigates from here.
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val preferences: SettingsPreferences,
    private val clearCache: ClearCacheUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    init {
        getCurrentUser()
            .onEach { user -> _state.update { it.copy(email = user?.email.orEmpty()) } }
            .launchIn(viewModelScope)
        preferences.observeThemeMode()
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
        preferences.observeNotificationsEnabled()
            .onEach { enabled -> _state.update { it.copy(notificationsEnabled = enabled) } }
            .launchIn(viewModelScope)
        preferences.observeAnalyticsConsent()
            .onEach { consent -> _state.update { it.copy(analyticsEnabled = consent == true) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnAnalyticsToggle -> viewModelScope.launch {
                preferences.setAnalyticsConsent(
                    event.enabled
                )
            }
            is SettingsEvent.OnThemeSelect -> viewModelScope.launch { preferences.setThemeMode(event.mode) }
            is SettingsEvent.OnNotificationsToggle -> changeNotifications(event.enabled)
            SettingsEvent.OnNotificationPermissionDenied -> viewModelScope.launch {
                _effect.send(SettingsEffect.ShowMessage(UiText.Resource(R.string.settings_notifications_denied)))
            }
            SettingsEvent.OnClearCacheClick -> clearLocalCache()
            SettingsEvent.OnSignOutClick -> viewModelScope.launch { logout() }
            SettingsEvent.OnDeleteAccountClick ->
                _state.update { it.copy(showDeleteDialog = true, deleteError = null) }
            SettingsEvent.OnDeleteDismiss -> dismissDeleteDialog()
            is SettingsEvent.OnDeleteConfirm -> confirmDeletion(event.password)
        }
    }

    private fun changeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val result = preferences.setNotificationsEnabled(enabled)
            if (result is Result.Error) _effect.send(SettingsEffect.ShowMessage(result.type.toUiText()))
        }
    }

    private fun clearLocalCache() {
        viewModelScope.launch {
            val result = clearCache()
            val message = if (result is Result.Error) {
                result.type.toUiText()
            } else {
                UiText.Resource(
                    R.string.settings_cache_cleared
                )
            }
            _effect.send(SettingsEffect.ShowMessage(message))
        }
    }

    private fun dismissDeleteDialog() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(showDeleteDialog = false, deleteError = null) }
    }

    private fun confirmDeletion(password: String) {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            deleteAccount(password).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isDeleting = true, deleteError = null) }
                    is Result.Success -> _state.update { it.copy(isDeleting = false, showDeleteDialog = false) }
                    is Result.Error -> _state.update { it.copy(isDeleting = false, deleteError = result.type) }
                }
            }
        }
    }
}
