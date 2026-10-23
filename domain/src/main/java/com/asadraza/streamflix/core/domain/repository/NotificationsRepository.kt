package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/** Opt-in to announcements about new releases, delivered as push notifications. */
interface NotificationsRepository {
    val enabled: Flow<Boolean>

    /** Fails (and leaves the setting unchanged) when the subscription cannot be updated, e.g. offline. */
    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
