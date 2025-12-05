package com.asadraza.streamflix.core.database.datasource

import androidx.paging.PagingSource
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for local data source
 *
 * Now includes Paging 3 support for offline-first pagination
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

    // Paging 3 support - Room auto-generates PagingSource
    fun getMoviesByCategoryPagingSource(category: String): PagingSource<Int, MovieEntity>

    fun searchMoviesPagingSource(query: String): PagingSource<Int, MovieEntity>

    suspend fun getMovieCountByCategory(category: String): Int

    // Movie detail operations
    fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?>

    suspend fun insertMovieDetail(movieDetail: MovieDetailEntity)

    suspend fun deleteOldMovieDetails(timestamp: Long)

    // Remote keys operations (for RemoteMediator)
    suspend fun insertRemoteKey(remoteKey: RemoteKeyEntity)

    suspend fun getRemoteKeyById(id: String): RemoteKeyEntity?

    suspend fun getLatestRemoteKey(categoryOrQuery: String): RemoteKeyEntity?

    suspend fun clearRemoteKeys(categoryOrQuery: String)

    suspend fun getRemoteKeyCreationTime(categoryOrQuery: String): Long?
}