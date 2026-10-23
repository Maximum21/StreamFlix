package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result

interface CacheRepository {
    /** Drops the locally cached catalogue; it is fetched again on demand. */
    suspend fun clearCache(): Result<Unit>
}
