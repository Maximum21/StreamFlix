package com.asadraza.streamflix.core.model.movie

import kotlinx.serialization.Serializable

/**
 * Detailed movie information (for detail screen)
 *
 * Separate from Movie for Single Responsibility Principle
 * Used in detail screen after user taps a movie
 * Fetched separately from movie list
 */
data class MovieDetail(
    val id: String,
    val title: String,
    val description: String,
    val backdropPath: String?,
    val posterPath: String?,
    val releaseDate: String,
    val runtime: Int?, // in minutes
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<Genre>,
    val productionCompanies: List<ProductionCompany>,
    val tagline: String?,
    val status: String,
    val budget: Long,
    val revenue: Long,
    val originalLanguage: String,
    val spokenLanguages: List<SpokenLanguage>,
    val videos: List<Video> // Trailers, teasers
) {
    val fullBackdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }

    val fullPosterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val ratingOutOfFive: Float
        get() = (voteAverage / 2).toFloat()

    /**
     * Formatted runtime (e.g., "2h 30m")
     */
    val formattedRuntime: String
        get() = runtime?.let {
            val hours = it / 60
            val minutes = it % 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        } ?: "N/A"

    /**
     * Get official trailer
     * prefer YouTube trailers
     */
    val officialTrailer: Video?
        get() = videos
            .filter { it.site == "YouTube" && it.type == "Trailer" }
            .firstOrNull()
}

@Serializable
data class ProductionCompany(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val originCountry: String
)
@Serializable
data class SpokenLanguage(
    val code: String,
    val name: String
)
@Serializable
data class Video(
    val id: String,
    val key: String, // YouTube video ID
    val name: String,
    val site: String, // e.g., "YouTube"
    val type: String, // e.g., "Trailer", "Teaser"
    val official: Boolean
) {
    val youtubeUrl: String
        get() = "https://www.youtube.com/watch?v=$key"
}