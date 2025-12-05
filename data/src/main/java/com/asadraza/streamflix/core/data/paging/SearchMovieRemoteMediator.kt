package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import java.io.IOException

/**
 * RemoteMediator for Search-based movie pagination
 *
 * SIMPLIFIED APPROACH (same as CategoryMovieRemoteMediator):
 * - Uses ONE remote key per search query (not per movie)
 * - Tracks: currentPage, nextKey, prevKey for the query
 * - Much more reliable than per-movie key lookup
 *
 * KEY DIFFERENCES FROM CategoryMovieRemoteMediator:
 * - Always refreshes on new query (no cache for search)
 * - Stores results with "search" category
 */
@OptIn(ExperimentalPagingApi::class)
class SearchMovieRemoteMediator(
    private val query: String,
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : RemoteMediator<Int, MovieEntity>() {

    // Single key ID for this search query's pagination state
    private val remoteKeyId = "search_$query"

    /**
     * For search, always launch refresh to get fresh results
     */
    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>
    ): MediatorResult {
        return try {
            // 1. Determine which page to load using SINGLE remote key
            val currentRemoteKey = localDataSource.getRemoteKeyById(remoteKeyId)

            val page = when (loadType) {
                LoadType.REFRESH -> {
                    // Always start from page 1 on refresh
                    STARTING_PAGE_INDEX
                }
                LoadType.PREPEND -> {
                    // We don't support prepend
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    // Get next page from our single remote key
                    val nextKey = currentRemoteKey?.nextKey
                    if (nextKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    nextKey
                }
            }

            // 2. Fetch from network
            val response = remoteDataSource.searchMoviesPaginated(query, page)
            val movies = response.results
            val endOfPaginationReached = page >= response.totalPages || movies.isEmpty()

            // 3. Clear old data on REFRESH only
            if (loadType == LoadType.REFRESH) {
                localDataSource.clearRemoteKeys(remoteKeyId)
                // Note: We don't delete movies because they might belong to other categories
            }

            // 4. Calculate keys for next/prev
            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1

            // 5. Save SINGLE remote key for this search query
            val remoteKey = RemoteKeyEntity(
                id = remoteKeyId,  // e.g., "search_batman"
                categoryOrQuery = remoteKeyId,
                prevKey = prevKey,
                nextKey = nextKey,
                currentPage = page
            )

            // 6. Convert DTOs to entities - mark as "search" category
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
                    category = "search",
                    cachedAt = System.currentTimeMillis()
                )
            }

            // 7. Insert into database - single key!
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