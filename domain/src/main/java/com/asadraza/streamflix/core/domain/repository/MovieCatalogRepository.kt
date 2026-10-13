package com.asadraza.streamflix.core.domain.repository

import androidx.paging.PagingData
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for movie catalog operations
 *
 * RESPONSIBILITIES:
 * - Get movies by category (Trending, Popular, etc.)
 * - Refresh category data
 * - Paginated category loading for infinite scroll
 */
interface MovieCatalogRepository {

    /**
     * Get movies by category
     * Returns Flow for reactive, error-handled stream
     */
    suspend fun getMoviesByCategory(category: MovieCategory): Flow<Result<List<Movie>>>

    /**
     * Get movies by category with Paging 3 support
     * Returns PagingData for efficient infinite scrolling
     *
     * @param category Movie category (Trending, Popular, etc.)
     * @return Flow of PagingData for LazyColumn/LazyGrid
     */
    fun getMoviesByCategoryPaginated(category: MovieCategory): Flow<PagingData<Movie>>

    /**
     * Refresh movies (force network fetch)
     * Pull-to-refresh functionality
     */
    suspend fun refreshMovies(category: MovieCategory)
}