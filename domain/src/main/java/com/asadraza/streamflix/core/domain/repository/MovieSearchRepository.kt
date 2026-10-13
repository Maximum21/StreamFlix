package com.asadraza.streamflix.core.domain.repository

import androidx.paging.PagingData
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for movie search operations
 *
 * RESPONSIBILITIES:
 * - Search movies by query string
 * - Paginated search for infinite scrolling
 */
interface MovieSearchRepository {

    /**
     * Search movies by query
     * Returns matching movies based on title, description, etc.
     */
    suspend fun searchMovies(query: String): Flow<Result<List<Movie>>>

    /**
     * Search movies with Paging 3 support
     * Returns PagingData for efficient infinite scrolling
     *
     * @param query Search query string
     * @return Flow of PagingData for LazyColumn/LazyGrid
     */
    fun searchMoviesPaginated(query: String): Flow<PagingData<Movie>>
}