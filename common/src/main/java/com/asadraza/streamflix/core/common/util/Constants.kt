package com.asadraza.streamflix.core.common.util

/**
 * Application constants
 *
 * Centralized constants used across all modules
 */
object Constants {
    // Network
    const val BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    const val YOUTUBE_BASE_URL = "https://www.youtube.com/watch?v="

    // Image Sizes
    const val POSTER_SIZE_W500 = "w500"
    const val BACKDROP_SIZE_ORIGINAL = "original"
    const val BACKDROP_SIZE_W1280 = "w1280"

    // Pagination
    // TMDB returns fixed pages of 20 results
    const val TMDB_PAGE_SIZE = 20
    const val PAGE_SIZE = TMDB_PAGE_SIZE
    const val INITIAL_LOAD_SIZE = 40

    // Roughly four rows ahead, so the next page is requested before the user reaches the end
    const val PREFETCH_DISTANCE = 12

    // Cache
    const val CACHE_DURATION_HOURS = 24

    // Search
    const val SEARCH_DEBOUNCE_MILLIS = 500L
    const val SEARCH_MIN_LENGTH = 3
}

object ImageUrl {
    fun poster(path: String?, size: String = Constants.POSTER_SIZE_W500): String? =
        path?.let { "${Constants.IMAGE_BASE_URL}$size$it" }

    // "original" can be several megapixels; w1280 is plenty for a full-width 16:9 header
    fun backdrop(path: String?, size: String = Constants.BACKDROP_SIZE_W1280): String? =
        path?.let { "${Constants.IMAGE_BASE_URL}$size$it" }
}
