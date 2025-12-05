package com.asadraza.streamflix.core.database.datasource

import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for local data source
 */
interface ILocalDataSource {

    // Movie operations
    fun getMoviesByCategory(category: String): Flow<List<MovieEntity>>

    fun getAllMovies(): Flow<List<MovieEntity>>

    fun searchMovies(query: String): Flow<List<MovieEntity>>

    suspend fun getMovieById(movieId: String): MovieEntity?

    suspend fun insertMovies(movies: List<MovieEntity>)

    suspend fun deleteOldMovies(timestamp: Long)

    suspend fun deleteMoviesByCategory(category: String)

    // Movie detail operations
    fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?>

    suspend fun insertMovieDetail(movieDetail: MovieDetailEntity)

    suspend fun deleteOldMovieDetails(timestamp: Long)
}