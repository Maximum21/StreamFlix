package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.movie.MovieDetail
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for movie detail operations
 *
 * RESPONSIBILITIES:
 * - Get detailed information about a specific movie
 */
interface MovieDetailRepository {

    /**
     * Get movie details by ID
     * Returns comprehensive movie information including cast, crew, etc.
     */
    suspend fun getMovieDetails(movieId: String): Flow<Result<MovieDetail>>


    /**
     * Get movie details by ID
     * Returns comprehensive movie information including cast, crew, etc.
     */
//    suspend fun getStreamUrl(movieId: String, quality: String): Flow<Result<String>>


}