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
    // in minutes
    val runtime: Int?,
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
    // Trailers, teasers
    val videos: List<Video>,
    // Top-billed actors, most prominent first
    val cast: List<CastMember> = emptyList(),
    val director: String? = null,
    val similar: List<RelatedMovie> = emptyList()
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
        get() =
            runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            } ?: "N/A"

    /**
     * The video to play for this title, all from YouTube: an official trailer first, then any
     * trailer, then a teaser, so titles that only have a teaser can still be previewed.
     */
    val officialTrailer: Video?
        get() {
            val youtube = videos.filter { it.site == "YouTube" }
            return youtube.firstOrNull { it.type == "Trailer" && it.official }
                ?: youtube.firstOrNull { it.type == "Trailer" }
                ?: youtube.firstOrNull { it.type == "Teaser" }
        }
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
    // YouTube video ID
    val key: String,
    val name: String,
    // e.g., "YouTube"
    val site: String,
    // e.g., "Trailer", "Teaser"
    val type: String,
    val official: Boolean
) {
    val youtubeUrl: String
        get() = "https://www.youtube.com/watch?v=$key"
}

@Serializable
data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?
)

/** Just enough of a related title to show a poster and open its detail page. */
@Serializable
data class RelatedMovie(
    val id: String,
    val title: String,
    val posterPath: String?
)
