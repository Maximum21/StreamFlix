package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.data.mapper.toDomain
import com.asadraza.streamflix.core.data.mapper.toEntity
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
 * Repository implementation with enhanced Result
 *
 */

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

    override suspend fun refreshMovies(category: MovieCategory) {
        try {
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

//    override suspend fun getStreamUrl(movieId: String, quality: String): Flow<Result<String>>{
//        return try {
//            // In production, this would call a streaming service API
//            // For demo, return a sample HLS stream URL
//            val streamUrl = when (quality) {
//                "480p" -> "https://example.com/streams/$movieId/480p/playlist.m3u8"
//                "720p" -> "https://example.com/streams/$movieId/720p/playlist.m3u8"
//                "1080p" -> "https://example.com/streams/$movieId/1080p/playlist.m3u8"
//                "2160p" -> "https://example.com/streams/$movieId/2160p/playlist.m3u8"
//                else -> "https://example.com/streams/$movieId/auto/playlist.m3u8"
//            }
//
//
//        } catch (e: Exception) {
//            Result.Error(
//                exception = e,
//                type = ErrorType.Unknown(null)
//            )
//        }
//    }
}