package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieListResponse
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class CategoryMovieRemoteMediator(
    private val category: MovieCategory,
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : PagedMovieRemoteMediator(RemoteKeyIds.category(category.id), localDataSource) {
    override suspend fun initialize(): InitializeAction {
        val cacheTimeout = TimeUnit.HOURS.toMillis(Constants.CACHE_DURATION_HOURS.toLong())
        val createdAt = localDataSource.getRemoteKey(RemoteKeyIds.category(category.id))?.createdAt
        val isFresh = createdAt != null && System.currentTimeMillis() - createdAt < cacheTimeout
        return if (isFresh) InitializeAction.SKIP_INITIAL_REFRESH else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun fetchPage(page: Int): MovieListResponse = when (category) {
        MovieCategory.Trending -> remoteDataSource.getTrendingMoviesPaginated(page)
        MovieCategory.Popular -> remoteDataSource.getPopularMoviesPaginated(page)
        MovieCategory.TopRated -> remoteDataSource.getTopRatedMoviesPaginated(page)
        MovieCategory.Upcoming -> remoteDataSource.getUpcomingMoviesPaginated(page)
        MovieCategory.NowPlaying -> remoteDataSource.getNowPlayingMoviesPaginated(page)
    }

    override suspend fun savePage(
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    ) = localDataSource.saveCategoryPage(category.id, movies, startPosition, remoteKey, clearExisting)
}
