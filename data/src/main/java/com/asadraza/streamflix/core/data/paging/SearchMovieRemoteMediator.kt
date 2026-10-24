package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieListResponse
import java.util.concurrent.TimeUnit

/**
 * Results are stored per query, so a new query never mixes with another one's rows.
 */
@OptIn(ExperimentalPagingApi::class)
class SearchMovieRemoteMediator(
    private val query: String,
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : PagedMovieRemoteMediator(RemoteKeyIds.search(query), localDataSource) {
    // Results are reused for a short while so toggling a filter or retyping a query is instant
    override suspend fun initialize(): InitializeAction = initializeWithin(SEARCH_FRESHNESS_MILLIS)

    override suspend fun fetchPage(page: Int): MovieListResponse = remoteDataSource.searchMoviesPaginated(query, page)

    override suspend fun savePage(
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    ) = localDataSource.saveSearchPage(query, movies, startPosition, remoteKey, clearExisting)
}

private val SEARCH_FRESHNESS_MILLIS = TimeUnit.MINUTES.toMillis(10)
