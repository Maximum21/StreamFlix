package com.asadraza.streamflix.core.network.datasource

import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieDto
import com.asadraza.streamflix.core.network.model.MovieListResponse

/**
 * Interface for remote data source
 *
 * - Repository doesn't know about concrete RemoteDataSource
 * - Easy to swap implementations (different API, mock, etc.)
 * - Better testability
 */
interface IRemoteDataSource {

    suspend fun getTrendingMovies(page: Int): List<MovieDto>

    suspend fun getPopularMovies(page: Int): List<MovieDto>

    suspend fun getTopRatedMovies(page: Int): List<MovieDto>

    suspend fun getUpcomingMovies(page: Int): List<MovieDto>

    suspend fun getNowPlayingMovies(page: Int): List<MovieDto>

    suspend fun getMovieDetails(movieId: Int): MovieDetailDto

    suspend fun searchMovies(query: String, page: Int): List<MovieDto>

    suspend fun getTrendingMoviesPaginated(page: Int): MovieListResponse

    suspend fun getPopularMoviesPaginated(page: Int): MovieListResponse

    suspend fun getTopRatedMoviesPaginated(page: Int): MovieListResponse

    suspend fun getUpcomingMoviesPaginated(page: Int): MovieListResponse

    suspend fun getNowPlayingMoviesPaginated(page: Int): MovieListResponse

    suspend fun searchMoviesPaginated(query: String, page: Int): MovieListResponse

    suspend fun discoverMovies(
        page: Int,
        genres: String? = null,
        year: Int? = null,
        sortBy: String? = null
    ): List<MovieDto>
}