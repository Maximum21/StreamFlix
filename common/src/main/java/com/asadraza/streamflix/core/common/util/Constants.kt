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

    // Database
    const val DATABASE_NAME = "streamflix_database"
    const val DATABASE_VERSION = 1

    // Pagination
    const val PAGE_SIZE = 20
    const val INITIAL_LOAD_SIZE = 20
    const val PREFETCH_DISTANCE = 5

    // Cache
    const val CACHE_DURATION_HOURS = 24

    // Search
    const val SEARCH_DEBOUNCE_MILLIS = 500L
    const val SEARCH_MIN_LENGTH = 3

    // Rate Limiting
    const val API_RATE_LIMIT_PER_SECOND = 4
}