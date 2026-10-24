package com.asadraza.streamflix.core.database.datasource

import androidx.paging.PagingSource
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import kotlinx.coroutines.flow.Flow

interface ILocalDataSource {
    fun getMoviesByCategoryPagingSource(category: String): PagingSource<Int, MovieEntity>

    fun searchMoviesPagingSource(query: String): PagingSource<Int, MovieEntity>

    suspend fun getRemoteKey(id: String): RemoteKeyEntity?

    /**
     * Writes one page of a category list. The remote key, the movies and (when [clearExisting])
     * the removal of the previous list happen in a single transaction, so readers never observe
     * a half-written page.
     */
    suspend fun saveCategoryPage(
        category: String,
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    )

    /** Same contract as [saveCategoryPage], for the results of one search query. */
    suspend fun saveSearchPage(
        query: String,
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    )

    fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?>

    suspend fun insertMovieDetail(movieDetail: MovieDetailEntity)

    /** Removes every cached list, search result, remote key and detail in one transaction. */
    suspend fun clearCache()
}
