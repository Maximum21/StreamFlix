package com.asadraza.streamflix.feature.settings

import com.asadraza.streamflix.core.domain.usecase.flags.ObserveNotificationsEnabledUseCase
import com.asadraza.streamflix.core.domain.usecase.flags.SetNotificationsEnabledUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetThemeModeUseCase
import javax.inject.Inject

/** The device-level preferences the screen shows, grouped so the ViewModel stays small. */
class SettingsPreferences
@Inject
constructor(
    val observeAnalyticsConsent: ObserveAnalyticsConsentUseCase,
    val setAnalyticsConsent: SetAnalyticsConsentUseCase,
    val observeNotificationsEnabled: ObserveNotificationsEnabledUseCase,
    val setNotificationsEnabled: SetNotificationsEnabledUseCase,
    val observeThemeMode: ObserveThemeModeUseCase,
    val setThemeMode: SetThemeModeUseCase
)
