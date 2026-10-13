package com.asadraza.streamflix.core.common.extensions

import com.asadraza.streamflix.core.common.result.ErrorMapper
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen

/**
 * Extension to wrap Flow emission in Result
 *
 * Automatically handles loading and error states
 * Every repository method returning Flow
 */

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading()) }
        .catch { emit(Result.Error(exception = Throwable(it.message ?: "Unknown Error"),
            type = ErrorMapper.map(Throwable(it.message ?: "Unknown Error")))) }
}

/**
 * Extension to handle retry logic
 * Consistent retry behavior across app
 */
fun <T> Flow<Result<T>>.retryOnError(
    retries: Int = 3,
    delayMillis: Long = 1000
): Flow<Result<T>> {
    var retryCount = 0

    return this.retryWhen { cause, _ ->
        if (retryCount < retries) {
            retryCount++
            delay(delayMillis)
            true
        } else {
            false
        }
    }
}