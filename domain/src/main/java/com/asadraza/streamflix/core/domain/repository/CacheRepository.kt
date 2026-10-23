package com.asadraza.streamflix.core.domain.repository

interface CacheRepository {
    /** Drops the locally cached catalogue; it is fetched again on demand. */
    suspend fun clearCache()
}
