package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for movie search operations
 *
 * RESPONSIBILITIES:
 * - Search movies by query string
 */
interface MovieSearchRepository {

    /**
     * Search movies by query
     * Returns matching movies based on title, description, etc.
     */
    suspend fun searchMovies(query: String): Flow<Result<List<Movie>>>
}