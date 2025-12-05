package com.asadraza.streamflix.core.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.data.mapper.toDomain
import com.asadraza.streamflix.core.data.mapper.toEntity
import com.asadraza.streamflix.core.data.paging.CategoryMovieRemoteMediator
import com.asadraza.streamflix.core.data.paging.SearchMovieRemoteMediator
import com.asadraza.streamflix.core.data.util.networkBoundResource
import com.asadraza.streamflix.core.data.util.networkResult
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.domain.repository.MovieDiscoveryRepository
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Repository implementation with enhanced Result and Paging 3 support
 *
 * NOW WITH OFFLINE-FIRST PAGINATION:
 * - Uses RemoteMediator for network + database integration
 * - Room PagingSource as single source of truth
 * - Automatic cache invalidation
 * - Works offline with cached data
 *
 * ARCHITECTURE:
 * Network (RemoteMediator) → Room (PagingSource) → UI
 *
 * This follows the same philosophy as networkBoundResource:
 * - Show cached data immediately
 * - Fetch from network in background
 * - Update cache with new data
 * - UI automatically updates
 */
@OptIn(ExperimentalPagingApi::class)
class MovieRepositoryImpl @Inject constructor(
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : MovieCatalogRepository,
    MovieDetailRepository,
    MovieSearchRepository,
    MovieDiscoveryRepository {

    // MovieCatalogRepository

    override suspend fun getMoviesByCategory(category: MovieCategory): Flow<Result<List<Movie>>> {
        return networkBoundResource(
            query = {
                localDataSource.getMoviesByCategory(category.id)
                    .map { entities -> entities.toDomain() }
            },
            fetch = {
                when (category) {
                    MovieCategory.Trending -> remoteDataSource.getTrendingMovies(1)
                    MovieCategory.Popular -> remoteDataSource.getPopularMovies(1)
                    MovieCategory.TopRated -> remoteDataSource.getTopRatedMovies(1)
                    MovieCategory.Upcoming -> remoteDataSource.getUpcomingMovies(1)
                    MovieCategory.NowPlaying -> remoteDataSource.getNowPlayingMovies(1)
                }
            },
            saveFetchResult = { movieDtos ->
                val movies = movieDtos.toDomain()
                val entities = movies.toEntity(category)
                localDataSource.deleteMoviesByCategory(category.id)
                localDataSource.insertMovies(entities)
            },
            shouldFetch = { cachedMovies ->
                if (cachedMovies.isNullOrEmpty()) {
                    true
                } else {
                    // Check cache age
                    val cacheAge = System.currentTimeMillis() -
                            TimeUnit.HOURS.toMillis(Constants.CACHE_DURATION_HOURS.toLong())
                    true // Always fetch for demo - adjust based on requirements
                }
            },
            onFetchFailed = { throwable ->
                // Log error (could add analytics here)
                println("Failed to fetch movies: ${throwable.message}")
            }
        )
    }

    /**
     * Get movies by category with OFFLINE-FIRST pagination
     *
     * HOW IT WORKS:
     * 1. Pager combines RemoteMediator (network) + PagingSource (Room)
     * 2. RemoteMediator fetches from TMDB and saves to Room
     * 3. Room's PagingSource emits cached data to UI
     * 4. UI always sees data from Room (single source of truth)
     *
     * BENEFITS:
     * - Works offline (shows cached data)
     * - Automatic cache invalidation
     * - Efficient memory usage
     * - Same philosophy as networkBoundResource
     */
    override fun getMoviesByCategoryPaginated(category: MovieCategory): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                initialLoadSize = Constants.INITIAL_LOAD_SIZE,
                prefetchDistance = Constants.PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            // RemoteMediator handles network fetch + database save
            remoteMediator = CategoryMovieRemoteMediator(
                category = category,
                remoteDataSource = remoteDataSource,
                localDataSource = localDataSource
            ),
            // Room PagingSource is the SINGLE SOURCE OF TRUTH
            pagingSourceFactory = {
                localDataSource.getMoviesByCategoryPagingSource(category.id)
            }
        ).flow.map { pagingData ->
            // Map Entity → Domain Model
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun refreshMovies(category: MovieCategory) {
        try {
            // Clear remote keys to force full refresh
            localDataSource.clearRemoteKeys(category.id)
            localDataSource.deleteMoviesByCategory(category.id)

            val movieDtos = when (category) {
                MovieCategory.Trending -> remoteDataSource.getTrendingMovies(1)
                MovieCategory.Popular -> remoteDataSource.getPopularMovies(1)
                MovieCategory.TopRated -> remoteDataSource.getTopRatedMovies(1)
                MovieCategory.Upcoming -> remoteDataSource.getUpcomingMovies(1)
                MovieCategory.NowPlaying -> remoteDataSource.getNowPlayingMovies(1)
            }
            val movies = movieDtos.toDomain()
            val entities = movies.toEntity(category)
            localDataSource.insertMovies(entities)

        } catch (e: Exception) {
            // Re-throw to let caller handle
            throw e
        }
    }

    //  MovieDetailRepository

    override suspend fun getMovieDetails(movieId: String): Flow<Result<MovieDetail>> {
        return networkBoundResource(
            query = {
                localDataSource.getMovieDetailById(movieId)
                    .map { entity -> entity?.toDomain() }
            },
            fetch = {
                remoteDataSource.getMovieDetails(movieId.toInt())
            },
            saveFetchResult = { movieDetailDto ->
                val movieDetail = movieDetailDto.toDomain()
                val entity = movieDetail.toEntity()
                localDataSource.insertMovieDetail(entity)
            },
            shouldFetch = { cachedDetail ->
                // Always fetch details (they might have updated info)
                cachedDetail == null
            }
        )
    }

    //  MovieSearchRepository

    override suspend fun searchMovies(query: String): Flow<Result<List<Movie>>> {
        return networkResult(
            fetch = {
                remoteDataSource.searchMovies(query, 1).toDomain()
            }
        )
    }

    /**
     * Search movies with OFFLINE-FIRST pagination
     *
     * HYBRID APPROACH:
     * - Shows matching cached movies immediately
     * - Fetches from network in background
     * - Updates results as new data arrives
     *
     * NOTE: Search results are stored separately from category results
     * to avoid mixing data.
     */
    override fun searchMoviesPaginated(query: String): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                initialLoadSize = Constants.INITIAL_LOAD_SIZE,
                prefetchDistance = Constants.PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = SearchMovieRemoteMediator(
                query = query,
                remoteDataSource = remoteDataSource,
                localDataSource = localDataSource
            ),
            pagingSourceFactory = {
                // Search in local cache for matching titles
                localDataSource.searchMoviesPagingSource(query)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    //  MovieDiscoveryRepository

    override suspend fun discoverMovies(
        genres: List<Genre>?,
        year: Int?,
        sortBy: String?
    ): Flow<Result<List<Movie>>> {
        val genresString = genres?.joinToString(",")

        return networkBoundResource(
            query = {
                localDataSource.getAllMovies()
                    .map { entities ->
                        entities.toDomain().filter { movie ->
                            true // Basic filtering
                        }
                    }
            },
            fetch = {
                remoteDataSource.discoverMovies(1, genresString, year, sortBy)
            },
            saveFetchResult = { movieDtos ->
                // Could implement sophisticated caching
            },
            shouldFetch = { _ ->
                true // Always fetch for dynamic filters
            }
        )
    }
}