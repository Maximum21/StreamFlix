package com.asadraza.streamflix.core.network.api

import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * PI service interface
 * Retrofit generates implementation
 */
interface TmdbApiService {
    /**
     * Get trending movies
     * Home screen "Trending Now" section
     */
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Get popular movies
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Get top rated movies
     */
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Get upcoming movies
     */
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Get now playing movies
     */
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Get movie details
     * Detail screen needs more info than list
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String = "videos"
    ): MovieDetailDto

    /**
     * Search movies
     * Search functionality
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): MovieListResponse

    /**
     * Discover movies with filters
     * Advanced filtering by genre, year, etc.
     */
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("page") page: Int = 1,
        @Query("with_genres") genres: String? = null,
        @Query("primary_release_year") year: Int? = null,
        @Query("sort_by") sortBy: String? = null
    ): MovieListResponse
}