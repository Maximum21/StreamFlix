package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.library.WatchedMovie
import kotlinx.coroutines.flow.Flow

/** Trailers the active profile watched, most recent first. */
interface WatchHistoryRepository {
    fun observeRecent(): Flow<Result<List<WatchedMovie>>>

    /** Called repeatedly while a trailer plays, so it reports failure as a value instead of a flow. */
    suspend fun record(item: WatchedMovie): Result<Unit>

    fun remove(movieId: String): Flow<Result<Unit>>
}
