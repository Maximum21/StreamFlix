package com.asadraza.streamflix.core.data.util

import com.asadraza.streamflix.core.common.util.Constants
import java.util.concurrent.TimeUnit

/**
 * Helper for cache validation
 *
 * Centralized cache policy ("concerns should be distributed")
 * When deciding whether to fetch fresh data
 */
object CachePolicy {

    /**
     * Check if cache is expired
     *
     * @param cachedAt Timestamp when data was cached (in milliseconds)
     * @param durationHours How long cache is valid (default from Constants)
     * @return true if cache is older than the specified duration
     */
    fun isCacheExpired(
        cachedAt: Long,
        durationHours: Int = Constants.CACHE_DURATION_HOURS
    ): Boolean {
        val now = System.currentTimeMillis()
        val cacheAge = now - cachedAt
        val maxAge = TimeUnit.HOURS.toMillis(durationHours.toLong())
        return cacheAge > maxAge
    }

    /**
     * Check if cache is empty or null
     *
     * @param data The cached data list
     * @return true if data is null or empty
     */
    fun <T> isCacheEmpty(data: List<T>?): Boolean {
        return data.isNullOrEmpty()
    }

    /**
     * Determine if fresh data should be fetched
     *
     * Combines multiple conditions for cache validity
     * Fetch if cache is empty OR expired
     *
     * @param data The cached data list
     * @param cachedAt Timestamp when data was cached (null means no cache)
     * @return true if fresh data should be fetched
     */
    fun <T> shouldFetch(data: List<T>?, cachedAt: Long?): Boolean {
        // No timestamp = no valid cache
        if (cachedAt == null) return true

        // Empty cache = fetch
        if (isCacheEmpty(data)) return true

        // Expired cache = fetch
        return isCacheExpired(cachedAt)
    }

    /**
     * Get cache age in human-readable format
     *
     * Useful for debugging and logging
     */
    fun getCacheAge(cachedAt: Long): String {
        val ageMillis = System.currentTimeMillis() - cachedAt
        val hours = TimeUnit.MILLISECONDS.toHours(ageMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ageMillis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
}