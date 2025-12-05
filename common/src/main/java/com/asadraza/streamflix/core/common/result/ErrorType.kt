package com.asadraza.streamflix.core.common.result

import com.asadraza.streamflix.core.common.error.DatabaseError
import com.asadraza.streamflix.core.common.error.NetworkError

/**
 * Categorized error types for type-safe error handling
 *
 * - Different errors need different UI/UX responses
 * - Network errors → show retry button
 * - Validation errors → highlight fields
 * - Authorization errors → logout/login
 * - Database errors → different handling than network
 *
 */
sealed class ErrorType {

    /**
     * Network-related errors
     * Includes: no internet, timeouts, server errors, parsing errors
     *
     * Show retry button, offline banner
     */
    data class Network(val error: NetworkError) : ErrorType() {
        override fun toString(): String = when (error) {
            is NetworkError.NoInternet -> "No internet connection"
            is NetworkError.Timeout -> "Request timed out"
            is NetworkError.ServerError -> "Server error: ${error.message}"
            is NetworkError.RateLimitExceeded -> "Too many requests"
            is NetworkError.ParsingError -> "Failed to parse response"
            is NetworkError.Unknown -> error.message
        }
    }

    /**
     * Database-related errors
     * Includes: read errors, write errors
     *
     * Show cached data warning, log issue
     */
    data class Database(val error: DatabaseError) : ErrorType() {
        override fun toString(): String = when (error) {
            is DatabaseError.ReadError -> "Failed to read from local storage"
            is DatabaseError.WriteError -> "Failed to save to local storage"
            is DatabaseError.Unknown -> error.message
        }
    }

    /**
     * Validation errors (user input)
     *
     * Highlight invalid fields, show inline errors
     */
    data class Validation(val message: String) : ErrorType() {
        override fun toString(): String = message
    }

    /**
     * Authorization/Authentication errors
     * Includes: 401 Unauthorized, 403 Forbidden, expired tokens
     *
     * Navigate to login, show session expired
     */
    data class Authorization(val message: String) : ErrorType() {
        override fun toString(): String = message
    }

    /**
     * HTTP errors (non-2xx status codes)
     *
     * Show specific error based on code
     */
    data class Http(val code: Int, val message: String) : ErrorType() {
        override fun toString(): String = "HTTP $code: $message"

        fun isClientError(): Boolean = code in 400..499
        fun isServerError(): Boolean = code in 500..599
    }

    /**
     * Unknown/unexpected errors
     * Catch-all for any exceptions not categorized above
     *
     * Show generic error, log for debugging
     */
    data class Unknown(val throwable: Throwable?) : ErrorType() {
        override fun toString(): String = throwable?.message ?: "Unknown error occurred"
    }
}


/**
 * Check if error is retryable
 */
fun ErrorType.isRetryable(): Boolean = when (this) {
    is ErrorType.Network -> true  // Network errors are usually retryable
    is ErrorType.Database -> false  // Database errors need investigation
    is ErrorType.Validation -> false  // User needs to fix input
    is ErrorType.Authorization -> false  // Need to re-authenticate
    is ErrorType.Http -> this.code >= 500  // Server errors are retryable
    is ErrorType.Unknown -> false  // Don't retry unknown errors
}

/**
 * Check if error requires user authentication
 */
fun ErrorType.requiresAuth(): Boolean = when (this) {
    is ErrorType.Authorization -> true
    is ErrorType.Http -> this.code == 401 || this.code == 403
    else -> false
}

/**
 * Check if error is network-related
 */
fun ErrorType.isNetworkError(): Boolean = this is ErrorType.Network

/**
 * Get user-friendly error message
 */
fun ErrorType.getUserMessage(): String = when (this) {
    is ErrorType.Network -> {
        when (error) {
            is NetworkError.NoInternet -> "Please check your internet connection"
            is NetworkError.Timeout -> "Request took too long. Please try again"
            is NetworkError.ServerError -> "Server is experiencing issues. Please try again later"
            is NetworkError.RateLimitExceeded -> "Too many requests. Please wait a moment"
            else -> "Network error occurred"
        }
    }
    is ErrorType.Database -> "Local storage error. Your data may not be saved"
    is ErrorType.Validation -> message
    is ErrorType.Authorization -> "Your session has expired. Please log in again"
    is ErrorType.Http -> {
        when {
            code == 404 -> "Content not found"
            code in 400..499 -> "Request failed: $message"
            code in 500..599 -> "Server error. Please try again later"
            else -> message
        }
    }
    is ErrorType.Unknown -> "An unexpected error occurred"
}