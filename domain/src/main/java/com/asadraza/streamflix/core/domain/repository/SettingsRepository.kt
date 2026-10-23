package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Preferences that belong to the device rather than to the signed-in account. */
interface SettingsRepository {
    /** `null` until the user has answered the consent prompt, so analytics stay off by default. */
    val analyticsConsent: Flow<Boolean?>

    suspend fun setAnalyticsConsent(granted: Boolean)

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
