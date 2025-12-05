package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for movie discovery operations
 *
 * RESPONSIBILITIES:
 * - Discover movies with filters (genre, year, sort)
 */
interface MovieDiscoveryRepository {

    /**
     * Discover movies with filters
     * Allows complex queries with multiple filter parameters
     */
    suspend fun discoverMovies(
        genres: List<Genre>? = null,
        year: Int? = null,
        sortBy: String? = null
    ): Flow<Result<List<Movie>>>
}