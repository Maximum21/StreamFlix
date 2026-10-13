package com.asadraza.streamflix.core.network.datasource

import android.util.Log
import com.asadraza.streamflix.core.network.api.TmdbApiService
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieDto
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
class RemoteDataSource @Inject constructor(
    private val apiService: TmdbApiService
) : IRemoteDataSource {

    override suspend fun getTrendingMovies(page: Int): List<MovieDto>  =
        safeApiCall { apiService.getTrendingMovies(page).results }

    override suspend fun getPopularMovies(page: Int): List<MovieDto> =
        safeApiCall { apiService.getPopularMovies(page).results }

    override suspend fun getTopRatedMovies(page: Int): List<MovieDto> =
        safeApiCall { apiService.getTopRatedMovies(page).results }

    override suspend fun getUpcomingMovies(page: Int): List<MovieDto> =
        safeApiCall { apiService.getUpcomingMovies(page).results }

    override suspend fun getNowPlayingMovies(page: Int): List<MovieDto> =
        safeApiCall { apiService.getNowPlayingMovies(page).results }

    override suspend fun getMovieDetails(movieId: Int): MovieDetailDto =
        safeApiCall { apiService.getMovieDetails(movieId) }

    override suspend fun searchMovies(query: String, page: Int): List<MovieDto> =
        safeApiCall { apiService.searchMovies(query, page).results }

    override suspend fun getTrendingMoviesPaginated(page: Int): MovieListResponse =
        safeApiCall { apiService.getTrendingMovies(page) }

    override suspend fun getPopularMoviesPaginated(page: Int): MovieListResponse =
        safeApiCall { apiService.getPopularMovies(page) }

    override suspend fun getTopRatedMoviesPaginated(page: Int): MovieListResponse =
        safeApiCall { apiService.getTopRatedMovies(page) }

    override suspend fun getUpcomingMoviesPaginated(page: Int): MovieListResponse =
        safeApiCall { apiService.getUpcomingMovies(page) }

    override suspend fun getNowPlayingMoviesPaginated(page: Int): MovieListResponse =
        safeApiCall { apiService.getNowPlayingMovies(page) }

    override suspend fun searchMoviesPaginated(
        query: String,
        page: Int
    ): MovieListResponse = safeApiCall { apiService.searchMovies(query,page) }

    override suspend fun discoverMovies(
        page: Int,
        genres: String?,
        year: Int?,
        sortBy: String?
    ): List<MovieDto> = safeApiCall { apiService.discoverMovies(page, genres, year, sortBy).results }
}