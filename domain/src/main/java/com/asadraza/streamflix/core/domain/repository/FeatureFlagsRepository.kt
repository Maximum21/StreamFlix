package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.model.settings.FeatureFlags
import kotlinx.coroutines.flow.Flow

/** Server-controlled switches. Every flag has a safe default, so the app works with no network. */
interface FeatureFlagsRepository {
    val flags: Flow<FeatureFlags>

    /** Fetches the latest values; a failure leaves the current ones in place. */
    suspend fun refresh()
}
