package com.asadraza.streamflix.core.common.result

/**
 * Enhanced Result class (Level 2)
 */
sealed class Result<out T> {

    /**
     * Success state with data
     *
     * @param data The successful result data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Loading state with optional stale data and progress
     *
     * @param data Optional stale/cached data to show while loading
     * @param progress Optional progress value (0.0 to 1.0)
     */
    data class Loading<T>(
        val data: T? = null,
        val progress: Float? = null
    ) : Result<T>()

    /**
     * Error state with exception, type, and optional stale data
     *
     * @param exception The actual exception that occurred
     * @param type Categorized error type for specific handling
     * @param data Optional stale/cached data to show despite error
     */
    data class Error<T>(
        val exception: Throwable,
        val type: ErrorType,
        val data: T? = null
    ) : Result<T>() {
        // Convenience property for backward compatibility
        val message: String get() = exception.message ?: "Unknown error"
    }
}


/**
 * Safe data extraction
 * Returns data if Success, null otherwise
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Loading -> data
    is Result.Error -> data
}

/**
 * Get data or throw exception
 */
fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Loading -> data ?: throw IllegalStateException("No data available in Loading state")
    is Result.Error -> throw exception
}

/**
 * Transform success data
 * Preserves error and loading states
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Loading -> Result.Loading(
        data = data?.let(transform),
        progress = progress
    )
    is Result.Error -> Result.Error(
        exception = exception,
        type = type,
        data = data?.let(transform)
    )
}

/**
 * Chainable success action
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Chainable error action
 */
inline fun <T> Result<T>.onError(action: (ErrorType, Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(type, exception)
    return this
}

/**
 * Chainable loading action
 */
inline fun <T> Result<T>.onLoading(action: (T?, Float?) -> Unit): Result<T> {
    if (this is Result.Loading) action(data, progress)
    return this
}

/**
 * Check if result is successful
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Check if result is loading
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

/**
 * Check if result is error
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Check if result has any data (success, or stale in loading/error)
 */
fun <T> Result<T>.hasData(): Boolean = when (this) {
    is Result.Success -> true
    is Result.Loading -> data != null
    is Result.Error -> data != null
}