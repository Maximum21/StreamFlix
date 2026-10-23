package com.asadraza.streamflix.core.database.datasource

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.asadraza.streamflix.core.database.StreamFlixDatabase
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.dao.RemoteKeyDao
import com.asadraza.streamflix.core.database.entity.MovieCategoryEntity
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.SearchResultEntity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class LocalDataSource
@Inject
constructor(
    private val database: StreamFlixDatabase,
    private val movieDao: MovieDao,
    private val movieDetailDao: MovieDetailDao,
    private val remoteKeyDao: RemoteKeyDao
) : ILocalDataSource {
    override fun getMoviesByCategoryPagingSource(category: String): PagingSource<Int, MovieEntity> =
        movieDao.categoryPagingSource(category)

    override fun searchMoviesPagingSource(query: String): PagingSource<Int, MovieEntity> =
        movieDao.searchPagingSource(query)

    override suspend fun getRemoteKey(id: String): RemoteKeyEntity? = remoteKeyDao.getRemoteKeyById(id)

    override suspend fun saveCategoryPage(
        category: String,
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    ) {
        database.withTransaction {
            if (clearExisting) {
                remoteKeyDao.delete(remoteKey.id)
                movieDao.deleteCategoryEntries(category)
            }
            movieDao.upsertMovies(movies)
            movieDao.insertCategoryEntries(
                movies.mapIndexed { index, movie ->
                    MovieCategoryEntity(category, movie.id, startPosition + index)
                }
            )
            remoteKeyDao.insert(remoteKey)
            if (clearExisting) movieDao.deleteOrphanMovies()
        }
    }

    override suspend fun saveSearchPage(
        query: String,
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    ) {
        database.withTransaction {
            if (clearExisting) {
                remoteKeyDao.delete(remoteKey.id)
                movieDao.deleteSearchResults(query)
                movieDao.deleteSearchResultsOlderThan(System.currentTimeMillis() - SEARCH_RETENTION_MILLIS)
            }
            movieDao.upsertMovies(movies)
            movieDao.insertSearchResults(
                movies.mapIndexed { index, movie ->
                    SearchResultEntity(query, movie.id, startPosition + index)
                }
            )
            remoteKeyDao.insert(remoteKey)
            if (clearExisting) movieDao.deleteOrphanMovies()
        }
    }

    override fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?> =
        movieDetailDao.getMovieDetailById(movieId)

    override suspend fun insertMovieDetail(movieDetail: MovieDetailEntity) {
        movieDetailDao.insertMovieDetail(movieDetail)
    }

    override suspend fun clearCache() {
        database.withTransaction {
            movieDao.deleteAllMovies()
            movieDetailDao.deleteAll()
            remoteKeyDao.deleteAll()
        }
    }

    private companion object {
        val SEARCH_RETENTION_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
