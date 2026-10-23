package com.asadraza.streamflix.core.network.datasource

import com.asadraza.streamflix.core.network.api.TmdbApiService
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieListResponse
import com.asadraza.streamflix.core.network.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source implementation
 *
 * Implements interface for better abstraction
 * Wraps API service, allows mocking in tests
 * This is the ONLY place that knows about TmdbApiService
 */
@Singleton
class RemoteDataSource
@Inject
constructor(
    private val apiService: TmdbApiService
) : IRemoteDataSource {
    override suspend fun getMovieDetails(movieId: Int): MovieDetailDto = safeApiCall {
        apiService.getMovieDetails(
            movieId
        )
    }

    override suspend fun getTrendingMoviesPaginated(page: Int): MovieListResponse = safeApiCall {
        apiService.getTrendingMovies(page)
    }

    override suspend fun getPopularMoviesPaginated(page: Int): MovieListResponse = safeApiCall {
        apiService.getPopularMovies(page)
    }

    override suspend fun getTopRatedMoviesPaginated(page: Int): MovieListResponse = safeApiCall {
        apiService.getTopRatedMovies(page)
    }

    override suspend fun getUpcomingMoviesPaginated(page: Int): MovieListResponse = safeApiCall {
        apiService.getUpcomingMovies(page)
    }

    override suspend fun getNowPlayingMoviesPaginated(page: Int): MovieListResponse = safeApiCall {
        apiService.getNowPlayingMovies(
            page
        )
    }

    override suspend fun searchMoviesPaginated(query: String, page: Int): MovieListResponse =
        safeApiCall { apiService.searchMovies(query, page) }

    // "|" means any of the genres; a comma would require all of them
    override suspend fun discoverMoviesByGenresPaginated(genreIds: Set<Int>, page: Int): MovieListResponse =
        safeApiCall {
            apiService.discoverMovies(
                page = page,
                genres = genreIds.sorted().joinToString("|"),
                sortBy = "popularity.desc"
            )
        }
}
