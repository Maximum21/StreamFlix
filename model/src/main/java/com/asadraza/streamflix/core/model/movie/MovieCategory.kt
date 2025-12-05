package com.asadraza.streamflix.core.model.movie

/**
 * Represents different movie categories/sections
 *
 * Type-safe category representation instead of strings
 * Used in home screen to display different sections
 * Sealed class ensures compile-time safety
 */
sealed class MovieCategory(val id: String, val title: String) {
    object Trending : MovieCategory("trending", "Trending Now")
    object TopRated : MovieCategory("top_rated", "Top Rated")
    object Popular : MovieCategory("popular", "Popular")
    object Upcoming : MovieCategory("upcoming", "Coming Soon")
    object NowPlaying : MovieCategory("now_playing", "Now Playing")

    companion object {
        fun getAll(): List<MovieCategory> = listOf(
            Trending,
            TopRated,
            Popular,
            Upcoming,
            NowPlaying
        )

        fun fromId(id: String): MovieCategory? = getAll().find { it.id == id }
    }
}