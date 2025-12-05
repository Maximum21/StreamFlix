package com.asadraza.streamflix.core.network.datasource

import android.util.Log
import com.asadraza.streamflix.core.network.api.TmdbApiService
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieDto
import com.asadraza.streamflix.core.network.model.MovieListResponse
import javax.inject.Inject

/**
 * Remote data source implementation
 *
 * Implements interface for better abstraction
 * Wraps API service, allows mocking in tests
 * This is the ONLY place that knows about TmdbApiService
 */
class RemoteDataSource @Inject constructor(
    private val apiService: TmdbApiService
) : IRemoteDataSource {

    override suspend fun getTrendingMovies(page: Int): List<MovieDto> {
        Log.e("testingpages","====$page")
        return apiService.getTrendingMovies(page).results
    }

    override suspend fun getPopularMovies(page: Int): List<MovieDto> {
        return apiService.getPopularMovies(page).results
    }

    override suspend fun getTopRatedMovies(page: Int): List<MovieDto> {
        return apiService.getTopRatedMovies(page).results
    }

    override suspend fun getUpcomingMovies(page: Int): List<MovieDto> {
        return apiService.getUpcomingMovies(page).results
    }

    override suspend fun getNowPlayingMovies(page: Int): List<MovieDto> {
        return apiService.getNowPlayingMovies(page).results
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDetailDto {
        return apiService.getMovieDetails(movieId)
    }

    override suspend fun searchMovies(query: String, page: Int): List<MovieDto> {
        return apiService.searchMovies(query, page).results
    }

    override suspend fun getTrendingMoviesPaginated(page: Int): MovieListResponse {
        return apiService.getTrendingMovies(page)
    }

    override suspend fun getPopularMoviesPaginated(page: Int): MovieListResponse {
        return apiService.getPopularMovies(page)
    }

    override suspend fun getTopRatedMoviesPaginated(page: Int): MovieListResponse {
        return apiService.getTopRatedMovies(page)
    }

    override suspend fun getUpcomingMoviesPaginated(page: Int): MovieListResponse {
        return apiService.getUpcomingMovies(page)
    }

    override suspend fun getNowPlayingMoviesPaginated(page: Int): MovieListResponse {
        return apiService.getNowPlayingMovies(page)
    }

    override suspend fun searchMoviesPaginated(
        query: String,
        page: Int
    ): MovieListResponse {
        return apiService.searchMovies(query,page)
    }

    override suspend fun discoverMovies(
        page: Int,
        genres: String?,
        year: Int?,
        sortBy: String?
    ): List<MovieDto> {
        return apiService.discoverMovies(page, genres, year, sortBy).results
    }
}