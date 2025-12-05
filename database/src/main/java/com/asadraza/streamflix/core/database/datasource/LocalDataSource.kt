package com.asadraza.streamflix.core.database.datasource

import android.util.Log
import androidx.paging.PagingSource
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.dao.RemoteKeyDao
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local data source implementation for database operations
 * Provides Abstraction over DAOs (testable)
 * When Repository needs cached data from Data layer
 * It Wraps DAOs, allows mocking in tests
 *
 * NOW WITH PAGING 3 SUPPORT:
 * - PagingSource methods for Room-backed pagination
 * - RemoteKey operations for RemoteMediator
 */
class LocalDataSource @Inject constructor(
    private val movieDao: MovieDao,
    private val movieDetailDao: MovieDetailDao,
    private val remoteKeyDao: RemoteKeyDao
) : ILocalDataSource {

    // Movies
    override fun getMoviesByCategory(category: String): Flow<List<MovieEntity>> {
        return movieDao.getMoviesByCategory(category)
    }

    override fun getAllMovies(): Flow<List<MovieEntity>> {
        return movieDao.getAllMovies()
    }

    override fun searchMovies(query: String): Flow<List<MovieEntity>> {
        return movieDao.searchMovies(query)
    }

    override suspend fun getMovieById(movieId: String): MovieEntity? {
        return movieDao.getMovieById(movieId)
    }

    override suspend fun insertMovies(movies: List<MovieEntity>) {
        movieDao.insertMovies(movies)
    }

    override suspend fun deleteOldMovies(timestamp: Long) {
        movieDao.deleteOldMovies(timestamp)
    }

    override suspend fun deleteMoviesByCategory(category: String) {
        movieDao.deleteMoviesByCategory(category)
    }

    // Paging 3 support
    override fun getMoviesByCategoryPagingSource(category: String): PagingSource<Int, MovieEntity> {
        return movieDao.getMoviesByCategoryPagingSource(category)
    }

    override fun searchMoviesPagingSource(query: String): PagingSource<Int, MovieEntity> {
        return movieDao.searchMoviesPagingSource(query)
    }

    override suspend fun getMovieCountByCategory(category: String): Int {
        return movieDao.getMovieCountByCategory(category)
    }

    // Movie Details
    override fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?> {
        return movieDetailDao.getMovieDetailById(movieId)
    }

    override suspend fun insertMovieDetail(movieDetail: MovieDetailEntity) {
        movieDetailDao.insertMovieDetail(movieDetail)
    }

    override suspend fun deleteOldMovieDetails(timestamp: Long) {
        movieDetailDao.deleteOldMovieDetails(timestamp)
    }

    // Remote Keys (for RemoteMediator pagination state)
    override suspend fun insertRemoteKey(remoteKey: RemoteKeyEntity) {
        remoteKeyDao.insert(remoteKey)
    }

    override suspend fun getRemoteKeyById(id: String): RemoteKeyEntity? {
        Log.e("testingkeys","=id=${id}")
        return remoteKeyDao.getRemoteKeyById(id)
    }

    override suspend fun getLatestRemoteKey(categoryOrQuery: String): RemoteKeyEntity? {
        return remoteKeyDao.getLatestRemoteKey(categoryOrQuery)
    }

    override suspend fun clearRemoteKeys(categoryOrQuery: String) {
        remoteKeyDao.clearRemoteKeys(categoryOrQuery)
    }

    override suspend fun getRemoteKeyCreationTime(categoryOrQuery: String): Long? {
        return remoteKeyDao.getCreationTime(categoryOrQuery)
    }
}