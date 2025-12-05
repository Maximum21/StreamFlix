package com.asadraza.streamflix.core.model.movie

/**
 * Domain model representing a movie
 * It is independent of data layer (DTOs) and UI layer (UI models)
 * Maps from DTO in data layer
 * maps to UI model in presentation layer
 */

data class Movie(
    val id: String,
    val title: String,
    val description: String,
    val backdropPath: String?,
    val posterPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val adult: Boolean,
    val video: Boolean
) {
    /**
     * Computed property for full backdrop URL
     */
    val fullBackdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }

    val fullPosterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    /**
     * Rating out of 5 stars
     */
    val ratingOutOfFive: Float
        get() = (voteAverage / 2).toFloat()
}