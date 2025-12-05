package com.asadraza.streamflix.core.database.datasource

import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local data source implementation for database operations
 * Provides Abstraction over DAOs (testable)
 * When Repository needs cached data from Data layer
 * It Wraps DAOs, allows mocking in tests
*/
class LocalDataSource @Inject constructor(
    private val movieDao: MovieDao,
    private val movieDetailDao: MovieDetailDao
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
}