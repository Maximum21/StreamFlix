package com.asadraza.streamflix.core.domain.usecase.settings

import com.asadraza.streamflix.core.domain.repository.CacheRepository
import com.asadraza.streamflix.core.domain.repository.SettingsRepository
import com.asadraza.streamflix.core.model.settings.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAnalyticsConsentUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean?> = settingsRepository.analyticsConsent
}

class SetAnalyticsConsentUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(granted: Boolean) = settingsRepository.setAnalyticsConsent(granted)
}

class ClearCacheUseCase @Inject constructor(private val cacheRepository: CacheRepository) {
    suspend operator fun invoke() = cacheRepository.clearCache()
}

class ObserveThemeModeUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settingsRepository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
}
