package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieListResponse
import java.util.concurrent.TimeUnit

/**
 * Pages of movies for a set of genres, stored like a category list so they are cached, ordered and
 * cleaned up by the same code.
 */
@OptIn(ExperimentalPagingApi::class)
class GenreMovieRemoteMediator(
    private val genreIds: Set<Int>,
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : PagedMovieRemoteMediator(RemoteKeyIds.list(listId(genreIds)), localDataSource) {
    override suspend fun initialize(): InitializeAction =
        initializeWithin(TimeUnit.HOURS.toMillis(Constants.CACHE_DURATION_HOURS.toLong()))

    override suspend fun fetchPage(page: Int): MovieListResponse =
        remoteDataSource.discoverMoviesByGenresPaginated(genreIds, page)

    override suspend fun savePage(
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    ) = localDataSource.saveCategoryPage(listId(genreIds), movies, startPosition, remoteKey, clearExisting)

    companion object {
        /** Order-independent, so selecting the same genres in a different order reuses the cache. */
        fun listId(genreIds: Set<Int>): String = "genres_" + genreIds.sorted().joinToString(",")
    }
}
