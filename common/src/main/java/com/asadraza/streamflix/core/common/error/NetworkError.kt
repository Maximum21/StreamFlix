package com.asadraza.streamflix.core.common.error

/**
 * Domain-specific errors
 *
 * Type-safe error representation for Mapping exceptions to user-friendly messages
 * Data layer maps exceptions to these, UI layer displays messages
 */
sealed class NetworkError : Throwable() {
    /**
     * No internet connection
     */
    object NoInternet : NetworkError() {
        override val message: String = "No internet connection"
    }

    /**
     * Server returned 4xx/5xx
     */
    data class ServerError(val code: Int, override val message: String) : NetworkError()

    /**
     * Request timeout
     */
    object Timeout : NetworkError() {
        override val message: String = "Request timed out"
    }

    /**
     * Rate limit exceeded
     */
    object RateLimitExceeded : NetworkError() {
        override val message: String = "Too many requests. Please try again later."
    }

    /**
     * Unknown error
     */
    data class Unknown(override val message: String) : NetworkError()

    /**
     * Parsing error (JSON)
     */
    object ParsingError : NetworkError() {
        override val message: String = "Failed to parse response"
    }
}

/**
 * Database errors
 */
sealed class DatabaseError : Throwable() {
    object ReadError : DatabaseError() {
        override val message: String = "Failed to read from database"
    }

    object WriteError : DatabaseError() {
        override val message: String = "Failed to write to database"
    }

    data class Unknown(override val message: String) : DatabaseError()
}