package com.asadraza.streamflix.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Recent search queries on this device, newest first. */
interface SearchHistoryRepository {
    val history: Flow<List<String>>

    suspend fun add(query: String)

    suspend fun remove(query: String)

    suspend fun clear()
}
