package com.asadraza.streamflix.feature.settings

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.core.ui.text.UiText

data class SettingsState(
    val email: String = "",
    val analyticsEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: ErrorType? = null
)

sealed interface SettingsEvent {
    data class OnAnalyticsToggle(val enabled: Boolean) : SettingsEvent

    data class OnThemeSelect(val mode: ThemeMode) : SettingsEvent

    data class OnNotificationsToggle(val enabled: Boolean) : SettingsEvent

    data object OnClearCacheClick : SettingsEvent

    data object OnSignOutClick : SettingsEvent

    data object OnDeleteAccountClick : SettingsEvent

    data class OnDeleteConfirm(val password: String) : SettingsEvent

    data object OnDeleteDismiss : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(val message: UiText) : SettingsEffect
}
