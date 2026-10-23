package com.asadraza.streamflix.core.domain.usecase.flags

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.FeatureFlagsRepository
import com.asadraza.streamflix.core.domain.repository.NotificationsRepository
import com.asadraza.streamflix.core.model.settings.FeatureFlags
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveFeatureFlagsUseCase @Inject constructor(private val repository: FeatureFlagsRepository) {
    operator fun invoke(): Flow<FeatureFlags> = repository.flags
}

class RefreshFeatureFlagsUseCase @Inject constructor(private val repository: FeatureFlagsRepository) {
    suspend operator fun invoke() = repository.refresh()
}

class ObserveNotificationsEnabledUseCase @Inject constructor(private val repository: NotificationsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.enabled
}

class SetNotificationsEnabledUseCase @Inject constructor(private val repository: NotificationsRepository) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setEnabled(enabled)
}
