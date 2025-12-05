package com.asadraza.streamflix.core.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * RemoteMediator for Category-based movie pagination
 *
 * SIMPLIFIED APPROACH:
 * - Uses ONE remote key per category (not per movie)
 * - Tracks: currentPage, nextKey, prevKey for the category
 * - Much more reliable than per-movie key lookup
 *
 * HOW IT WORKS:
 * 1. Paging 3 requests data
 * 2. RemoteMediator fetches from network
 * 3. Data is saved to Room database
 * 4. Room's PagingSource emits cached data to UI
 */
@OptIn(ExperimentalPagingApi::class)
class CategoryMovieRemoteMediator(
    private val category: MovieCategory,
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : RemoteMediator<Int, MovieEntity>() {

    // Single key ID for this category's pagination state
    private val remoteKeyId = "category_${category.id}"

    /**
     * Called before any loading to check if we should refresh
     */
    override suspend fun initialize(): InitializeAction {
        val cacheTimeout = TimeUnit.HOURS.toMillis(Constants.CACHE_DURATION_HOURS.toLong())
        val creationTime = localDataSource.getRemoteKeyCreationTime(category.id)

        return if (creationTime != null && System.currentTimeMillis() - creationTime < cacheTimeout) {
            // Cache is fresh, skip initial refresh
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            // Cache is stale or doesn't exist, refresh
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    /**
     * Main loading logic
     */
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>
    ): MediatorResult {
        return try {
            // 1. Determine which page to load using SINGLE remote key per category
            val currentRemoteKey = localDataSource.getRemoteKeyById(remoteKeyId)
            Log.e("testingkeys","==curetn key == ${currentRemoteKey?.nextKey} == ${currentRemoteKey?.prevKey}")
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    // Always start from page 1 on refresh
                    STARTING_PAGE_INDEX
                }
                LoadType.PREPEND -> {
                    // We don't support prepend (loading older pages at the top)
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    // Get next page from our single remote key
                    val nextKey = currentRemoteKey?.nextKey
                    if (nextKey == null) {
                        // No more pages to load
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    nextKey
                }
            }

            // 2. Fetch from network
            val response = when (category) {
                MovieCategory.Trending -> remoteDataSource.getTrendingMoviesPaginated(page)
                MovieCategory.Popular -> remoteDataSource.getPopularMoviesPaginated(page)
                MovieCategory.TopRated -> remoteDataSource.getTopRatedMoviesPaginated(page)
                MovieCategory.Upcoming -> remoteDataSource.getUpcomingMoviesPaginated(page)
                MovieCategory.NowPlaying -> remoteDataSource.getNowPlayingMoviesPaginated(page)
            }

            val movies = response.results
            val endOfPaginationReached = page >= response.totalPages || movies.isEmpty()

            // 3. Clear old data on REFRESH only
            if (loadType == LoadType.REFRESH) {
                localDataSource.clearRemoteKeys(category.id)
                localDataSource.deleteMoviesByCategory(category.id)
            }

            // 4. Calculate keys for next/prev
            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1

            // 5. Save SINGLE remote key for this category (not per movie!)
            val remoteKey = RemoteKeyEntity(
                id = remoteKeyId,  // e.g., "category_trending"
                categoryOrQuery = category.id,
                prevKey = prevKey,
                nextKey = nextKey,
                currentPage = page
            )

            // 6. Convert DTOs to entities
            val movieEntities = movies.map { movieDto ->
                MovieEntity(
                    id = movieDto.id.toString(),
                    title = movieDto.title,
                    description = movieDto.description,
                    backdropPath = movieDto.backdropPath,
                    posterPath = movieDto.posterPath,
                    releaseDate = movieDto.releaseDate ?: "",
                    voteAverage = movieDto.voteAverage,
                    voteCount = movieDto.voteCount,
                    genreIds = movieDto.genreIds.joinToString(","),
                    originalLanguage = movieDto.originalLanguage,
                    adult = movieDto.adult,
                    video = movieDto.video,
                    category = category.id,
                    cachedAt = System.currentTimeMillis()
                )
            }

            // 7. Insert into database - single key, not list!
            localDataSource.insertRemoteKey(remoteKey)
            localDataSource.insertMovies(movieEntities)

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)

        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        const val STARTING_PAGE_INDEX = 1
    }
}